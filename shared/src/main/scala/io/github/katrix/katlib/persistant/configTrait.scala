package io.github.katrix.katlib.persistant

import scala.annotation.{StaticAnnotation, compileTimeOnly}
import scala.collection.immutable.Seq
import scala.language.experimental.macros
import scala.meta._
import scala.meta.dialects.Paradise211

@compileTimeOnly("comment can only be used inside a @configTrait trait")
class comment(comment: String) extends StaticAnnotation

@compileTimeOnly("@configTrait can only be used on a trait")
class configTrait(name: String) extends StaticAnnotation {

	inline def apply(defn: Any): Any = meta {

		val configName = arg""""TODO""""

		val res = defn match {
			case q"trait $traitName {..$traitBody}" =>

				def innerTraits(stats: Seq[Stat]): Seq[(Type.Name, Seq[Stat])] = {
					stats.collect {
						case q"trait $traitName {..$traitBody}" => traitName -> traitBody
					}
				}

				def createTraitBody(varName: Pat.Var.Term, traitName: Type.Name, traitBody: Seq[Stat], parents: Seq[Term.Name]): (Some[Stat], Some[Stat], Seq[Stat]) = {
					val newParents = parents :+ varName.name
					val ctorTraitName = Ctor.Ref.Name(traitName.value)
					val (implConfigBody, implDefaultBody, save) = traitBody.map(getUsedBody(newParents, _, innerTraits(traitBody))).unzip3

					val configimpl = q"val $varName: $traitName = new $ctorTraitName {..${implConfigBody.flatten} }"
					val defaultImpl = q"val $varName: $traitName = new $ctorTraitName {..${implDefaultBody.flatten} }"

					(Some(configimpl), Some(defaultImpl), save.flatten)
				}

				def getUsedBody(parents: Seq[Term.Name], stat: Stat, innerSections: Seq[(Type.Name, Seq[Stat])]): (Option[Stat], Option[Stat], Seq[Stat]) = {

					def selectNamesLeft(names: Seq[Term.Name]): Term.Ref with Pat = names match {
						case Seq(first) => q"$first"
						case Seq(first, second) => q"$first.$second"
						case Seq(first, second, rest @ _*) => rest.foldLeft(q"$first.$second")((acc, parent) => q"$acc.$parent")
					}

					def getRefParents(name: Term.Name): Term.Ref with Pat = selectNamesLeft(parents :+ name)
					def getSelectRoot(name: Term.Name, root: Term.Name): Term.Select = selectNamesLeft(root +: parents :+ name).asInstanceOf[Term.Select]

					def createBody(comment: Option[Term.Arg], varName: Pat.Var.Term, tpe: Type, body: Term): (Some[Stat], None.type, Seq[Stat]) = {
						val name = varName.name
						val nodeName = Lit(getRefParents(name).syntax)

						val dataPath = getSelectRoot(name, Term.Name("data"))
						val defaultPath = getSelectRoot(name, Term.Name("DefaultConfig"))

						val typeToken = tpe match {
							case ident: Type.Name => q"TypeToken.of(classOf[$ident])"
							case _ => q"new TypeToken[$tpe] {}"
						}

						val save = comment match {
							case Some(foundComment) => q"cfgRoot.getNode($nodeName.split('.'): _*).setComment($foundComment).setValue($dataPath)"
							case None => q"cfgRoot.getNode($nodeName.split('.'): _*).setValue($dataPath)"
						}
						val configimpl = q"override val $varName: $tpe = Option(root.getNode($nodeName.split('.'): _*).getValue($typeToken)).getOrElse($defaultPath)"

						(Some(configimpl), None, Seq(save))
					}

					def getTyping(tpe: Option[Type], body: Term): Type = tpe.getOrElse(abort(body.pos, "Could not find type"))

					stat match {
						case q"@comment($comment) val $name: $tpe = $body" =>
							createBody(Some(comment), name.asInstanceOf[Pat.Var.Term], getTyping(tpe.asInstanceOf[Option[Type]], body), body)
						case q"val $name: $tpe = $body" =>
							createBody(None, name.asInstanceOf[Pat.Var.Term], getTyping(tpe.asInstanceOf[Option[Type]], body), body)
						case q"val $name: $tpe" if innerSections.exists(_._1.structure == tpe.structure) => //Yuck
							val (traitName, traitBody) = innerSections.find(_._1.structure == tpe.structure).get
							createTraitBody(name, traitName, traitBody, parents)
						case q"trait $_ {..$_}" => (None, None,Seq())
						case _ => abort(stat.pos, s"A configTrait can only have normal vals.\n${stat.syntax}")
					}
				}

				def stripComment(tree: Stat): Stat = {
					tree match {
						case q"@comment($_) val $name: $tpe = $body" => q"val ${name.asInstanceOf[Pat.Var.Term]}: $tpe = $body"
						case q"trait $name {..$body}" =>
							val strippedBody = body.map(stripComment)
							q"trait $name {..$strippedBody }"
						case _ => tree //We already do other checks that outlaws anything other than val
					}
				}

				val (implConfigBody, implDefaultBody, saveBody) = traitBody.map(getUsedBody(Seq(), _, innerTraits(traitBody))).unzip3
				val strippedBody = traitBody.map(stripComment)

				val traitNameString = traitName.value
				val objTraitName = Term.Name(traitNameString)
				val ctorTraitName = Ctor.Ref.Name(traitNameString)

				q"""
					trait $traitName {..$strippedBody}

					object $objTraitName {
						import _root_.ninja.leaping.configurate.ConfigurationNode
						import _root_.ninja.leaping.configurate.hocon.{HoconConfigurationLoader => HoconBuilder}
						import _root_.io.github.katrix.katlib.persistant.ConfigurateBase
						import _root_.io.github.katrix.katlib.KatPlugin
						import _root_.java.nio.file.Path
						import _root_.com.google.common.reflect.TypeToken

		 				object DefaultConfig extends $ctorTraitName {
			 				..${implDefaultBody.flatten}
		 				}

						def configImpl(root: ConfigurationNode) = new $ctorTraitName {
							..${implConfigBody.flatten}
						}

			 			def loader(dir: Path, customOptions: HoconBuilder => HoconBuilder)(implicit plugin: KatPlugin): ConfigurateBase[$traitName] = {
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
			case _ => abort("@configTrait can only be used on a trait")
		}

		println(res.syntax)
		res
	}
}