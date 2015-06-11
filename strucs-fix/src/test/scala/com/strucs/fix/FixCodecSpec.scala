package com.strucs.fix

import org.scalactic.TypeCheckedTripleEquals
import org.scalatest.{FlatSpec, Matchers}


/**
 *
 */
class FixCodecSpec extends FlatSpec with Matchers with TypeCheckedTripleEquals {

  "a FixCodec" should "encode a New Order Single" in {
    val fix = """8=FIX.4.2;
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
                |21=1;207=N;10=139;""".stripMargin
  }
}
