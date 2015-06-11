package com.strucs.fix.dict

import com.strucs.fix.FixCodec
import com.strucs.fix.FixCodec.FixTag
import FixCodec._
import org.strucs.{ValueClassWrapper, Wrapper}

/**
 * Tag names for FIX 4.2
 */
package object fix42 {
  case class BodyLength(v: Int) extends AnyVal
  object BodyLength {
    implicit val wrapper: Wrapper[BodyLength, Int] = new ValueClassWrapper(BodyLength.apply, _.v)
    implicit val codec: FixCodec[BodyLength] = new FixTag[BodyLength, Int](145)
  }
}
