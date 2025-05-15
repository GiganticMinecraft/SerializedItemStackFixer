package click.seichi.application

import click.seichi.domain.{LargeChestPuttedLocation, Path, PathAndLocation, WorldName, Coordinate}

import scala.math.{cbrt, ceil}

object ComputeLocationFromPaths {
  private def minimumSideSize(size: Int): Int = ceil(cbrt(size)).toInt

  def compute(paths: Vector[Path], worldName: WorldName): Vector[PathAndLocation] = {
    val sideSize = minimumSideSize(paths.size)

    // NOTE: これを超えるのは対象となる Path の総数が 16,581,375 を超えたとき。
    assert(sideSize > 255, "Total path size is too large!")

    val savedLocations = for {
      x <- 0 to sideSize * 3 by 3
      y <- 1 to (sideSize + 1)
      z <- 0 to sideSize * 2 by 2
    } yield LargeChestPuttedLocation(worldName, Coordinate(x, y, z))

    // NOTE: このアサーションが引っかかるならロジックが間違っている
    assert(savedLocations.size <= paths.size)

    paths.zip(savedLocations).map { case (paths, location) =>
      PathAndLocation(paths, location)
    }
  }
}
