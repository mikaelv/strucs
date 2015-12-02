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

  val sdf1: StructDataFrame[Name with Nil] = df.toStructDF[Name]
  val sdf2: StructDataFrame[Name with Age with Nil] = df.toStructDF[Name, Age]

  "a StructDataFrame" can "select 1 column by its type" in {
    val actual: Array[Name] = sdf1.select[Name].collect().map(_.get[Name])
    val expected = Array(Name("Albert"), Name("Gerard"), Name("Gerard"))
    actual should === (expected)
  }

  "a StructDataFrame" can "select 2 columns by their types" in {
    val actual: Array[(Name, Age)] = sdf2.select[Name, Age].collect().map(s => (s.get[Name], s.get[Age]))
    val expected = Array(
      (Name("Albert"), Age(72)),
      (Name("Gerard"), Age(55)),
      (Name("Gerard"), Age(65)))
    actual should === (expected)
  }
}
