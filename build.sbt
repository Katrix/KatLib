def removeSnapshot(str: String): String = if (str.endsWith("-SNAPSHOT")) str.substring(0, str.length - 9) else str

lazy val katLib = crossProject(SpongePlatform("5.0.0"), SpongePlatform("6.0.0"), SpongePlatform("7.0.0")).settings(
  name := s"KatLib-${removeSnapshot(spongeApiVersion.value)}",
  organization := "io.github.katrix",
  version := "2.4.0",
  scalaVersion := "2.12.4",
  assemblyShadeRules in assembly := Seq(
    ShadeRule.rename("scala.**"     -> "io.github.katrix.katlib.shade.scala.@1").inAll,
    ShadeRule.rename("shapeless.**" -> "io.github.katrix.katlib.shade.shapeless.@1").inAll
  ),
  assemblyJarName in assembly := s"A${(assemblyJarName in assembly).value}",
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
  spongePluginInfo := spongePluginInfo.value.copy(
    id = "katlib",
    name = Some("KatLib"),
    version = Some(s"${version.value}-${removeSnapshot(spongeApiVersion.value)}"),
    authors = Seq("Katrix"),
    dependencies =
      Set(DependencyInfo(LoadOrder.None, "spongeapi", Some(removeSnapshot(spongeApiVersion.value)), optional = false))
  ),
  oreDeploymentKey := (oreDeploymentKey in Scope.Global).?.value.flatten,
  libraryDependencies += "org.scalameta" %% "scalameta" % "1.8.0" % Provided,
  resolvers += Resolver.sonatypeRepo("releases"),
  resolvers += Resolver.bintrayIvyRepo("scalameta", "maven"),
  addCompilerPlugin("org.scalameta" % "paradise" % "3.0.0-M11" cross CrossVersion.full),
  scalacOptions += "-Xplugin-require:macroparadise",
  scalacOptions in (Compile, console) := Seq(), //macroparadise plugin doesn't work in repl yet.
  sources in (Compile, doc) := Nil, //macroparadise doesn't work with scaladoc yet.
  libraryDependencies += "org.scala-lang" % "scala-reflect" % scalaVersion.value % Provided,
  libraryDependencies += "com.chuusai"   %% "shapeless"  % "2.3.2" exclude ("org.typelevel", "macro-compat_2.12"), //Don't think macro-compat needs to be in the jar
  libraryDependencies += "org.jetbrains" % "annotations" % "15.0" % Provided,
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
