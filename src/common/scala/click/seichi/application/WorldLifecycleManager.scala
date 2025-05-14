package click.seichi.application

trait WorldLifecycleManager[F[_]] {
  def createWorld(worldName: String): F[Unit]
}
