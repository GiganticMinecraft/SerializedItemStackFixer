package click.seichi.bukkit

import click.seichi.typeclasses.OnMinecraftServerThread
import click.seichi.application.PutChest
import click.seichi.domain.LargeChestPuttedLocation
import cats.effect.SyncIO
import org.bukkit.{Bukkit, Location, Material}

class BukkitPutChest[F[_]: OnMinecraftServerThread] extends PutChest[F] {

  override def put(location: LargeChestPuttedLocation): F[Unit] = {
    OnMinecraftServerThread[F].runAction(SyncIO {
      val world = Bukkit.getServer.getWorld(location.worldName.name)

      val rightSideLocation = new Location(
        world,
        location.rightSideCoordinate.x,
        location.rightSideCoordinate.y,
        location.rightSideCoordinate.z,
      )
      val leftSideLocation = new Location(
        world,
        location.leftSideCoordinate.x,
        location.leftSideCoordinate.y,
        location.leftSideCoordinate.z,
      )

      rightSideLocation.getBlock.setType(Material.CHEST)
      leftSideLocation.getBlock.setType(Material.CHEST)
    })
  }

}
