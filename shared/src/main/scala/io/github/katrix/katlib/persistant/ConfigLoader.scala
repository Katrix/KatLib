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

import java.nio.file.Path

import scala.annotation.tailrec

import io.github.katrix.katlib.KatPlugin
import ninja.leaping.configurate.commented.CommentedConfigurationNode
import ninja.leaping.configurate.hocon.HoconConfigurationLoader

abstract class ConfigLoader[A <: Config](
		dir: Path,
		customOptions: HoconConfigurationLoader.Builder => HoconConfigurationLoader.Builder)(
		implicit plugin: KatPlugin
) extends ConfigurateBase[A, CommentedConfigurationNode, HoconConfigurationLoader](
	dir, "config.conf", path => customOptions(HoconConfigurationLoader.builder().setPath(path)).build()) {

	override def saveData(data: A): Unit = {

		@tailrec
		def inner(rest: Seq[CommentedConfigValue[_]]): Unit = {
			if(rest != Nil) {
				val value = rest.head
				value.applyToNode(cfgRoot)
				inner(rest.tail)
			}
		}

		inner(data.seq)
		saveFile()
	}
}
