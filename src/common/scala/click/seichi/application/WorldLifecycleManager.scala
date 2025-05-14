package click.seichi.application

import click.seichi.domain.WorldName

trait WorldLifecycleManager[F[_]] {
  def createWorld(worldName: WorldName): F[Unit]
  def deleteWorld(worldName: WorldName): F[Unit]
}
