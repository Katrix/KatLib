package io.github.katrix.katlib.persistant

import scala.annotation.{StaticAnnotation, compileTimeOnly}
import scala.language.experimental.macros
import scala.reflect.macros.whitebox

@compileTimeOnly("configTrait can only be used on a trait")
class configTrait(name: String) extends StaticAnnotation {
	def macroTransform(annottees: Any*): Any = macro MacroImpl.createConfig
}
@compileTimeOnly("comment can only be used inside a configTrait trait")
class comment(comment: String) extends StaticAnnotation

object MacroImpl {

	def createConfig(c: whitebox.Context)(annottees: c.Expr[Any]*): c.Expr[Any] = {
		import c.universe._

		val name: String = "TODO"

		val res = annottees.map(_.tree) match {
			case Seq(q"trait $traitName {..$body}") =>
				val q"object $name {..$objBody}" = body.head
				c.info(c.enclosingPosition, name.getClass.toString, force = true)

				def getUsedBody(parentName: Option[RefTree], tree: Tree): Seq[(Tree, Tree)] = {

					def getPathName(name: TermName) = parentName.map(parent => q"$parent.$name").getOrElse(q"$name")

					tree match {
						case q"@_root_.io.github.katrix.katlib.persistant.comment($comment) val $name = $body" =>
							val pathName = getPathName(name)
							val pathStringName = pathName.name.encodedName.toString

							val save = q"cfgRoot.getNode($pathStringName).setComment($comment).setValue(data.$pathName)"
							val impl = q"override val $name = Option(root.getNode($pathStringName)).getOrElse(DefaultConfig.$pathName)"

							Seq((impl, save))
						case q"val $name = $body" =>
							val pathName = getPathName(name)
							val pathStringName = pathName.name.encodedName.toString

							val save = q"cfgRoot.getNode($pathStringName).setValue(data.$pathName)"
							val impl = q"override val $name = Option(root.getNode($pathStringName)).getOrElse(DefaultConfig.$pathName)"

							Seq((impl, save))
						case q"object $name {..$objBody}" =>
							val newParent = getPathName(name)
							objBody.flatMap(getUsedBody(Some(newParent), _))
						case _ => c.abort(tree.pos, "A configTrait can only have normal vals")
					}
				}

				val usedBody = body.flatMap(getUsedBody(None, _))

				def stripComment(tree: Tree): Tree = {
					tree match {
						case q"@_root_.io.github.katrix.katlib.persistant.comment.comment($comment) val $name = $body" => q"val $name = $body"
						case q"object $name {..$body}" =>
							val strippedBody = body.map(stripComment(_))
							q"object $name {..$strippedBody }"
						case _ => tree //We already do other checks that outlaws anything other than val
					}
				}

				val strippedBody = body.map(stripComment(_))

				q"""
					trait $traitName {..$strippedBody}

					object ${traitName.toTermName} {
						import _root_.ninja.leaping.configurate.ConfigurationNode
						import _root_.io.github.katrix.katlib.persistant.ConfigurateBase
						import _root_.io.github.katrix.katlib.KatPlugin
						import _root_.java.nio.file.Path
						import _root_.ninja.leaping.configurate.hocon.{HoconConfigurationLoader => HoconBuilder}

		 				object DefaultConfig extends $traitName {}

						def configImpl(root: ConfigurationNode) = new $traitName {
							..${usedBody.map(_._1)}
						}

			 			def loader(dir: Path, customOptions: HoconBuilder => HoconBuilder)(implicit plugin: KatPlugin) = {
			 				new ConfigurateBase[$traitName](dir, $name, false) {

								override def loadVersionedData(version: String): $traitName = version match {
			 						case "undefined" => configImpl(cfgRoot)
				 					case _ => throw new IllegalStateException
								}

								override def saveData(data: $traitName): Unit = {
			 						..${usedBody.map(_._2)}
								}

								override protected val default = DefaultConfig
			 				}
			 			}
					}
				 """
			case _ => c.abort(c.enclosingPosition, "Annotation @configTrait can be used on traits")
		}


		c.Expr[Any](res)
	}
}