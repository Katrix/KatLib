package io.github.katrix.katlib.persistant

import shapeless._
import shapeless.ops.record.Selector

class CreateComment[Current](private val nodes: Seq[String]) {

  def >>(witness: Witness)(implicit mkComment: MkCreateComment[Current, witness.T]): CreateComment[mkComment.Out] =
    new CreateComment(nodes :+ mkComment.fieldName)
  def >#(comment: String): CommentTree = nodes.foldRight[CommentTree](CommentNode(comment))((str, node) => CommentBranch(Map(str -> node)))
}

object CreateComment {
  def apply[C]: CreateComment[C] = new CreateComment[C](Seq.empty)
}

trait MkCreateComment[Current, Field] {
  type Out
  def fieldName: String
}
object MkCreateComment {
  type Aux[Current, Field, Out0] = MkCreateComment[Current, Field] { type Out = Out0 }

  implicit def createComment[Current, Repr <: HList, Field <: Symbol, FieldType](
      implicit
      labelledGeneric: LabelledGeneric.Aux[Current, Repr],
      field: Selector.Aux[Repr, Field, FieldType],
      witness: Witness.Aux[Field]): Aux[Current, Field, FieldType] =
    new MkCreateComment[Current, Field] {
      override type Out = FieldType
      override def fieldName: String = witness.value.name
    }
}