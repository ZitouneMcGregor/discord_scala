package utils

import com.zaxxer.hikari.{HikariConfig, HikariDataSource}
import java.sql.Connection

object DatabaseConfig {
  private val hikariConfig = new HikariConfig()
  hikariConfig.setJdbcUrl("jdbc:mysql://localhost:3036/AdminDiscord")
  hikariConfig.setUsername("root")
  hikariConfig.setPassword("1234567aA*")
  hikariConfig.setDriverClassName("com.mysql.cj.jdbc.Driver")
  hikariConfig.setMaximumPoolSize(10)

  private val dataSource = new HikariDataSource(hikariConfig)

  def getConnection: Connection = dataSource.getConnection
}
