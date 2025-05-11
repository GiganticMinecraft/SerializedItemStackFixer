package click.seichi.domain

case class Segment(value: String) {
  require(value.nonEmpty, "`Segment` must be nonempty.")
}
