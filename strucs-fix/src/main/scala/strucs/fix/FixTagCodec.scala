package strucs.fix

import strucs.fix.dict.fix42.MsgType
import strucs.Wrapper
import strucs.fix.dict.fix42.{MsgType, BeginString}

import scala.util.{Failure, Success, Try}

/** Encodes a single tag / value pair.
  * Uses the value codec and the wrapper to encode/decode the value */
class FixTagCodec[W, V](val tag: Int)(implicit wrapper: Wrapper[W, V], valueCodec: FixValueCodec[V]) extends FixCodec[W] {
  override def encode(a: W): FixTagValue = FixTagValue(tag, valueCodec.encode(wrapper.value(a)))

  /** @param fix is always a FixGroup when called from outside */
  override def decode(fix: FixElement): Try[W] = fix match {
    case FixTagValue(t, value) if t == tag => valueCodec.decode(value) flatMap {
      wrapper.make(_).map(Success(_)).getOrElse(Failure(new FixDecodeException(s"Wrapper: $wrapper cannot parse $value in tag $t")))
    }

    // get the FixTagValue and call decode again
    case g@FixGroup(pairs) => g.get(tag).map(decode).getOrElse(Failure(new FixDecodeException(s"Cannot find tag $tag in $fix")))
    case m@FixMessage(beginStr, msgType, body) =>
      if (tag == MsgType.Tag)
        decode(msgType)
      else if (tag == BeginString.Tag)
        decode(beginStr)
      else
        body.get(tag).map(decode).getOrElse(Failure(new FixDecodeException(s"Cannot find tag $tag in $fix")))

    case _ => Failure(new FixDecodeException(s"Cannot decode $fix"))
  }
}