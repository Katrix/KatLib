def removeSnapshot(str: String): String = if (str.endsWith("-SNAPSHOT")) str.substring(0, str.length - 9) else str

lazy val commonSettings = Seq(
  organization := "net.katsstuff",
  version := "3.0.0-SNAPSHOT",
  scalacOptions ++= Seq(
    "-encoding",
    "utf-8",
    "-deprecation",
    "-feature",
    "-unchecked",
  ),
  scalaVersion := "3.1.1",
  //crossPaths := false,
  resolvers += Resolver.sonatypeRepo("snapshots")
)

lazy val exclusions = Seq(
  ExclusionRule("org.typelevel", "cats-core_2.13"),
  ExclusionRule("org.typelevel", "cats-kernel_2.13"),
  ExclusionRule("org.typelevel", "cats-core_3.0.0-M2"),
  ExclusionRule("org.typelevel", "cats-kernel_3.0.0-M2"),
  ExclusionRule("org.typelevel", "cats-effect_3.0.0-M2"),
  ExclusionRule("org.typelevel", "simulacrum-scalafix-annotations_3.0.0-M2"),
  ExclusionRule("org.typelevel", "jawn-parser_2.13"),
  ExclusionRule("io.circe", "circe-parser_2.13"),
  ExclusionRule("io.circe", "circe-core_2.13"),
  ExclusionRule("io.circe", "circe-jawn_2.13"),
  ExclusionRule("io.circe", "circe-numbers_2.13"),
  ExclusionRule("org.scala-lang", "scala3-library_3.0.0-M2")
)

lazy val katLibSponge =
  crossProject(SpongePlatform("8.0.0-SNAPSHOT"))
    .crossType(CrossType.Pure)
    .settings(
      commonSettings,
      name := s"katlib-sponge${removeSnapshot(spongeApiVersion.value)}",
      compileOrder := CompileOrder.JavaThenScala,
      excludeDependencies ++= exclusions,
      libraryDependencies += "org.typelevel" %% "cats-effect" % "2.3.1",
      libraryDependencies ++= Seq(
        "io.circe" %% "circe-core"   % "0.14.0-M2",
        "io.circe" %% "circe-parser" % "0.14.0-M2",
      ),
      libraryDependencies ++= Seq(
        "org.tpolecat" %% "doobie-core" % "0.10.0",
        ("org.tpolecat" %% "doobie-h2"   % "0.10.0").exclude("com.h2database", "h2")
      ),
      libraryDependencies += ("net.katsstuff" %% "minejson-text" % "0.3.2").withDottyCompat(scalaVersion.value),
      libraryDependencies ++= Seq(
        "net.katsstuff" %% "perspective"            % "0.0.5",
        "net.katsstuff" %% "perspective-derivation" % "0.0.5"
      ),
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
