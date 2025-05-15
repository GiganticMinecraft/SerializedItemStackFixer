package click.seichi.infra

import cats.effect.Sync
import click.seichi.domain.persistence.PathAndLocationPersistence
import click.seichi.domain.{Coordinate, LargeChestPuttedLocation, Path, PathAndLocation, WorldName}
import scalikejdbc._

class JdbcPathAndLocationPersistence[F[_]: Sync] extends PathAndLocationPersistence[F] {
  override def createSerializedItemStackFixerDatabase(): F[Unit] = Sync[F].delay {
    DB.localTx { implicit session =>
      sql"CREATE DATABASE IF NOT EXISTS serialized_itemstack_fixer".execute()
    }
  }

  override def createMigrationTargetPathWithLocationTable(): F[Unit] = Sync[F].delay {
    DB.localTx { implicit session =>
      sql"""CREATE TABLE IF NOT EXISTS migration_targets(
           | path VARCHAR(255) NOT NULL PRIMARY KEY,
           | world_name VARCHAR(255) NOT NULL,
           | left_side_x INT NOT NULL,
           | left_side_y INT NOT NULL,
           | left_side_z INT NOT NULL,
           | right_side_x INT NOT NULL,
           | right_side_y INT NOT NULL,
           | right_side_z INT NOT NULL
           | )
         """
        .stripMargin
        .execute()
    }
  }

  override def insertPathAndLocations(pathAndLocations: Vector[PathAndLocation]): F[Unit] = Sync[F].delay {
    DB.localTx { implicit session =>
      val params = pathAndLocations.map { pathAndLocation =>
        Seq(
          pathAndLocation.path.toString,
          pathAndLocation.location.worldName,
          pathAndLocation.location.leftSideCoordinate.x,
          pathAndLocation.location.leftSideCoordinate.y,
          pathAndLocation.location.leftSideCoordinate.z,
          pathAndLocation.location.rightSideCoordinate.x,
          pathAndLocation.location.rightSideCoordinate.y,
          pathAndLocation.location.rightSideCoordinate.z,
        )
      }

      sql"INSERT INTO serialized_itemstack_fixer.migration_targets VALUES (?, ?, ?)"
        .batch(params: _*)
        .apply[List]()
    }
  }

  override def fetchPathAndLocations(): F[Vector[PathAndLocation]] = Sync[F].delay {
    DB.readOnly { implicit session =>
      sql"SELECT path, world_name, left_side_x, left_side_y, left_side_z, right_side_x, right_side_y, right_side_z, FROM serialized_itemstack_fixer.migration_targets"
        .map { rs =>
          val path = Path.fromString(rs.string("path"))

          val savedLocation = LargeChestPuttedLocation(
            WorldName(rs.string("world_name")),
            Coordinate(
              rs.int("left_side_x"),
              rs.int("left_side_y"),
              rs.int("left_side_z")
            ),
            Coordinate(
              rs.int("right_side_x"),
              rs.int("right_side_y"),
              rs.int("right_side_z")
            ),
          )

          PathAndLocation(path, savedLocation)
        }
        .toList
        .apply()
        .toVector
    }
  }
}
