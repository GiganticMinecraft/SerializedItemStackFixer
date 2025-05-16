package click.seichi.application

import click.seichi.domain.{LargeChestPuttedLocation, DeserializedItemStacks}

trait GetChestContents[F[_], ItemStack] {
  /**
   * @return `location` のチェストの中身を取得する作用
   */
  def get(location: LargeChestPuttedLocation): F[DeserializedItemStacks[ItemStack]]
}
