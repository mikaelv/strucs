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
    (1, "Albert", 72, "Paris"),
    (2, "Gerard", 55, "London"),
    (3, "Gerard", 65, "London"))).toDF(
    "id",  "name"  , "age", "city")

  val sdf1: StructDataFrame[Name] = df.toStructDF[Name]
  val sdf2: StructDataFrame[Id with Name with Age with City] = df.toStructDF[Id, Name, Age, City]

  "a StructDataFrame" can "select 1 column by its type" in {
    val actual: Array[Name] = sdf1.select[Name].collect().map(_.get[Name])
    val expected = Array(Name("Albert"), Name("Gerard"), Name("Gerard"))
    actual should === (expected)
  }

  it should "prevent from selecting a column that is not in the DataFrame" in {
    assertTypeError("sdf1.select[AvgAge]")
  }

  it can "select 2 columns by their types" in {
    val actual: Array[(Name, Age)] = sdf2.select[Name, Age].collect().map(s => (s.get[Name], s.get[Age]))
    val expected = Array(
      (Name("Albert"), Age(72)),
      (Name("Gerard"), Age(55)),
      (Name("Gerard"), Age(65)))
    actual should === (expected)
  }

  it can "group by 2 columns using their types" in {
    val grouped = sdf2.groupBy[City, Name].agg[Age, AvgAge, MaxAge](avg, max)
    grouped.show()
    val actual = grouped.collect().map(s => (s.get[Name].v, s.get[AvgAge].v, s.get[MaxAge].v))
    val expected = Array(
      ("Gerard", 60D, 65),
      ("Albert", 72D, 72)
    )
    actual should === (expected)
  }


}
