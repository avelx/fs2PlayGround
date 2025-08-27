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