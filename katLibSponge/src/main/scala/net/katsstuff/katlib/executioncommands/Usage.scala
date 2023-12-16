package net.katsstuff.katlib.executioncommands

import org.spongepowered.api.command.registrar.tree.{ClientCompletionKey, ClientCompletionKeys, CommandTreeNode}

import scala.reflect.ClassTag

private enum BrigNode:
  case RealNode(key: String, node: Usage.ArgNodeWrapper, children: Vector[BrigNode])
  case DummyNode(children: Vector[BrigNode], addedChildren: Vector[BrigNode])
  case EmptyNode

  def addChild(child: BrigNode): BrigNode = this match
    case RealNode(key, node, children)      => RealNode(key, node, children :+ child)
    case DummyNode(children, addedChildren) => DummyNode(children, addedChildren :+ child)
    case EmptyNode                          => DummyNode(Vector(child), Vector.empty)

  def addToAllLeaves(newLeaves: Vector[BrigNode]): BrigNode = this match
    case RealNode(key, node, Vector()) => RealNode(key, node, newLeaves)
    case RealNode(key, node, children) => RealNode(key, node, children.map(_.addToAllLeaves(newLeaves)))
    case DummyNode(Vector(), Vector()) => DummyNode(newLeaves, Vector.empty)
    case DummyNode(children, Vector()) => DummyNode(children, newLeaves)
    case DummyNode(children, added)    => DummyNode(children, added.map(_.addToAllLeaves(newLeaves)))
    case EmptyNode                     => DummyNode(newLeaves, Vector.empty)

enum Usage(val isSimple: Boolean):
  case Required(
    s: String,
    completionKey: Usage.ClientCompletionKeyWrapper,
    argNodeConfigure: [A <: CommandTreeNode.Argument[A]] => A => A
  ) extends Usage(true)
  
  case Const(s: String)                 extends Usage(true)
  case Seq(head: Usage, tail: Usage)    extends Usage(false)
  case Choice(head: Usage, tail: Usage) extends Usage(false)
  case Optional(u: Usage)               extends Usage(true)
  case Many(minimum: Int, elem: Usage)  extends Usage(false)
  case Empty                            extends Usage(true)

