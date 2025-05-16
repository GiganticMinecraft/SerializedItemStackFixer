package click.seichi.bukkit

import cats.effect.{Sync, SyncIO}
import click.seichi.application.WorldLifecycleManager
import click.seichi.domain.WorldName
import click.seichi.typeclasses.OnMinecraftServerThread
import org.bukkit.World.Environment
import org.bukkit.{Bukkit, WorldCreator, WorldType}

class BukkitWorldLifecycleManager[F[_]: Sync: OnMinecraftServerThread] extends WorldLifecycleManager[F] {
  import cats.implicits._

  override def createWorld(worldName: WorldName): F[Unit] = {
    OnMinecraftServerThread[F].runAction(SyncIO {
      new WorldCreator(worldName.name)
        .environment(Environment.NORMAL)
        .`type`(WorldType.FLAT)
        .generateStructures(false)
        .createWorld()
    }).void
  }

  override def deleteWorld(worldName: WorldName): F[Unit] = OnMinecraftServerThread[F].runAction(SyncIO {
    Bukkit.unloadWorld(worldName.name, false)
  }).void
}
