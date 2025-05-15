package click.seichi.application

import click.seichi.domain.LargeChestPuttedLocation

trait PutChest[F[_]] {
  /**
   * @return `location` にチェストブロックを設置する作用
   */
  def put(location: LargeChestPuttedLocation): F[Unit]
}

object PutChest {
  def apply[F[_]](implicit ev: PutChest[F]): PutChest[F] = ev
}
