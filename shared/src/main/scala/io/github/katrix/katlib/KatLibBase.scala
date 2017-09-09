package io.github.katrix.katlib

import io.github.katrix.katlib.helper.LogHelper
import io.github.katrix.katlib.persistant.{CommentedConfigValue, Config}

trait KatLibBase extends KatPlugin {

  //Not actually used so far
  override def config: Config = new Config {
    override def seq: Seq[CommentedConfigValue[_]] = Seq()
  }

  private val SemVer = """(\d+)\.(\d+)\.(\d+).*""".r

  def checkSpongeVersion(spongeVersionOpt: Option[String], expectedVersion: String): Unit = {
    spongeVersionOpt match {
      case Some(spongeVersion) =>
        spongeVersion match {
          case SemVer(majorStr, minorStr, _) =>
            expectedVersion match {
              case SemVer(expectedMajorStr, expectedMinorStr, _) =>
                val major         = majorStr.toInt
                val expectedMajor = expectedMajorStr.toInt

                val minor         = minorStr.toInt
                val expectedMinor = expectedMinorStr.toInt

                if (major != expectedMajor)
                  LogHelper.warn(
                    s"This version of KatLib is not compiled against Sponge API $major.x.x. Stuff might and probably will break"
                  )

                if (minor < expectedMinor)
                  LogHelper.warn(
                    s"This version of KatLib is compiled against $expectedMajor.$expectedMinor.x, but $major.$minor.x was found. Stuff might not work as expected. Consider upgrading your version of Sponge"
                  )

              case _ => LogHelper.warn("This version of KatLib was compiled for an unrecognized version")
            }

          case _ =>
            LogHelper.warn("Unrecognized sponge version. KatLib and plugins depending on it might not work as expected")
        }
      case None =>
        LogHelper.warn("Could not find Sponge version. Stuff might break")
    }
  }
}
