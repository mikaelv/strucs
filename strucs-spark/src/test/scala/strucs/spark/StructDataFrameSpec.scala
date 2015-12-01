package strucs.spark

import org.apache.spark.sql.SQLContext
import org.apache.spark.{SparkContext, SparkConf}
import org.scalactic.TypeCheckedTripleEquals
import org.scalatest.{Matchers, FlatSpec}
import strucs.{Nil, Struct}

/**
 */
class StructDataFrameSpec extends FlatSpec with Matchers with TypeCheckedTripleEquals {
  val conf = new SparkConf().setAppName("Simple Application").setMaster("local[2]")
  val sc = new SparkContext(conf)
  val sqlc = new SQLContext(sc)
  import org.apache.spark.sql.functions._
  import sqlc.implicits._
  import StructDataFrame._


  val df = sc.makeRDD(Seq(
    ("Albert", 72),
    ("Gerard", 55),
    ("Gerard", 65))).toDF(
      "name"  , "age")

  val sdf: StructDataFrame[Name with Age with Nil] = df.toStructDF[Name, Age]

  "a StructDataFrame" can "select 1 column by its type" in {
    val actual: Array[Struct[Name with Nil]] = sdf.select[Name].collect()
    val expected = Array(Struct(Name("Albert")), Struct(Name("Gerard")), Struct(Name("Gerard")))
    actual should === (expected)
  }
}
