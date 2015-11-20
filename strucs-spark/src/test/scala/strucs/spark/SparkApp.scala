package strucs.spark

import org.apache.spark.sql.SQLContext
import org.apache.spark.{SparkConf, SparkContext}
import strucs.spark.StructDataFrame._
import strucs.{StructKey, StructKeyProvider}

abstract class KeyCompanion[T](key: String) {
  implicit val keyProvider: StructKeyProvider[T] = StructKeyProvider[T](StructKey(key))
}

// TODO explore tagged types
case class Name(v: String) extends AnyVal
object Name {
  implicit val keyProvider = StructKeyProvider[Name](StructKey("name"))
}
case class Age(v: Int) extends AnyVal
object Age {
  implicit val keyProvider = StructKeyProvider[Age](StructKey("age"))
}
case class AvgAge(v: Int) extends AnyVal
object AvgAge extends KeyCompanion("avgAge")

case class MaxAge(v: Int) extends AnyVal
object MaxAge extends KeyCompanion("maxAge")


/**
 */
object SparkApp extends App {
  val conf = new SparkConf().setAppName("Simple Application").setMaster("local[2]")
  val sc = new SparkContext(conf)
  val sqlc = new SQLContext(sc)
  import org.apache.spark.sql.functions._
  import sqlc.implicits._

  val df = sc.makeRDD(Seq(
    ("Albert", 72),
    ("Gerard", 55),
    ("Gerard", 65))).toDF(
     "name"  , "age")

  df.groupBy("name").agg(avg("age"), max("age")).show()

  val sdf = df.toStructDF[Name, Age]
  sdf.select[Name].show()
  val avgSdf = sdf.groupBy[Name].agg[Age, AvgAge](avg)
  avgSdf.show()
  avgSdf.select[AvgAge].show()

  sdf.groupBy[Name].agg[Age, AvgAge, MaxAge](avg, max).select[Name, MaxAge].show()
  // Actually I do not use the values contained in these types.
  // I should verify that avg cannot be called on a Wrapper[String]
}




