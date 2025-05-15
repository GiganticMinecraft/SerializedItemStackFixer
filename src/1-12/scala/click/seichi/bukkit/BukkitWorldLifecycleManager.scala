package click.seichi.bukkit

import cats.effect.Sync
import click.seichi.application.WorldLifecycleManager
import click.seichi.domain.WorldName
import org.bukkit.World.Environment
import org.bukkit.{Bukkit, WorldCreator, WorldType}

class BukkitWorldLifecycleManager[F[_]: Sync] extends WorldLifecycleManager[F] {

  import cats.implicits._

  override def createWorld(worldName: WorldName): F[Unit] = Sync[F].delay {
    new WorldCreator(worldName.name)
      .environment(Environment.NORMAL)
      .`type`(WorldType.FLAT)
      .generateStructures(false)
      .createWorld()
  }

  override def deleteWorld(worldName: WorldName): F[Unit] = Sync[F].delay {
    Bukkit.unloadWorld(worldName.name, false)
  }
}
