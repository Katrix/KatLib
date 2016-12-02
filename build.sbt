lazy val commonSettings = Seq(
	name := s"KatLib-${spongeApiVersion.value}",
	organization := "io.github.katrix",
	version := "2.0.0",
	scalaVersion := "2.12.0",
	assemblyShadeRules in assembly := Seq(
		ShadeRule.rename("scala.**" -> "io.github.katrix.katlib.shade.scala.@1").inAll,
		ShadeRule.rename("shapeless.**" -> "io.github.katrix.katlib.shade.shapeless.@1").inAll
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

	spongePluginInfo := spongePluginInfo.value.copy(
		id = "katlib",
		name = Some("KatLib"),
		version = Some(s"${spongeApiVersion.value}-${version.value}"),
		authors = Seq("Katrix"),
		dependencies = Set(DependencyInfo("spongeapi", Some(spongeApiVersion.value)))
	),

	libraryDependencies += "com.chuusai" %% "shapeless" % "2.3.2",

	artifactName := { (sv, module, artifact) => s"​${artifact.name}-${module.revision}.${artifact.extension}"},
	assemblyJarName <<= (name, version) map { (name, version) => s"​$name-assembly-$version.jar" }
)

lazy val katLibShared = (project in file("shared"))
	.enablePlugins(SpongePlugin)
	.settings(commonSettings: _*)
	.settings(
		name := "KatLib-Shared",
		assembleArtifact := false,
		spongeMetaCreate := false,
		//Default version, needs to build correctly against all supported versions
		spongeApiVersion := "4.1.0",
		libraryDependencies += "org.scala-lang" % "scala-reflect" % scalaVersion.value % Provided
	)

lazy val katLibV410 = (project in file("4.1.0"))
	.enablePlugins(SpongePlugin)
	.dependsOn(katLibShared)
	.settings(commonSettings: _*)
	.settings(spongeApiVersion := "4.1.0")

lazy val katLibV500 = (project in file("5.0.0"))
	.enablePlugins(SpongePlugin)
	.dependsOn(katLibShared)
	.settings(commonSettings: _*)
	.settings(spongeApiVersion := "5.0.0")

lazy val katLibV600 = (project in file("6.0.0"))
	.enablePlugins(SpongePlugin)
	.dependsOn(katLibShared)
	.settings(commonSettings: _*)
	.settings(spongeApiVersion := "6.0.0-SNAPSHOT")

lazy val katLibRoot = (project in file("."))
	.settings(publishArtifact := false)
	.disablePlugins(AssemblyPlugin)
	.aggregate(katLibShared, katLibV410, katLibV500, katLibV600)

resolvers += Resolver.defaultLocal