package click.seichi.domain

case class DeserializedItemStacksWithPath[ItemStack](
  itemStacks: DeserializedItemStacks[ItemStack],
  path: Path
)
