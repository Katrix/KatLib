logLevel := Level.Warn
resolvers += "SpongePowered-Snapshots" at "https://repo-new.spongepowered.org/repository/maven-snapshots"
addSbtPlugin("net.katsstuff" % "sbt-spongyinfo" % "2.0.0-SNAPSHOT")
addSbtPlugin("ch.epfl.lamp" % "sbt-dotty" % "0.5.1")
