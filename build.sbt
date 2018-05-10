def removeSnapshot(str: String): String = if (str.endsWith("-SNAPSHOT")) str.substring(0, str.length - 9) else str

lazy val circeVersion = "0.9.1"

lazy val commonSettings = Seq(
  organization := "net.katsstuff",
  version := "3.0.0-SNAPSHOT",
  scalacOptions ++= Seq(
    "-deprecation",
    "-encoding",
    "utf-8",
    "-explaintypes",
    "-feature",
    "-unchecked",
    "-Xcheckinit",
    "-Xlint",
    "-Yno-adapted-args",
    "-Ypartial-unification",
    "-Ywarn-dead-code",
    "-Ywarn-infer-any",
    "-Ywarn-nullary-override",
    "-Ywarn-nullary-unit",
    "-Ywarn-unused",
  ),
  scalaVersion := "2.12.6",
  crossPaths := false,
  addCompilerPlugin("org.spire-math" %% "kind-projector" % "0.9.6")
)

lazy val katLibBase = project
  .settings(
    commonSettings,
    name := "katlib-base",
    libraryDependencies += "com.chuusai"   %% "shapeless"   % "2.3.3",
    libraryDependencies += "org.typelevel" %% "cats-core"   % "1.1.0",
    libraryDependencies += "org.typelevel" %% "cats-effect" % "0.10",
    libraryDependencies ++= Seq("io.circe" %% "circe-core", "io.circe" %% "circe-generic", "io.circe" %% "circe-parser")
      .map(_ % circeVersion),
    libraryDependencies += "org.jetbrains" % "annotations" % "15.0" % Provided,
    libraryDependencies += "net.katsstuff" %% "scammander" % "0.4"
  )

lazy val katLibSponge = crossProject(SpongePlatform("5.1.0"), SpongePlatform("6.0.0"), SpongePlatform("7.0.0"))
  .crossType(CrossType.Pure)
  .settings(
    commonSettings,
    name := s"katlib-sponge${removeSnapshot(spongeApiVersion.value)}",
    resolvers += Resolver.sonatypeRepo("snapshots"),
    libraryDependencies += "io.circe" %% "circe-config" % "0.4.1",
    libraryDependencies += "org.scala-lang" % "scala-reflect" % scalaVersion.value % Provided,
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
  .spongeSettings("5.1.0")(libraryDependencies += "net.katsstuff" %% "scammanderSponge_sponge5.1" % "0.3")
  .spongeSettings("6.0.0")(libraryDependencies += "net.katsstuff" %% "scammanderSponge_sponge6.0" % "0.3")
  .spongeSettings("7.0.0")(libraryDependencies += "net.katsstuff" %% "scammanderSponge_sponge7.0" % "0.3")

lazy val katLibBukkit = project.settings(
  commonSettings,
  name := "katlib-bukkit",
  resolvers ++= Seq(
    "spigotmc-snapshots" at "https://hub.spigotmc.org/nexus/content/repositories/snapshots",
    Resolver.sonatypeRepo("snapshots"),
    "dmulloy2-repo" at "http://repo.dmulloy2.net/nexus/repository/public/"
  ),
  libraryDependencies += "io.circe" %% "circe-yaml" % "0.8.0",
  libraryDependencies += "org.spigotmc"           % "spigot-api"         % "1.12.2-R0.1-SNAPSHOT" % Provided,
  libraryDependencies += "com.comphenix.protocol" % "ProtocolLib-API"    % "4.4.0-SNAPSHOT" % Provided notTransitive (),
  libraryDependencies += "net.katsstuff"          %% "scammander-bukkit" % "0.4",
  libraryDependencies += "net.katsstuff"          %% "typenbt"           % "0.3",
  libraryDependencies += "net.katsstuff"          %% "minejson"          % "0.1"
)

lazy val katLibSpongeV510 = katLibSponge.spongeProject("5.1.0")
lazy val katLibSpongeV600 = katLibSponge.spongeProject("6.0.0")
lazy val katLibSpongeV700 = katLibSponge.spongeProject("7.0.0")

lazy val katLibRoot = (project in file("."))
  .disablePlugins(AssemblyPlugin)
  .aggregate(katLibBase, katLibBukkit, katLibSpongeV510, katLibSpongeV600, katLibSpongeV700)
