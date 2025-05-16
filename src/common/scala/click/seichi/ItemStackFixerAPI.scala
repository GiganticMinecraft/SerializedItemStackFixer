package click.seichi

import cats.Parallel
import cats.effect.Sync
import click.seichi.application.{ComputeLocationFromPaths, DeserializedItemStacksIntoChest, GetChestContents, PutChest, WorldLifecycleManager}
import click.seichi.domain.{DeserializedItemStacksWithPath, Segment, WorldName}
import click.seichi.domain.persistence.PathAndLocationPersistence
import click.seichi.infra.{JdbcFourDimensionalPocketItemStackPersistence, JdbcGachaDataItemStackPersistence, JdbcPathAndLocationPersistence, JdbcSharedInventoryItemStackPersistence}
import click.seichi.typeclasses.SerializeAndDeserialize
import click.seichi.typeclasses.concurrent.NonServerThreadContextShift

class ItemStackFixerAPI[F[_]: Sync: NonServerThreadContextShift: Parallel, ItemStack] {

  import cats.implicits._

  def itemStackIntoWorld(
    implicit deserializedItemStacksIntoChest: DeserializedItemStacksIntoChest[F, ItemStack],
    putChest: PutChest[F],
    serializeAndDeserialize: SerializeAndDeserialize[Nothing, Vector[ItemStack]],
    worldLifecycleManager: WorldLifecycleManager[F]
  ): F[Unit] = {
    val pathAndLocationPersistence: PathAndLocationPersistence[F] = new JdbcPathAndLocationPersistence[F]

    for {
      _ <- NonServerThreadContextShift[F].shift
      _ <- pathAndLocationPersistence.createSerializedItemStackFixerDatabase()
      _ <- pathAndLocationPersistence.createMigrationTargetPathWithLocationTable()
      deserializedItemStacksWithPath <- Vector(
        new JdbcFourDimensionalPocketItemStackPersistence[F, ItemStack],
        new JdbcSharedInventoryItemStackPersistence[F, ItemStack],
        new JdbcGachaDataItemStackPersistence[F, ItemStack],
      ).flatTraverse(_.readSerializedItemStacks)
      worldName <- Sync[F].pure(WorldName("formigration"))
      pathAndLocations <- Sync[F].pure(
        ComputeLocationFromPaths.compute(deserializedItemStacksWithPath.map(_.path), worldName)
      )
      _ <- worldLifecycleManager.createWorld(worldName)
      _ <- pathAndLocations.zip(deserializedItemStacksWithPath)
        .parTraverse { case (pathAndLocation, deserializedItemStacksWithPath) =>
          putChest.put(pathAndLocation.location) >> deserializedItemStacksIntoChest
            .intoChest(pathAndLocation.location, deserializedItemStacksWithPath.itemStacks)
        }
      _ <- pathAndLocationPersistence.insertPathAndLocations(pathAndLocations)
    } yield ()
  }

  def loadItemStackFromWorld(
    implicit serializeAndDeserialize: SerializeAndDeserialize[Nothing, Vector[ItemStack]],
    worldLifecycleManager: WorldLifecycleManager[F],
    getChestContents: GetChestContents[F, ItemStack]
  ): F[Unit] = {
    val pathAndLocationPersistence: PathAndLocationPersistence[F] = new JdbcPathAndLocationPersistence[F]

    for {
      _ <- NonServerThreadContextShift[F].shift
      pathAndLocations <- pathAndLocationPersistence.fetchPathAndLocations()
      persistenceWithPathAndLocations <- Sync[F].pure {
        pathAndLocations.flatMap { pathAndLocation =>
          pathAndLocation.path.segments.toVector match {
            case Vector(Segment("playerdata"), Segment("shareinv"), _) =>
              Some((new JdbcSharedInventoryItemStackPersistence(), pathAndLocations))
            case Vector(Segment("playerdata"), Segment("inventory"), _) =>
              Some((new JdbcFourDimensionalPocketItemStackPersistence(), pathAndLocations))
            case Vector(Segment("gachadata"), _) =>
              Some((new JdbcGachaDataItemStackPersistence, pathAndLocations))
            case _ => None
          }
        }
      }
      _ <- persistenceWithPathAndLocations.traverse { case (persistence, pathAndLocations) =>
        for {
          deserializedItemStacksWithPaths <- pathAndLocations.traverse { pathAndLocation =>
            getChestContents.get(pathAndLocation.location).map { chestContents =>
              DeserializedItemStacksWithPath(chestContents, pathAndLocation.path)
            }
          }
          _ <- persistence.writeSerializedItemStacks(deserializedItemStacksWithPaths)
        } yield ()
      }
      worldName <- Sync[F].pure(WorldName("formigration"))
      _ <- worldLifecycleManager.deleteWorld(worldName)
    } yield {}
  }

}
