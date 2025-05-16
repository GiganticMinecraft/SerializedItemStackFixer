package click.seichi.infra

import cats.data.NonEmptyVector
import cats.effect.Sync
import click.seichi.domain.DeserializedItemStacksWithPath
import click.seichi.domain.persistence.ItemStackPersistence
import click.seichi.domain.DeserializedItemStacks
import click.seichi.domain.Path
import click.seichi.domain.Segment
import click.seichi.typeclasses.SerializeAndDeserialize
import scalikejdbc._

class JdbcSharedInventoryItemStackPersistence[F[_]: Sync, ItemStack](
  implicit serializeAndDeserialize: SerializeAndDeserialize[Nothing, Vector[ItemStack]]
) extends ItemStackPersistence[F, ItemStack] {
  override def readSerializedItemStacks: F[Vector[DeserializedItemStacksWithPath[ItemStack]]] = Sync[F].delay {
    DB.readOnly { implicit session =>
      sql"SELECT uuid, shareinv FROM playerdata WHERE shareinv IS NOT NULL"
        .map { rs =>
          val id = rs.string("uuid")
          val shareinv = rs.string("shareinv")

          val path = Path(
            NonEmptyVector(
              Segment("playerdata"),
              Vector(
                Segment("shareinv"),
                Segment(id)
              )
            )
          )

          DeserializedItemStacksWithPath(
            DeserializedItemStacks(serializeAndDeserialize.deserialize(shareinv).merge),
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
      val batchParams = deserializedItemStacksWithPaths.map { deserializedItemStacksWithPath =>
        Seq(
          serializeAndDeserialize.serialize(deserializedItemStacksWithPath.itemStacks.itemStacks),
          deserializedItemStacksWithPath.path.segments.last.value
        )
      }

      sql"UPDATE playerdata SET shareinv = ? WHERE uuid = ?"
        .batch(batchParams: _*)
        .apply[List]()
    }
  }
}
