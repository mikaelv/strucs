package com.strucs.fix

import com.strucs.fix.dict.fix42._
import org.joda.time.{DateTimeZone, DateTime}
import org.scalactic.TypeCheckedTripleEquals
import org.scalatest.{FlatSpec, Matchers}
import org.strucs.{ComposeCodec, Struct}
import FixCodec._


/**
 * Examples taken from https://www.nyse.com/publicdocs/nyse/markets/nyse/NYSE_CCG_FIX_Sample_Messages_v4.2.pdf
 */
class FixCodecSpec extends FlatSpec with Matchers with TypeCheckedTripleEquals {

  "a FixCodec" should "encode a New Order Single" in {
    val struct = Struct.empty +
      BeginString("FIX.4.2") +
      MsgType("D") +
      MsgSeqNum("4") + // TODO remove ?: protocol field
      SenderCompID("ABC_DEFG01") +
      SendingTime(new DateTime(2009,3,23,15,40,29, DateTimeZone.UTC)) +
      TargetCompID("CCG") +
      OnBehalfOfCompID("XYZ") +
      ClOrdId("NF 0542/03232009") +
      Side("1") + // TODO enum
      OrderQty("100") + // TODO BigDecimal
      Symbol("CVS") +
      OrdType("1") + // TODO enum
      TimeInForce("0") +
      Rule80A("A") + // TODO enum
      TransactTime(new DateTime(2009,3,23,15,40,29, DateTimeZone.UTC)) +
      HandlInst("1") + // TODO enum
      SecurityExchange("N")

    val actualFix = struct.toFixString



    // TODO implement BodyLength and CheckSum calculations in the message
    /*val expectedFix = """8=FIX.4.2;
                |9=145;
                |35=D;
                |34=4;
                |49=ABC_DEFG01;
                |52=20090323-15:40:29;
                |56=CCG;
                |115=XYZ;
                |11=NF 0542/03232009;
                |54=1;
                |38=100;
                |55=CVS;
                |40=1;
                |59=0;
                |47=A;
                |60=20090323-15:40:29;
                |21=1;
                |207=N;
                |10=139;""".stripMargin.replaceAll(";", "\u0001")*/

    val expectedFix =
      """8=FIX.4.2
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
        |""".stripMargin.replaceAll("\n", "\u0001")

    actualFix should === (expectedFix)
  }
}


