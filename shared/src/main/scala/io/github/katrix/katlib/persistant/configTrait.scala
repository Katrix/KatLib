package io.github.katrix.katlib.persistant

import scala.annotation.{StaticAnnotation, compileTimeOnly}
import scala.collection.immutable.Seq
import scala.meta._

@compileTimeOnly("@comment can only be used inside a @cfg class")
class comment(comment: String) extends StaticAnnotation

@compileTimeOnly("@cfg can only be used on a class")
class cfg(name: String) extends StaticAnnotation {

  inline def apply(defn: Any): Any = meta {

    val configName = this match {
      case q"new $_(${arg @ Lit(name: String)})" if name.contains(".") => arg
      case q"new $_($_)" => abort("Name needs to have an extension")
      case _             => abort("@cfg needs a name")
    }

    val res = defn match {
      case q"class $className {..$classBody}" =>
        //Collects all inner classes and their bodies as a map using the structure of Type.Name as a key
        def innerClasses(stats: Seq[Stat]): Map[String, (Type.Name, Seq[Stat])] = stats.collect {
          case q"class $className {..$classBody}" => (className.structure, (className, classBody))
        }.toMap

        //Creates a load and save body for inner classes
        def createClassBody(valName: Pat.Var.Term, className: Type.Name, classBody: Seq[Stat], parents: Seq[Term.Name]): (Some[Stat], Seq[Stat]) = {
          val newParents       = parents :+ valName.name
          val ctorClassName    = Ctor.Ref.Name(className.value)
          val newInnerClasses  = innerClasses(classBody)
          val (loadBody, save) = classBody.map((stat: Stat) => getUsedBody(stat, newParents, newInnerClasses)).unzip

          val load = q"override val $valName: $className = new $ctorClassName {..${loadBody.flatten} }"

          (Some(load), save.flatten)
        }

        //Creates a load and save body for a statement
        def getUsedBody(stat: Stat, parents: Seq[Term.Name], innerClasses: Map[String, (Type.Name, Seq[Stat])]) = {

          //Creates a Ref term comprised of different names
          def selectNames(names: Seq[Term.Name]): Term.Ref with Pat = names match {
            case Seq(first)                     => q"$first"
            case Seq(first, second)             => q"$first.$second"
            case Seq(first, second, rest @ _ *) => rest.foldLeft(q"$first.$second")((acc, parent) => q"$acc.$parent")
          }

          def getRefParents(name: Term.Name): Term.Ref with Pat = selectNames(parents :+ name)
          def getSelectRoot(name: Term.Name, root: Term.Name): Term.Select = selectNames(root +: parents :+ name).asInstanceOf[Term.Select]

          def createLoadSaveStat(comment: Option[Term.Arg], valName: Pat.Var.Term, tpe: Type, body: Term): (Some[Stat], Seq[Stat]) = {
            val name     = valName.name
            val nodeName = Lit.String(getRefParents(name).syntax.replace("`", ""))

            val dataPath    = getSelectRoot(name, Term.Name("data"))
            val defaultPath = getSelectRoot(name, Term.Name("DefaultConfig"))

            val typeToken = tpe match {
              case clazz: Type.Name => q"TypeToken.of(classOf[$clazz])"
              case _ => q"new TypeToken[$tpe] {}"
            }

            val save = comment match {
              case Some(foundComment) => q"cfgRoot.getNode($nodeName.split('.'): _*).setComment($foundComment).setValue($dataPath)"
              case None               => q"cfgRoot.getNode($nodeName.split('.'): _*).setValue($dataPath)"
            }

            val load =
              q"override val $valName: $tpe = Option(cfgRoot.getNode($nodeName.split('.'): _*).getValue($typeToken)).getOrElse($defaultPath)"

            (Some(load), Seq(save))
          }

          def getTyping(tpe: Option[Type], body: Term): Type = tpe.getOrElse(abort(body.pos, "Could not find type"))

          stat match {
            case q"@comment($comment) val ${name: Pat.Var.Term}: $tpe = $body" =>
              val typing = getTyping(tpe.asInstanceOf[Option[Type]], body)
              if (innerClasses.contains(typing.structure)) abort(stat.pos, "A inner class can't have a comment")
              createLoadSaveStat(Some(comment), name, typing, body)
            case q"val ${name: Pat.Var.Term}: ${tpe} = $body" =>
              val typing = getTyping(tpe.asInstanceOf[Option[Type]], body)
              innerClasses.get(typing.structure) match {
                case Some((tName, stats)) => createClassBody(name, tName, stats, parents)
                case None                 => createLoadSaveStat(None, name, typing, body)
              }

            case q"class $_ {..$_}" => (None, Nil)
            case _                  => abort(stat.pos, s"A @cfg can only have normal vals.\n${stat.syntax}")
          }
        }

        //Strips the @comment annotation from the class
        def stripComment(tree: Stat): Stat =
          tree match {
            case q"@comment($_) val ${name: Pat.Var.Term}: $tpe = $body" => q"val $name: $tpe = $body"
            case q"class $name {..$body}" =>
              val strippedBody = body.map(stripComment)
              q"class $name {..$strippedBody }"
            case _ => tree //We already do other checks that outlaws anything other than val
          }

        val (loadBody, saveBody) = classBody.map((stat: Stat) => getUsedBody(stat, Nil, innerClasses(classBody))).unzip
        val strippedBody         = classBody.map(stripComment)

        val classNameString = className.value
        val objClassName    = Term.Name(classNameString)
        val ctorClassName   = Ctor.Ref.Name(classNameString)
        val defaultTemplate = Template(Nil, Seq(ctorClassName), Term.Param(Nil, Name.Anonymous(), None, None), None)

        q"""
					class $className {..$strippedBody}

					object $objClassName {
						import _root_.ninja.leaping.configurate.commented.CommentedConfigurationNode
						import _root_.ninja.leaping.configurate.hocon.{HoconConfigurationLoader => HoconLoader}
						import _root_.io.github.katrix.katlib.persistant.ConfigurateBase
						import _root_.io.github.katrix.katlib.KatPlugin
						import _root_.java.nio.file.Path
						import _root_.com.google.common.reflect.TypeToken

		 				val DefaultConfig = new $defaultTemplate

			 			def loader(dir: Path, configBuilder: Path => HoconLoader, beforeLoad: CommentedConfigurationNode => Unit = _ => ())(
			 					implicit plugin: KatPlugin): ConfigurateBase[$className, CommentedConfigurationNode, HoconLoader] = {
			 				new ConfigurateBase[$className, CommentedConfigurationNode, HoconLoader](dir, $configName, configBuilder) {

								override def loadData: $className = {
								  beforeLoad(cfgRoot)
                  new $ctorClassName {
									  ..${loadBody.flatten}
								  }
                }
								override def saveData(data: $className): Unit = {
			 						..${saveBody.flatten}
									saveFile()
								}

                def reload(): Unit = {
                  cfgRoot = loadRoot()
                }
			 				}
			 			}
					}
				 """
      case _ => abort("@cfg can only be used on a concrete class")
    }

    //println(res.syntax)
    res
  }
}
