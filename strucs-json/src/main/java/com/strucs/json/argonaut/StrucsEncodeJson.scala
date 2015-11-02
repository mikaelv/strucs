package com.strucs.json.argonaut

import argonaut._
import org.strucs.Struct.Nil
import org.strucs.{Wrapper, Struct, StructKeyProvider, ComposeCodec}
import Argonaut._
import scala.language.experimental.macros

/** Encodes a Struct using Argonaut's EncodeJson typeclass */
object StrucsEncodeJson {
  /** Build a field:value pair encoder */
  def fromWrapper[W, V](fieldName: String)(implicit wrapper: Wrapper[W, V], valueEncode: EncodeJson[V]): EncodeJson[W] = new EncodeJson[W] {
    override def encode(w: W): Json = jSingleObject(fieldName, valueEncode.encode(wrapper.value(w)))
  }

  /** Automatically create an EncodeJson for any Struct[A]
    * @tparam T mixin, each type M in the mixin must have an implicit EncodeJson[M] in scope */
  implicit def makeEncodeJson[T]: EncodeJson[Struct[T]] = macro ComposeCodec.macroImpl[EncodeJson[_], T]

  implicit object ComposeEncodeJson extends ComposeCodec[EncodeJson] {
    /** Build a Codec for an empty Struct */
    override def zero: EncodeJson[Struct[Nil]] = new EncodeJson[Struct[Nil]] {
      override def encode(a: Struct[Nil]): Json = jObject(JsonObject.empty)
    }


    /** Build a Codec using a field codec a and a codec b for the rest of the Struct */
    override def prepend[A: StructKeyProvider, B](ca: EncodeJson[A], cb: => EncodeJson[Struct[B]]): EncodeJson[Struct[A with B]] = new EncodeJson[Struct[A with B]] {
      override def encode(struct: Struct[A with B]): Json = {
        val bjson = cb.encode(struct.shrink[B])
        val ajson = ca.encode(struct.get[A])
        ajson.assoc match {
          case Some(assoc :: Nil) => assoc ->: bjson
          case _ => sys.error(s"Cannot prepend $ajson to $bjson")
        }
      }
    }
  }



  val nospacePreserveOrder = nospace.copy(preserveOrder = true)

  /** Pimp Struct with helpful methods */
  implicit class EncodeJsonOps[A](struct: Struct[A])(implicit encode: EncodeJson[Struct[A]]) {
    def toJson: Json = encode.encode(struct)
    def toJsonString: String = nospacePreserveOrder.pretty(toJson)
  }

}

object StrucsDecodeJson {
  /** Build a field:value pair decoder */
  def fromWrapper[W, V](fieldName: String)(implicit wrapper: Wrapper[W, V], valueDecode: DecodeJson[V]): DecodeJson[W] = new DecodeJson[W] {
    override def decode(c: HCursor): DecodeResult[W] = {
      val wrapperDecode = valueDecode.flatMap { value =>
        new DecodeJson[W] {
          override def decode(c: HCursor): DecodeResult[W] = wrapper.make(value) match {
            case None => DecodeResult.fail("Invalid value " + value, c.history)
            case Some(w) => DecodeResult.ok(w)
          }
        }
      }

      c.get(fieldName)(wrapperDecode)
    }
  }

  implicit def makeDecodeJson[T]: DecodeJson[Struct[T]] = macro ComposeCodec.macroImpl[DecodeJson[_], T]

  implicit object ComposeDecodeJson extends ComposeCodec[DecodeJson] {
    /** Build a Codec for an empty Struct */
    override def zero: DecodeJson[Struct[Nil]] = new DecodeJson[Struct[Nil]] {
      override def decode(c: HCursor): DecodeResult[Struct[Nil]] = DecodeResult.ok(Struct.empty)

    }

    /** Build a Codec using a field codec a and a codec b for the rest of the Struct */
    override def prepend[A: StructKeyProvider, B](ca: DecodeJson[A], cb: => DecodeJson[Struct[B]]): DecodeJson[Struct[A with B]] = new DecodeJson[Struct[A with B]] {
      override def decode(c: HCursor): DecodeResult[Struct[A with B]] = {
        for {
          structb <- cb.decode(c)
          a <- ca.decode(c)
        } yield structb.add[A](a)
      }

    }
  }
}

/** Encode and Decode a Struct using Argonaut's CodecJson */
object StrucsCodecJson {
  def fromWrapper[W, V](fieldName: String)(implicit wrapper: Wrapper[W, V], valueEncode: EncodeJson[V], valueDecode: DecodeJson[V]): CodecJson[W] = CodecJson.derived[W](
    StrucsEncodeJson.fromWrapper[W, V](fieldName), StrucsDecodeJson.fromWrapper[W, V](fieldName))
}
