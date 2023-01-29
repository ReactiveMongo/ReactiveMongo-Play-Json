package reactivemongo

import _root_.play.api.libs.json.{
  Format,
  JsError,
  JsNumber,
  JsObject,
  JsPath,
  JsResult,
  JsString,
  JsSuccess,
  JsValue,
  Json,
  OFormat,
  OWrites,
  Reads,
  Writes
}

import reactivemongo.api.bson.{
  BSONDateTime,
  BSONDocument,
  BSONDocumentHandler,
  BSONDocumentReader,
  BSONDocumentWriter,
  BSONDouble,
  BSONHandler,
  BSONInteger,
  BSONJavaScript,
  BSONLong,
  BSONObjectID,
  BSONReader,
  BSONString,
  BSONSymbol,
  BSONTimestamp,
  BSONWriter,
  Macros
}

import reactivemongo.play.json.TestCompat.{ toJsObject, JsonValidationError }
import reactivemongo.play.json.compat._

import org.specs2.specification.core.Fragment

import com.github.ghik.silencer.silent

final class HandlerConverterSpec
    extends org.specs2.mutable.Specification
    with HandlerConverterSpecCompat {

  "Handler converters".title

  "Converters" should {
    "from JSON" >> {
      import json2bson._

      "convert reader" >> {
        "for BSONLong" in {
          implicit val jr: Reads[Long] = Reads[Long] { _ => JsSuccess(1L) }
          def bvr: BSONReader[Long] = jr
          def bdr: BSONDocumentReader[Long] = implicitly[Reads[Long]]

          toReaderConv(jr).readTry(BSONLong(2L)) must beSuccessfulTry(1L) and {
            bvr.readTry(BSONLong(3L)) must beSuccessfulTry(1L)
          } and {
            bvr.readTry(dsl.long(4L)) must beSuccessfulTry(1L)
          } and {
            bdr.readTry(
              BSONDocument(f"$$numberLong" -> 1)
            ) must beSuccessfulTry(1L)
          }
        }

        "for BSONObjectID" in {
          import ExtendedJsonFixtures.boid

          implicit val jr: Reads[BSONObjectID] =
            Reads[BSONObjectID] { _ => JsSuccess(boid) }

          def br1: BSONReader[BSONObjectID] = jr
          def br2: BSONReader[BSONObjectID] =
            implicitly[Reads[BSONObjectID]]

          toReaderConv(jr).readTry(boid) must beSuccessfulTry(boid) and {
            br1.readTry(dsl.objectID(boid)) must beSuccessfulTry(boid)
          } and {
            br2.readTry(dsl.objectID(boid)) must beSuccessfulTry(boid)
          }
        }
      }

      "convert writer" >> {
        "for JsValue" in {
          val w: BSONWriter[JsValue] = implicitly[BSONWriter[JsValue]]

          w.writeTry(JsString("foo")) must beSuccessfulTry(
            BSONString("foo")
          ) and {
            w.writeOpt(Json.obj("bar" -> 1)) must beSome(
              BSONDocument("bar" -> 1)
            )
          }
        }
      }

      "convert handler" in {
        object Foo

        implicit val jh: Format[Foo.type] = Format[Foo.type](
          Reads { _ => JsSuccess(Foo) },
          Writes { (_: Foo.type) => JsNumber(1) }
        )

        def bh: BSONHandler[Foo.type] = jh

        bh.writeTry(Foo) must beSuccessfulTry(BSONInteger(1)) and {
          bh.readTry(BSONInteger(2)) must beSuccessfulTry(Foo)
        }
      }
    }

    "to JSON" >> {
      import bson2json._

      "convert reader" >> {
        "for Unit" in {
          implicit val br = BSONReader[Unit] { _ => () }
          def jr: Reads[Unit] = br

          fromReaderConv(br).reads(JsNumber(1)) must beLike[JsResult[Unit]] {
            case JsSuccess((), _) =>
              jr.reads(JsNumber(2)) must beLike[JsResult[Unit]] {
                case JsSuccess((), _) => ok
              }
          }
        }

        "for document" in {
          import ExtendedJsonFixtures.{ bdoc, jdoc }

          val br = implicitly[BSONDocumentReader[BSONDocument]]
          val jr1: Reads[BSONDocument] = br
          val jr2 = implicitly[Reads[BSONDocument]]

          jr1.reads(jdoc) must_=== JsSuccess(bdoc) and {
            jr2.reads(jdoc) must_=== JsSuccess(bdoc)
          }
        }
      }

      "convert handler" in {
        object Bar

        implicit val bh = BSONHandler[Bar.type](_ => Bar, _ => BSONDouble(1.2D))

        def jh: Format[Bar.type] = bh

        jh.reads(dsl.double(3.4D)) must beLike[JsResult[Bar.type]] {
          case JsSuccess(Bar, _) =>
            jh.writes(Bar) must_=== JsNumber(1.2D)
        }
      }
    }

    Fragment.foreach(HandlerFixtures.fixtures) {
      case (js, bson) =>
        s"between $js & $bson" >> {
          "convert writer to BSON" in {
            import json2bson._

            implicit val jw = Writes[Unit] { _ => js }
            def bw: BSONWriter[Unit] = jw

            toWriterConv(jw).writeTry({}) must beSuccessfulTry(bson) and {
              bw.writeTry({}) must beSuccessfulTry(bson)
            }
          }

          "convert writer to JSON" in {
            import bson2json._

            implicit val bw = BSONWriter[Int] { _ => bson }
            def jw: Writes[Int] = bw

            fromWriterConv(bw).writes(1) must_=== js and {
              jw.writes(2) must_=== js
            }
          }
        }
    }

    "from JSON object" >> {
      import json2bson._

      "in writer" >> {
        "as document for BSONDouble" in {
          val doc = BSONDocument("ok" -> 1)
          implicit val jw: OWrites[Double] =
            OWrites[Double] { _ => Json.obj("ok" -> 1) }

          def bw1: BSONDocumentWriter[Double] = jw
          def bw2 = implicitly[BSONDocumentWriter[Double]]

          toWriterConv(jw).writeTry(1.0D) must beSuccessfulTry(doc) and {
            bw1.writeTry(1.1D) must beSuccessfulTry(doc)
          } and {
            bw2.writeTry(1.2D) must beSuccessfulTry(doc)
          }
        }

        "using compatibility conversion for JsObject" in {
          @silent def jw = implicitly[OWrites[JsObject]]
          val bw1: BSONDocumentWriter[JsObject] = jw
          @silent def bw2 = implicitly[BSONDocumentWriter[JsObject]]

          val jo = Json.obj("foo" -> 1)
          val bo = BSONDocument("foo" -> 1)

          bw1.writeTry(jo) must beSuccessfulTry(bo) and {
            bw2.writeTry(jo) must beSuccessfulTry(bo)
          }
        }

        "using compatibility conversion for BSONObjectID" in {
          import bson2json._

          import ExtendedJsonFixtures.{ boid, joid }
          val w = implicitly[Writes[BSONObjectID]]

          w.writes(boid) must beTypedEqualTo(joid)
        }
      }

      "in reader" in {
        implicit val jr: Reads[Float] = Reads[Float](_ => JsSuccess(1.2F))
        def br1: BSONReader[Float] = jr
        def br2: BSONReader[Float] = implicitly[Reads[Float]]

        toDocumentReaderConv(jr).readTry(
          BSONDocument("ok" -> 1)
        ) must beSuccessfulTry(1.2F) and {
          br1.readTry(BSONDocument("ok" -> 2)) must beSuccessfulTry(1.2F)
        } and {
          br2.readTry(BSONDocument("ok" -> 3)) must beSuccessfulTry(1.2F)
        }
      }

      "in handler" in {
        implicit val jh = OFormat[None.type](
          Reads { _ => JsSuccess(None) },
          OWrites { (_: None.type) => Json.obj() }
        )

        val bh: BSONDocumentHandler[None.type] = jh

        bh.readTry(BSONDocument("ok" -> 1)) must beSuccessfulTry(None) and {
          bh.writeTry(None) must beSuccessfulTry(BSONDocument.empty)
        }
      }
    }

    "to JSON object" >> {
      import bson2json._

      "in writer" in {
        val doc = Json.obj("ok" -> JsNumber(2))
        implicit val bw: BSONDocumentWriter[Int] =
          BSONDocumentWriter[Int](_ => BSONDocument("ok" -> BSONInteger(2)))

        def jw1: OWrites[Int] = bw
        def jw2 = implicitly[OWrites[Int]]

        fromWriterConv(bw).writes(1) must_=== doc and {
          jw1.writes(2) must_=== doc
        } and {
          jw2.writes(2) must_=== doc
        }
      }

      "in reader" in {
        implicit val br = BSONDocumentReader[None.type](_ => None)
        def jr: Reads[None.type] = br

        fromReaderConv(br)
          .reads(Json.obj("ok" -> 1)) must beLike[JsResult[None.type]] {
          case JsSuccess(None, _) =>
            jr.reads(Json.obj("ok" -> 2)) must beLike[JsResult[None.type]] {
              case JsSuccess(None, _) => ok
            }
        }
      }

      "in handler" in {
        implicit val bh: BSONDocumentHandler[Unit] =
          BSONDocumentHandler[Unit](_ => (), _ => BSONDocument("foo" -> 1L))

        val jh: OFormat[Unit] = bh

        jh.reads(Json.obj("ok" -> 1)) must_=== JsSuccess({}) and {
          jh.writes({}) must_=== Json.obj("foo" -> 1L)
        }
      }
    }

    "resolve JSON codecs for BSON values" >> {
      def spec[T: Reads: Writes] = {
        "using Reads" in {
          implicitly[Reads[T]] must not(beNull)
        }

        "using Writes" in {
          implicitly[Writes[T]] must not(beNull)
        }

        "using Format" in {
          implicitly[Format[T]] must not(beNull)
        }
      }

      import bson2json._

      "for BSONDateTime" >> spec[BSONDateTime]

      "for BSONJavaScript" >> spec[BSONJavaScript]

      "for BSONObjectID" >> spec[BSONObjectID]

      "for BSONTimestamp" >> spec[BSONTimestamp]
    }

    "convert in lax mode" >> {
      import bson2json._

      "for BSONDateTime" >> {
        import ExtendedJsonFixtures.{ bdt => fixture }

        lazy val js: JsValue = {
          import lax._ // From-ToValue
          Json.toJson(fixture)
        }

        "fromDateTime" in {
          lax.fromDateTime(fixture) must_== js and {
            js must_=== JsNumber(fixture.value)
          }
        }

        "toValue" in {
          { // without lax
            js.validate[BSONDateTime] must beLike[JsResult[BSONDateTime]] {
              case JsError(
                    (
                      JsPath,
                      JsonValidationError(
                        "BSONLong != BSONDateTime" :: Nil
                      ) :: Nil
                    ) :: Nil
                  ) =>
                js.validate[Long] must_=== JsSuccess(fixture.value)
            }
          } and {
            import lax._
            js.validate[BSONDateTime] must_=== JsSuccess(fixture)
          }
        }

        "be written" >> {
          // !! Using BSONValue in model is not recommended,
          // not to couple model with DB related types, anyway ...

          import HandlerFixtures.FooDateTime

          implicit val fooWriter: BSONDocumentWriter[FooDateTime] =
            Macros.writer[FooDateTime]

          val foo = FooDateTime("bar", fixture)
          lazy val fooJs: JsObject = {
            import lax._

            toJsObject(foo) // via fromDocumentWriter
          }

          "to JSON" in {
            // fromDocumentWriter with lax
            fromDocumentWriter[FooDateTime](fooWriter, lax)
              .writes(foo) must_=== fooJs and {
              // Check implicits integration
              import lax._

              fromDocumentWriter[FooDateTime].writes(foo) must_=== fooJs
            } and {
              fooJs must_=== Json.obj(
                "bar" -> "bar",
                "v" -> JsNumber(fixture.value)
              )
            }
          }

          "to BSON document" in {
            // BSON conversion from JSON representation
            lax.toDocument(fooJs) must_=== BSONDocument(
              "bar" -> "bar",
              "v" -> BSONLong(fixture.value)
            )
          }

          "using fromReader" in {
            val fooBsonReader: BSONDocumentReader[FooDateTime] = {
              import lax.bsonDateTimeReader

              Macros.reader[FooDateTime]
            }

            lax.dateTimeReads.reads(JsNumber(fixture.value)) must_=== JsSuccess(
              fixture
            ) and {
              fromReader[FooDateTime](Macros.reader[FooDateTime], lax)
                .reads(fooJs) must beLike[JsResult[FooDateTime]] {
                case JsError(
                      (
                        JsPath,
                        JsonValidationError(
                          "Fails to handle 'v': BSONLong != BSONDateTime" ::
                          Nil
                        ) :: Nil
                      ) :: Nil
                    ) =>
                  ok
              }
            } and {
              fromReader[FooDateTime](fooBsonReader, lax)
                .reads(fooJs) must_=== JsSuccess(foo)
            } and {
              fooJs.validate[FooDateTime](fooBsonReader) must_=== JsSuccess(foo)
            } and {
              implicit val fooJsonReader: Reads[FooDateTime] = {
                import lax.dateTimeReads

                Reads[FooDateTime] { js =>
                  for {
                    bar <- (js \ "bar").validate[String]
                    v <- (js \ "v").validate[BSONDateTime]
                  } yield FooDateTime(bar, v)
                }
              }

              fooJs.validate[FooDateTime] must_=== JsSuccess(foo)
            } and {
              // Check implicits integration
              implicit def br: BSONDocumentReader[FooDateTime] = fooBsonReader

              fooJs.validate[FooDateTime] must_=== JsSuccess(foo)
            }
          }
        }
      }

      "for BSONJavaScript" >> {
        val fixture = BSONJavaScript("foo()")

        lazy val js: JsValue = {
          import lax._ // From-ToValue
          Json.toJson(fixture)
        }

        "fromJavaScript" in {
          lax.fromJavaScript(fixture) must_== js and {
            js must_=== JsString(fixture.value)
          }
        }

        "toValue" in {
          // toValue ...

          { // without lax
            js.validate[BSONJavaScript] must beLike[JsResult[BSONJavaScript]] {
              case JsError(
                    (
                      JsPath,
                      JsonValidationError(
                        "BSONString != BSONJavaScript" :: Nil
                      ) :: Nil
                    ) :: Nil
                  ) =>
                js.validate[String] must_=== JsSuccess(fixture.value)
            }
          } and {
            import lax._
            js.validate[BSONJavaScript] must_=== JsSuccess(fixture)
          }
        }

        "be written" >> {
          // !! Using BSONValue in model is not recommended,
          // not to couple model with DB related types, anyway ...

          import HandlerFixtures.FooJavaScript

          implicit val fooWriter: BSONDocumentWriter[FooJavaScript] =
            Macros.writer[FooJavaScript]

          val foo = FooJavaScript("bar", fixture)
          lazy val fooJs: JsObject = {
            import lax._

            toJsObject(foo) // via fromDocumentWriter
          }

          "to JSON" in {
            // fromDocumentWriter with lax
            fromDocumentWriter[FooJavaScript](fooWriter, lax)
              .writes(foo) must_=== fooJs and {
              // Check implicits integration
              import lax._

              fromDocumentWriter[FooJavaScript].writes(foo) must_=== fooJs
            } and {
              fooJs must_=== Json.obj(
                "bar" -> "bar",
                "v" -> JsString(fixture.value)
              )
            }
          }

          "to BSONDocument" in {
            // BSON conversion from JSON representation
            lax.toDocument(fooJs) must_=== BSONDocument(
              "bar" -> "bar",
              "v" -> BSONString(fixture.value)
            )
          }

          "using fromReader" in {
            // fromReader
            val fooBsonReader: BSONDocumentReader[FooJavaScript] = {
              import lax.javaScriptBSONReader

              Macros.reader[FooJavaScript]
            }

            lax.javaScriptReads.reads(
              JsString(fixture.value)
            ) must_=== JsSuccess(fixture) and {
              fromReader[FooJavaScript](Macros.reader[FooJavaScript], lax)
                .reads(fooJs) must beLike[JsResult[FooJavaScript]] {
                case JsError(
                      (
                        JsPath,
                        JsonValidationError(
                          "Fails to handle 'v': BSONString != BSONJavaScript" ::
                          Nil
                        ) :: Nil
                      ) :: Nil
                    ) =>
                  ok
              }
            } and {
              fromReader[FooJavaScript](fooBsonReader, lax)
                .reads(fooJs) must_=== JsSuccess(foo)
            } and {
              fooJs.validate[FooJavaScript](fooBsonReader) must_=== JsSuccess(
                foo
              )
            } and {
              implicit val fooJsonReader: Reads[FooJavaScript] = {
                import lax.javaScriptReads

                Reads[FooJavaScript] { js =>
                  for {
                    bar <- (js \ "bar").validate[String]
                    v <- (js \ "v").validate[BSONJavaScript]
                  } yield FooJavaScript(bar, v)
                }
              }

              fooJs.validate[FooJavaScript] must_=== JsSuccess(foo)
            } and {
              // Check implicits integration
              implicit def br: BSONDocumentReader[FooJavaScript] = fooBsonReader

              fooJs.validate[FooJavaScript] must_=== JsSuccess(foo)
            }
          }
        }
      }

      "for BSONObjectID" >> {
        import ExtendedJsonFixtures.{ boid => fixture }

        lazy val js: JsValue = {
          import lax._ // From-ToValue
          Json.toJson(fixture)
        }

        "fromObjectID" in {
          lax.fromObjectID(fixture) must_== js and {
            js must_=== JsString(fixture.stringify)
          }
        }

        "toValue" in {
          { // without lax
            js.validate[BSONObjectID] must beLike[JsResult[BSONObjectID]] {
              case JsError(
                    (
                      JsPath,
                      JsonValidationError(
                        "BSONString != BSONObjectID" :: Nil
                      ) :: Nil
                    ) :: Nil
                  ) =>
                js.validate[String] must_=== JsSuccess(fixture.stringify)
            }
          } and {
            import lax._
            js.validate[BSONObjectID] must_=== JsSuccess(fixture)
          }
        }

        "be written" >> {
          // !! Using BSONValue in model is not recommended,
          // not to couple model with DB related types, anyway ...

          import HandlerFixtures.FooObjectID

          implicit val fooWriter: BSONDocumentWriter[FooObjectID] =
            Macros.writer[FooObjectID]

          val foo = FooObjectID("bar", fixture)
          lazy val fooJs: JsObject = {
            import lax._

            toJsObject(foo) // via fromDocumentWriter
          }

          "to JSON" in {
            // fromDocumentWriter with lax
            fromDocumentWriter[FooObjectID](fooWriter, lax)
              .writes(foo) must_=== fooJs and {
              // Check implicits integration
              import lax._

              fromDocumentWriter[FooObjectID].writes(foo) must_=== fooJs
            } and {
              fooJs must_=== Json.obj(
                "bar" -> "bar",
                "v" -> JsString(fixture.stringify)
              )
            }
          }

          "to BSONDocument" in {
            // BSON conversion from JSON representation
            lax.toDocument(fooJs) must_=== BSONDocument(
              "bar" -> "bar",
              "v" -> BSONString(fixture.stringify)
            )
          }

          "using BSON reader" in {
            // fromReader
            val fooBsonReader: BSONDocumentReader[FooObjectID] = {
              import lax.bsonObjectIDReader

              Macros.reader[FooObjectID]
            }

            lax.objectIDReads.reads(
              JsString(fixture.stringify)
            ) must_=== JsSuccess(fixture) and {
              fromReader[FooObjectID](Macros.reader[FooObjectID], lax)
                .reads(fooJs) must beLike[JsResult[FooObjectID]] {
                case JsError(
                      (
                        JsPath,
                        JsonValidationError(
                          "Fails to handle 'v': BSONString != BSONObjectID" ::
                          Nil
                        ) :: Nil
                      ) :: Nil
                    ) =>
                  ok
              }
            } and {
              fromReader[FooObjectID](fooBsonReader, lax)
                .reads(fooJs) must_=== JsSuccess(foo)
            } and {
              fooJs.validate[FooObjectID](fooBsonReader) must_=== JsSuccess(foo)

            } and {
              implicit val fooJsonReader: Reads[FooObjectID] = {
                import lax.objectIDReads

                Reads[FooObjectID] { js =>
                  for {
                    bar <- (js \ "bar").validate[String]
                    v <- (js \ "v").validate[BSONObjectID]
                  } yield FooObjectID(bar, v)
                }
              }

              fooJs.validate[FooObjectID] must_=== JsSuccess(foo)
            } and {
              // Check implicits integration
              implicit def br: BSONDocumentReader[FooObjectID] = fooBsonReader

              fooJs.validate[FooObjectID] must_=== JsSuccess(foo)
            }
          }
        }
      }

      "for BSONSymbol" >> {
        val fixture = BSONSymbol("sym")

        lazy val js: JsValue = {
          import lax._ // From-ToValue
          Json.toJson(fixture)
        }

        "fromSymbol" in {
          lax.fromSymbol(fixture) must_== js and {
            js must_=== JsString(fixture.value)
          }
        }

        "toValue" in {
          // without lax
          js.validate[BSONSymbol] must beLike[JsResult[BSONSymbol]] {
            case JsError(
                  (
                    JsPath,
                    JsonValidationError(
                      "BSONString != BSONSymbol" :: Nil
                    ) :: Nil
                  ) :: Nil
                ) =>
              js.validate[String] must_=== JsSuccess(fixture.value)
          } and {
            import lax._
            js.validate[BSONSymbol] must_=== JsSuccess(fixture)
          }
        }

        "be written" >> {
          // !! Using BSONValue in model is not recommended,
          // not to couple model with DB related types, anyway ...

          import HandlerFixtures.FooSymbol

          implicit val fooWriter: BSONDocumentWriter[FooSymbol] =
            Macros.writer[FooSymbol]

          val foo = FooSymbol("bar", fixture)
          lazy val fooJs: JsObject = {
            import lax._

            toJsObject(foo) // via fromDocumentWriter
          }

          "to JSON" in {
            // fromDocumentWriter with lax
            fromDocumentWriter[FooSymbol](fooWriter, lax)
              .writes(foo) must_=== fooJs and {
              // Check implicits integration
              import lax._

              fromDocumentWriter[FooSymbol].writes(foo) must_=== fooJs
            } and {
              fooJs must_=== Json.obj(
                "bar" -> "bar",
                "v" -> JsString(fixture.value)
              )
            }
          }

          "to BSONDocument" in {
            // BSON conversion from JSON representation
            lax.toDocument(fooJs) must_=== BSONDocument(
              "bar" -> "bar",
              "v" -> BSONString(fixture.value)
            )
          }

          "using fromReader" in {
            val fooBsonReader: BSONDocumentReader[FooSymbol] = {
              import lax.bsonSymbolReader

              Macros.reader[FooSymbol]
            }

            lax.symbolReads.reads(JsString(fixture.value)) must_=== JsSuccess(
              fixture
            ) and {
              fromReader[FooSymbol](Macros.reader[FooSymbol], lax)
                .reads(fooJs) must beLike[JsResult[FooSymbol]] {
                case JsError(
                      (
                        JsPath,
                        JsonValidationError(
                          "Fails to handle 'v': BSONString != BSONSymbol" ::
                          Nil
                        ) :: Nil
                      ) :: Nil
                    ) =>
                  ok
              }
            } and {
              fromReader[FooSymbol](fooBsonReader, lax)
                .reads(fooJs) must_=== JsSuccess(foo)
            } and {
              fooJs.validate[FooSymbol](fooBsonReader) must_=== JsSuccess(foo)

            } and {
              implicit val fooJsonReader: Reads[FooSymbol] = {
                import lax.symbolReads

                Reads[FooSymbol] { js =>
                  for {
                    bar <- (js \ "bar").validate[String]
                    v <- (js \ "v").validate[BSONSymbol]
                  } yield FooSymbol(bar, v)
                }
              }

              fooJs.validate[FooSymbol] must_=== JsSuccess(foo)
            } and {
              // Check implicits integration
              implicit def br: BSONDocumentReader[FooSymbol] = fooBsonReader

              fooJs.validate[FooSymbol] must_=== JsSuccess(foo)
            }
          }
        }
      }

      "for BSONTimestamp" >> {
        import ExtendedJsonFixtures.{ bts => fixture }

        lazy val js: JsValue = {
          import lax._ // From-ToValue
          Json.toJson(fixture)
        }

        "from timestamp" in {
          lax.fromTimestamp(fixture) must_== js and {
            js must_=== JsNumber(fixture.value)
          }
        }

        "validated as BSONTimestamp" in {
          // toValue ...

          { // without lax
            js.validate[BSONTimestamp] must beLike[JsResult[BSONTimestamp]] {
              case JsError(
                    (
                      JsPath,
                      JsonValidationError(
                        "BSONLong != BSONTimestamp" :: Nil
                      ) :: Nil
                    ) :: Nil
                  ) =>
                js.validate[Long] must_=== JsSuccess(fixture.value)
            }
          } and {
            import lax._
            js.validate[BSONTimestamp] must_=== JsSuccess(fixture)
          }
        }

        {
          // !! Using BSONValue in model is not recommended,
          // not to couple model with DB related types, anyway ...

          import HandlerFixtures.FooTimestamp

          implicit val fooWriter: BSONDocumentWriter[FooTimestamp] =
            Macros.writer[FooTimestamp]

          val foo = FooTimestamp("bar", fixture)
          val fooJs: JsObject = {
            import lax._

            toJsObject(foo) // via fromDocumentWriter
          }

          "fromDocumentWriter with lax" in {
            fromDocumentWriter[FooTimestamp](fooWriter, lax)
              .writes(foo) must_=== fooJs and {
              // Check implicits integration
              import lax._

              fromDocumentWriter[FooTimestamp].writes(foo) must_=== fooJs
            } and {
              fooJs must_=== Json.obj(
                "bar" -> "bar",
                "v" -> JsNumber(fixture.value)
              )
            }
          }

          "BSON conversion from JSON representation" in {
            lax.toDocument(fooJs) must_=== BSONDocument(
              "bar" -> "bar",
              "v" -> BSONLong(fixture.value)
            )
          }

          // fromReader
          lazy val fooBsonReader: BSONDocumentReader[FooTimestamp] = {
            import lax.bsonTimestampReader

            Macros.reader[FooTimestamp]
          }

          "using BSON reader" in {
            lax.timestampReads.reads(
              JsNumber(fixture.value)
            ) must_=== JsSuccess(fixture) and {
              fromReader[FooTimestamp](Macros.reader[FooTimestamp], lax)
                .reads(fooJs) must beLike[JsResult[FooTimestamp]] {
                case JsError(
                      (
                        JsPath,
                        JsonValidationError(
                          "Fails to handle 'v': BSONLong != BSONTimestamp" ::
                          Nil
                        ) :: Nil
                      ) :: Nil
                    ) =>
                  ok
              }
            }
          }

          "thus be read" in {
            {
              fromReader[FooTimestamp](fooBsonReader, lax)
                .reads(fooJs) must_=== JsSuccess(foo)
            } and {
              fooJs.validate[FooTimestamp](fooBsonReader) must_=== JsSuccess(
                foo
              )

            } and {
              implicit val fooJsonReader: Reads[FooTimestamp] = {
                import lax.timestampReads

                Reads[FooTimestamp] { js =>
                  for {
                    bar <- (js \ "bar").validate[String]
                    v <- (js \ "v").validate[BSONTimestamp]
                  } yield FooTimestamp(bar, v)
                }
              }

              fooJs.validate[FooTimestamp] must_=== JsSuccess(foo)
            } and {
              // Check implicits integration
              implicit def br: BSONDocumentReader[FooTimestamp] = fooBsonReader

              fooJs.validate[FooTimestamp] must_=== JsSuccess(foo)
            }
          }
        }
      }
    }
  }
}

