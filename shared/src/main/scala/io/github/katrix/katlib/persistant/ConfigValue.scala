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
import ninja.leaping.configurate.ConfigurationNode
import ninja.leaping.configurate.commented.CommentedConfigurationNode
import ninja.leaping.configurate.objectmapping.ObjectMappingException


/**
	* An object that holds an individual value in the config
	*/
sealed trait ConfigValue[A, NodeType <: ConfigurationNode] {
	type Self <: ConfigValue[A, NodeType]
	def value: A
	def value_=(value: A): Self
	def typeToken: TypeToken[A]
	def path: Seq[String]

	def applyToNode(node: NodeType): Unit
}

final case class DataConfigValue[A](value: A, implicit val typeToken: TypeToken[A], path: Seq[String])
	extends ConfigValue[A, ConfigurationNode] {
	override type Self = DataConfigValue[A]
	override def applyToNode(node: ConfigurationNode): Unit = node.getNode(path: _*).value_=(value)
	override def value_=(value: A): Self = copy(value = value)
}
final case class CommentedConfigValue[A](value: A, implicit val typeToken: TypeToken[A], comment: String, path: Seq[String])
	extends ConfigValue[A, CommentedConfigurationNode] {
	override type Self = CommentedConfigValue[A]
	override def applyToNode(node: CommentedConfigurationNode): Unit = node.getNode(path: _*).setComment(comment).value_=(value)
	override def value_=(value: A): Self = copy(value = value)
}

object ConfigValue {

	def apply[A: TypeToken](value: A, comment: String, path: Seq[String]): CommentedConfigValue[A] = {
		CommentedConfigValue(value, implicitly[TypeToken[A]], comment, path)
	}

	def apply[A: TypeToken](value: A, path: Seq[String]): DataConfigValue[A] = {
		DataConfigValue(value, implicitly[TypeToken[A]], path)
	}

	def apply[A, NodeType <: ConfigurationNode](
			node: NodeType, existing: ConfigValue[A, NodeType])(implicit plugin: KatPlugin): ConfigValue[A, NodeType] = {
		Try(Option(node.getNode(existing.path: _*).getValue(existing.typeToken)).get).map(found => existing.value = found).recover {
			case _: ObjectMappingException =>
				LogHelper.error(s"Failed to deserialize value of ${existing.path.mkString(", ")}, using the default instead")
				existing
			case _: NoSuchElementException =>
				LogHelper.warn(s"No value found for ${existing.path.mkString(", ")}, using default instead")
				existing
		}.get
	}
}