package click.seichi.bukkit.typeclasses

import click.seichi.bukkit.ItemListSerialization
import click.seichi.typeclasses.SerializeAndDeserialize
import org.bukkit.inventory.ItemStack

import scala.jdk.CollectionConverters._

class BukkitItemStackSerializeAndDeserialize[F[_]] extends SerializeAndDeserialize[Nothing, Vector[ItemStack]] {
  override def serialize(value: Vector[ItemStack]): String = {
    ItemListSerialization.serializeToBase64(value.toList.asJava)
  }

  override def deserialize(serialized: String): Either[Nothing, Vector[ItemStack]] = {
    Right(ItemListSerialization.deserializeFromBase64(serialized).asScala.toVector)
  }
}
