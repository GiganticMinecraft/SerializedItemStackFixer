package click.seichi.domain

import cats.data.NonEmptyVector

case class Path(segments: NonEmptyVector[Segment]) {

  override def toString: String =
    segments.map(_.value).toVector.mkString(".")

}
