package click.seichi

import cats.effect.{ContextShift, IO}
import click.seichi.application.{DeserializedItemStacksIntoChest, PutChest, WorldLifecycleManager}
import click.seichi.bukkit.typeclasses.{BukkitItemStackSerializeAndDeserialize, BukkitItemStackSerializeAndDeserializeForGacha, OnBukkitServerThread}
import click.seichi.bukkit.{BukkitDeserializedItemStacksIntoChest, BukkitPutChest, BukkitWorldLifecycleManager}
import click.seichi.infra.ScalikeJdbcConfiguration
import click.seichi.typeclasses.concurrent.{NonServerThreadContextShift, NonServerThreadContextShiftTag}
import click.seichi.typeclasses.{OnMinecraftServerThread, SerializeAndDeserialize, tag}
import org.bukkit.World.Environment
import org.bukkit.command.{Command, CommandSender}
import org.bukkit.entity.Player
import org.bukkit.event.Listener
import org.bukkit.inventory.ItemStack
import org.bukkit.plugin.java.JavaPlugin
import org.bukkit.{Bukkit, WorldCreator, WorldType}

import java.util.concurrent.Executors
import scala.concurrent.ExecutionContext

class SerializedItemStackFixer extends JavaPlugin with Listener {

  private val cachedThreadPool: ExecutionContext =
    ExecutionContext.fromExecutor(Executors.newCachedThreadPool())

  implicit val asyncShift: NonServerThreadContextShift[IO] = {
    tag.apply[NonServerThreadContextShiftTag][ContextShift[IO]](
      IO.contextShift(cachedThreadPool)
    )
  }

  implicit val plugin: JavaPlugin = this

  override def onEnable(): Unit = {
    Bukkit.getServer.getPluginManager.registerEvents(this, this)
    val config: Config = Config.loadFrom

    import config._

    ScalikeJdbcConfiguration.initializeConnectionPool(s"$getURL/seichiassist", getUser, getPassword)
    ScalikeJdbcConfiguration.initializeGlobalConfigs()
  }

  override def onDisable(): Unit = {}

  override def onCommand(sender: CommandSender, command: Command, label: String, args: Array[String]): Boolean = {
    super.onCommand(sender, command, label, args)

    val helpMessage ="/fixer deserialize - serialize された ItemStack をワールド内のチェストに移動します"

    if (!label.equalsIgnoreCase("fixer") || args.isEmpty || (!args(0).equalsIgnoreCase("deserialize") && !args(0).equalsIgnoreCase("tel"))) {
      sender.sendMessage(helpMessage)
      return true
    }

    if (args(0).equalsIgnoreCase("tel")) {
      new WorldCreator("formigration")
        .environment(Environment.NORMAL)
        .`type`(WorldType.FLAT)
        .generateStructures(false)
        .createWorld()

      sender.asInstanceOf[Player].teleport(Bukkit.getWorld("formigration").getSpawnLocation)

      return true
    }

    implicit val onMinecraftServerThread: OnMinecraftServerThread[IO] =
      new OnBukkitServerThread[IO]
    implicit val putChest: PutChest[IO] = new BukkitPutChest[IO]
    implicit val deserializedItemStacksIntoChest: DeserializedItemStacksIntoChest[IO, ItemStack] =
      new BukkitDeserializedItemStacksIntoChest[IO]
    implicit val worldLifecycleManager: WorldLifecycleManager[IO] = new BukkitWorldLifecycleManager[IO]
    implicit val serializeAndDeserialize: SerializeAndDeserialize[Nothing, Vector[ItemStack]] =
      new BukkitItemStackSerializeAndDeserialize[IO]
    implicit val serializeAndDeserializeForGacha: SerializeAndDeserialize[Nothing, ItemStack] =
      new BukkitItemStackSerializeAndDeserializeForGacha

    new ItemStackFixerAPI[IO, ItemStack].itemStackIntoWorld.runAsync {
      case Left(error) =>
        IO {
          error.printStackTrace()
        }
      case Right(_) => IO.unit
    }.unsafeRunSync()

    true
  }


}