def removeSnapshot(str: String): String = if (str.endsWith("-SNAPSHOT")) str.substring(0, str.length - 9) else str

val circeVersion = "0.9.1"

lazy val katLib = crossProject(SpongePlatform("5.0.0"), SpongePlatform("6.0.0"), SpongePlatform("7.0.0"))
  .crossType(CrossType.Pure)
  .settings(
    name := s"KatLib-${removeSnapshot(spongeApiVersion.value)}",
    organization := "net.katsstuff",
    version := "3.0.0-SNAPSHOT",
    scalaVersion := "2.12.4",
    assemblyShadeRules in assembly := Seq(
      ShadeRule.rename("cats.**"                    -> "io.github.katrix.homesweethome.shade.cats.@1").inAll,
      ShadeRule.rename("com.google.protobuf.**"     -> "io.github.katrix.homesweethome.shade.protobuf.@1").inAll,
      ShadeRule.rename("com.trueaccord.lenses.**"   -> "io.github.katrix.homesweethome.shade.lenses.@1").inAll,
      ShadeRule.rename("com.trueaccord.scalapb.**"  -> "io.github.katrix.homesweethome.shade.scalapb.@1").inAll,
      ShadeRule.rename("fansi.**"                   -> "io.github.katrix.homesweethome.shade.fansi.@1").inAll,
      ShadeRule.rename("fastparse.**"               -> "io.github.katrix.homesweethome.shade.fastparse.@1").inAll,
      ShadeRule.rename("io.circe.**"                -> "io.github.katrix.homesweethome.shade.circe.@1").inAll,
      ShadeRule.rename("jawn.**"                    -> "io.github.katrix.homesweethome.shade.jawn.@1").inAll,
      ShadeRule.rename("machinist.**"               -> "io.github.katrix.homesweethome.shade.machinist.@1").inAll, //Zap?
      ShadeRule.rename("metaconfig.**"              -> "io.github.katrix.homesweethome.shade.metaconfig.@1").inAll,
      ShadeRule.rename("org.langmeta.**"            -> "io.github.katrix.homesweethome.shade.langmeta.@1").inAll,
      ShadeRule.rename("org.scalameta.**"           -> "io.github.katrix.homesweethome.shade.scalameta.@1").inAll,
      ShadeRule.rename("org.typelevel.paiges.**"    -> "io.github.katrix.homesweethome.shade.paiges.@1").inAll,
      ShadeRule.rename("pprint.**"                  -> "io.github.katrix.homesweethome.shade.pprint.@1").inAll,
      ShadeRule.rename("scala.**"                   -> "io.github.katrix.homesweethome.shade.scala.@1").inAll,
      ShadeRule.rename("scalapb.**"                 -> "io.github.katrix.homesweethome.shade.scalapb.@1").inAll,
      ShadeRule.rename("shapeless.**"               -> "io.github.katrix.homesweethome.shade.shapeless.@1").inAll,
      ShadeRule.rename("sourcecode.**"              -> "io.github.katrix.homesweethome.shade.sourcecode.@1").inAll,
      ShadeRule.rename("io.github.katrix.katlib.**" -> "io.github.katrix.homesweethome.shade.katlib.@1").inAll,
      ShadeRule.zap("macrocompat.**").inAll
    ),
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
    libraryDependencies += "org.scala-lang" % "scala-reflect" % scalaVersion.value % Provided,
    libraryDependencies += "com.chuusai"    %% "shapeless"    % "2.3.3",
    libraryDependencies += "org.jetbrains"  % "annotations"   % "15.0" % Provided,
    libraryDependencies += "org.typelevel"  %% "cats-core"    % "1.0.1",
    libraryDependencies ++= Seq("io.circe" %% "circe-core", "io.circe" %% "circe-generic", "io.circe" %% "circe-parser")
      .map(_ % circeVersion),
    libraryDependencies += "com.geirsson"  %% "metaconfig-core"            % "0.6.0",
    libraryDependencies += "com.geirsson"  %% "metaconfig-typesafe-config" % "0.6.0",
    libraryDependencies += "net.katsstuff" %% "scammander-sponge7"         % "0.2",
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
