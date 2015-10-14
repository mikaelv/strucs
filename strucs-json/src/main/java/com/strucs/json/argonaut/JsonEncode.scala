package com.strucs.json.argonaut

import argonaut.{Argonaut, JsonObject, Json}
import org.strucs.Struct.Nil
import org.strucs.{Wrapper, Struct, StructKeyProvider, ComposeCodec}
import Argonaut._
import scala.language.experimental.macros

/**
 */
trait JsonEncode[A] {
  def encode(a: A): Json
}

object JsonEncode {
  /** Build a field:value pair encoder */
  def single[W, V](fieldName: String)(implicit wrapper: Wrapper[W, V], valueEncode: JsonValueEncode[V]): JsonEncode[W] = new JsonEncode[W] {
    override def encode(w: W): Json = jSingleObject(fieldName, valueEncode.encode(wrapper.value(w)))
  }

  /** Automatically create a JsonEncode for any Struct[A]
    * @tparam T mixin, each type M in the mixin must have an implicit JsonEncode[M] in scope */
  implicit def makeJsonEncode[T]: JsonEncode[Struct[T]] = macro ComposeCodec.macroImpl[JsonEncode[_], T]


  implicit object ComposeJsonEncode extends ComposeCodec[JsonEncode] {
    /** Build a Codec for an empty Struct */
    override def zero: JsonEncode[Struct[Nil]] = new JsonEncode[Struct[Nil]] {
      override def encode(a: Struct[Nil]): Json = jObject(JsonObject.empty)
    }


    /** Build a Codec using a field codec a and a codec b for the rest of the Struct */
    override def prepend[A: StructKeyProvider, B](ca: JsonEncode[A], cb: => JsonEncode[Struct[B]]): JsonEncode[Struct[A with B]] = new JsonEncode[Struct[A with B]] {
      override def encode(a: Struct[A with B]): Json = {
        val bjson = cb.encode(a.shrink[B])
        val ajson = ca.encode(a.get[A])

        val aAssoc = ajson.assoc.get.head   // TODO !!!! should not assume anything about ajson
        aAssoc ->: bjson

      }
    }
  }

  /** Pimp Struct with helpful methods */
  implicit class JsonEncodeOps[A](struct: Struct[A])(implicit encode: JsonEncode[Struct[A]]) {
    def toJson: Json = encode.encode(struct)
    def toJsonString: String = toJson.nospaces
  }

}
