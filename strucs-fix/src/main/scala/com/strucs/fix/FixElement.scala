package com.strucs.fix

/**
 * Can be a tag/value pair, a Group of tag/value pair, or a complete message
 */
sealed trait FixElement

case class FixTagValue(tag:Int, value: String) extends FixElement
