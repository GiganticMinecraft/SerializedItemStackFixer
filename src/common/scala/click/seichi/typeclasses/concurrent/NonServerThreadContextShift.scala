package click.seichi.typeclasses.concurrent

object NonServerThreadContextShift {

  def apply[F[_]](implicit cs: NonServerThreadContextShift[F]): NonServerThreadContextShift[F] =
    cs

}
