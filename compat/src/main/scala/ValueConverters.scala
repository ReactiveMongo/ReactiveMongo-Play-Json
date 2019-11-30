package reactivemongo.play.json.compat

import java.util.Base64

import java.time.ZonedDateTime
import java.time.format.DateTimeFormatter

import scala.language.implicitConversions

import scala.util.{ Failure, Success }
import scala.util.control.NonFatal

import play.api.libs.json.{
  JsArray,
  JsBoolean,
  JsNull,
  JsNumber,
  JsObject,
  JsString,
  JsValue
}

import reactivemongo.api.bson.{
  BSONArray,
  BSONBinary,
  BSONBoolean,
  BSONDateTime,
  BSONDecimal,
  BSONDocument,
  BSONDouble,
  BSONInteger,
  BSONJavaScript,
  BSONJavaScriptWS,
  BSONLong,
  BSONMaxKey,
  BSONMinKey,
  BSONNull,
  BSONObjectID,
  BSONRegex,
  BSONString,
  BSONSymbol,
  BSONTimestamp,
  BSONUndefined,
  BSONValue,
  Digest,
  Subtype
}

/**
 * See [[compat$]] and [[ValueConverters]].
 */
object ValueConverters extends ValueConverters

/**
 * @define syntaxDocBaseUrl https://docs.mongodb.com/manual/reference/mongodb-extended-json
 * @define specsUrl https://github.com/mongodb/specifications/blob/master/source/extended-json.rst
 *
 * Implicit conversions for value types between
 * `play.api.libs.json` and `reactivemongo.api.bson`,
 * using [[$syntaxDocBaseUrl MongoDB Extended JSON]] syntax (v2).
 *
 * {{{
 * import reactivemongo.play.json.compat.ValueConverters._
 *
 * def foo(v: BSONValue): JsValue =
 *   implicitly[JsValue](v) // ValueConverters.fromValue
 *
 * def bar(v: JsValue): BSONValue =
 *   implicitly[BSONValue](v) // ValueConverters.toValue
 * }}}
 *
 * ''Note:'' Logger `reactivemongo.api.play.json.ValueConverters` can be used to debug.
 *
 * See [[https://github.com/mongodb/specifications/blob/master/source/extended-json.rst#conversion-table specifications]].
 */
