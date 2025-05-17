package click.seichi.bukkit.typeclasses

import click.seichi.bukkit.BukkitSerialization
import click.seichi.typeclasses.SerializeAndDeserialize
import org.bukkit.Bukkit
import org.bukkit.inventory.ItemStack

class BukkitItemStackSerializeAndDeserializeForGacha extends SerializeAndDeserialize[Nothing, ItemStack]{
  override def serialize(itemStack: ItemStack): String = {
    val inventory = Bukkit.getServer.createInventory(null, 9)
    inventory.setItem(0, itemStack)
    BukkitSerialization.toBase64(inventory)
  }

  override def deserialize(serialized: String): Either[Nothing, ItemStack] =
    Right(BukkitSerialization.fromBase64(serialized).getItem(0))

}
