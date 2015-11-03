package strucs

import scala.language.experimental.macros
import scala.reflect.macros.blackbox

/** typeclass for wrapper types, such as case class Name(v: String)
  * a wrapper can be built from a value, and we can extract a value from it
  * @tparam W type of the Wrapper
  * @tparam V type of the value
  */
trait Wrapper[W, V] {
  def make(v: V): Option[W] // TODO use Try or Either ? Look at Spray or akka-http for error handling
  def value(w: W): V
}

object Wrapper {
  def macroImpl[W: c.WeakTypeTag, V : c.WeakTypeTag](c: blackbox.Context) = {
    import c.universe._
    val wsym = c.weakTypeOf[W].typeSymbol
    val vtype = c.weakTypeOf[V]

    if (!wsym.isClass || !wsym.asClass.isCaseClass) c.abort(c.enclosingPosition, s"$wsym is not a case class. Please define a Wrapper[$wsym, ?] manually")
    val fields = wsym.typeSignature.decls.toList.collect{ case x: TermSymbol if x.isVal && x.isCaseAccessor => x }
    if (fields.size != 1) c.abort(c.enclosingPosition, s"$wsym must have exactly one field. Please define a Wrapper[$wsym, ?] manually")
    val field = fields.head.getter

    val expr = q"new strucs.CaseClassWrapper[$wsym, $vtype](new $wsym(_), _.$field)"
    //c.info(c.enclosingPosition, expr.toString, true)
    expr
  }

  /** Automatically creates a Wrapper for case classes */
  implicit def materializeWrapper[W, V]: Wrapper[W, V] = macro macroImpl[W, V]

  // TODO can we get rid of this ? It simplifies declaration a Wrapper in companion objects, but it looks suspicious. Maybe wrapper could be a class ?
  // Basically I want to transform a function value into a method => google it !
  /** Creates a new Wrapper implementation */
  def apply[W, V](pMake: V => Option[W], pValue: W => V): Wrapper[W, V] = new Wrapper[W, V] {
    override def make(v: V): Option[W] = pMake(v)

    override def value(w: W): V = pValue(w)
  }

}






/** Implementation of a Wrapper for a case class */
class CaseClassWrapper[W, V](_apply: V => W, _value: W => V) extends Wrapper[W, V] {

  override def value(w: W): V = _value(w)

  override def make(v: V): Option[W] = Some(_apply(v))
}


