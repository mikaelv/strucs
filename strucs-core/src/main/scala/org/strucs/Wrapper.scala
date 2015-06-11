package org.strucs

/** typeclass for wrapper types, such as case class Name(v: String)
  * a wrapper can be built from a value, and we can extract a value from it
  * @tparam W type of the Wrapper
  * @tparam V type of the value
  */
trait Wrapper[W, V] {
  def make(v: V): Option[W]
  def value(w: W): V
}

object Wrapper {

}

/** TODO use a macro materializer to define this automatically */
class ValueClassWrapper[W, V](_apply: V => W, _value: W => V) extends Wrapper[W, V] {

  override def value(w: W): V = _value(w)

  override def make(v: V): Option[W] = Some(_apply(v))
}


