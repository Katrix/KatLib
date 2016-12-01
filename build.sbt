lazy val commonSettings = Seq(
	name := s"KatLib-${spongeApiVersion.value}",
	organization := "io.github.katrix",
	version := "1.1.0",
	scalaVersion := "2.11.8",
	assemblyShadeRules in assembly := Seq(
		ShadeRule.rename("scala.**" -> "io.github.katrix.katlib.shade.scala.@1").inAll
	),
	scalacOptions += "-Xexperimental",
	crossPaths := false,

	spongePluginInfo := spongePluginInfo.value.copy(
		id = "katlib",
		name = Some("KatLib"),
		version = Some(s"${spongeApiVersion.value}-${version.value}"),
		authors = Seq("Katrix"),
		dependencies = Set(DependencyInfo("spongeapi", Some(spongeApiVersion.value)))
	),

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

		resolvers += Resolver.url("scalameta", url("http://dl.bintray.com/scalameta/maven"))(Resolver.ivyStylePatterns),
		addCompilerPlugin("org.scalameta" % "paradise" % "3.0.0.132" cross CrossVersion.full),
		scalacOptions += "-Xplugin-require:macroparadise",
		scalacOptions in (Compile, console) := Seq(), //macroparadise plugin doesn't work in repl yet.
		sources in (Compile, doc) := Nil, //macroparadise doesn't work with scaladoc yet.
		libraryDependencies += "org.scalameta" %% "scalameta" % "1.4.0.544"
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