package click.seichi.bukkit

import cats.effect.SyncIO
import click.seichi.application.GetChestContents
import click.seichi.domain.{DeserializedItemStacks, LargeChestPuttedLocation}
import click.seichi.typeclasses.OnMinecraftServerThread
import org.bukkit.block.Chest
import org.bukkit.inventory.ItemStack
import org.bukkit.{Bukkit, Location}

class BukkitGetChestContents[F[_]: OnMinecraftServerThread] extends GetChestContents[F, ItemStack] {
  override def get(location: LargeChestPuttedLocation): F[DeserializedItemStacks[ItemStack]] =
    OnMinecraftServerThread[F].runAction(SyncIO {
      val world = Bukkit.getServer.getWorld(location.worldName.name)
      val chestLocation = new Location(
        world,
        location.rightSideCoordinate.x,
        location.rightSideCoordinate.y,
        location.rightSideCoordinate.z,
      )

      val blockState = chestLocation.getBlock.getState

      assert(blockState.isInstanceOf[Chest])

      val chestInventory = blockState.asInstanceOf[Chest].getInventory

      DeserializedItemStacks(chestInventory.getContents.toList.toVector)
    })
}
