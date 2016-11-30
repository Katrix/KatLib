/*
 * This file is part of KatLib, licensed under the MIT License (MIT).
 *
 * Copyright (c) 2016 Katrix
 *
 * Permission is hereby granted, free of charge, to any person obtaining a copy of this software and
 * associated documentation files (the "Software"), to deal in the Software without restriction,
 * including without limitation the rights to use, copy, modify, merge, publish, distribute,
 * sublicense, and/or sell copies of the Software, and to permit persons to whom the Software is
 * furnished to do so, subject to the following conditions:
 *
 * The above copyright notice and this permission notice shall be included in all copies or
 * substantial portions of the Software.
 *
 * THE SOFTWARE IS PROVIDED "AS IS", WITHOUT WARRANTY OF ANY KIND, EXPRESS OR IMPLIED, INCLUDING BUT
 * NOT LIMITED TO THE WARRANTIES OF MERCHANTABILITY, FITNESS FOR A PARTICULAR PURPOSE AND
 * NONINFRINGEMENT. IN NO EVENT SHALL THE AUTHORS OR COPYRIGHT HOLDERS BE LIABLE FOR ANY CLAIM,
 * DAMAGES OR OTHER LIABILITY, WHETHER IN AN ACTION OF CONTRACT, TORT OR OTHERWISE, ARISING FROM,
 * OUT OF OR IN CONNECTION WITH THE SOFTWARE OR THE USE OR OTHER DEALINGS IN THE SOFTWARE.
 */
package io.github.katrix.katlib.persistant

import java.io.IOException
import java.nio.file.Path

import scala.concurrent.Future
import scala.util.{Failure, Try}

import io.github.katrix.katlib.KatPlugin
import io.github.katrix.katlib.helper.LogHelper
import ninja.leaping.configurate.commented.CommentedConfigurationNode
import ninja.leaping.configurate.hocon.HoconConfigurationLoader

/**
	* Base class for all configurate files
	* Does not hold on to state itself, instead it serializes it to some object.
	* Think of it as a giant serializer for files instead of nodes.
	*
	* @param configDir The path where the file should be saved
	* @param name The name of the file
	* @param data If this file should hold data or not
	* @tparam A What is returned when reading this file
	*/
abstract class ConfigurateBase[A](configDir: Path, name: String, data: Boolean,
		customOptions: HoconConfigurationLoader.Builder => HoconConfigurationLoader.Builder)(implicit plugin: KatPlugin) {

	def this(configDir: Path, name: String, data: Boolean)(implicit plugin: KatPlugin) {
		this(configDir, name, data, identity)
	}

	protected val path     : Path                     = configDir.resolve(s"$name${if(data) ".json"else ".conf"}")
	protected val cfgLoader: HoconConfigurationLoader = customOptions(HoconConfigurationLoader.builder.setPath(path)).build()
	protected var cfgRoot  : CommentedConfigurationNode = loadRoot()

	{
		val parent = path.getParent.toFile
		if(!parent.exists && !parent.mkdirs) {
			LogHelper.error(s"Something went wrong when creating the directory for the file used by ${getClass.getName}")
		}
	}


	/**
		* Loads the data that this [[ConfigurateBase]] is responsible for.
		*/
	final def loadData(): A = {
		val config = Option(versionNode.getString) match {
			case None =>
				LogHelper.error(s"Could not find version data for $name. Using default instead.")
				default
			case Some(version) => loadVersionedData(version)
		}

		saveData(config)
		config
	}

	/**
		* Loads data given some version
		*/
	protected def loadVersionedData(version: String): A

	/**
		* The default data.
		*/
	protected val default: A

	/**
		* Saves the specified data
		*/
	protected def saveData(data: A): Unit

	protected def loadRoot(): CommentedConfigurationNode = {
		Try(cfgLoader.load()).recover {
			case e: IOException =>
				LogHelper.error(
					s"""Could not load configurate file for ${plugin.container.name}.
						 |If this is the first time starting the plugin this is normal""".stripMargin, e)
				cfgLoader.createEmptyNode()
		}.get
	}

	protected def saveFile(): Future[Unit] = {
		import scala.concurrent.ExecutionContext.Implicits.global
		val future = Future(cfgLoader.save(cfgRoot))
		future.onComplete {
			case Failure(e) => e.printStackTrace()
			case _ =>
		}

		future
	}

	protected def versionNode: CommentedConfigurationNode = cfgRoot.getNode("version")
}
