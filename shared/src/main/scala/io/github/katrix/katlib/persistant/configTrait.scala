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

		val configName: String = "TODO"

		val res = annottees.map(_.tree) match {
			case Seq(q"trait $traitName {..$traitBody}") =>
				val q"val $valName: $tpe = $valBody" = traitBody.head
				c.info(c.enclosingPosition, tpe.getClass.toString, force = true)

				def getUsedBody(parents: Seq[TermName], tree: Tree): (Tree, Seq[Tree]) = {

					def getRefTreeParents(name: TermName): RefTree = parents match {
						case Seq() => q"$name"
						case Seq(first) => q"$first.$name"
						case Seq(first, second, rest @ _*) =>
							val allParents = rest.foldLeft(Select(q"$first", second))((acc, parent) => Select(acc, parent))
							Select(allParents, name)
					}

					def getSelectRoot(name: TermName, root: TermName): Select = {
						if(parents.nonEmpty) {
							val allParents = parents.drop(1).foldLeft(Select(q"$root", parents.head))((acc, parent) => Select(acc, parent))
							Select(allParents, name)
						}
						else Select(q"$root", name)
					}

					def createBody(comment: Option[Tree], name: TermName, tpe: Tree, body: Tree): (Tree, Seq[Tree]) = {
						val refTree = getRefTreeParents(name)

						//TODO: Not the best
						val pathStringName = if(refTree.qualifier.nonEmpty) refTree.qualifier.toString() + "." + refTree.name.decodedName.toString
						else refTree.name.decodedName.toString

						val dataPath = getSelectRoot(name, "data")
						val defaultPath = getSelectRoot(name, "DefaultConfig")

						val typeToken = tpe match {
							case ident: Ident => q"TypeToken.of(classOf[$ident])"
							case _ => q"new TypeToken[$tpe] {}"
						}

						val save = comment match {
							case Some(foundComment) => q"cfgRoot.getNode($pathStringName.split('.'): _*).setComment($foundComment).setValue($dataPath)"
							case None => q"cfgRoot.getNode($pathStringName.split('.'): _*).setValue($dataPath)"
						}
						val impl = if(parents.isEmpty) {
							q"override val $name = Option(root.getNode($pathStringName.split('.'): _*).getValue($typeToken)).getOrElse($defaultPath)"
						}
						else q"val $name = Option(root.getNode($pathStringName.split('.'): _*).getValue($typeToken)).getOrElse($defaultPath)"

						(impl, Seq(save))
					}

					tree match {
						case q"@_root_.io.github.katrix.katlib.persistant.comment($comment) val $name: $tpe = $body" =>
							createBody(Some(comment), name, tpe, body)
						case q"val $name: $tpe = $body" => createBody(None, name, tpe, body)
						case q"object $name {..$objBody}" =>
							val newParents = parents :+ name
							val children = objBody.map(getUsedBody(newParents, _))
							val (implChildren, saveChildren) = children.unzip
							val impl = q"object $name {..$implChildren}"
							(impl, saveChildren.flatten)
						case _ => c.abort(tree.pos, "A configTrait can only have normal vals")
					}
				}

				val (implBody, saveBody) = traitBody.map(getUsedBody(Seq(), _)).unzip

				def stripComment(tree: Tree): Tree = {
					tree match {
						case q"@_root_.io.github.katrix.katlib.persistant.comment.comment($comment) val $name: $tpe = $body" => q"val $name: $tpe = $body"
						case q"object $name {..$body}" =>
							val strippedBody = body.map(stripComment(_))
							q"object $name {..$strippedBody }"
						case _ => tree //We already do other checks that outlaws anything other than val
					}
				}

				val strippedBody = traitBody.map(stripComment(_))

				q"""
					trait $traitName {..$strippedBody}

					object ${traitName.toTermName} {
						import _root_.ninja.leaping.configurate.ConfigurationNode
						import _root_.ninja.leaping.configurate.hocon.{HoconConfigurationLoader => HoconBuilder}
						import _root_.io.github.katrix.katlib.persistant.ConfigurateBase
						import _root_.io.github.katrix.katlib.KatPlugin
						import _root_.java.nio.file.Path
						import _root_.com.google.common.reflect.TypeToken

		 				object DefaultConfig extends $traitName {}

						def configImpl(root: ConfigurationNode) = new $traitName {
							..$implBody
						}

			 			def loader(dir: Path, customOptions: HoconBuilder => HoconBuilder)(implicit plugin: KatPlugin) = {
			 				new ConfigurateBase[$traitName](dir, $configName, false) {

								override def loadVersionedData(version: String): $traitName = version match {
			 						case "undefined" => configImpl(cfgRoot)
				 					case _ => throw new IllegalStateException
								}

								override def saveData(data: $traitName): Unit = {
			 						..${saveBody.flatten}
								}

								override protected val default = DefaultConfig
			 				}
			 			}
					}
				 """
			case _ => c.abort(c.enclosingPosition, "Annotation @configTrait can be used on traits")
		}

		c.info(c.enclosingPosition, showCode(res), force = true)
		//c.info(c.enclosingPosition, res.toString(), force = true)

		c.Expr[Any](res)
	}
}