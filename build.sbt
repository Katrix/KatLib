def removeSnapshot(str: String): String = if (str.endsWith("-SNAPSHOT")) str.substring(0, str.length - 9) else str

val circeVersion = "0.9.1"

lazy val katLib = crossProject(SpongePlatform("5.0.0"), SpongePlatform("6.0.0"), SpongePlatform("7.0.0"))
  .crossType(CrossType.Pure)
  .settings(
    name := s"KatLib-${removeSnapshot(spongeApiVersion.value)}",
    organization := "net.katsstuff",
    version := "3.0.0-SNAPSHOT",
    scalaVersion := "2.12.4",
    scalacOptions ++= Seq(
      "-deprecation",
      "-feature",
      "-unchecked",
      "-Xlint",
      "-Yno-adapted-args",
      "-Ywarn-dead-code",
      "-Ywarn-unused-import"
    ),
    crossPaths := false,
    resolvers += Resolver.sonatypeRepo("snapshots"),
    libraryDependencies += "org.scala-lang" % "scala-reflect" % scalaVersion.value % Provided,
    libraryDependencies += "com.chuusai"    %% "shapeless"    % "2.3.3",
    libraryDependencies += "org.jetbrains"  % "annotations"   % "15.0" % Provided,
    libraryDependencies += "org.typelevel"  %% "cats-core"    % "1.0.1",
    libraryDependencies ++= Seq("io.circe" %% "circe-core", "io.circe" %% "circe-generic", "io.circe" %% "circe-parser")
      .map(_ % circeVersion),
    libraryDependencies += "com.geirsson"  %% "metaconfig-core"            % "0.6.0",
    libraryDependencies += "com.geirsson"  %% "metaconfig-typesafe-config" % "0.6.0",
    libraryDependencies += "net.katsstuff" %% "scammander-sponge7"         % "0.3-SNAPSHOT",
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

lazy val katLibV500 = katLib.spongeProject("5.0.0")
lazy val katLibV600 = katLib.spongeProject("6.0.0")
lazy val katLibV700 = katLib.spongeProject("7.0.0")

lazy val katLibRoot = (project in file("."))
  .disablePlugins(AssemblyPlugin)
  .aggregate(katLibV500, katLibV600, katLibV700)
