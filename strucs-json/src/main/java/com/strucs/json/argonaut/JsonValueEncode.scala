package com.strucs.json.argonaut

import argonaut.Json

/**
 * typeclass. defines how basic types (String, Int, ...) can be encoded/decoded to/from Json
 */
trait JsonValueEncode[A] {
  def encode(a: A): Json
}

object JsonValueEncode {
  implicit object StringValueEncode extends JsonValueEncode[String] {
    override def encode(a: String): Json = Json.jString(a)
  }
}

