package strucs


import scala.reflect.api.Universe

/** Extracts the constituents of a Mixin: String with Int => List(String, Int) */
object FieldExtractor {

  def extractTypes(mixin: Universe#Type, acc: List[Universe#Type] = List.empty)(implicit universe: Universe): List[Universe#Type] = {
    import universe._
    val nilSymbol = typeOf[Nil].typeSymbol
    mixin match {
      // Filter out Nil
      case t if t.typeSymbol == nilSymbol  => acc
      // Constituents of a Mixin => recursive call
      case RefinedType(types, _) => types.foldLeft(acc) { (accTypes, tpe) => extractTypes(tpe, accTypes) }
      // type reference, such as myStrut.Mixin => we need to resolve the reference
      case TypeRef(pre @ SingleType(ppre, psym), sym, args) if !psym.isModule => extractTypes(sym.typeSignatureIn(pre), acc)
      case _ => mixin +: acc
    }
  }

}
