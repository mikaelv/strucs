package strucs.fix

import strucs.fix.dict.fix42.MsgType

import scala.util.{Failure, Success, Try}

/**
 * Can be a tag/value pair or a Group of tag/value pair
 */
sealed trait FixElement {
  def +(other: FixElement): FixElement

  def toFixString: String

  def toGroup: FixGroup
}



/** Tag/Value Pair */
case class FixTagValue(tag: Int, value: String) extends FixElement {
  override def +(other: FixElement): FixElement = other match {
    case FixGroup(pairs) => FixGroup(this +: pairs)
    case t@FixTagValue(_, _) => FixGroup(Vector(this, t))
    case m@FixMessage(_, _, FixGroup(pairs)) => m.copy(body = FixGroup(this +: pairs))
  }

  def toFixString: String = s"$tag=$value"

  def toGroup: FixGroup = new FixGroup(Vector(this))
}

object FixTagValue {
  def decode(tagValue: String): FixTagValue =  {
    // TODO error handling
    val split = tagValue.split("=")
    FixTagValue(split(0).toInt, split(1))
  }
}



/** Group of tags. Can be the header, body, or trailer */
case class FixGroup(pairs: Vector[FixTagValue]) extends FixElement {
  import FixGroup.SOH

  override def +(other: FixElement): FixElement = other match {
    case FixGroup(o) => FixGroup(pairs ++ o)
    case t@FixTagValue(_, _) => FixGroup(pairs :+ t)
    case m@FixMessage(_, _, FixGroup(o)) => m.copy(body = FixGroup(pairs ++ o))
  }

  def toFixString: String = pairs.map(_.toFixString).mkString("", SOH, "")

  def checksum: Int = toFixString.sum

  def length: Int = toFixString.length

  def get(tag: Int): Option[FixTagValue] = pairs.find(_.tag == tag)

  def remove(tags: Set[Int]): FixGroup = FixGroup(pairs.filterNot(pair => tags.contains(pair.tag)))

  def toGroup: FixGroup = this
}

object FixGroup {
  def apply(pairs: (Int, String)*): FixGroup = FixGroup(pairs.map( kv => FixTagValue(kv._1, kv._2)).toVector)

  def empty: FixGroup = new FixGroup(Vector.empty)

  /** Separator between key value pairs */
  val SOH = "\u0001"

  // TODO error handling
  def decode(fix: String): Try[FixGroup] = Try {
    new FixGroup(fix.split(SOH).map {
      FixTagValue.decode(_)
    }.toVector)
  }
}




import FixGroup.SOH

/** Represents a Fix message, encodes the length and checksum when writing to a String */
case class FixMessage(beginString: FixTagValue, msgType: FixTagValue, body: FixGroup) extends FixElement {

  override def +(other: FixElement): FixElement = body + other

  override def toGroup: FixGroup = body


  def headerWithLength: FixGroup = {
    val bodyLength = (msgType.toGroup.length + 1 + body.length + 1).toString
    new FixGroup(Vector(beginString, FixTagValue(9, bodyLength), msgType))
  }

  def trailer: FixGroup = FixGroup(10 -> ((headerWithLength.checksum + 1 + body.checksum + 1) % 256).formatted("%03d"))

  def toFixString: String = headerWithLength.toFixString + SOH + body.toFixString + SOH + trailer.toFixString + SOH
}

object FixMessage {
  def decode(fix: String): Try[FixMessage] = {
    FixGroup.decode(fix) flatMap { g =>
      val optMsg = for {
        tag35 <- g.get(35)
        tag8 <- g.get(8)
      } yield FixMessage(tag8, tag35, g.remove(Set(8, 35, 9, 10)))
      optMsg match {
        case None => Failure(new FixDecodeException(s"Tags 8 or 35 not found in $fix"))
        case Some(msg) => Success(msg)
      }
    }
  }

}