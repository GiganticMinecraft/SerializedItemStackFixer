package click.seichi.domain

case class Coordinate(x: Int, y: Int, z: Int)

case class LargeChestPuttedLocation(worldName: WorldName, leftSideCoordinate: Coordinate, rightSideCoordinate: Coordinate) {
  require(
    Math.abs(leftSideCoordinate.x - rightSideCoordinate.x) == 1 ||
      Math.abs(leftSideCoordinate.z - rightSideCoordinate.z) == 1
  )
}

object LargeChestPuttedLocation {
  def apply(worldName: WorldName, leftSideCoordinate: Coordinate): LargeChestPuttedLocation = {
    LargeChestPuttedLocation(
      worldName,
      leftSideCoordinate,
      leftSideCoordinate.copy(x = leftSideCoordinate.x + 1)
    )
  }
}
