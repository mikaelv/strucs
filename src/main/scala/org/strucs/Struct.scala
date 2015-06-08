package org.strucs


/**
 * Extensible data structure with type safety
 * @tparam F mixin of all the fields types
 */
case class Struct[F](private val fields: Map[StructKey, Any]) {
  type tpe = F

  /** Adds a field. Pass an Option[T] if the field is optional */
  def add[T](value: T)(implicit k: StructKeyProvider[T], ev: F <:!< T ) : Struct[F with T] = new Struct[F with T](fields + (k.key -> value))

  /** Updates an existing field */
  def update[T](value: T)(implicit k: StructKeyProvider[T], ev: F <:< T ) : Struct[F] = new Struct[F](fields + (k.key -> value))

  /** Add all fields from another Struct */
  def merge[F2](rec: Struct[F2]): Struct[F with F2] = new Struct[F with F2](fields ++ rec.fields)

  /** Get a field */
  def get[T](implicit k: StructKeyProvider[T], ev: F <:< T): T = fields(k.key).asInstanceOf[T]

  private[this] def getByKey(key: StructKey): Any = fields(key)
}

object Struct {
  def apply[T](t: T)(implicit k: StructKeyProvider[T]): Struct[T] = new Struct(Map(k.key -> t))
}





