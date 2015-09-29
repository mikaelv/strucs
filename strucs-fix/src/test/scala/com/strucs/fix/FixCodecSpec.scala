package com.strucs.fix

import com.strucs.fix.dict.fix42._
import org.joda.time.{DateTimeZone, DateTime}
import org.scalactic.TypeCheckedTripleEquals
import org.scalatest.{FlatSpec, Matchers}
import org.strucs.{ComposeCodec, Struct}
import FixCodec._
import FixGroup.SOH

import scala.util.Success


/**
 * More examples at http://fiximulator.org/FIXimulator_Thesis.pdf
 */
class FixCodecSpec extends FlatSpec with Matchers with TypeCheckedTripleEquals {

  /** It's easier to use \n or ; for separating key/value pairs in testcases. */
  private implicit class SemicolonToSOH(s: String) {
    def toSOH: String = s.stripMargin.replaceAll("[;\n]", SOH)
  }

  "a FixCodec" should "encode/decode a New Order Single" in {
    val struct = Struct.empty +
      BeginString.Fix42 +
      MsgType.OrderSingle +
      MsgSeqNum("4") + // TODO remove ?: protocol field
      SenderCompID("ABC_DEFG01") +
      SendingTime(new DateTime(2009,3,23,15,40,29, DateTimeZone.UTC)) +
      TargetCompID("CCG") +
      OnBehalfOfCompID("XYZ") +
      ClOrdId("NF 0542/03232009") +
      (Side.Buy: Side) +
      OrderQty(100) +
      Symbol("CVS") +
      OrdType("1") + // TODO enum
      TimeInForce("0") +
      Rule80A("A") + // TODO enum
      TransactTime(new DateTime(2009,3,23,15,40,29, DateTimeZone.UTC)) +
      HandlInst("1") + // TODO enum
      SecurityExchange("N")

    val fixString =
      """8=FIX.4.2
        |9=146
        |35=D
        |34=4
        |49=ABC_DEFG01
        |52=20090323-15:40:29
        |56=CCG
        |115=XYZ
        |11=NF 0542/03232009
        |54=1
        |38=100
        |55=CVS
        |40=1
        |59=0
        |47=A
        |60=20090323-15:40:29
        |21=1
        |207=N
        |10=195
        |""".toSOH


    val encodedFix = struct.toFixMessageString
    encodedFix should be (fixString)

    val decodedStruct = fixString.toStruct[struct.Mixin]
    decodedStruct.get should === (struct)
  }


  val fixMsgString = "8=FIX.4.2;9=65;35=A;49=SERVER;56=CLIENT;34=177;52=20090107-18:15:16;98=0;108=30;10=062;".toSOH

  "A FixMessage" should "encode with length and checksum" in {
    // Example from https://en.wikipedia.org/wiki/Financial_Information_eXchange#Body_length
    val msg = FixMessage(BeginString.Fix42TV, MsgType.LogonTV, FixGroup(49 -> "SERVER", 56 -> "CLIENT", 34 -> "177", 52 -> "20090107-18:15:16", 98 -> "0", 108 -> "30"))
    msg.toFixString should be (fixMsgString)
  }

  "A FixMessage" should "decode" in {
    FixMessage.decode(fixMsgString) should ===(Success(FixMessage(BeginString.Fix42TV, MsgType.LogonTV, FixGroup(49 -> "SERVER", 56 -> "CLIENT", 34 -> "177", 52 -> "20090107-18:15:16", 98 -> "0", 108 -> "30"))))
  }

  "A FixMessage" should "keep the same fix string after decode and encode" in {
    val fixStr = "8=FIX.4.2;9=137;35=D;34=8;49=BANZAI;52=20081005-14:35:46.672;56=FIXIMULATOR;11=1223217346597;21=1;38=5000;40=1;54=2;55=IBM;59=0;60=20081005-14:35:46.668;10=153;".toSOH
    FixMessage.decode(fixStr) map (_.toFixString) should === (Success(fixStr))
  }


}


