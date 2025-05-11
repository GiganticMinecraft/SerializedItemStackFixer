package click.seichi

import click.seichi.application.{DeserializedItemStacksIntoChest, PutChest}

class ItemStackFixerAPI[F[_], ItemStack, Location] {
  def itemStackIntoWorld(
    implicit deserializedItemStacksIntoChest: DeserializedItemStacksIntoChest[F, ItemStack],
    putChest: PutChest[F, Location]
  ): F[Unit] = {
    ???
  }
}
