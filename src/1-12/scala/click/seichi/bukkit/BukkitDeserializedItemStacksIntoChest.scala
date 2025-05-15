package click.seichi.bukkit

import click.seichi.application.DeserializedItemStacksIntoChest
import click.seichi.domain.{DeserializedItemStacks, LargeChestPuttedLocation}
import click.seichi.typeclasses.OnMinecraftServerThread
import org.bukkit.inventory.ItemStack
import org.bukkit.Bukkit
import org.bukkit.Location
import org.bukkit.block.Chest
import org.bukkit.block.DoubleChest;
import cats.effect.SyncIO

class BukkitDeserializedItemStacksIntoChest[F[_]: OnMinecraftServerThread] extends DeserializedItemStacksIntoChest[F, ItemStack] {
  import cats.implicits._

  override def intoChest(
    largeChestPuttedLocation: LargeChestPuttedLocation,
    deserializedItemStacks: DeserializedItemStacks[ItemStack]
  ): F[Unit] = {
    OnMinecraftServerThread[F].runAction(SyncIO {
      val world = Bukkit.getServer.getWorld(largeChestPuttedLocation.worldName.name)
      val chestLocation = new Location(
        world,
        largeChestPuttedLocation.rightSideCoordinate.x,
        largeChestPuttedLocation.rightSideCoordinate.y,
        largeChestPuttedLocation.rightSideCoordinate.z,
      )

      val blockState = chestLocation.getBlock.getState

      assert(blockState.isInstanceOf[Chest])

      val chestInventory = blockState.asInstanceOf[Chest].getBlockInventory.getHolder.asInstanceOf[DoubleChest].getInventory

      deserializedItemStacks.itemStacks.zipWithIndex.map { case (itemStack, index) =>
        chestInventory.setItem(index, itemStack)
      }
    }.void)
  }
}
