package com.strucs.fix

/**
 *
 */
class FixEncodeException(msg: String, optThrowable: Option[Throwable] = None) extends Exception(msg, optThrowable.orNull) {

}

/**
 *
 */
class FixDecodeException(msg: String, optThrowable: Option[Throwable] = None) extends Exception(msg, optThrowable.orNull) {

}
