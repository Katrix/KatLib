def removeSnapshot(str: String): String = if (str.endsWith("-SNAPSHOT")) str.substring(0, str.length - 9) else str

def deployKeySetting = oreDeploymentKey := (oreDeploymentKey in Scope.Global).?.value.flatten

lazy val commonSettings = Seq(
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
  libraryDependencies += "com.chuusai"   %% "shapeless"  % "2.3.2" exclude ("org.typelevel", "macro-compat_2.12"), //Don't think macro-compat needs to be in the jar
  libraryDependencies += "org.jetbrains" % "annotations" % "15.0" % Provided
)

lazy val katLibShared = (project in file("shared"))
  .enablePlugins(SpongePlugin)
  .settings(
    commonSettings,
    spongeMetaCreate := false,
    oreDeploy := None,
    name := "KatLib-Shared",
    //Default version, needs to build correctly against all supported versions
    spongeApiVersion := "5.0.0",
    libraryDependencies += "org.scalameta" %% "scalameta" % "1.8.0" % Provided,
    resolvers += Resolver.sonatypeRepo("releases"),
    resolvers += Resolver.bintrayIvyRepo("scalameta", "maven"),
    addCompilerPlugin("org.scalameta" % "paradise" % "3.0.0-M11" cross CrossVersion.full),
    scalacOptions += "-Xplugin-require:macroparadise",
    scalacOptions in (Compile, console) := Seq(), //macroparadise plugin doesn't work in repl yet.
    sources in (Compile, doc) := Nil, //macroparadise doesn't work with scaladoc yet.
    libraryDependencies += "org.scala-lang" % "scala-reflect" % scalaVersion.value % Provided
  )

lazy val katLibV500: Project = (project in file("5.0.0"))
  .enablePlugins(SpongePlugin)
  .dependsOn(katLibShared)
  .settings(commonSettings, spongeApiVersion := "5.0.0", deployKeySetting)

lazy val katLibV600: Project = (project in file("6.0.0"))
  .enablePlugins(SpongePlugin)
  .dependsOn(katLibShared)
  .settings(commonSettings, spongeApiVersion := "6.0.0", deployKeySetting)

lazy val katLibV700: Project = (project in file("7.0.0"))
  .enablePlugins(SpongePlugin)
  .dependsOn(katLibShared)
  .settings(commonSettings, spongeApiVersion := "7.0.0", deployKeySetting)

lazy val katLibRoot = (project in file("."))
  .settings(
    publishArtifact := false,
    assembleArtifact := false,
    spongeMetaCreate := false,
    publish := {},
    publishLocal := {}
  )
  .disablePlugins(AssemblyPlugin)
  .aggregate(katLibShared, katLibV500, katLibV600, katLibV700)
