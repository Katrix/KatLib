package io.github.katrix.katlib

import java.nio.file.Path

import org.slf4j.Logger
import org.spongepowered.api.Platform.Component
import org.spongepowered.api.Sponge
import org.spongepowered.api.config.ConfigDir
import org.spongepowered.api.event.Listener
import org.spongepowered.api.event.game.state.{GameConstructionEvent, GameInitializationEvent}
import org.spongepowered.api.plugin.{Plugin, PluginContainer}

import com.google.common.reflect.TypeToken
import com.google.inject.Inject

import io.github.katrix.katlib.helper.Implicits.RichOptional
import io.github.katrix.katlib.helper.LogHelper
import io.github.katrix.katlib.lib.LibKatLibPlugin
import io.github.katrix.katlib.persistant.{Config, ConfigValue}

object KatLib {

	final val CompiledAgainst = "6.0.0"
	final val Version         = s"$CompiledAgainst-1.1.0"
	final val ConstantVersion = "6.0.0-1.1.0"
	assert(Version == ConstantVersion)

	private var _plugin: KatLib = _
	implicit def plugin: KatLib = _plugin
}

@Plugin(id = LibKatLibPlugin.Id, name = LibKatLibPlugin.Name, version = KatLib.ConstantVersion, authors = Array("Katrix"))
class KatLib @Inject()(
		logger: Logger,
		@ConfigDir(sharedRoot = true)
		configDir: Path,
		container: PluginContainer
) extends ImplKatPlugin(logger, configDir, container, LibKatLibPlugin.Id) {

	//Not actually used so far
	override def config: Config = new Config {
		override def seq: Seq[ConfigValue[_]] = Seq(version)
		override val version: ConfigValue[String] = ConfigValue("0", TypeToken.of(classOf[String]), "Don't touch this", Seq("version"))
	}

	@Listener
	def gameConstruct(event: GameConstructionEvent): Unit = {
		KatLib._plugin = this
	}

	@Listener
	def gameInit(event: GameInitializationEvent): Unit = {
		Sponge.getPlatform.getContainer(Component.API).getVersion.toOption match {
			case Some(version) if version != KatLib.CompiledAgainst => LogHelper.warn(
				s"KatLib is not compiled against $version. KatLib (and plugins depending on it) might break")(this)
			case None => LogHelper.warn("Could not find API version for Sponge. KatLib (and plugins depending on it) might break")(this)
			case Some(_) =>
		}
		Sponge.getCommandManager.register(this, pluginCmd.commandSpec, pluginCmd.aliases: _*)
	}
}