object HandlerFixtures {
  import _root_.play.api.libs.json.{
    JsArray,
    JsNull,
    JsObject,
    JsString,
    JsValue
  }

  import reactivemongo.api.bson.{
    BSONArray,
    BSONBinary,
    BSONBoolean,
    BSONDecimal,
    BSONJavaScript,
    BSONJavaScriptWS,
    BSONMaxKey,
    BSONMinKey,
    BSONNull,
    BSONString,
    BSONSymbol,
    BSONUndefined,
    BSONValue
  }

  import _root_.reactivemongo.play.json.compat.{ dsl, JsTrue, ValueConverters }
  import ExtendedJsonFixtures.{
    joid,
    jdt,
    jts,
    jsJavaScript,
    jre,
    boid,
    bdt,
    bre,
    bts,
    jsBinUuid,
    jsJavaScriptWS,
    uuid
  }

  val jarr = JsArray(
    Seq(
      joid,
      JsString("foo"),
      jdt,
      dsl.symbol("bar"),
      jts,
      jsJavaScript("lorem()"),
      jre,
      JsArray(Seq(JsNumber(1), JsNumber(2L))),
      JsNumber(3.4D)
    )
  )

  val barr = BSONArray(
    boid,
    BSONString("foo"),
    bdt,
    BSONSymbol("bar"),
    bts,
    BSONJavaScript("lorem()"),
    bre,
    BSONArray(BSONInteger(1), BSONInteger(2 /* #note1 */ )),
    BSONDouble(3.4D)
  )

