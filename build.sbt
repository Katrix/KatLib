def removeSnapshot(str: String): String = if (str.endsWith("-SNAPSHOT")) str.substring(0, str.length - 9) else str

lazy val commonSettings = Seq(
  organization := "net.katsstuff",
  version := "3.0.0-SNAPSHOT",
  scalacOptions ++= Seq(
    "-encoding",
    "utf-8",
    "-deprecation",
    "-feature",
    "-unchecked"
  ),
  scalaVersion := "3.0.0-M3",
  //crossPaths := false,
  resolvers += Resolver.sonatypeRepo("snapshots")
)

lazy val katLibSponge =
  crossProject(SpongePlatform("8.0.0-SNAPSHOT"))
    .crossType(CrossType.Pure)
    .settings(
      commonSettings,
      name := s"katlib-sponge${removeSnapshot(spongeApiVersion.value)}",
      libraryDependencies ++= Seq(
        ("io.circe" %% "circe-core"   % "0.12.3").withDottyCompat(scalaVersion.value),
        ("io.circe" %% "circe-parser" % "0.12.3").withDottyCompat(scalaVersion.value),
        ("io.circe" %% "circe-config" % "0.8.0").withDottyCompat(scalaVersion.value)
      ),
      libraryDependencies += ("net.katsstuff" %% "minejson-text" % "0.3.2").withDottyCompat(scalaVersion.value),
      description := "Scala loader and misc utilities for Sponge",
      spongeV8.pluginLoader := "scala_plain",
      spongeV8.pluginInfo := Seq(
        spongeV8.pluginInfo.value.head.copy(
          id = "katlib",
          mainClass = "net.katsstuff.katlib.KatLib",
          contributors = Seq(
            spongeV8.Contributor("Katrix")
          )
        )
      ),
      //https://github.com/portable-scala/sbt-crossproject/issues/74
      Seq(Compile, Test).flatMap(inConfig(_) {
        unmanagedResourceDirectories ++= {
          unmanagedSourceDirectories.value
            .map(src => (src / ".." / "resources").getCanonicalFile)
            .filterNot(unmanagedResourceDirectories.value.contains)
            .distinct
        }
      })
    )

lazy val katLibSpongeV800 = katLibSponge.spongeProject("8.0.0-SNAPSHOT")

lazy val katLibRoot = (project in file("."))
//.disablePlugins(AssemblyPlugin)
  .aggregate(katLibSpongeV800)
