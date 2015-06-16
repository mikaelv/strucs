package com.strucs.fix

/**
 * Can be a tag/value pair or a Group of tag/value pair
 */
sealed trait FixElement {
  def +(other: FixElement): FixElement

  def toFixString: String
}



/** Tag/Value Pair */
case class FixTagValue(tag: Int, value: String) extends FixElement {
  override def +(other: FixElement): FixElement = other match {
    case FixGroup(pairs) => FixGroup(this +: pairs)
    case t@FixTagValue(_, _) => FixGroup(Vector(this, t))
    case FixNil => this
  }

  def toFixString: String = s"$tag=$value"
}

/** Group of tags. Can be the header, body, or trailer */
case class FixGroup(pairs: Vector[FixTagValue]) extends FixElement {
  override def +(other: FixElement): FixElement = other match {
    case FixGroup(o) => FixGroup(pairs ++ o)
    case t@FixTagValue(_, _) => FixGroup(pairs :+ t)
    case FixNil => this
  }

  def toFixString: String = pairs.map(_.toFixString).mkString("", "\u0001", "\u0001")
}

case object FixNil extends FixElement {
  override def +(other: FixElement): FixElement = other

  override def toFixString: String = ""
}

/* TODO Message does not extend FixElement, composing encoders looks weird with it. Is it legitimate ? */
case class FixMessage(header: FixGroup, body: FixGroup, trailer: FixGroup)