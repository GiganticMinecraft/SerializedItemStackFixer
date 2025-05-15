package click.seichi.infra

import scalikejdbc._

object ScalikeJdbcConfiguration {

  private val connectionPoolSettings: ConnectionPoolSettings =
    ConnectionPoolSettings(
      initialSize = 5,
      maxSize = 20,
      connectionTimeoutMillis = 100000L
    )

  private val loggingSettings: LoggingSQLAndTimeSettings = LoggingSQLAndTimeSettings(
    enabled = true,
    singleLineMode = true,
    printUnprocessedStackTrace = false,
    stackTraceDepth = 15,
    warningEnabled = false
  )

  def initializeConnectionPool(url: String, user: String, password: String): Unit = {
    ConnectionPool.singleton(url, user, password, connectionPoolSettings)
  }

  def initializeGlobalConfigs(): Unit = {
    GlobalSettings.loggingSQLAndTime = loggingSettings
  }


}
