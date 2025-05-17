package click.seichi.infra

import cats.data.NonEmptyVector
import cats.effect.Sync
import click.seichi.domain.{DeserializedItemStacks, DeserializedItemStacksWithPath, Path, Segment}
import click.seichi.domain.persistence.ItemStackPersistence
import click.seichi.typeclasses.SerializeAndDeserialize
import scalikejdbc._

class JdbcGachaDataItemStackPersistence[F[_]: Sync, ItemStack](
  serializeAndDeserialize: SerializeAndDeserialize[Nothing, ItemStack]
) extends ItemStackPersistence[F, ItemStack] {
  override def readSerializedItemStacks: F[Vector[DeserializedItemStacksWithPath[ItemStack]]] = Sync[F].delay {
    DB.readOnly { implicit session =>
      sql"SELECT id, itemstack FROM gachadata WHERE itemstack IS NOT NULL"
        .map { rs =>
          val id = rs.string("id")
          val path = Path(
            NonEmptyVector(
              Segment("gachadata"),
              Vector(Segment(id))
            )
          )

          serializeAndDeserialize.deserialize(
            rs.string("itemstack")
          )
          .map(itemStack => DeserializedItemStacksWithPath(DeserializedItemStacks(Vector(itemStack)), path))
          .merge
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
          serializeAndDeserialize.serialize(
            deserializedItemStacksWithPath.itemStacks.itemStacks.head
          ),
          deserializedItemStacksWithPath.path.segments.toVector.last.value,
        )
      }

      sql"UPDATE gachadata SET itemstack = ? WHERE id = ?"
        .batch(batchParams: _*)
        .apply[List]()
    }
  }
}
