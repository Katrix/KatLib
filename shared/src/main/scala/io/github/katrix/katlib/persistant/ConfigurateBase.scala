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
import ninja.leaping.configurate.ConfigurationNode
import ninja.leaping.configurate.loader.ConfigurationLoader

abstract class ConfigurateBase[A, NodeType <: ConfigurationNode, LoaderType <: ConfigurationLoader[NodeType]](
    configDir: Path,
    name: String,
    pathToLoader: Path => LoaderType
)(implicit plugin: KatPlugin) {

  protected val path:      Path       = configDir.resolve(name)
  protected val cfgLoader: LoaderType = pathToLoader(path)
  protected var cfgRoot:   NodeType   = loadRoot()

  {
    val file = path.toAbsolutePath.toFile
    file.getParentFile.mkdirs()
    file.createNewFile()
  }

  protected def loadRoot(): NodeType =
    Try(cfgLoader.load()).recover {
      case e: IOException =>
        LogHelper.error(
          s"""Could not load configurate file for ${plugin.container.name}.
             |If this is the first time starting the plugin this is normal""".stripMargin, e)
        cfgLoader.createEmptyNode()
    }.get

  def loadData:          A
  def saveData(data: A): Unit

  protected def saveFile(): Future[Unit] = {
    import scala.concurrent.ExecutionContext.Implicits.global
    val future = Future(cfgLoader.save(cfgRoot))
    future.onComplete {
      case Failure(e) => e.printStackTrace()
      case _          =>
    }

    future
  }

}
