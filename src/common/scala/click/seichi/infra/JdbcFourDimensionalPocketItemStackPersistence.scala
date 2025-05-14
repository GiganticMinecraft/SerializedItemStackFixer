package click.seichi.infra

import cats.data.NonEmptyVector
import cats.effect.Sync
import click.seichi.domain.DeserializedItemStacksWithPath
import click.seichi.domain.persistence.ItemStackPersistence
import click.seichi.typeclasses.SerializeAndDeserialize
import click.seichi.domain.Segment
import click.seichi.domain.DeserializedItemStacks
import click.seichi.domain.Path
import scalikejdbc._

class JdbcFourDimensionalPocketItemStackPersistence[F[_]: Sync, ItemStack](
  implicit serializeAndDeserialize: SerializeAndDeserialize[Nothing, Vector[ItemStack]]
) extends ItemStackPersistence[F, ItemStack] {
  override def readSerializedItemStacks: F[Vector[DeserializedItemStacksWithPath[ItemStack]]] = Sync[F].delay {
    DB.readOnly { implicit session =>
      sql"SELECT uuid, inventory FROM playerdata"
        .map { rs =>
          val id = rs.string("uuid")
          val inventory = rs.string("inventory")

          val path = Path(
            NonEmptyVector(
              Segment("playerdata"),
              Vector(Segment(id))
            )
          )

          DeserializedItemStacksWithPath(
            DeserializedItemStacks(serializeAndDeserialize.deserialize(inventory).merge),
            path
          )
        }
        .toList
        .apply()
        .toVector
    }
  }

  override def writeSerializedItemStacks(
    deserializedItemStacksWithPaths: Vector[DeserializedItemStacksWithPath[ItemStack]]
  ): F[Unit] = Sync[F].delay {
    DB.localTx { implicit session =>
      val params = deserializedItemStacksWithPaths.map { deserializedItemStacksWithPath =>
        Seq(
          serializeAndDeserialize.serialize(deserializedItemStacksWithPath.itemStacks.itemStacks),
          deserializedItemStacksWithPath.path.segments.last.value
        )
      }

      sql"UPDATE playerdata SET inventory = ? WHERE uuid = ?"
        .batch(params: _*)
        .apply[List]()
    }
  }
}
