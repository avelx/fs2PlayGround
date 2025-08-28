package config

case class AppConfig(postgreSQL: PostgreSQLConfig)

case class PostgreSQLConfig(
                             host: String,
                             port: Int,
                             user: String,
                             password: String,
                             database: String,
                             max: Int
                           )

object PostgreSQLConfig {
  def default: PostgreSQLConfig =
    PostgreSQLConfig(
      host = "localhost",
      user = "postgres",
      password = "my-password",
      database = "store",
      port = 5432,
      max = 5
    )
}