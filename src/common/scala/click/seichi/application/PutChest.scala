package click.seichi.application

trait PutChest[F[_], Location] {
  /**
   * @return `location` にチェストブロックを設置する作用
   */
  def put(location: Location): F[Unit]
}

object PutChest {
  def apply[F[_], Location](implicit ev: PutChest[F, Location]): PutChest[F, Location] = ev
}
