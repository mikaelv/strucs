package strucs.spark

import org.apache.spark.rdd.RDD
import org.apache.spark.sql.{Column, GroupedData, DataFrame}
import strucs.{Struct, StructKeyProvider}

/**
 * Wraps a DataFrame to make all operations type safe
 */
class StructDataFrame[F] private(val df: DataFrame) {
  def select[A](implicit k: StructKeyProvider[A], ev: F <:< A): StructDataFrame[A] =
    new StructDataFrame[A](df.select(k.key.value))

  def select[A, B](implicit ka: StructKeyProvider[A], kb: StructKeyProvider[B], eva: F <:< A, evb: F <:<B): StructDataFrame[A with B] =
    new StructDataFrame[A with B](df.select(ka.key.value, kb.key.value))

  def groupBy[G](implicit k: StructKeyProvider[G], ev: F <:< G): StructGroupedData[G, F] =
    new StructGroupedData[G, F](df.groupBy(k.key.value))


  def show() = df.show()

  def structRDD: RDD[Struct[F]] = df.rdd.map(row => ???)
}

object StructDataFrame {

  implicit class DataFrameOps(df: DataFrame) {
    def toStructDF[A, B](implicit ka: StructKeyProvider[A], kb: StructKeyProvider[B]): StructDataFrame[A with B] = {
      // TODO verify that df's schema is compatible with A and B
      new StructDataFrame[A with B](df)
    }

    def toStructDF[A, B, C](implicit ka: StructKeyProvider[A], kb: StructKeyProvider[B], kc: StructKeyProvider[C]): StructDataFrame[A with B with C] = {
      // TODO verify that df's schema is compatible with A, B, C
      new StructDataFrame[A with B with C](df)
    }
  }

}

import StructDataFrame._
class StructGroupedData[G : StructKeyProvider, F](g: GroupedData) {
  /** Calls an aggregate function expr on column A. The aggregate function returns a new column B */
  def agg[A, B](expr: String => Column)(implicit ka: StructKeyProvider[A], kb: StructKeyProvider[B], ev: F <:< A): StructDataFrame[G with B] =
    g.agg(expr(ka.key.value).as(kb.key.value)).toStructDF[G, B]

  def agg[A, B, C](exprB: String => Column, exprC: String => Column)(implicit ka: StructKeyProvider[A], kb: StructKeyProvider[B], kc: StructKeyProvider[C], ev: F <:< A): StructDataFrame[G with B with C] = {
    val a = ka.key.value
    g.agg(exprB(a).as(kb.key.value), exprC(a).as(kc.key.value)).toStructDF[G, B, C]
  }
}
