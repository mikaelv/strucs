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
  import StructDataFrame.WrapperAny

  def select(columns: Col[_]): StructDataFrame[columns.Mixin] =
    new StructDataFrame[columns.Mixin](df.select(columns.columns :_*), columns.wrappers)

  // These are not strictly necessary, but they make the API nicer
  def select[A >: F : WrapperAny : StructKeyProvider]: StructDataFrame[A] = select(Col[A])
  def select[A >: F : WrapperAny : StructKeyProvider,
             B >: F : WrapperAny : StructKeyProvider ]: StructDataFrame[A with B] = select(Col[A] + Col[B])


  def groupBy[G](columns: Col[G]): StructGroupedData[G, F] = // TODO use Col[_] as in select, to avoid inference to Nothing
    new StructGroupedData[G, F](df.groupBy(columns.columns: _*), columns)

  def groupBy[A >: F : WrapperAny : StructKeyProvider]: StructGroupedData[A, F] = groupBy(Col[A])

  def groupBy[A >: F : WrapperAny : StructKeyProvider,
              B >: F : WrapperAny : StructKeyProvider]: StructGroupedData[A with B, F] = groupBy[A with B](Col[A] + Col[B])



  def collect(): Array[Struct[F]] = rdd.collect()

  def show() = df.show()

  def rdd: RDD[Struct[F]] = df.rdd.map(row => Struct(RowMap(row, wrappers)))
}


/** Wraps a Row in order to access it as if it was a Map */
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

/** Transforms a set of Types into a set of Column that can be used in spark's api */
trait Col[A] { self =>
  type Mixin = A
  def columns: Seq[Column]
  def wrappers: Map[StructKey, Wrapper[_, _]]

  // TODO type constraint B >: F ??
  def +[B](colB: Col[B]): Col[A with B] = new Col[A with B] {
    def columns = self.columns ++ colB.columns
    def wrappers = self.wrappers ++ colB.wrappers
  }
}

object Col {
  // TODO create a ColumnProvider instead of StructKeyProvider? That would allow to manage expressions as well
  def apply[A](implicit k: StructKeyProvider[A], wa: Wrapper[A, _]): Col[A] = new Col[A] {

    def columns = Seq(new Column(k.key.value))

    def wrappers = Map(k.key -> wa)
  }
}


object StructDataFrame {

  type WrapperAny[A] = Wrapper[A, _]

  implicit class DataFrameOps(df: DataFrame) {

    // TODO verify that df's schema is compatible with cols
    def toStructDF(cols: Col[_]): StructDataFrame[cols.Mixin] = new StructDataFrame[cols.Mixin](df, cols.wrappers)

    def toStructDF[A : StructKeyProvider : WrapperAny]: StructDataFrame[A] = toStructDF(Col[A])

    def toStructDF[A : StructKeyProvider : WrapperAny,
                   B : StructKeyProvider : WrapperAny]: StructDataFrame[A with B] = toStructDF(Col[A] + Col[B])

    def toStructDF[A : StructKeyProvider : WrapperAny,
                   B : StructKeyProvider : WrapperAny,
                   C : StructKeyProvider : WrapperAny]: StructDataFrame[A with B with C] = toStructDF(Col[A] + Col[B] + Col[C])

    def toStructDF[A : StructKeyProvider : WrapperAny,
                   B : StructKeyProvider : WrapperAny,
                   C : StructKeyProvider : WrapperAny,
                   D : StructKeyProvider : WrapperAny]: StructDataFrame[A with B with C with D] = toStructDF(Col[A] + Col[B] + Col[C] + Col[D])


  }

}

import StructDataFrame._
class StructGroupedData[G, F](g: GroupedData, cols: Col[G]) {
  /** Calls an aggregate function expr on column A. The aggregate function returns a new column B */
  def agg[A >: F, B](expr: String => Column)(implicit ka: StructKeyProvider[A], kb: StructKeyProvider[B], wb: Wrapper[B, _]): StructDataFrame[G with B] =
    g.agg(expr(ka.key.value).as(kb.key.value)).toStructDF(cols + Col[B])

  def agg[A >: F, B, C](exprB: String => Column, exprC: String => Column)(implicit ka: StructKeyProvider[A], kb: StructKeyProvider[B], kc: StructKeyProvider[C], wb: Wrapper[B, _], wc: Wrapper[C, _]): StructDataFrame[G with B with C] = {
    val a = ka.key.value
    g.agg(exprB(a).as(kb.key.value), exprC(a).as(kc.key.value)).toStructDF(cols + Col[B] + Col[C])
  }
}
