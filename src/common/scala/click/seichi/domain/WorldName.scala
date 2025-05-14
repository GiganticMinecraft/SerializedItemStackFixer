package click.seichi.domain

case class WorldName(name: String) {
  require(name.nonEmpty)
}
