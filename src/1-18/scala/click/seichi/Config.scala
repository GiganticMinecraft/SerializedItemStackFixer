package click.seichi

import org.bukkit.configuration.file.FileConfiguration
import org.bukkit.plugin.java.JavaPlugin

final class Config private (val config: FileConfiguration) {
  def getUser: String = config.getString("user")
  def getPassword: String = config.getString("password")

  def getURL: String = {
    val portComponent = {
      val port = config.getString("port", "")
      if (port.isEmpty) "" else ":" + port
    }
    val hostComponent = config.getString("host")

    s"jdbc:mysql://$hostComponent$portComponent"
  }
}

object Config {
  def loadFrom(implicit plugin: JavaPlugin): Config = {
    plugin.saveDefaultConfig()
    plugin.reloadConfig()
    new Config(plugin.getConfig)
  }
}
