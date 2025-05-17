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
      val leftSideChestLocation = new Location(
        world,
        location.leftSideCoordinate.x,
        location.leftSideCoordinate.y,
        location.leftSideCoordinate.z,
      )
      val rightSideChestLocation = new Location(
        world,
        location.rightSideCoordinate.x,
        location.rightSideCoordinate.y,
        location.rightSideCoordinate.z,
      )

      world.loadChunk(location.rightSideCoordinate.x, location.rightSideCoordinate.z)
      world.loadChunk(location.leftSideCoordinate.x, location.leftSideCoordinate.z)

      val leftSideBlockState = leftSideChestLocation.getBlock.getState
      val rightSideBlockState = rightSideChestLocation.getBlock.getState

      assert(leftSideBlockState.isInstanceOf[Chest])
      assert(rightSideBlockState.isInstanceOf[Chest])

      val leftSideChest = leftSideBlockState.asInstanceOf[Chest]
      val rightSideChest = rightSideBlockState.asInstanceOf[Chest]
      leftSideChest.update(true, false)
      rightSideChest.update(true, false)

      val leftSideChestInventoryContents = leftSideChest.getInventory.getContents.toVector
      val rightSideChestInventoryContents = rightSideChest.getInventory.getContents.toVector

      val inventoryContents = leftSideChestInventoryContents ++ rightSideChestInventoryContents

      DeserializedItemStacks(inventoryContents)
    })
}
