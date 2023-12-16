package net.katsstuff.homesweethome.lib

object LibPerm:

  final val Root   = "homesweethome"
  final val Reload = s"$Root.reload"

  final val HomeLimitOption     = "homeLimit"
  final val ResidentLimitOption = "residentLimit"

  final val Home       = s"$Root.home"
  final val HomeTp     = s"$Home.tp"
  final val HomeDelete = s"$Home.remove"
  final val HomeList   = s"$Home.list"
  final val HomeSet    = s"$Home.set"
  final val HomeInvite = s"$Home.invite"
  final val HomeAccept = s"$Home.accept"
  final val HomeGoto   = s"$Home.goto"

  final val HomeResident       = s"$Home.residents"
  final val HomeResidentList   = s"$HomeResident.list"
  final val HomeResidentAdd    = s"$HomeResident.add"
  final val HomeResidentRemove = s"$HomeResident.remove"

  final val HomeOther       = s"$Home.other"
  final val HomeOtherTp     = s"$HomeOther.tp"
  final val HomeOtherDelete = s"$HomeOther.remove"
  final val HomeOtherList   = s"$HomeOther.list"
  final val HomeOtherSet    = s"$HomeOther.set"
  final val HomeOtherInvite = s"$HomeOther.invite"

  final val HomeOtherResident       = s"$HomeOther.residents"
  final val HomeOtherResidentList   = s"$HomeOtherResident.list"
  final val HomeOtherResidentAdd    = s"$HomeOtherResident.add"
  final val HomeOtherResidentRemove = s"$HomeOtherResident.remove"

