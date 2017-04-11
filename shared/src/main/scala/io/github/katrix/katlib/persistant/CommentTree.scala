package io.github.katrix.katlib.persistant

sealed trait CommentTree {
  def getChild(node: String): CommentTree
}

case class CommentNode(comment: String) extends CommentTree {
  override def getChild(node: String): CommentTree = NoComment
}

case class CommentBranch(children: Map[String, CommentTree]) extends CommentTree {
  override def getChild(node: String): CommentTree = children.getOrElse(node, NoComment)
}

case object NoComment extends CommentTree {
  override def getChild(node: String): CommentTree = NoComment
}
