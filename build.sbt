lazy val commonSettings = Seq(
	organization := "io.github.katrix",
	scalaVersion := "2.11.8",
	resolvers += "SpongePowered" at "https://repo.spongepowered.org/maven",
	//resolvers += Resolver.sonatypeRepo("releases"),
	//addCompilerPlugin("org.spire-math" %% "kind-projector" % "0.8.0"),
	assemblyShadeRules in assembly := Seq(
		ShadeRule.rename("scala.**" -> "io.github.katrix.katlib.shade.scala.@1").inAll
	),
	scalacOptions += "-Xexperimental",
	crossPaths := false
)

lazy val katLibShared = project in file("shared") settings (commonSettings: _*) settings(
	name := "KatLib",
	version := "1.0.0",
	assembleArtifact := false,
	//Default version, needs to build correctly against all supported versions
	libraryDependencies += "org.spongepowered" % "spongeapi" % "4.1.0" % "provided"
	/*
	libraryDependencies ++= Seq( //TODO: Add shade rule
		"org.typelevel" %% "cats-kernel" % "0.7.0",
		"org.typelevel" %% "cats-core" % "0.7.0",
		"org.typelevel" %% "cats-macros" % "0.7.0",
		"org.typelevel" %% "cats-free" % "0.7.0"
	)
	*/
	)

lazy val katLibV410 = project in file("4.1.0") dependsOn katLibShared settings (commonSettings: _*) settings(
	name := "KatLib-4.1.0",
	version := "1.0.0",
	libraryDependencies += "org.spongepowered" % "spongeapi" % "4.1.0" % "provided"
	)

lazy val katLibV500 = project in file("5.0.0") dependsOn katLibShared settings (commonSettings: _*) settings(
	name := "KatLib-5.0.0",
	version := "1.0.0",
	libraryDependencies += "org.spongepowered" % "spongeapi" % "5.0.0-SNAPSHOT" % "provided"
	)

lazy val katLibRoot = project in file(".") settings (publishArtifact := false) disablePlugins AssemblyPlugin aggregate
	(katLibShared, katLibV410, katLibV500)