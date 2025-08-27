package resources

import cats.FlatMap.nonInheritedOps.toFlatMapOps
import cats.effect.{Resource, Temporal}
import cats.effect.kernel.Concurrent
import cats.effect.std.Console
import config.{AppConfig, PostgreSQLConfig}
import fs2.io.net.Network
import natchez.Trace.Implicits.noop
import org.typelevel.log4cats.Logger
import skunk.{Session, SessionPool}
import skunk.*
import skunk.codec.text.*
import skunk.implicits.*

sealed abstract class AppResources[F[_]](
    val postgres: Resource[F, Session[F]],
)

object AppResources {

  def make[F[_]: Concurrent: Logger: Temporal: Network: Console](
      cfg: AppConfig
  ): Resource[F, AppResources[F]] = {

    def checkPostgresConnection(
        postgres: Resource[F, Session[F]]
    ): F[Unit] =
      postgres.use { session =>
        session.unique(sql"select version();".query(text)).flatMap { v =>
          Logger[F].info(s"Connected to Postgres $v")
        }
      }

    def mkPostgreSqlResource(c: PostgreSQLConfig): SessionPool[F] =
      Session
        .pooled[F](
          host = c.host,
          port = c.port,
          user = c.user,
          password = Some(c.password),
          database = c.database,
          max = c.max
        )
        .evalTap(checkPostgresConnection)

    (
      mkPostgreSqlResource(cfg.postgreSQL)
    ).map(new AppResources[F](_) {})
  }

}