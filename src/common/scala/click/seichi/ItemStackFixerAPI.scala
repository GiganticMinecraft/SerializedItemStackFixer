package click.seichi

import cats.effect.Sync
import click.seichi.application.{ComputeLocationFromPaths, DeserializedItemStacksIntoChest, PutChest, WorldLifecycleManager}
import click.seichi.domain.persistence.ItemStackPersistence
import click.seichi.domain.WorldName
import click.seichi.infra.{JdbcFourDimensionalPocketItemStackPersistence, JdbcGachaDataItemStackPersistence, JdbcSharedInventoryItemStackPersistence}
import click.seichi.typeclasses.SerializeAndDeserialize

class ItemStackFixerAPI[F[_]: Sync, ItemStack] {

  import cats.implicits._

  def itemStackIntoWorld(
    implicit deserializedItemStacksIntoChest: DeserializedItemStacksIntoChest[F, ItemStack],
    putChest: PutChest[F],
    serializeAndDeserialize: SerializeAndDeserialize[Nothing, Vector[ItemStack]],
    worldLifecycleManager: WorldLifecycleManager[F]
  ): F[Unit] = {
    val fourDimensionalPocketItemStackPersistence: ItemStackPersistence[F, ItemStack] =
      new JdbcFourDimensionalPocketItemStackPersistence[F, ItemStack]
    val sharedInventoryItemStackPersistence: ItemStackPersistence[F, ItemStack] =
      new JdbcSharedInventoryItemStackPersistence[F, ItemStack]
    val gachaDataItemStackPersistence: ItemStackPersistence[F, ItemStack] =
      new JdbcGachaDataItemStackPersistence[F, ItemStack]

    val deserializedItemStacksWithPathF = Vector(
      fourDimensionalPocketItemStackPersistence,
      sharedInventoryItemStackPersistence,
      gachaDataItemStackPersistence,
    ).flatTraverse(_.readSerializedItemStacks)

    val worldName = WorldName("formigration")

    for {
      deserializedItemStacksWithPath <- deserializedItemStacksWithPathF
      pathAndLocations <- Sync[F].pure(
        ComputeLocationFromPaths.compute(deserializedItemStacksWithPath.map(_.path), worldName)
      )
      _ <- worldLifecycleManager.createWorld(worldName)
      _ <- pathAndLocations.zip(deserializedItemStacksWithPath)
        .traverse { case (pathAndLocation, deserializedItemStacksWithPath) =>
          putChest.put(pathAndLocation.location) >> deserializedItemStacksIntoChest
            .intoChest(deserializedItemStacksWithPath.itemStacks)
        }
    } yield ()

  }
}
