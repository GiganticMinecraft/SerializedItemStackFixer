package click.seichi.application

trait WorldGenerator[F[_]] {
  def generateWorld(worldName: String): F[Unit]
}
