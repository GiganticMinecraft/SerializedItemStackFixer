package click.seichi

import cats.Parallel
import cats.effect.Sync
import click.seichi.application.{ComputeLocationFromPaths, DeserializedItemStacksIntoChest, PutChest, WorldLifecycleManager}
import click.seichi.domain.WorldName
import click.seichi.infra.{JdbcFourDimensionalPocketItemStackPersistence, JdbcGachaDataItemStackPersistence, JdbcSharedInventoryItemStackPersistence}
import click.seichi.typeclasses.SerializeAndDeserialize
import click.seichi.typeclasses.concurrent.NonServerThreadContextShift

class ItemStackFixerAPI[F[_]: Sync: NonServerThreadContextShift: Parallel, ItemStack] {

  import cats.implicits._

  def itemStackIntoWorld(
    implicit deserializedItemStacksIntoChest: DeserializedItemStacksIntoChest[F, ItemStack],
    putChest: PutChest[F],
    serializeAndDeserialize: SerializeAndDeserialize[Nothing, Vector[ItemStack]],
    worldLifecycleManager: WorldLifecycleManager[F]
  ): F[Unit] = for {
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
  } yield ()

}
