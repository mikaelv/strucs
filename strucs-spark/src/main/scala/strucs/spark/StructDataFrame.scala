package strucs.spark

import org.apache.spark.rdd.RDD
import org.apache.spark.sql.{Row, Column, GroupedData, DataFrame}
import strucs.{Nil, StructKey, Struct, StructKeyProvider}


/**
 * Wraps a DataFrame to make all operations type safe
 */
class StructDataFrame[F] private(val df: DataFrame) {
  // TODO create a ColumnProvider ?
  def select[A](implicit k: StructKeyProvider[A], ev: F <:< A): StructDataFrame[A with Nil] =
    new StructDataFrame[A with Nil](df.select(k.key.value))

  def select[A, B](implicit ka: StructKeyProvider[A], kb: StructKeyProvider[B], eva: F <:< A, evb: F <:<B): StructDataFrame[A with B with Nil] =
    new StructDataFrame[A with B with Nil](df.select(ka.key.value, kb.key.value))

  def groupBy[G](implicit k: StructKeyProvider[G], ev: F <:< G): StructGroupedData[G, F] =
    new StructGroupedData[G, F](df.groupBy(k.key.value))


  def collect(): Array[Struct[F]] = rdd.collect()

  def show() = df.show()

  def rdd: RDD[Struct[F]] = df.rdd.map(row => Struct(RowMap(row)))
}

case class RowMap(row: Row) extends Map[StructKey, Any] {
  // It is easier to rebuild a brand new map rather than fiddling with row.schema
  override def +[B1 >: Any](kv: (StructKey, B1)): Map[StructKey, B1] = Map(keyValues: _*) + kv

  override def get(key: StructKey): Option[Any] = Some(row.getAs[Any](key.value))

  def keyValues: Seq[(StructKey, Any)] = (row.schema.fieldNames.map(StructKey.apply) zip row.toSeq).toSeq

  override def iterator: Iterator[(StructKey, Any)] = keyValues.iterator

  override def -(key: StructKey): Map[StructKey, Any] = ??? // not called from Struct
}

object StructDataFrame {

  implicit class DataFrameOps(df: DataFrame) {
    def toStructDF[A, B](implicit ka: StructKeyProvider[A], kb: StructKeyProvider[B]): StructDataFrame[A with B with Nil] = {
      // TODO verify that df's schema is compatible with A and B

      // TODO pass a Wrapper, and store it in the StructDataFrame, so that we can build A from the type contained in the DF
      new StructDataFrame[A with B with Nil](df)
    }

    def toStructDF[A, B, C](implicit ka: StructKeyProvider[A], kb: StructKeyProvider[B], kc: StructKeyProvider[C]): StructDataFrame[A with B with C with Nil] = {
      // TODO verify that df's schema is compatible with A, B, C
      new StructDataFrame[A with B with C with Nil](df)
    }
  }

}

import StructDataFrame._
class StructGroupedData[G : StructKeyProvider, F](g: GroupedData) {
  /** Calls an aggregate function expr on column A. The aggregate function returns a new column B */
  def agg[A, B](expr: String => Column)(implicit ka: StructKeyProvider[A], kb: StructKeyProvider[B], ev: F <:< A): StructDataFrame[G with B with Nil] =
    g.agg(expr(ka.key.value).as(kb.key.value)).toStructDF[G, B]

  def agg[A, B, C](exprB: String => Column, exprC: String => Column)(implicit ka: StructKeyProvider[A], kb: StructKeyProvider[B], kc: StructKeyProvider[C], ev: F <:< A): StructDataFrame[G with B with C with Nil] = {
    val a = ka.key.value
    g.agg(exprB(a).as(kb.key.value), exprC(a).as(kc.key.value)).toStructDF[G, B, C]
  }
}
