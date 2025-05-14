package click.seichi.application

import click.seichi.domain.SavedLocation

trait PutChest[F[_]] {
  /**
   * @return `location` にチェストブロックを設置する作用
   */
  def put(location: SavedLocation): F[Unit]
}

object PutChest {
  def apply[F[_]](implicit ev: PutChest[F]): PutChest[F] = ev
}
