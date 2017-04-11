package io.github.katrix.katlib.persistant

sealed trait CommentTree {
  def getChild(node: String): CommentTree
  def merge(other: CommentTree): CommentTree
}
object CommentTree {
  def fromMany(trees: CommentTree*): CommentTree = trees.fold(NoComment)((t1, t2) => t1.merge(t2))
}

case class CommentNode(comment: String) extends CommentTree {
  override def getChild(node: String): CommentTree = NoComment
  override def merge(other: CommentTree): CommentTree = other match {
    case CommentNode(otherComment) => CommentNode(s"$comment\n$otherComment")
    case _ => this
  }
}

case class CommentBranch(children: Map[String, CommentTree]) extends CommentTree {
  override def getChild(node: String): CommentTree = children.getOrElse(node, NoComment)
  override def merge(other: CommentTree): CommentTree = other match {
    case CommentBranch(otherChildren) =>
      val keys = children.keys ++ otherChildren.keys
      val merged = keys.map(node => node -> children.getOrElse(node, NoComment).merge(otherChildren.getOrElse(node, NoComment))).toMap

      CommentBranch(merged)
    case _ => this
  }
}

case object NoComment extends CommentTree {
  override def getChild(node: String): CommentTree = NoComment
  override def merge(other: CommentTree): CommentTree = other
}
