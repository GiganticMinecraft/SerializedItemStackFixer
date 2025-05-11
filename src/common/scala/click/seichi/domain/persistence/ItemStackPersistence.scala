package click.seichi.domain.persistence

import click.seichi.domain.DeserializedItemStacksWithPath

trait ItemStackPersistence[F[_], ItemStack] {
  def readSerializedItemStacks: F[Vector[DeserializedItemStacksWithPath[ItemStack]]]
  def writeSerializedItemStacks(deserializedItemStacksWithPaths: Vector[DeserializedItemStacksWithPath[ItemStack]]): F[Unit]
}
