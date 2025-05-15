package click.seichi

import cats.effect.{ContextShift, IO}
import click.seichi.application.{DeserializedItemStacksIntoChest, PutChest, WorldLifecycleManager}
import click.seichi.bukkit.typeclasses.{BukkitItemStackSerializeAndDeserialize, OnBukkitServerThread}
import click.seichi.bukkit.{BukkitDeserializedItemStacksIntoChest, BukkitPutChest, BukkitWorldLifecycleManager}
import click.seichi.infra.ScalikeJdbcConfiguration
import click.seichi.typeclasses.concurrent.{NonServerThreadContextShift, NonServerThreadContextShiftTag}
import click.seichi.typeclasses.{OnMinecraftServerThread, SerializeAndDeserialize, tag}
import org.bukkit.Bukkit
import org.bukkit.command.{Command, CommandSender}
import org.bukkit.event.Listener
import org.bukkit.inventory.ItemStack
import org.bukkit.plugin.java.JavaPlugin

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

    if (!label.equalsIgnoreCase("fixer") || args.isEmpty || !args(0).equalsIgnoreCase("deserialize")) {
      sender.sendMessage(helpMessage)
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

    new ItemStackFixerAPI[IO, ItemStack].itemStackIntoWorld.unsafeRunSync()

    true
  }


}