package net.katsstuff.katlib.internal.util

//noinspection MutatorLikeMethodIsParameterless
case class Zipper[+A](lefts: Seq[A], focus: A, rights: Seq[A]) {
  def left: Option[Zipper[A]] = if (lefts.nonEmpty) Some(Zipper(lefts.init, lefts.last, focus +: rights)) else None

  def right: Option[Zipper[A]] = if (rights.nonEmpty) Some(Zipper(lefts :+ focus, rights.head, rights.tail)) else None

  def first: Option[Zipper[A]] =
    if (lefts.nonEmpty) Some(Zipper(Nil, lefts.head, lefts.tail ++ (focus +: rights))) else None

  def last: Option[Zipper[A]] =
    if (rights.nonEmpty) Some(Zipper((lefts :+ focus) ++ rights.init, rights.last, Nil)) else None

  def leftN(amount: Int): Option[Zipper[A]] =
    if (amount == 0) Some(this)
    else if (amount < 0) rightN(-amount)
    else if (lefts.lengthCompare(amount) >= 0) {
      val (newLefts, newRights) = lefts.splitAt(lefts.size - amount)

      Some(Zipper(newLefts, newRights.head, (newRights.tail :+ focus) ++ rights))
    } else None

  def rightN(amount: Int): Option[Zipper[A]] =
    if (amount == 0) Some(this)
    else if (amount < 0) leftN(-amount)
    else if (rights.lengthCompare(amount) >= 0) {
      val (newLefts, newRights) = rights.splitAt(amount)

      Some(Zipper(lefts ++ (focus +: newLefts.init), newLefts.last, newRights))
    } else None

  def goto(i: Int): Option[Zipper[A]] = if (i >= 0 && i < size) rightN(i - focusIndex) else None

  def size: Int = lefts.size + rights.size + 1

  def focusIndex: Int = lefts.size

  def deleteGoLeft: Option[Zipper[A]] = if (lefts.nonEmpty) Some(Zipper(lefts.init, lefts.last, rights)) else None

  def deleteGoRight: Option[Zipper[A]] = if (rights.nonEmpty) Some(Zipper(lefts, rights.head, rights.tail)) else None

  def modifyFocus[B >: A](f: A => B): Zipper[B] = Zipper(lefts, f(focus), rights)

  def set[B >: A](v: B): Zipper[B] = Zipper(lefts, v, rights)

  def map[B](f: A => B): Zipper[B] = Zipper(lefts.map(f), f(focus), rights.map(f))

  def setLefts[B >: A](newLefts: Seq[B]): Zipper[B] = Zipper(newLefts, focus, rights)

  def setRights[B >: A](newRights: Seq[B]): Zipper[B] = Zipper(lefts, focus, newRights)
}