trait ValueConverters
  extends ValueConvertersCompat with LowPriority1Converters {

  private val syntaxDocBaseUrl =
    "https://docs.mongodb.com/manual/reference/mongodb-extended-json"

  private val logger = org.slf4j.LoggerFactory.getLogger(getClass)

  implicit def fromArray(bson: BSONArray): JsArray =
    JsArray(bson.values.map(fromValue))

  /**
   * See [[$syntaxDocBaseUrl/#bson.Binary syntax]]:
   *
   * {{{
   * { "\$binary":
   *    {
   *       "base64": "<payload>",
   *       "subType": "<t>"
   *    }
   * }
   * }}}
   */
  implicit final def fromBinary(bin: BSONBinary): JsObject =
    JsObject(Map[String, JsValue](
      f"$$binary" -> JsObject(Map[String, JsValue](
        "base64" -> JsString(base64Enc encodeToString bin.byteArray),
        f"subType" -> JsString(
          Digest.hex2Str(Array(bin.subtype.value.toByte)))))))

  implicit final def fromBoolean(bson: BSONBoolean): JsBoolean =
    if (bson.value) JsTrue else JsFalse

  /**
   * See [[$syntaxDocBaseUrl/#bson.Date syntax]]:
   *
   * {{{
   * { "\$date": { "\$numberLong": "<millis>" } }
   * }}}
   */
  implicit final def fromDateTime(bson: BSONDateTime): JsObject =
    JsObject(Map[String, JsValue](f"$$date" -> dsl.long(bson.value)))

  /**
   * See [[$syntaxDocBaseUrl/#bson.Decimal128 syntax]]:
   *
   * {{{
   * { "\$numberDecimal": "<number>" }
   * }}}
   */
  implicit final def fromDecimal(bson: BSONDecimal): JsObject =
    JsObject(Map[String, JsValue](f"$$numberDecimal" -> JsString(bson.toString)))

  /** Converts to a JSON object */
  implicit def fromDocument(bson: BSONDocument): JsObject =
    JsObject(bson.elements.map(elem => elem.name -> fromValue(elem.value)))

  /**
   * See [[$syntaxDocBaseUrl/#bson.Double syntax]]:
   *
   * {{{
   * // For finite numbers:
   * { "\$numberDouble": "<decimal string>" }
   *
   * // For other numbers:
   * { "\$numberDouble": <"Infinity"|"-Infinity"|"NaN"> }
   * }}}
   */
  implicit final def fromDouble(bson: BSONDouble): JsObject =
    dsl.double(bson.value)

  /**
   * See [[$syntaxDocBaseUrl/#bson.Int32 syntax]]:
   *
   * {{{
   * { "\$numberInt": "<number>" }
   * }}}
   */
  implicit final def fromInteger(bson: BSONInteger): JsObject =
    dsl.int(bson.value)

  /**
   * {{{
   * { "\$code": "<javascript>" }
   * }}}
   */
  implicit final def fromJavaScript(bson: BSONJavaScript): JsObject =
    JsObject(Map[String, JsValue](f"$$code" -> JsString(bson.value)))

  /**
   * {{{
   * {
   *   "\$code": "<javascript>",
   *   "\$scope": { }
   * }
   * }}}
   */
  implicit final def fromJavaScriptWS(bson: BSONJavaScriptWS): JsObject =
    JsObject(Map[String, JsValue](
      f"$$code" -> JsString(bson.value),
      f"$$scope" -> fromDocument(bson.scope)))

  /**
   * See [[$syntaxDocBaseUrl/#bson.Int64 syntax]]:
   *
   * {{{
   * { "\$numberLong": "<number>" }
   * }}}
   */
  implicit final def fromLong(bson: BSONLong): JsObject =
    dsl.long(bson.value)

  private[reactivemongo] val JsMaxKey =
    JsObject(Map[String, JsValue](f"$$maxKey" -> JsNumber(1)))

  /**
   * {{{
   * { "\$maxKey": 1 }
   * }}}
   */
  implicit final val fromMaxKey: BSONMaxKey => JsObject = _ => JsMaxKey

  private[reactivemongo] val JsMinKey =
    JsObject(Map[String, JsValue](f"$$minKey" -> JsNumber(1)))

  /**
   * {{{
   * { "\$minKey": 1 }
   * }}}
   */
  implicit final val fromMinKey: BSONMinKey => JsObject = _ => JsMinKey

  implicit val fromNull: BSONNull => JsNull.type = _ => JsNull

  /**
   * See [[$syntaxDocBaseUrl/#bson.ObjectId syntax]]:
   *
   * {{{
   * { "\$oid": "<ObjectId bytes>" }
   * }}}
   */
  implicit final def fromObjectID(bson: BSONObjectID): JsObject =
    JsObject(Map[String, JsValue](f"$$oid" -> JsString(bson.stringify)))

  /**
   * See [[$syntaxDocBaseUrl/#bson.Regular-Expression syntax]]:
   *
   * {{{
   * { "\$regularExpression":
   *    {
   *       "pattern": "<regexPattern>",
   *       "options": "<options>"
   *   }
   * }
   * }}}
   */
  implicit final def fromRegex(rx: BSONRegex): JsObject = {
    val builder = scala.collection.mutable.Map.empty[String, JsValue]

    builder.put("pattern", JsString(rx.value))

    if (rx.flags.nonEmpty) {
      builder.put("options", JsString(rx.flags))
    }

    JsObject(Map[String, JsValue](
      f"$$regularExpression" -> JsObject(builder.toMap)))
  }

  implicit final def fromStr(bson: BSONString): JsString = JsString(bson.value)

  /**
   * {{{
   * { "\$symbol": "<name>" }
   * }}}
   */
  implicit final def fromSymbol(bson: BSONSymbol): JsObject =
    JsObject(Map[String, JsValue](f"$$symbol" -> JsString(bson.value)))

  /**
   * See [[$syntaxDocBaseUrl/#bson.Timestamp syntax]]:
   *
   * {{{
   * { "\$timestamp": {"t": <t>, "i": <i>} }
   * }}}
   */
  implicit final def fromTimestamp(ts: BSONTimestamp): JsObject =
    JsObject(Map[String, JsValue](
      f"$$timestamp" -> JsObject(Map[String, JsValue](
        "t" -> JsNumber(ts.time), "i" -> JsNumber(ts.ordinal)))))

  private[reactivemongo] val JsUndefined =
    JsObject(Map[String, JsValue](f"$$undefined" -> JsTrue))

  /**
   * {{{
   * { "\$undefined": true }
   * }}}
   */
  implicit final val fromUndefined: BSONUndefined => JsObject = _ => JsUndefined

  // ---

  implicit final def toArray(js: JsArray): BSONArray =
    BSONArray(js.value.map(toValue))

  implicit val toNull: JsNull.type => BSONNull =
    _ => BSONNull

  /**
   * If the number:
   *
   * - is not whole then it's converted to BSON double,
   * - is a valid integer then it's converted to a BSON integer (int32),
   * - otherwise it's converted to a BSON long integer (int64).
   */
  implicit final def toNumber(js: JsNumber): BSONValue = {
    val v = js.value

    if (!v.ulp.isWhole) {
      BSONDouble(v.toDouble)
    } else if (v.isValidInt) {
      BSONInteger(v.toInt)
    } else {
      BSONLong(v.toLong)
    }
  }

  /**
   * First checks whether an explicit type (e.g. `\$binary`) is specified,
   * otherwise converts to a BSON document.
   */
  implicit final def fromObject(js: JsObject): BSONValue = js match {
    case BinaryObject(bin) => bin
    case DateObject(date) => date
    case Decimal128Object(dec) => dec
    case DoubleObject(d) => d

    case JavaScriptWSObject(js) => js
    case JavaScriptObject(js) => js

    case Int32Object(i) => i
    case Int64Object(i) => i
    case MaxKeyObject() => BSONMaxKey
    case MinKeyObject() => BSONMinKey
    case ObjectIdObject(oid) => oid
    case RegexObject(re) => re
    case TimestampObject(ts) => ts
    case UndefinedObject() => BSONUndefined
    case SymbolObject(sym) => sym
    case _ => toDocument(js)
  }

  implicit final def toStr(js: JsString): BSONValue = {
    if (js.value == null) BSONNull
    else BSONString(js.value)
  }

  /** See [[$specsUrl#conversion-table syntax]] */
  object JavaScriptObject {
    def unapply(obj: JsObject): Option[BSONJavaScript] =
      (obj \ f"$$code").asOpt[String].map(BSONJavaScript(_))
  }

  /** See [[$specsUrl#conversion-table syntax]] */
  object JavaScriptWSObject {
    def unapply(obj: JsObject): Option[BSONJavaScriptWS] = for {
      scope <- (obj \ f"$$scope").toOption.collect {
        case obj @ JsObject(_) => toDocument(obj)
      }
      code <- (obj \ f"$$code").asOpt[String]
    } yield BSONJavaScriptWS(code, scope)
  }

  /**
   * See [[$syntaxDocBaseUrl/#bson.Binary syntax]]
   */
  private[json] object BinaryObject {
    def unapply(obj: JsObject): Option[BSONBinary] = for {
      bin <- (obj \ f"$$binary").toOption.collect {
        case obj @ JsObject(_) => obj
      }
      hexaValue <- (bin \ "base64").asOpt[String]

      hexaTpe <- (bin \ "subType").asOpt[String]
      tpeByte <- Digest.str2Hex(hexaTpe).headOption

      subtpe <- try {
        Some(Subtype(tpeByte))
      } catch {
        case NonFatal(cause) =>
          logger.debug(s"Invalid Binary 'subType': $hexaTpe; See $syntaxDocBaseUrl/#bson.Binary", cause)

          None
      }

      bytes <- try {
        Some(base64Dec decode hexaValue)
      } catch {
        case NonFatal(cause) =>
          logger.debug(s"Invalid Binary 'base64' value; $syntaxDocBaseUrl/#bson.Binary", cause)

          None
      }
    } yield BSONBinary(bytes, subtpe)
  }

  /**
   * See [[$syntaxDocBaseUrl/#bson.Date syntax]]
   */
  private[json] object DateObject {
    @SuppressWarnings(Array("LooksLikeInterpolatedString"))
    def unapply(obj: JsObject): Option[BSONDateTime] =
      (obj \ f"$$date").toOption.flatMap {
        case JsString(repr) => try {
          val dt = ZonedDateTime.parse(repr, DateTimeFormatter.ISO_DATE_TIME)

          Some(BSONDateTime(dt.toInstant.toEpochMilli))
        } catch {
          case NonFatal(cause) =>
            logger.debug(s"Invalid relaxed Date: $repr; $syntaxDocBaseUrl/#bson.Date", cause)

            None
        }

        case Int64Object(ms) =>
          Some(BSONDateTime(ms.value))

        case _ => None
      }
  }

  /**
   * See [[$syntaxDocBaseUrl/#bson.Decimal128 syntax]]
   */
  private[json] object Decimal128Object {
    @SuppressWarnings(Array("LooksLikeInterpolatedString"))
    def unapply(obj: JsObject): Option[BSONDecimal] =
      (obj \ f"$$numberDecimal").asOpt[String].flatMap { repr =>
        BSONDecimal.parse(repr) match {
          case Success(v) => Some(v)

          case Failure(cause) => {
            logger.debug(s"Invalid JSON Decimal; See $syntaxDocBaseUrl/#bson.Decimal128", cause)

            None
          }
        }
      }
  }

  /**
   * See [[$syntaxDocBaseUrl/#bson.Double syntax]]
   */
  private[json] object DoubleObject {
    @SuppressWarnings(Array("LooksLikeInterpolatedString"))
    def unapply(obj: JsObject): Option[BSONDouble] =
      (obj \ f"$$numberDouble").asOpt[String].flatMap {
        case "-Infinity" => Some(BSONDouble(Double.NegativeInfinity))
        case "Infinity" => Some(BSONDouble(Double.PositiveInfinity))
        case "NaN" => Some(BSONDouble(Double.NaN))

        case repr => try {
          Some(BSONDouble(repr.toDouble))
        } catch {
          case NonFatal(cause) =>
            logger.debug(s"Invalid JSON Double; See $syntaxDocBaseUrl/#bson.Double", cause)

            None
        }
      }
  }

  /**
   * See [[$syntaxDocBaseUrl/#bson.Int32 syntax]]
   */
  private[json] object Int32Object {
    @SuppressWarnings(Array("LooksLikeInterpolatedString"))
    def unapply(obj: JsObject): Option[BSONInteger] =
      (obj \ f"$$numberInt").asOpt[String].flatMap { repr =>
        try {
          Some(BSONInteger(repr.toInt))
        } catch {
          case NonFatal(cause) =>
            logger.debug(s"Invalid JSON Int32; See $syntaxDocBaseUrl/#bson.Int32", cause)

            None
        }
      }
  }

  /**
   * See [[$syntaxDocBaseUrl/#bson.Int64 syntax]]
   */
  private[json] object Int64Object {
    @SuppressWarnings(Array("LooksLikeInterpolatedString"))
    def unapply(obj: JsObject): Option[BSONLong] =
      (obj \ f"$$numberLong").asOpt[String].flatMap { repr =>
        try {
          Some(BSONLong(repr.toLong))
        } catch {
          case NonFatal(cause) =>
            logger.debug(s"Invalid JSON Int64; See $syntaxDocBaseUrl/#bson.Int64", cause)

            None
        }
      }
  }

  private val JsOne = JsNumber(1)

  /**
   * See [[$syntaxDocBaseUrl/#bson.MaxKey syntax]]
   */
  private[json] object MaxKeyObject {
    @SuppressWarnings(Array("LooksLikeInterpolatedString"))
    def unapply(obj: JsObject): Boolean =
      obj.value.get(f"$$maxKey") match {
        case Some(JsOne) => true
        case Some(JsBoolean(true)) => true
        case _ => false
      }
  }

  /**
   * See [[$syntaxDocBaseUrl/#bson.MaxKey syntax]]
   */
  private[json] object MinKeyObject {
    @SuppressWarnings(Array("LooksLikeInterpolatedString"))
    def unapply(obj: JsObject): Boolean =
      obj.value.get(f"$$minKey") match {
        case Some(JsOne) => true
        case Some(JsBoolean(true)) => true
        case _ => false
      }
  }

  /**
   * See [[$syntaxDocBaseUrl/#bson.ObjectId syntax]]
   */
  private[json] object ObjectIdObject {
    @SuppressWarnings(Array("LooksLikeInterpolatedString"))
    def unapply(obj: JsObject): Option[BSONObjectID] = {
      if (obj.fields.size != 1) None
      else (obj \ f"$$oid").asOpt[String].flatMap { repr =>
        BSONObjectID.parse(repr) match {
          case Success(oid) => Some(oid)

          case Failure(cause) => {
            logger.debug(s"Invalid ObjectId: $repr; See $syntaxDocBaseUrl/#bson.ObjectId", cause)

            None
          }
        }
      }
    }
  }

  /**
   * See [[$syntaxDocBaseUrl/#bson.Regular-Expression syntax]]
   */
  private[json] object RegexObject {
    @SuppressWarnings(Array("LooksLikeInterpolatedString"))
    def unapply(js: JsObject): Option[BSONRegex] =
      (js \ f"$$regularExpression").toOption.flatMap {
        case obj @ JsObject(_) => (obj \ "pattern").asOpt[String].map { rx =>
          BSONRegex(rx, (obj \ "options").asOpt[String].getOrElse(""))
        }

        case _ => None
      }
  }

  /**
   * See [[$syntaxDocBaseUrl/#bson.Timestamp syntax]]
   */
  private[json] object TimestampObject {
    @SuppressWarnings(Array("LooksLikeInterpolatedString"))
    def unapply(js: JsObject): Option[BSONTimestamp] =
      (js \ f"$$timestamp").toOption.flatMap {
        case obj @ JsObject(_) => {
          for {
            time <- (obj \ "t").asOpt[Int]
            ord <- (obj \ "i").asOpt[Int]
          } yield BSONTimestamp(time, ord)
        }

        case _ => None
      }
  }

  private[json] object UndefinedObject {
    @SuppressWarnings(Array("LooksLikeInterpolatedString"))
    def unapply(obj: JsObject): Boolean =
      obj.value.get(f"$$undefined") match {
        case Some(JsOne) => true
        case Some(JsBoolean(true)) => true
        case _ => false
      }
  }

  private[json] object SymbolObject {
    @SuppressWarnings(Array("LooksLikeInterpolatedString"))
    def unapply(obj: JsObject): Option[BSONSymbol] =
      (obj \ f"$$symbol").asOpt[String].map(BSONSymbol(_))
  }

  private lazy val base64Enc = Base64.getEncoder

  private lazy val base64Dec = Base64.getDecoder
}

