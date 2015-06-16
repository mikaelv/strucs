package com.strucs.fix.dict

import com.strucs.fix.FixCodec
import com.strucs.fix.FixCodec.FixTagCodec
import org.joda.time.DateTime
import org.strucs.Wrapper.materializeWrapper

/**
 * Tag names for FIX 4.2
 */
package object fix42 {

  case class BeginString(v: String) extends AnyVal
  object BeginString {
    implicit val codec: FixCodec[BeginString] = new FixTagCodec[BeginString, String](8)
  }

  case class BodyLength(v: Int) extends AnyVal
  object BodyLength {
    implicit val codec: FixCodec[BodyLength] = new FixTagCodec[BodyLength, Int](9)
  }

  case class CheckSum(v: Int) extends AnyVal
  object CheckSum {
    implicit val codec: FixCodec[CheckSum] = new FixTagCodec[CheckSum, Int](10)
  }

  case class ClOrdId(v: String) extends AnyVal
  object ClOrdId {
    implicit val codec: FixCodec[ClOrdId] = new FixTagCodec[ClOrdId, String](11)
  }

  case class HandlInst(v: String) extends AnyVal
  object HandlInst {
    implicit val codec: FixCodec[HandlInst] = new FixTagCodec[HandlInst, String](21)
  }

  case class MsgSeqNum(v: String) extends AnyVal
  object MsgSeqNum {
    implicit val codec: FixCodec[MsgSeqNum] = new FixTagCodec[MsgSeqNum, String](34)
  }

  case class MsgType(v: String) extends AnyVal
  object MsgType {
    implicit val codec: FixCodec[MsgType] = new FixTagCodec[MsgType, String](35)
  }

  case class OrderQty(v: String) extends AnyVal
  object OrderQty {
    implicit val codec: FixCodec[OrderQty] = new FixTagCodec[OrderQty, String](38)
  }

  case class OrdType(v: String) extends AnyVal
  object OrdType {
    implicit val codec: FixCodec[OrdType] = new FixTagCodec[OrdType, String](40)
  }

  case class Rule80A(v: String) extends AnyVal
  object Rule80A {
    implicit val codec: FixCodec[Rule80A] = new FixTagCodec[Rule80A, String](47)
  }

  case class SenderCompID(v: String) extends AnyVal
  object SenderCompID {
    implicit val codec: FixCodec[SenderCompID] = new FixTagCodec[SenderCompID, String](49)
  }

  case class SendingTime(v: DateTime) extends AnyVal
  object SendingTime {
    implicit val codec: FixCodec[SendingTime] = new FixTagCodec[SendingTime, DateTime](52)
  }

  case class Side(v: String) extends AnyVal
  object Side {
    implicit val codec: FixCodec[Side] = new FixTagCodec[Side, String](54)
  }

  case class Symbol(v: String) extends AnyVal
  object Symbol {
    implicit val codec: FixCodec[Symbol] = new FixTagCodec[Symbol, String](55)
  }

  case class TargetCompID(v: String) extends AnyVal
  object TargetCompID {
    implicit val codec: FixCodec[TargetCompID] = new FixTagCodec[TargetCompID, String](56)
  }

  case class TimeInForce(v: String) extends AnyVal
  object TimeInForce {
    implicit val codec: FixCodec[TimeInForce] = new FixTagCodec[TimeInForce, String](59)
  }

  case class TransactTime(v: DateTime) extends AnyVal
  object TransactTime {
    implicit val codec: FixCodec[TransactTime] = new FixTagCodec[TransactTime, DateTime](60)
  }


  case class OnBehalfOfCompID(v: String) extends AnyVal
  object OnBehalfOfCompID {
    implicit val codec: FixCodec[OnBehalfOfCompID] = new FixTagCodec[OnBehalfOfCompID, String](115)
  }

  case class SecurityExchange(v: String) extends AnyVal
  object SecurityExchange {
    implicit val codec: FixCodec[SecurityExchange] = new FixTagCodec[SecurityExchange, String](207)
  }



}
