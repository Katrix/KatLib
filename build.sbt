def removeSnapshot(str: String): String = if (str.endsWith("-SNAPSHOT")) str.substring(0, str.length - 9) else str

lazy val exclusions =
  Seq(
    ExclusionRule("org.spongepowered", "spongeapi"),
    ExclusionRule("com.typesafe", "config"),
    ExclusionRule("org.spigotmc", "spigot-api"),
    ExclusionRule("org.yaml", "snakeyaml"),
    ExclusionRule("org.scala-lang", "scala-reflect"),
  )

lazy val circeVersion      = "0.9.3"
lazy val scammanderVersion = "0.6-SNAPSHOT"

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
    "-language:higherKinds"
  ),
  scalaVersion := "2.12.6",
  crossPaths := false,
  addCompilerPlugin("org.spire-math" %% "kind-projector" % "0.9.7"),
  resolvers += Resolver.sonatypeRepo("snapshots")
)

lazy val katLibBase = project
  .settings(
    commonSettings,
    name := "katlib-base",
    libraryDependencies += "com.chuusai"   %% "shapeless"   % "2.3.3",
    libraryDependencies += "org.typelevel" %% "cats-core"   % "1.1.0" excludeAll (exclusions: _*),
    libraryDependencies += "org.typelevel" %% "cats-effect" % "1.0.0-RC2" excludeAll (exclusions: _*),
    libraryDependencies ++= Seq("io.circe" %% "circe-core", "io.circe" %% "circe-generic", "io.circe" %% "circe-parser")
      .map(_ % circeVersion excludeAll (exclusions: _*)),
    libraryDependencies += "net.katsstuff" %% "scammander"    % scammanderVersion excludeAll (exclusions: _*),
    libraryDependencies += "net.katsstuff" %% "minejson-text" % "0.2-SNAPSHOT"
  )

def scammanderSponge(apiVersion: String) =
  "net.katsstuff" %% s"scammander-sponge_sponge$apiVersion" % scammanderVersion excludeAll (exclusions: _*)

lazy val katLibSponge = crossProject(SpongePlatform("5.1.0"), SpongePlatform("6.0.0"), SpongePlatform("7.0.0"))
  .crossType(CrossType.Pure)
  .settings(
    commonSettings,
    name := s"katlib-sponge${removeSnapshot(spongeApiVersion.value)}",
    resolvers += Resolver.sonatypeRepo("snapshots"),
    libraryDependencies += "io.circe"       %% "circe-config" % "0.4.1" excludeAll (exclusions: _*),
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
  .configure(_.dependsOn(katLibBase))
  .spongeSettings("5.1.0")(libraryDependencies += scammanderSponge("5.1"))
  .spongeSettings("6.0.0")(libraryDependencies += scammanderSponge("6.0"))
  .spongeSettings("7.0.0")(libraryDependencies += scammanderSponge("7.0"))

lazy val katLibBukkit = project
  .settings(
    commonSettings,
    name := "katlib-bukkit",
    resolvers ++= Seq(
      "spigotmc-snapshots" at "https://hub.spigotmc.org/nexus/content/repositories/snapshots",
      Resolver.sonatypeRepo("snapshots"),
      "dmulloy2-repo" at "http://repo.dmulloy2.net/nexus/repository/public/",
      "vault-repo" at "http://nexus.hc.to/content/repositories/pub_releases"
    ),
    libraryDependencies += "io.circe"               %% "circe-yaml"        % "0.8.0" excludeAll (exclusions: _*),
    libraryDependencies += "org.spigotmc"           % "spigot-api"         % "1.13-pre7-R0.1-SNAPSHOT" % Provided,
    libraryDependencies += "com.comphenix.protocol" % "ProtocolLib-API"    % "4.4.0-SNAPSHOT" % Provided notTransitive (),
    libraryDependencies += "net.milkbowl.vault"     % "VaultAPI"           % "1.6" % Provided,
    libraryDependencies += "net.katsstuff"          %% "scammander-bukkit" % scammanderVersion excludeAll (exclusions: _*),
    assemblyShadeRules in assembly := Seq(
      ShadeRule.rename("cats.**"                     -> "net.katsstuff.katlib.shade.cats.@1").inAll,
      ShadeRule.rename("fastparse.**"                -> "net.katsstuff.katlib.shade.fastparse.@1").inAll,
      ShadeRule.rename("io.circe.**"                 -> "net.katsstuff.katlib.shade.circe.@1").inAll,
      ShadeRule.rename("jawn.**"                     -> "net.katsstuff.katlib.shade.jawn.@1").inAll,
      ShadeRule.rename("machinist.**"                -> "net.katsstuff.katlib.shade.machinist.@1").inAll, //Zap?
      ShadeRule.rename("scala.**"                    -> "net.katsstuff.katlib.shade.scala.@1").inAll,
      ShadeRule.rename("shapeless.**"                -> "net.katsstuff.katlib.shade.shapeless.@1").inAll,
      ShadeRule.rename("sourcecode.**"               -> "net.katsstuff.katlib.shade.sourcecode.@1").inAll,
      ShadeRule.rename("net.katsstuff.scammander.**" -> "net.katsstuff.katlib.shade.scammander.@1").inAll,
      ShadeRule.rename("net.katsstuff.minejson.**"   -> "net.katsstuff.katlib.shade.minejson.@1").inAll,
      ShadeRule.rename("net.katsstuff.typenbt.**"    -> "net.katsstuff.katlib.shade.typenbt.@1").inAll,
      ShadeRule.zap("macrocompat.**").inAll,
    ),
  )
  .dependsOn(katLibBase)

lazy val katLibSpongeV510 = katLibSponge.spongeProject("5.1.0")
lazy val katLibSpongeV600 = katLibSponge.spongeProject("6.0.0")
lazy val katLibSpongeV700 = katLibSponge.spongeProject("7.0.0")

lazy val katLibRoot = (project in file("."))
  .disablePlugins(AssemblyPlugin)
  .aggregate(katLibBase, katLibBukkit, katLibSpongeV510, katLibSpongeV600, katLibSpongeV700)
