package com.strucs.fix.dict

import com.strucs.fix.FixCodec
import com.strucs.fix.FixCodec.FixTag
import org.strucs.Wrapper.materializeWrapper

/**
 * Tag names for FIX 4.2
 */
package object fix42 {
  case class BodyLength(v: Int) extends AnyVal
  object BodyLength {
    implicit val codec: FixCodec[BodyLength] = new FixTag[BodyLength, Int](145)
  }
}