  val jdoc = JsObject(
    Map[String, JsValue](
      "oid" -> joid,
      "str" -> JsString("foo"),
      "dt" -> jdt,
      "sym" -> dsl.symbol("bar"),
      "ts" -> jts,
      "nested" -> JsObject(
        Map[String, JsValue](
          "foo" -> JsString("bar"),
          "lorem" -> JsNumber(Long.MaxValue)
        )
      ),
      "js" -> jsJavaScript("lorem()"),
      "re" -> jre,
      "array" -> jarr,
      "double" -> JsNumber(3.4D)
    )
  )

  val bdoc = BSONDocument(
    "oid" -> boid,
    "str" -> BSONString("foo"),
    "dt" -> bdt,
    "sym" -> BSONSymbol("bar"),
    "ts" -> bts,
    "nested" -> BSONDocument("foo" -> "bar", "lorem" -> Long.MaxValue),
    "js" -> BSONJavaScript("lorem()"),
    "re" -> bre,
    "array" -> barr,
    "double" -> BSONDouble(3.4D)
  )

  val fixtures = Seq[(JsValue, BSONValue)](
    jsBinUuid -> BSONBinary(uuid),
    JsTrue -> BSONBoolean(true),
    JsNumber(1.23D) -> BSONDouble(1.23D),
    JsString("Foo") -> BSONString("Foo"),
    JsNumber(1) -> BSONInteger(1),
    JsNumber(1L) -> BSONInteger(1), // #note1: no int/long discriminator in JSON
    JsNumber(Long.MaxValue) -> BSONLong(Long.MaxValue),
    joid -> boid,
    jdt -> bdt,
    jts -> bts,
    dsl.decimal(BigDecimal("0")) -> BSONDecimal.PositiveZero,
    jre -> bre,
    jsJavaScript("foo()") -> BSONJavaScript("foo()"),
    jsJavaScriptWS("bar()") -> BSONJavaScriptWS("bar()", BSONDocument.empty),
    dsl.symbol("sym") -> BSONSymbol("sym"),
    ValueConverters.JsUndefined -> BSONUndefined,
    JsNull -> BSONNull,
    ValueConverters.JsMaxKey -> BSONMaxKey,
    ValueConverters.JsMinKey -> BSONMinKey,
    jarr -> barr,
    jdoc -> bdoc
  )

  case class Foo(bar: String)
  case class FooDateTime(bar: String, v: BSONDateTime)
  case class FooJavaScript(bar: String, v: BSONJavaScript)
  case class FooObjectID(bar: String, v: BSONObjectID)
  case class FooSymbol(bar: String, v: BSONSymbol)
  case class FooTimestamp(bar: String, v: BSONTimestamp)
}
