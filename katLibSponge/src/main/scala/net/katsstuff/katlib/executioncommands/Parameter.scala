package net.katsstuff.katlib.executioncommands

import org.spongepowered.api.command.CommandCause
import org.spongepowered.api.command.parameter.managed.{ValueCompleter, ValueParser}
import org.spongepowered.api.command.registrar.tree.{ClientCompletionKey, CommandTreeNode}

trait Parameter[A]:
  //type ParsedType

  //def parser: Parser[ParsedType]

  //def process(source: CommandCause, parsed: ParsedType): Either[CommandFailureNEL, A]

  //def parse(source: CommandCause, input: String): ParseResult[Either[CommandFailureNEL, A]] =
  //  parser.parseOnly(input).map(process(source, _))

  //def suggestions(source: CommandCause): StateT[List, List[RawCmdArg], Seq[String]]
  
  def optimisticUsage: Usage
  
  def usage(cause: CommandCause): Usage = optimisticUsage

  def named(newName: String): Parameter[A] = ??? // new Parameter.NamedParameter(this, newName)

  def ~[B](other: Parameter[B]): Parameter[(A, B)] = ??? //new Parameter.TupleParameter[A, B](this, other)

  def ~>[B](other: Parameter[B]): Parameter[B] = (this ~ other).map(_._2)

  def <~[B](other: Parameter[B]): Parameter[A] = (this ~ other).map(_._1)

  def ||[B](other: Parameter[B]): Parameter[Either[A, B]] = ??? // new Parameter.EitherParameter[A, B](this, other)

  def |(other: Parameter[A]): Parameter[A] = ??? //new Parameter.FallbackParameter[A](this, other)

  def map[B](f: A => B): Parameter[B] = emap(a => Right(f(a)))

  def emap[B](f: A => Either[CommandFailureNEL, B]): Parameter[B] = Parameter.MappedParameter(this, f)

  def defaultsTo[B](default: B)(implicit ev: A =:= Option[B]): Parameter[B] = map(a => ev(a).getOrElse(default))

object Parameter:
  
  class FromSpongeParameter[A, K <: CommandTreeNode.Argument[K]](
    key: String,
    parser: ValueParser[A], 
    completer: ValueCompleter,
    completionKey: ClientCompletionKey[K]
  ) extends Parameter[A] {
    override def optimisticUsage: Usage = Usage.simpleRequired(key, completionKey)
  }
  
  //def singleParameter[A]
  
  //class SingleParameter[A]
  
  //class TupleParameter[A, B](val first: Parameter[A], val second: Parameter[B]) extends Parameter[(A, B)]
  
  //class EitherParameter[A, B](val left: Parameter[A], val right: Parameter[B]) extends Parameter[Either[A, B]]
  
  //class FallbackParameter[A](val first: Parameter[A], val second: Parameter[A]) extends Parameter[A]
  
  //def literal(ss: NonEmptyList[String]): Parameter[String] = new LiteralChoicesParameter(ss)
  
  //class LiteralChoicesParameter(ss: NonEmptyList[String])
  
  //def optional[A](p: Parameter[A]): Parameter[Option[A]] = OptionalParameter[A](p)
  
  //class OptionalParameter[A](val p: Parameter[A]) extends Parameter[Option[A]]
  
  //def manyN[A](parameter: Parameter[A], minimum: Int): Parameter[Seq[A]] = ManyParameter[A](parameter, minimum)
  
  //final class ManyParameter[A](val p: Parameter[A], minimum: Int) extends Parameter[Seq[A]]

  class NamedParameter[A](val p: Parameter[A], val name: String) extends Parameter[A]:
    
    override def optimisticUsage: Usage = p.optimisticUsage match
      case Usage.Optional(Usage.Required(_, completion, configure)) =>
        Usage.Optional(Usage.Required(name, completion, configure))
      case Usage.Many(minimum, Usage.Required(_, completion, configure)) =>
        Usage.Many(minimum, Usage.Required(name, completion, configure))
      case _ =>
        Usage.Required(name, ???, ???)

    override def usage(source: CommandCause): Usage = p.usage(source) match
      case Usage.Optional(Usage.Required(_, completion, configure)) => 
        Usage.Optional(Usage.Required(name, completion, configure))
      case Usage.Many(minimum, Usage.Required(_, completion, configure)) => 
        Usage.Many(minimum, Usage.Required(name, completion, configure))
      case _ => 
        Usage.Required(name, ???, ???)
  end NamedParameter
  
  //def choicesSingle
  
  //def choices
  
  //class ChoicesSingleParameter

  //class ChoicesManyParameter

  class MappedParameter[A, B](val p: Parameter[A], f: A => Either[CommandFailureNEL, B]) extends Parameter[B] {
    override def optimisticUsage: Usage = p.optimisticUsage

    override def usage(cause: CommandCause): Usage = p.usage(cause)
  }
  
end Parameter
