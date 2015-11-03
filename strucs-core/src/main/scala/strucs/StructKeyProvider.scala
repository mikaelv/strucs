package strucs

import scala.reflect.runtime.universe._


/** Type class that gives a key of a given type, for usage in the Struct Map */
case class StructKeyProvider[T](key: StructKey)

/**
 *
 */
object StructKeyProvider {
  /** Provides the implicit value for StructKey[Option[T]] when the implicit value for StructKey[T] is in scope */
  implicit def convertToOption[T](implicit rk: StructKeyProvider[T]): StructKeyProvider[Option[T]] = new StructKeyProvider[Option[T]](StructKey(rk.key.value))
  implicit def convertToSome[T](implicit rk: StructKeyProvider[T]): StructKeyProvider[Some[T]] = new StructKeyProvider[Some[T]](StructKey(rk.key.value))

  /** Provides a key using the Class name. Beware of ADT types: use Option(x) instead of Some(x) */
  implicit def simpleNameStructKeyProvider[A : TypeTag]: StructKeyProvider[A] = StructKeyProvider[A](StructKey(implicitly[TypeTag[A]].tpe.typeSymbol.toString))
}
