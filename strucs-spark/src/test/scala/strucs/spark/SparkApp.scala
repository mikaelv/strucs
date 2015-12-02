package strucs.spark

import org.apache.spark.rdd.RDD
import org.apache.spark.sql.SQLContext
import org.apache.spark.{SparkConf, SparkContext}
import strucs.spark.StructDataFrame._
import strucs._

abstract class KeyCompanion[T](key: String) {
  implicit val keyProvider: StructKeyProvider[T] = StructKeyProvider[T](StructKey(key))
}

// TODO explore tagged types
case class Name(v: String) extends AnyVal
object Name {
  implicit val keyProvider = StructKeyProvider[Name](StructKey("name"))
  implicit val wrapper = Wrapper.materializeWrapper[Name, String]
}
case class Age(v: Int) extends AnyVal
object Age {
  implicit val keyProvider = StructKeyProvider[Age](StructKey("age"))
  implicit val wrapper = Wrapper.materializeWrapper[Age, Int]

}
case class AvgAge(v: Int) extends AnyVal
object AvgAge extends KeyCompanion("avgAge") {
  implicit val wrapper = Wrapper.materializeWrapper[AvgAge, Int]
}

case class MaxAge(v: Int) extends AnyVal
object MaxAge extends KeyCompanion("maxAge") {
  implicit val wrapper = Wrapper.materializeWrapper[MaxAge, Int]
}


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
  // Standard DataFrame: runtime failure if these fields do not exist
  df.groupBy("name").agg(avg("age"), max("age")).show()

  // StructDataFrame: method calls are type safe after the initial conversion
  val sdf: StructDataFrame[Name with Age with Nil]= df.toStructDF[Name, Age]
  sdf.select[Name].show()
  val avgSdf = sdf.groupBy[Name].agg[Age, AvgAge](avg)
  avgSdf.show()
  avgSdf.select[AvgAge].show()

  sdf.groupBy[Name].agg[Age, AvgAge, MaxAge](avg, max).select[Name, MaxAge].show() // TODO it would be nice to verify that avg cannot be called on a non-numeric type

  // RDD style
  val rdd: RDD[Struct[Name with AvgAge with Nil]] = avgSdf.rdd.map(s => Struct.empty + s.get[Name] + s.get[AvgAge])
  println(rdd.collect().mkString("\n"))

}




