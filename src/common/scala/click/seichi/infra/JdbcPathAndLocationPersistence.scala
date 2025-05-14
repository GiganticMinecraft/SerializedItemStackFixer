package click.seichi.infra

import cats.effect.Sync
import click.seichi.domain.persistence.PathAndLocationPersistence
import click.seichi.domain.{Path, PathAndLocation, SavedLocation, WorldName}
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
           | x INT NOT NULL,
           | y INT NOT NULL,
           | z INT NOT NULL
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
          pathAndLocation.location.x,
          pathAndLocation.location.y,
          pathAndLocation.location.z,
        )
      }

      sql"INSERT INTO serialized_itemstack_fixer.migration_targets VALUES (?, ?, ?)"
        .batch(params: _*)
        .apply[List]()
    }
  }

  override def fetchPathAndLocations(): F[Vector[PathAndLocation]] = Sync[F].delay {
    DB.readOnly { implicit session =>
      sql"SELECT path, world_name, x, y, z FROM serialized_itemstack_fixer.migration_targets"
        .map { rs =>
          val path = Path.fromString(rs.string("path"))

          val savedLocation = SavedLocation(
            WorldName(rs.string("world_name")),
            rs.int("x"),
            rs.int("y"),
            rs.int("z"),
          )

          PathAndLocation(path, savedLocation)
        }
        .toList
        .apply()
        .toVector
    }
  }
}
