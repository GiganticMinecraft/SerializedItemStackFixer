package click.seichi

import cats.Parallel
import cats.effect.Sync
import click.seichi.application._
import click.seichi.domain.persistence.PathAndLocationPersistence
import click.seichi.domain.{DeserializedItemStacksWithPath, Segment, WorldName}
import click.seichi.infra.{JdbcFourDimensionalPocketItemStackPersistence, JdbcGachaDataItemStackPersistence, JdbcPathAndLocationPersistence, JdbcSharedInventoryItemStackPersistence}
import click.seichi.typeclasses.SerializeAndDeserialize
import click.seichi.typeclasses.concurrent.NonServerThreadContextShift

class ItemStackFixerAPI[F[_]: Sync: NonServerThreadContextShift: Parallel, ItemStack] {

  import cats.implicits._

  def itemStackIntoWorld(
    implicit deserializedItemStacksIntoChest: DeserializedItemStacksIntoChest[F, ItemStack],
    serializedAndDeserializeForGacha: SerializeAndDeserialize[Nothing, ItemStack],
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
        new JdbcGachaDataItemStackPersistence[F, ItemStack](serializedAndDeserializeForGacha),
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
      _ <- Sync[F].delay(println("Done!"))
    } yield ()
  }

  def loadItemStackFromWorld(
    implicit serializeAndDeserialize: SerializeAndDeserialize[Nothing, Vector[ItemStack]],
    serializedAndDeserializeForGacha: SerializeAndDeserialize[Nothing, ItemStack],
    worldLifecycleManager: WorldLifecycleManager[F],
    getChestContents: GetChestContents[F, ItemStack]
  ): F[Unit] = {
    val pathAndLocationPersistence: PathAndLocationPersistence[F] = new JdbcPathAndLocationPersistence[F]

    for {
      _ <- NonServerThreadContextShift[F].shift
      pathAndLocations <- pathAndLocationPersistence.fetchPathAndLocations()
      persistenceWithPathAndLocations <- Sync[F].pure {
        pathAndLocations
          .groupBy(_.path.segments.head)
          .flatMap { case (segment, pathAndLocations) =>
            segment match {
              case Segment("playerdata") =>
                pathAndLocations
                  .groupBy(_.path.segments.toVector(1))
                  .map { case (segment, pathAndLocations) =>
                    segment match {
                      case Segment("shareinv") =>
                        Some((new JdbcSharedInventoryItemStackPersistence(), pathAndLocations))
                      case Segment("inventory") =>
                        Some((new JdbcFourDimensionalPocketItemStackPersistence(), pathAndLocations))
                      case _ => None
                    }
                  }
                  .toVector
              case Segment("gachadata") =>
                Vector(Some((new JdbcGachaDataItemStackPersistence(serializedAndDeserializeForGacha), pathAndLocations)))
              case _ => Vector(None)
            }
          }
          .toVector
          .flatten
      }
      worldName <- Sync[F].pure(WorldName("formigration"))
      // NOTE: プラグインによって作成されたワールドは明示的に読み込む必要がある
      _ <- worldLifecycleManager.createWorld(worldName)
      _ <- Sync[F].delay(println(persistenceWithPathAndLocations.size))
      _ <- persistenceWithPathAndLocations.parTraverse { case (persistence, pathAndLocations) =>
        for {
          deserializedItemStacksWithPaths <- pathAndLocations.parTraverse { pathAndLocation =>
            getChestContents.get(pathAndLocation.location).map { chestContents =>
              DeserializedItemStacksWithPath(chestContents, pathAndLocation.path)
            }
          }
          _ <- persistence.writeSerializedItemStacks(deserializedItemStacksWithPaths)
        } yield ()
      }
      _ <- worldLifecycleManager.deleteWorld(worldName)
      _ <- Sync[F].delay(println("Done!"))
    } yield {}
  }

}
