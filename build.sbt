ThisBuild / version := "0.1.0-SNAPSHOT"

ThisBuild / scalaVersion := "3.3.6"

lazy val root = (project in file("."))
  .settings(
    name := "fs2PlayGround"
  )


// available for 2.12, 2.13, 3.2
libraryDependencies += "co.fs2" %% "fs2-core" % "3.12.0"

// optional I/O library
libraryDependencies += "co.fs2" %% "fs2-io" % "3.12.0"

// optional reactive streams interop
libraryDependencies += "co.fs2" %% "fs2-reactive-streams" % "3.12.0"

// optional scodec interop
libraryDependencies += "co.fs2" %% "fs2-scodec" % "3.12.0"

libraryDependencies += "com.github.zainab-ali" %% "aquascape" % "0.3.0"

libraryDependencies += "com.lihaoyi" %% "os-lib" % "0.11.3"

libraryDependencies += "org.tpolecat" %% "skunk-core" % "0.6.4"

libraryDependencies += "org.typelevel" %% "log4cats-slf4j" % "2.3.1"