private[json] sealed trait LowPriority1Converters
  extends LowPriority2Converters { _: ValueConverters =>

  implicit final def toBoolean(js: JsBoolean): BSONBoolean =
    BSONBoolean(js.value)

  /** See [[toValue]] */
  implicit final def toDocument(js: JsObject): BSONDocument =
    BSONDocument(js.fields.map {
      case (nme, v) => nme -> toValue(v)
    })
}

private[json] sealed trait LowPriority2Converters {
  _: ValueConverters with LowPriority1Converters =>

  implicit final def fromValue(bson: BSONValue): JsValue = bson match {
    case arr: BSONArray => fromArray(arr)
    case bin: BSONBinary => fromBinary(bin)

    case BSONBoolean(true) => JsTrue
    case BSONBoolean(_) => JsFalse

    case dt: BSONDateTime => fromDateTime(dt)
    case dec: BSONDecimal => fromDecimal(dec)
    case doc: BSONDocument => fromDocument(doc)
    case d: BSONDouble => fromDouble(d)
    case i: BSONInteger => fromInteger(i)

    case js: BSONJavaScript => fromJavaScript(js)
    case jsw: BSONJavaScriptWS => fromJavaScriptWS(jsw)

    case l: BSONLong => fromLong(l)

    case BSONMaxKey => JsMaxKey
    case BSONMinKey => JsMinKey
    case BSONNull => JsNull

    case oid: BSONObjectID => fromObjectID(oid)
    case re: BSONRegex => fromRegex(re)
    case str: BSONString => fromStr(str)
    case sym: BSONSymbol => fromSymbol(sym)
    case ts: BSONTimestamp => fromTimestamp(ts)

    case _ => JsUndefined
  }

  implicit final def toValue(js: JsValue): BSONValue = js match {
    case arr @ JsArray(_) => toArray(arr)

    case JsFalse => BSONBoolean(false)
    case JsTrue => BSONBoolean(true)
    case JsBoolean(b) => BSONBoolean(b)

    case JsNull => BSONNull

    case num @ JsNumber(_) => toNumber(num)
    case obj @ JsObject(_) => fromObject(obj)
    case str @ JsString(_) => toStr(str)
  }
}
