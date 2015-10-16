package com.strucs.json.argonaut

import argonaut.{EncodeJson, Argonaut, JsonObject, Json}
import org.strucs.Struct.Nil
import org.strucs.{Wrapper, Struct, StructKeyProvider, ComposeCodec}
import Argonaut._
import scala.language.experimental.macros

/** Encodes a Struct using Argonaut's EncodeJson typeclass */
object EncodeJson {
  /** Build a field:value pair encoder */
  implicit def fromWrapper[W, V](fieldName: String)(implicit wrapper: Wrapper[W, V], valueEncode: EncodeJson[V]): EncodeJson[W] = new EncodeJson[W] {
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
      override def encode(a: Struct[A with B]): Json = {
        val bjson = cb.encode(a.shrink[B])
        val ajson = ca.encode(a.get[A])
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
