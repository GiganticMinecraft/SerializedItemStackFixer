package click.seichi.domain

import cats.data.NonEmptyVector

case class Path(segments: NonEmptyVector[Segment]) {

  override def toString: String =
    segments.map(_.value).toVector.mkString(".")

}

object Path {
  def fromString(value: String): Path = {
    val segments = value
      .split(".")
      .map(Segment)
      .toVector
    assert(segments.nonEmpty)

    Path(NonEmptyVector.fromVectorUnsafe(segments))
  }
}
