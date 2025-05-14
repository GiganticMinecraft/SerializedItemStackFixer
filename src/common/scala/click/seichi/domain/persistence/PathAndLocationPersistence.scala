package click.seichi.domain.persistence

import click.seichi.domain.PathAndLocation

trait PathAndLocationPersistence[F[_]] {

  def createSerializedItemStackFixerDatabase(): F[Unit]

  def createMigrationTargetPathWithLocationTable(): F[Unit]

  def insertPathAndLocations(pathAndLocation: Vector[PathAndLocation]): F[Unit]

  def fetchPathAndLocations(): F[Vector[PathAndLocation]]

}
