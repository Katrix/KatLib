package net.katsstuff.katlib.command

import org.spongepowered.api.command.CommandSource
import org.spongepowered.api.service.pagination.PaginationList
import org.spongepowered.api.text.Text
import org.spongepowered.api.text.action.TextActions

import net.katsstuff.scammander.sponge._
import net.katsstuff.katlib.helper.Implicits._
import org.spongepowered.api.text.format.TextColors._
import org.spongepowered.api.text.format.TextStyles._

import net.katsstuff.katlib.KatPlugin

trait KatLibCommands extends SpongeBaseAll {

  implicit def plugin: KatPlugin

  override type Title = Text

  private val Branch = "├─"
  private val Line   = "│"
  private val End    = "└─"

  override def sendMultipleCommandHelp(
      title: Text,
      source: CommandSource,
      commands: Set[ChildCommand[_, _]]
  ): CommandStep[CommandSuccess] = {
    val pages = PaginationList.builder()

    val helpTexts = commands.toSeq
      .filter(_.command.testPermission(source))
      .sortBy(_.aliases.head)
      .flatMap { child =>
        createTreeCommandHelp(
          source,
          child.aliases.mkString("/", "|", ""),
          s"/${child.aliases.head}",
          child.command,
          detail = false
        )
      }

    pages.title(title).contents(helpTexts: _*).sendTo(source)
    Command.successStep()
  }

  override def sendCommandHelp(
      title: Text,
      source: CommandSource,
      command: StaticChildCommand[_, _],
      path: List[String]
  ): CommandStep[CommandSuccess] = {
    if (command.testPermission(source)) {
      val commandName = path.mkString("/", " ", "")
      val pages       = PaginationList.builder()
      pages
        .title(title)
        .contents(createTreeCommandHelp(source, commandName, commandName, command, detail = true): _*)
        .sendTo(source)
      Command.successStep()
    } else Command.errorStep("You don't have the permission to see the help for this command")
  }

  def createTreeCommandHelp(
      source: CommandSource,
      commandName: String,
      fullCommandName: String,
      command: StaticChildCommand[_, _],
      detail: Boolean,
      indent: Int = 0,
      isIndentEnd: Boolean = false
  ): Seq[Text] = {
    val usage = command.getUsage(source)

    val helpBase =
      t"$GREEN$UNDERLINE$commandName $usage".toBuilder.onClick(TextActions.suggestCommand(fullCommandName)).build()

    val commandHelp        = command.getHelp(source).toOption
    val commandDescription = command.getShortDescription(source).toOption

    val withHover =
      commandDescription.fold(helpBase)(desc => helpBase.toBuilder.onHover(TextActions.showText(t"$desc")).build())

    val withExtra = if (detail) {
      commandHelp.orElse(commandDescription).fold(withHover)(desc => t"$withHover - $desc")
    } else {
      commandDescription.fold(withHover)(desc => t"$withHover - $desc")
    }

    val children = command.command.children.toSeq.sortBy(_.aliases.head)
    val childHelp = if (children.nonEmpty) {

      val childrenTopHelp = children.init.flatMap {
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
      childrenTopHelp ++ lastChildHelp
    } else Nil

    if (indent == 1) {
      val piece    = if (isIndentEnd) End else Branch
      val indented = t"$piece $withExtra"

      indented +: childHelp
    } else if (indent != 0) {
      val end      = if (isIndentEnd) End else Branch
      val spaces   = (indent - 1) * 2
      val space    = " " * spaces
      val indented = t"$Line$space$end $withExtra"

      indented +: childHelp
    } else withExtra +: childHelp
  }
}
