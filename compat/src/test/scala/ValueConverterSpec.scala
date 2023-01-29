package reactivemongo

import scala.util.Success

import _root_.play.api.libs.json._

import reactivemongo.api.bson.{
  BSON,
  BSONDocumentWriter,
  BSONReader,
  BSONValue
}

final class ValueConverterSpec extends org.specs2.mutable.Specification {
  "Value converters".title

  "Case class" should {
    "be properly serialized using default Play JSON handlers" >> {
      stopOnFail

      "for Lorem" >> valueConversionSpec(PlayFixtures.lorem)

      "for Bar" >> valueConversionSpec(PlayFixtures.bar)

      "for Foo" >> valueConversionSpec(PlayFixtures.foo)
    }
  }

  // ---

  private def valueConversionSpec[T: OWrites](
      value: T
    )(implicit
      jsr: Reads[T]
    ) = {

    import _root_.reactivemongo.play.json.compat.ValueConverters._

    val jsIn: JsValue = Json.toJson(value)
    val bsonIn: BSONValue = jsIn
    val jsOut: JsValue = bsonIn

    "be extracted" in {
      jsIn aka BSONValue.pretty(bsonIn) must_=== jsOut and {
        val o = Json.obj("bson" -> bsonIn) // Json.JsValueWrapper conversion

        (o \ "bson").get must_=== jsOut
      }
    }

    "be written to JSON" in {
      jsOut.validate[T] aka Json.stringify(jsOut) must beLike[JsResult[T]] {
        case JsSuccess(out, _) => out must_=== value
      }
    }

    "be written to BSON" in {
      import _root_.reactivemongo.play.json.compat.json2bson._

      val bsonW: BSONDocumentWriter[T] = implicitly[OWrites[T]]
      val bsonR: BSONReader[T] = jsr

      bsonW.writeTry(value) must beSuccessfulTry[BSONValue].like {
        case written =>
          Success(written) must beSuccessfulTry(bsonIn) and {
            bsonR.readTry(written) must beSuccessfulTry(value)
          }
      } and {
        // Not implicit conversion, but implicit derived instances
        BSON.write(value) must beSuccessfulTry[BSONValue].like {
          case written =>
            Success(written) must beSuccessfulTry(bsonIn) and {
              BSON.read(written)(bsonR) must beSuccessfulTry(value)
            }
        }
      }
    }
  }
}

@com.github.ghik.silencer.silent("Unused import" /* TestCompat play-2.6+ */ )
object PlayFixtures {
  import java.util.{ Date, Locale, UUID }
  import java.time.{
    Instant,
    LocalDate,
    LocalDateTime,
    OffsetDateTime,
    ZoneId,
    ZonedDateTime
  }
  import java.time.{ Duration => JDuration }

  import TestCompat._

  case class Foo(
      int: Int,
      short: Short,
      byte: Byte,
      str: String,
      locale: Locale,
      flag: Boolean,
      bar: Bar)

  object Foo {

    implicit val writes: OWrites[Foo] = OWrites[Foo] { foo =>
      Json.obj(
        "int" -> foo.int,
        "short" -> foo.short,
        "byte" -> foo.byte,
        "str" -> foo.str,
        "locale" -> foo.locale,
        "flag" -> foo.flag,
        "bar" -> foo.bar
      )
    }

    implicit val reads: Reads[Foo] = Reads[Foo] { js =>
      for {
        int <- (js \ "int").validate[Int]
        short <- (js \ "short").validate[Short]
        byte <- (js \ "byte").validate[Byte]
        str <- (js \ "str").validate[String]
        locale <- (js \ "locale").validate[Locale]
        flag <- (js \ "flag").validate[Boolean]
        bar <- (js \ "bar").validate[Bar]
      } yield Foo(int, short, byte, str, locale, flag, bar)
    }
  }

  case class Bar(
      duration: JDuration,
      long: Long,
      float: Float,
      double: Double,
      bigDec: BigDecimal,
      bigInt: BigInt,
      jbigInt: java.math.BigInteger,
      lorem: Lorem)

  object Bar {

    implicit val writes: OWrites[Bar] = OWrites[Bar] { bar =>
      Json.obj(
        "duration" -> bar.duration,
        "long" -> bar.long,
        "float" -> bar.float,
        "double" -> bar.double,
        "bigDec" -> bar.bigDec,
        "bigInt" -> bar.bigInt,
        "jbigInt" -> bar.jbigInt,
        "lorem" -> bar.lorem
      )
    }

    implicit val reads: Reads[Bar] = Reads[Bar] { js =>
      for {
        duration <- (js \ "duration").validate[JDuration]
        long <- (js \ "long").validate[Long]
        float <- (js \ "float").validate[Float]
        double <- (js \ "double").validate[Double]
        bigDec <- (js \ "bigDec").validate[BigDecimal]
        bigInt <- (js \ "bigInt").validate[BigInt]
        jbigInt <- (js \ "jbigInt").validate[java.math.BigInteger]
        lorem <- (js \ "lorem").validate[Lorem]
      } yield Bar(duration, long, float, double, bigDec, bigInt, jbigInt, lorem)
    }
  }

  case class Lorem(
      id: UUID,
      date: Date,
      instant: Instant,
      localDate: LocalDate,
      localDateTime: LocalDateTime,
      offsetDateTime: OffsetDateTime,
      zid: ZoneId,
      zdt: ZonedDateTime)

  object Lorem {

    implicit val writes: OWrites[Lorem] = OWrites[Lorem] { lorem =>
      Json.obj(
        "id" -> lorem.id.toString,
        "date" -> lorem.date,
        "instant" -> lorem.instant,
        "localDate" -> lorem.localDate,
        "localDateTime" -> lorem.localDateTime,
        "offsetDateTime" -> lorem.offsetDateTime,
        "zid" -> lorem.zid,
        "zdt" -> lorem.zdt
      )
    }

    implicit val reads: Reads[Lorem] = Reads[Lorem] { js =>
      for {
        id <- (js \ "id").validate[UUID]
        date <- (js \ "date").validate[Date]
        instant <- (js \ "instant").validate[Instant]
        localDate <- (js \ "localDate").validate[LocalDate]
        localDateTime <- (js \ "localDateTime").validate[LocalDateTime]
        offsetDateTime <- (js \ "offsetDateTime").validate[OffsetDateTime]
        zid <- (js \ "zid").validate[ZoneId]
        zdt <- (js \ "zdt").validate[ZonedDateTime]
      } yield Lorem(
        id,
        date,
        instant,
        localDate,
        localDateTime,
        offsetDateTime,
        zid,
        zdt
      )
    }
  }

  val lorem = Lorem(
    id = UUID.randomUUID(),
    date = new Date(),
    instant = Instant.now(),
    localDate = LocalDate.now(),
    localDateTime = LocalDateTime.now(),
    offsetDateTime = OffsetDateTime.now(),
    zid = ZoneId.systemDefault(),
    zdt = ZonedDateTime.now()
  )

  val bar = Bar(
    duration = JDuration.ofDays(2),
    long = 2L,
    float = 3.45F,
    double = 67.891D,
    bigDec = BigDecimal("10234.56789"),
    bigInt = BigInt(Long.MaxValue),
    jbigInt = new java.math.BigInteger("9876"),
    lorem = lorem
  )

  val foo = Foo(
    int = 1,
    short = 2,
    byte = 3,
    str = "value",
    locale = Locale.FRANCE,
    flag = false,
    bar = bar
  )

}
