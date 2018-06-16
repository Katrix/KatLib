package net.katstuff.katlib.command

import scala.language.implicitConversions

import cats.kernel.Monoid
import cats.syntax.all._
import cats.{FlatMap, ~>}
import net.katsstuff.minejson.text.{Text, _}
import net.katsstuff.scammander
import net.katsstuff.scammander.{HelpCommands, ScammanderBaseAll}
import net.katstuff.katlib.algebras.{Localized, Pagination}

abstract class KatLibCommands[F[_]: FlatMap, G[_], Page: Monoid, CommandSource, Player, User](
    val pagination: Pagination.Aux[F, CommandSource, Page],
    val FtoG: F ~> G,
    val localized: Localized[F, CommandSource]
) extends ScammanderBaseAll[G] with HelpCommands[G] {

  override type RootSender = CommandSource

  /**
    * Helper for creating an alias when registering a command.
    */
  object KAlias {
    def apply(first: String, aliases: String*): Seq[String] = first +: aliases
  }

  /**
    * Helper for creating a alias when registering a command.
    */
  object KPermission {
    def apply(perm: String): Some[String] = Some(perm)
    val none:                None.type    = None
  }

  /**
    * Helper for creating a help when registering a command.
    */
  object KHelp {
    def apply(f: CommandSource => Text): CommandSource => Option[Text] = f andThen Some.apply
    def apply(text: Text):               CommandSource => Option[Text] = _ => Some(text)
    val none:                            CommandSource => None.type    = _ => None
  }

  /**
    * Helper for creating an description when registering a command.
    */
  object KDescription {
    def apply(f: CommandSource => Text): CommandSource => Option[Text] = f andThen Some.apply
    def apply(text: Text):               CommandSource => Option[Text] = _ => Some(text)
    val none:                            CommandSource => None.type    = _ => None
  }

  implicit def playerParam: Parameter[Player]

  implicit def userParam: Parameter[Set[User]]

  implicit def playerValidator: UserValidator[Player]

  implicit def userValidator: UserValidator[User]

  implicit def commandOps(
      command: ComplexCommand
  ): CommandSyntax[G, CommandSource, RunExtra, TabExtra, Result, StaticChildCommand]

  private val pageOps = pagination.pageOperations

  override type Title = Text

  private val Branch = "├─"
  private val Line   = "│"
  private val End    = "└─"

  def testPermission(command: StaticChildCommand, source: CommandSource): G[Boolean]

  def commandUsage(command: StaticChildCommand, source: CommandSource): G[Text]

  def commandHelp(command: StaticChildCommand, source: CommandSource): G[Option[Text]]

  def commandDescription(command: StaticChildCommand, source: CommandSource): G[Option[Text]]

  override def sendMultipleCommandHelp(
      title: Text,
      source: CommandSource,
      commands: Set[ChildCommand]
  ): G[CommandSuccess] = {
    import cats.instances.list._

    for {
      xs <- commands.toList.traverse(c => testPermission(c.command, source).tupleRight(c))
      helpTexts <- xs.collect { case (test, c) if test => c }.sortBy(_.aliases.head).flatTraverse { child =>
        createTreeCommandHelp(
          source,
          child.aliases.mkString("/", "|", ""),
          s"/${child.aliases.head}",
          child.command,
          detail = false
        )
      }
      helpPage = pageOps.setTitle(title) |+| pageOps.setContent(helpTexts)
      _ <- FtoG(pagination.sendPage(helpPage, source))
    } yield Command.success()
  }

  override def sendCommandHelp(
      title: Text,
      source: CommandSource,
      command: StaticChildCommand,
      path: List[String]
  ): G[CommandSuccess] = {
    val commandName = path.mkString("/", " ", "")
    F.ifM(testPermission(command, source))(
      createTreeCommandHelp(source, commandName, commandName, command, detail = true).flatMap { helpTexts =>
        val page = pageOps.setTitle(title) |+| pageOps.setContent(helpTexts)
        FtoG(pagination.sendPage(page, source).as(Command.success()))
      },
      Command.errorF("You don't have the permission to see the help for this command")
    )
  }

  def createTreeCommandHelp(
      source: CommandSource,
      commandName: String,
      fullCommandName: String,
      command: StaticChildCommand,
      detail: Boolean,
      indent: Int = 0,
      isIndentEnd: Boolean = false
  ): G[List[Text]] =
    for {
      usage       <- commandUsage(command, source)
      help        <- commandHelp(command, source)
      description <- commandDescription(command, source)
      res <- {
        //This can techically blow the stack as we're not using recusion methods, but I'm too lazy to fix it
        //Doubt a command tree will go that deep anyway

        import cats.instances.list._
        val helpBase = t"$Green$Underlined$commandName $usage".onClick = ClickAction.SuggestCommand(fullCommandName)

        val withHover = description.fold(helpBase)(desc => helpBase.onHover = HoverAction.ShowText(t"$desc"))

        val withExtra =
          if (detail) help.orElse(description).fold(withHover)(desc => t"$withHover - $desc")
          else description.fold(withHover)(desc => t"$withHover - $desc")

        val children = command.command.children.toSeq.sortBy(_.aliases.head)
        val childHelp = if (children.nonEmpty) {

          val childrenTopHelp = children.init.toList.flatTraverse {
            case ChildCommand(aliases, childCommand) =>
              createTreeCommandHelp(
                source,
                aliases.mkString("|"),
                s"$fullCommandName ${aliases.head}",
                childCommand,
                detail = false,
                indent = indent + 1
              )
          }
          val lastChild = children.last
          val lastChildHelp = createTreeCommandHelp(
            source,
            lastChild.aliases.mkString("|"),
            s"$fullCommandName ${lastChild.aliases.head}",
            lastChild.command,
            detail = false,
            indent = indent + 1,
            isIndentEnd = true
          )
          F.map2(childrenTopHelp, lastChildHelp)(_ ++ _)
        } else F.pure(Nil: List[Text])

        if (indent == 1) {
          val piece    = if (isIndentEnd) End else Branch
          val indented = F.pure(t"$piece $withExtra")

          F.map2(indented, childHelp)(_ :: _)
        } else if (indent != 0) {
          val end      = if (isIndentEnd) End else Branch
          val spaces   = (indent - 1) * 2
          val space    = " " * spaces
          val indented = F.pure(t"$Line$space$end $withExtra")

          F.map2(indented, childHelp)(_ :: _)
        } else F.map2(F.pure(withExtra), childHelp)(_ :: _)
      }
    } yield res
}

trait CommandSyntax[G[_], RootSender, RunExtra, TabExtra, Result, StaticChildCommand] {
  type ChildCommand = scammander.ComplexChildCommand[G, StaticChildCommand]

  def toChild(
      aliases: Set[String],
      permission: Option[String] = None,
      help: RootSender => Option[String] = _ => None,
      description: RootSender => Option[String] = _ => None
  ): ChildCommand
}
