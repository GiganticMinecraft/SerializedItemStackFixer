package click.seichi.application

import click.seichi.domain.DeserializedItemStacks

trait DeserializedItemStacksIntoChest[F[_], ItemStack] {
  def intoChest(deserializedItemStacks: DeserializedItemStacks[ItemStack]): F[Unit]
}
