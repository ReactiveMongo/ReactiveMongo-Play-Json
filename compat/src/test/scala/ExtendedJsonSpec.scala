package reactivemongo

import _root_.play.api.libs.json.{
  JsArray,
  JsBoolean,
  JsNull,
  JsNumber,
  JsObject,
  JsString,
  JsValue,
  Json
}

import reactivemongo.api.bson.{
  BSONArray,
  BSONBinary,
  BSONBoolean,
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
  BSONString,
  BSONSymbol,
  BSONUndefined,
  BSONValue,
  Subtype
}

import reactivemongo.play.json.compat.{ dsl, JsFalse, JsTrue }
import reactivemongo.play.json.compat.extended._

import org.specs2.specification.core.Fragment

final class ExtendedJsonSpec extends org.specs2.mutable.Specification {

  "Extended JSON value converters".title

  import ExtendedJsonFixtures._

  "Scalar value converters" should {
    "support binary" >> {
      val bytes = "Test".getBytes("UTF-8")

      Fragment.foreach(
        Seq[(JsObject, BSONBinary)](
          jsBinUuid -> BSONBinary(uuid),
          jsBinGeneric -> BSONBinary(bytes, Subtype.GenericBinarySubtype)
        )
      ) {
        case (l, n) =>
          s"from JSON $l" in {
            implicitly[BSONValue](l) must equalTo(n)
          }

          s"to BSON $n" in {
            implicitly[JsObject](n) must ===(l)
          }
      }
    }

    "support boolean" >> {
      "from JSON" in {
        implicitly[BSONBoolean](JsBoolean(true)) must ===(
          BSONBoolean(
            true
          )
        ) and {
          implicitly[BSONBoolean](JsBoolean(false)) must ===(BSONBoolean(false))
        } and {
          implicitly[BSONBoolean](JsTrue) must ===(BSONBoolean(true))
        } and {
          implicitly[BSONBoolean](JsFalse) must ===(BSONBoolean(false))
        }
      }

      "to BSON" in {
        implicitly[JsBoolean](BSONBoolean(true)) must ===(JsBoolean(true)) and {
          implicitly[JsBoolean](BSONBoolean(false)) must ===(JsBoolean(false))
        }
      }
    }

    "support date/time" >> {
      "from JSON" in {
        implicitly[BSONValue](jdt) must equalTo(bdt)
      }

      "to BSON" in {
        implicitly[JsObject](bdt) must ===(jdt)
      }
    }

    "support decimal" >> {
      lazy val jsInfinity = Json.obj(f"$$numberDecimal" -> "Infinity")
      lazy val jsZero = dsl.decimal(0)

      "from JSON" in {
        implicitly[BSONValue](jsInfinity) must ===(
          BSONDecimal.PositiveInf
        ) and {
          implicitly[BSONValue](jsZero) must ===(BSONDecimal.PositiveZero)
        }
      }

      "to BSON" in {
        implicitly[JsObject](BSONDecimal.PositiveInf) must ===(jsInfinity) and {
          implicitly[JsObject](BSONDecimal.PositiveZero) must ===(jsZero)
        }
      }
    }

    "support double" >> {
      val raw = 1.23D

      "from JSON" in {
        implicitly[BSONValue](dsl.double(raw)) must ===(BSONDouble(raw)) and {
          implicitly[BSONValue](JsNumber(raw)) must ===(BSONDouble(raw))
        }
      }

      "to BSON" in {
        implicitly[JsObject](BSONDouble(raw)) must ===(dsl.double(raw))
      }
    }

    "support integer" >> {
      "from JSON" in {
        implicitly[BSONValue](dsl.int(1)) must ===(BSONInteger(1)) and {
          implicitly[BSONValue](JsNumber(2)) must ===(BSONInteger(2))
        }
      }

      "to BSON" in {
        implicitly[JsObject](BSONInteger(2)) must ===(dsl.int(2))
      }
    }

    "support JavaScript" >> {
      val raw = "foo()"

      "from JSON" in {
        implicitly[BSONValue](jsJavaScript(raw)) must ===(BSONJavaScript(raw))
      }

      "to BSON" in {
        implicitly[JsObject](BSONJavaScript(raw)) must ===(jsJavaScript(raw))
      }
    }

    "support JavaScript/WS" >> {
      val raw = "bar('lorem')"

      "from JSON" in {
        implicitly[BSONValue](jsJavaScriptWS(raw)) must ===(
          BSONJavaScriptWS(
            raw,
            BSONDocument.empty
          )
        )
      }

      "to BSON" in {
        implicitly[JsObject](
          BSONJavaScriptWS(raw, BSONDocument.empty)
        ) must ===(jsJavaScriptWS(raw))
      }
    }

    "support long" >> {
      "from JSON" in {
        implicitly[BSONValue](dsl.long(1L)) must ===(BSONLong(1L)) and {
          implicitly[BSONValue](JsNumber(Long.MaxValue)) must ===(
            BSONLong(
              Long.MaxValue
            )
          )
        }
      }

      "to BSON" in {
        implicitly[JsValue](BSONLong(2L)) must ===(dsl.long(2L))
      }
    }

    "support null" >> {
      "from JSON" in {
        implicitly[BSONNull](JsNull) must ===(BSONNull)
      }

      "to BSON" in {
        implicitly[JsNull.type](BSONNull) must ===(JsNull)
      }
    }

    "support maxKey" >> {
      "from JSON" in {
        implicitly[BSONValue](JsMaxKey) must ===(BSONMaxKey)
      }

      "to BSON" in {
        implicitly[JsObject](BSONMaxKey) must ===(JsMaxKey)
      }
    }

    "support minKey" >> {
      "from JSON" in {
        implicitly[BSONValue](JsMinKey) must ===(BSONMinKey)
      }

      "to BSON" in {
        implicitly[JsObject](BSONMinKey) must ===(JsMinKey)
      }
    }

    "support object ID" >> {
      "from JSON" in {
        implicitly[BSONValue](joid) must ===(boid)
      }

      "to BSON" in {
        implicitly[JsObject](boid) must ===(joid)
      }
    }

    "support string" >> {
      val raw = "Foo"

      "from JSON" in {
        implicitly[BSONValue](JsString(raw)) must ===(BSONString(raw))
      }

      "to BSON" in {
        implicitly[JsString](BSONString(raw)) must ===(JsString(raw))
      }
    }

    "support symbol" >> {
      val raw = "Foo"

      "from JSON" in {
        implicitly[BSONValue](dsl.symbol(raw)) must ===(BSONSymbol(raw))
      }

      "to BSON" in {
        implicitly[JsObject](BSONSymbol(raw)) must ===(dsl.symbol(raw))
      }
    }

    "support timestamp" >> {
      "from JSON" in {
        toValue(jts) must ===(bts) and {
          implicitly[BSONValue](jts) must ===(bts)
        }
      }

      "to BSON" in {
        implicitly[JsObject](bts) must ===(jts)
      }
    }

    "support regexp" >> {
      "from JSON" in {
        implicitly[BSONValue](jre) must ===(bre)
      }

      "to BSON" in {
        implicitly[JsObject](bre) must ===(jre)
      }
    }

    "support undefined" >> {
      "from JSON" in {
        implicitly[BSONValue](JsUndefined) must ===(BSONUndefined)
      }

      "to BSON" in {
        implicitly[JsObject](BSONUndefined) must ===(JsUndefined)
      }
    }
  }

  "Non-scalar value converters" should {
    "support array" >> {
      "from JSON" in {
        implicitly[BSONArray](jarr) must ===(barr)
      }

      "to BSON" in {
        implicitly[JsArray](barr) must ===(jarr)
      }
    }

    "support document" >> {
      "from JSON" in {
        implicitly[BSONDocument](jdoc) must ===(bdoc)
      }

      "to BSON" in {
        implicitly[JsObject](bdoc) must ===(jdoc)
      }
    }
  }

  "Opaque values" should {
    Fragment.foreach(fixtures) {
      case (json, bson) =>
        s"from JSON $json" in {
          implicitly[BSONValue](json) must ===(bson)
        }

        s"$bson to BSON" in {
          implicitly[JsValue](bson) must ===(json)
        }
    }
  }
}
