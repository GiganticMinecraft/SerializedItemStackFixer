package click.seichi.application

import click.seichi.domain.{DeserializedItemStacks, LargeChestPuttedLocation}

trait DeserializedItemStacksIntoChest[F[_], ItemStack] {
  def intoChest(
    largeChestPuttedLocation: LargeChestPuttedLocation,
    deserializedItemStacks: DeserializedItemStacks[ItemStack]
 ): F[Unit]
}
