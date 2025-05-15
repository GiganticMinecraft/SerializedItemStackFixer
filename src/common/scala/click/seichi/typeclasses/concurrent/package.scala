package click.seichi.typeclasses

import cats.effect.ContextShift
import click.seichi.typeclasses.tag.@@

package object concurrent {

  trait NonServerThreadContextShiftTag

  type NonServerThreadContextShift[F[_]] = ContextShift[F] @@ NonServerThreadContextShiftTag

}
