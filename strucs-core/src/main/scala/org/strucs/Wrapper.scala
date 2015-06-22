package org.strucs

import scala.reflect.macros.blackbox
import scala.language.experimental.macros

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
  def macroImpl[W: c.WeakTypeTag, V : c.WeakTypeTag](c: blackbox.Context) = {
    import c.universe._
    val wsym = c.weakTypeOf[W].typeSymbol
    val vsym = c.weakTypeOf[V].typeSymbol

    if (!wsym.isClass || !wsym.asClass.isCaseClass) c.abort(c.enclosingPosition, s"$wsym is not a case class")
    val fields = wsym.typeSignature.decls.toList.collect{ case x: TermSymbol if x.isVal && x.isCaseAccessor => x }
    if (fields.size != 1) c.abort(c.enclosingPosition, s"$wsym must have exactly one field")
    val field = fields.head.getter

    val expr = q"new org.strucs.CaseClassWrapper[$wsym, $vsym](new $wsym(_), _.$field)"
    //c.info(c.enclosingPosition, expr.toString, true)
    expr
  }

  /** Automatically creates a Wrapper for case classes */
  implicit def materializeWrapper[W, V]: Wrapper[W, V] = macro macroImpl[W, V]
}

/** Implementation of a Wrapper for a case class */
class CaseClassWrapper[W, V](_apply: V => W, _value: W => V) extends Wrapper[W, V] {

  override def value(w: W): V = _value(w)

  override def make(v: V): Option[W] = Some(_apply(v))
}


