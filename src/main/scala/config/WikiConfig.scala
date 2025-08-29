package config

import pureconfig.ConfigSource
import pureconfig.*
import pureconfig.error.ConfigReaderFailures
import pureconfig.generic.derivation.default._

case class WikiConfig(isProd: Boolean) derives ConfigReader

object WikiConfig {
  def load: WikiConfig = {
    ConfigSource
      .default
      .load[WikiConfig]
      .toOption
      .getOrElse(WikiConfig(false))

  }
}
