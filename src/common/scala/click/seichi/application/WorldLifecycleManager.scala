package click.seichi.application

trait WorldLifecycleManager[F[_]] {
  def createWorld(worldName: String): F[Unit]
  def deleteWorld(worldName: String): F[Unit]
}
