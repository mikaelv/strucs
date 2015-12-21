package strucs.spark

import org.apache.spark.rdd.RDD
import org.apache.spark.sql.Column
import org.apache.spark.sql.{Row, Column, GroupedData, DataFrame}
import strucs._



/**
 * Wraps a DataFrame to make all operations type safe
 * @param wrappers keep the wrapper for each field so that we can lazily call wrapper.make
 *                 when we want to extract single fields from a row (see RowMap)
 */
class StructDataFrame[F](val df: DataFrame, wrappers: Map[StructKey, Wrapper[_, _]]) extends Serializable {
  type WrapperAny[A] = Wrapper[A, _]

  /** Transforms a set of Types into a set of Column that can be used in spark's api */
  trait Col[A] { self =>
    type Mixin = A
    def columns: Seq[Column]
    def wrappers: Map[StructKey, Wrapper[_, _]]

    def +[B >: F](colB: Col[B]): Col[A with B] = new Col[A with B] {
      def columns = self.columns ++ colB.columns
      def wrappers = self.wrappers ++ colB.wrappers
    }
  }

  object Col {
    // TODO create a ColumnProvider instead of StructKeyProvider? That would allow to manage expressions as well
    def apply[A >: F](implicit k: StructKeyProvider[A], wa: Wrapper[A, _]): Col[A] = new Col[A] {
      def columns = Seq(new Column(k.key.value))

      def wrappers = Map(k.key -> wa)
    }
  }


  def select(columns: Col[_]): StructDataFrame[columns.Mixin] =
    new StructDataFrame[columns.Mixin](df.select(columns.columns :_*), columns.wrappers)

  // These are not strictly necessary, but they make the API nicer
  def select[A >: F : WrapperAny : StructKeyProvider]: StructDataFrame[A] = select(Col[A])
  def select[A >: F : WrapperAny : StructKeyProvider, B >: F : WrapperAny : StructKeyProvider ]: StructDataFrame[A with B] = select(Col[A] + Col[B])


  def groupBy[G](implicit k: StructKeyProvider[G], ev: F <:< G): StructGroupedData[G, F] =
    new StructGroupedData[G, F](df.groupBy(k.key.value))


  def collect(): Array[Struct[F]] = rdd.collect()

  def show() = df.show()

  def rdd: RDD[Struct[F]] = df.rdd.map(row => Struct(RowMap(row, wrappers)))
}



case class RowMap(row: Row, wrappers: Map[StructKey, Wrapper[_, _]]) extends Map[StructKey, Any] {
  // It is easier to rebuild a brand new map rather than fiddling with row.schema
  override def +[B1 >: Any](kv: (StructKey, B1)): Map[StructKey, B1] = Map(keyValues: _*) + kv

  override def get(key: StructKey): Option[Any] = wrappers(key) match {
    case wrapper:Wrapper[w, v] => wrapper.make(row.getAs[v](key.value))
  }

  def keyValues: Seq[(StructKey, Any)] = (row.schema.fieldNames.map(StructKey.apply) zip row.toSeq).toSeq

  override def iterator: Iterator[(StructKey, Any)] = keyValues.iterator

  override def -(key: StructKey): Map[StructKey, Any] = ??? // not called from Struct
}



object StructDataFrame {

  implicit class DataFrameOps(df: DataFrame) {

    def toStructDF[A](implicit ka: StructKeyProvider[A], wa: Wrapper[A, _]): StructDataFrame[A with Nil] = {
      new StructDataFrame[A with Nil](df, Map(ka.key -> wa))
    }


    def toStructDF[A, B](implicit ka: StructKeyProvider[A], kb: StructKeyProvider[B], wa: Wrapper[A, _], wb: Wrapper[B, _]): StructDataFrame[A with B with Nil] = {
      // TODO verify that df's schema is compatible with A and B

      // TODO pass a Wrapper, and store it in the StructDataFrame, so that we can build A from the type contained in the DF
      new StructDataFrame[A with B with Nil](df, Map(ka.key -> wa, kb.key -> wb))
    }

    def toStructDF[A, B, C](implicit ka: StructKeyProvider[A], kb: StructKeyProvider[B], kc: StructKeyProvider[C]): StructDataFrame[A with B with C with Nil] = {
      // TODO verify that df's schema is compatible with A, B, C
      new StructDataFrame[A with B with C with Nil](df, ???)
    }
  }

}

import StructDataFrame._
class StructGroupedData[G : StructKeyProvider, F](g: GroupedData) {
  /** Calls an aggregate function expr on column A. The aggregate function returns a new column B */
  def agg[A >: F, B](expr: String => Column)(implicit ka: StructKeyProvider[A], kb: StructKeyProvider[B], wb: Wrapper[B, _], wg: Wrapper[G, _]): StructDataFrame[G with B with Nil] =
    g.agg(expr(ka.key.value).as(kb.key.value)).toStructDF[G, B]

  def agg[A, B, C](exprB: String => Column, exprC: String => Column)(implicit ka: StructKeyProvider[A], kb: StructKeyProvider[B], kc: StructKeyProvider[C], ev: F <:< A): StructDataFrame[G with B with C with Nil] = {
    val a = ka.key.value
    g.agg(exprB(a).as(kb.key.value), exprC(a).as(kc.key.value)).toStructDF[G, B, C]
  }
}
