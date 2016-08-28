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

import scala.util.Try

import com.google.common.reflect.TypeToken

import io.github.katrix.katlib.KatPlugin
import io.github.katrix.katlib.helper.Implicits.RichConfigurationNode
import io.github.katrix.katlib.helper.LogHelper
import ninja.leaping.configurate.commented.CommentedConfigurationNode
import ninja.leaping.configurate.objectmapping.ObjectMappingException

case class ConfigValue[A](value: A, implicit val typeToken: TypeToken[A], comment: String, path: Seq[String]) {

	def setNode(node: CommentedConfigurationNode): Unit = {
		node.getNode(path: _*).setComment(comment).value_=(value)
	}
}

object ConfigValue {

	def apply[A: TypeToken](value: A, comment: String, path: Seq[String]): ConfigValue[A] = {
		new ConfigValue(value, implicitly[TypeToken[A]], comment, path)
	}

	def apply[A](node: CommentedConfigurationNode, existing: ConfigValue[A])(implicit plugin: KatPlugin): ConfigValue[A] = {
		Try(node.getNode(existing.path: _*).getValue(existing.typeToken)).map(value => existing.copy(value = value)).recover {
			case e: ObjectMappingException =>
				LogHelper.info(s"Failed to get value of ${existing.path}, using default instead")
				existing
		}.get
	}
}