object Usage:
  
  def simpleRequired[A <: CommandTreeNode.Argument[A]](key: String, completionKey: ClientCompletionKey[A]): Usage.Required =
    Usage.Required(key, ClientCompletionKeyWrapper(completionKey), [A <: CommandTreeNode.Argument[A]] => (arg: A) => arg)

  private def printHeadNested[A](u: Usage)(implicit tag: ClassTag[A]): String =
    if (u.isSimple || tag.runtimeClass.isInstance(u)) printUsage(u)
    else s"(${printUsage(u)})"
    
  trait ClientCompletionKeyWrapper:
    type A <: CommandTreeNode.Argument[A]
    def completionKey: ClientCompletionKey[A]
  
  object ClientCompletionKeyWrapper:
    def apply[A0 <: CommandTreeNode.Argument[A0]](key: ClientCompletionKey[A0]): ClientCompletionKeyWrapper { type A = A0 } =
      new ClientCompletionKeyWrapper:
        override type A = A0
        override def completionKey: ClientCompletionKey[A0] = key

  trait ArgNodeWrapper:
    self =>
    type A <: CommandTreeNode.Argument[A]
    def arg: A
    def addChild(key: String, node: ArgNodeWrapper): ArgNodeWrapper =
      ArgNodeWrapper(arg.child(key, node.arg))

    def executable(): ArgNodeWrapper =
      val node = arg.executable()
      ArgNodeWrapper(node)

  object ArgNodeWrapper:
    def apply[A0 <: CommandTreeNode.Argument[A0]](cmdArg: A0): ArgNodeWrapper { type A = A0 } = new ArgNodeWrapper:
      override type A = A0
      override def arg: A0 = cmdArg

  private def usageToBrigNode(usage: Usage): BrigNode =
    usage match
      case Required(s, completionKey, configure) =>
        val rawNode = completionKey.completionKey.createNode()
        val node = configure(rawNode)

        BrigNode.RealNode(s, ArgNodeWrapper(node), Vector.empty)

      case Const(s) =>
        //TODO: Am I supposed to be able to do this?
        val node = CommandTreeNode.literal().requires(e => true)
        BrigNode.RealNode(s, ArgNodeWrapper(node), Vector.empty)

      case Seq(head, tail) => 
        usageToBrigNode(head).addChild(usageToBrigNode(tail))

      case Choice(head, tail) =>
        BrigNode.DummyNode(Vector(usageToBrigNode(head), usageToBrigNode(tail)), Vector.empty)

      case Optional(u) =>
        BrigNode.DummyNode(Vector(usageToBrigNode(u), BrigNode.EmptyNode), Vector.empty)

      case Many(0, elem) =>
        ???

      case Many(minimum, elem) =>
        usageToBrigNode(elem).addChild(usageToBrigNode(Many(minimum - 1, elem)))

      case Empty =>
        BrigNode.DummyNode(Vector.empty, Vector.empty) //Empty dummy node is correct here I think

  private def removeDummyBrigNodes(brigNode: BrigNode): scala.Seq[BrigNode] =
    brigNode match
      case BrigNode.RealNode(key, nodeArg, children) =>
        scala.Seq(BrigNode.RealNode(key, nodeArg, children.flatMap(removeDummyBrigNodes)))

      case BrigNode.DummyNode(original, Vector()) =>
        original.flatMap(removeDummyBrigNodes)

      case BrigNode.DummyNode(original, added) =>
        val processedAdded = added.flatMap(removeDummyBrigNodes)
        original.map(_.addToAllLeaves(processedAdded)).flatMap(removeDummyBrigNodes)

      case BrigNode.EmptyNode =>
        scala.Seq(BrigNode.EmptyNode)
        
  private def brigNodeToCommandNode(brigNode: BrigNode): Option[(String, ArgNodeWrapper)] = 
    brigNode match {
      case BrigNode.RealNode(key, node, children) =>
        val nodwWithChildren = children.flatMap(brigNodeToCommandNode).foldLeft(node) { case (acc, (key, child)) => acc.addChild(key, child) }
        val nodeWithExecutable = if children.contains(BrigNode.EmptyNode) then node.executable() else node
        
        Some((key, nodeWithExecutable))
        
      case BrigNode.DummyNode(_, _) => throw new IllegalArgumentException("Did not remove all dummy nodes from BrigNode")
      case BrigNode.EmptyNode => None
    }

  def createRootCommandNode(usage: Usage): CommandTreeNode[CommandTreeNode.Root] =
    val freshRootNode = CommandTreeNode.root()
    val brigNodeWithDummy = usageToBrigNode(usage) 
    val brigNodes = removeDummyBrigNodes(brigNodeWithDummy)
    val commandNodes = brigNodes.flatMap(brigNodeToCommandNode)
      
    val root = commandNodes.foldLeft(freshRootNode) { case (rootNode, (key, node)) => rootNode.child(key, node.arg) }
    
    if brigNodes.contains(BrigNode.EmptyNode) then root.executable() else root

  def printUsage(usage: Usage): String = usage match
    case Optional(Required(s, _, _)) => s"[$s]"
    case Required(s, _, _)           => s"<$s>"
    case Const(s)                    => s
    case Seq(head, tail)             => s"${printHeadNested[Seq](head)} ${printUsage(tail)}"
    case Choice(head, tail)          => s"${printHeadNested[Choice](head)} | ${printUsage(tail)}"
    case Optional(u)                 => s"[${printHeadNested[Optional](u)}]"
    case Many(0, Required(s, _, _))  => s"[$s...]"
    case Many(0, elem)               => s"[${printHeadNested[Many](elem)}...]"
    case Many(1, Required(s, _, _))  => s"<$s...>"
    case Many(1, elem)               => s"<${printHeadNested[Many](elem)}...>"
    case Many(n, elem)               => printUsage(Seq(elem, Many(n - 1, elem)))
    case Empty                       => ""

  def merge(first: Usage, second: Usage): Usage = (first, second) match
    case (Required(s1, ck1, process1), Required(s2, ck2, process2)) 
      if s1 == s2 && ck1.completionKey.key == ck2.completionKey.key => 
        val mergedProcess = [A <: CommandTreeNode.Argument[A]] => (arg: A) => 
          val processed1 = process1(arg)
          process2(processed1)
        Required(s1, ck1, mergedProcess)
    case (Const(s1), Const(s2)) if s1 == s2       => Const(s1)
    case (Seq(u11, u12), Seq(u21, u22))           => Seq(merge(u11, u21), merge(u12, u22))
    case (Choice(u11, u12), Choice(u21, u22))     => Choice(merge(u11, u21), merge(u12, u22))
    case (Optional(u1), Optional(u2))             => Optional(merge(u1, u2))
    case (Many(n1, u1), Many(n2, u2)) if n1 == n2 => Many(n1, merge(u1, u2))
    case (u, Empty)                               => u
    case (Empty, u)                               => u
    case (u1, u2)                                 => Choice(u1, u2)
end Usage
