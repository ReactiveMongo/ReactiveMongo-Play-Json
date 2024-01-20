import _root_.play.api.libs.json._

import reactivemongo.play.TestUtils._
import reactivemongo.play.json.TestCompat.JsonValidationError

import org.specs2.matcher.TypecheckMatchers._

import _root_.reactivemongo.api.bson._
import reactivemongo.ExtendedJsonFixtures

final class HandlerUseCaseSpec extends org.specs2.mutable.Specification {
  "Handler use cases".title

  // Global compatibility import:
  import reactivemongo.play.json.compat._

  "User" should {
    import HandlerUseCaseSpec.User
    import ExtendedJsonFixtures.boid

    val user = User(
      boid,
      "lorem",
      "ipsum",
      created = BSONTimestamp(987654321L),
      lastModified = BSONDateTime(123456789L),
      sym = Some(BSONSymbol("foo"))
    )

    "not be serializable on JSON without 'bson2json'" in {
      sys.env
        .get("PLAY_VERSION")
        .filterNot(v => v.startsWith("2.10") || v.startsWith("3."))
        .fold[org.specs2.execute.Result](skipped) { _ =>
          lazy val res = typecheck("Json.toJson(user)")

          {
            res must failWith(
              "(ambiguous implicit|No Json serializer found).*"
            )
          } or {
            res must failWith("Ambiguous given instances.*Writes.*")
          }
        }
    }

    "be represented in JSON using 'bson2json' conversions" >> {
      import bson2json._

      {
        val expected = s"""{
          "_id": {"$$oid":"${boid.stringify}"},
          "username": "lorem",
          "role": "ipsum",
          "created": {
            "$$timestamp": {"t":0,"i":987654321}
          },
          "lastModified": {
            "$$date": {"$$numberLong":"123456789"}
          },
          "sym": {
            "$$symbol":"foo"
          }
        }"""

        lazy val jsn = Json.parse(expected)

        s"with JSON extended syntax '${jsn}'" >> {
          lazy val userJs = Json.toJson(user)

          "thus be converted to expected JSON" in {
            userJs must_=== jsn
          }

          "thus be validated using JSON reader" in {
            // resolved from User.bsonReader
            val jsonReader: Reads[User] = implicitly[Reads[User]]

            userJs.validate[User](jsonReader) must_=== JsSuccess(user)
          }

          "thus be written using OWrites" in {
            val jsonWriter: OWrites[User] = implicitly[OWrites[User]]

            Json.toJson(user)(jsonWriter) must_=== userJs
          }
        }
      }

      {
        val expected = s"""{
          "_id": "${boid.stringify}",
          "username": "lorem",
          "role": "ipsum",
          "created": 987654321,
          "lastModified": 123456789,
          "sym": "foo"
        }"""

        lazy val jsn = Json.parse(expected)

        s"with lax syntax '${jsn}' provided by appropriate import" >> {
          import lax._ // <-- required import for lax syntax

          lazy val userLaxJs = Json.toJson(user) // via fromDocumentWriter

          "be converted to JSON" in {
            userLaxJs must_=== jsn
          }

          "thus be validated from JSON" in {
            userLaxJs.validate[User] must beLike[JsResult[User]] {
              case JsError(
                    (
                      JsPath,
                      JsonValidationError(
                        "Fails to handle '_id': String ('5dded45b0000000000000000') != BSONObjectID" ::
                        Nil
                      ) :: Nil
                    ) :: Nil
                  ) =>
                ok
            }
          }

          "thus be validated using BSON reader" in {
            // Overrides BSONReaders for OID/Timestamp/DateTime
            // so that the BSON representation matches the JSON lax one
            implicit val bsonReader: BSONDocumentReader[User] =
              Macros.reader[User]

            // Resolved from bsonReader
            val jsonReader: Reads[User] = implicitly[Reads[User]]

            userLaxJs.validate[User](jsonReader) must_=== JsSuccess(user)
          }

          "thus be written to JSON using BSON writer" in {
            // Overrides BSONWriters for OID/Timestamp/DateTime
            // so that the BSON representation matches the JSON lax one
            implicit val bsonWriter: BSONDocumentWriter[User] =
              Macros.writer[User]

            // Resolved from bsonWriter
            val jsonWriter: OWrites[User] = implicitly[OWrites[User]]

            Json.toJson(user)(jsonWriter) must_=== userLaxJs
          }
        }
      }
    }

    "be represented in BSON using 'json2bson' conversions" >> {
      import HandlerUseCaseSpec.Street
      import json2bson._

      {
        val doc = BSONDocument("number" -> 1, "name" -> "rue de la lune")

        val street = Street(Some(1), "rue de la lune")

        s"with JSON syntax '${doc}'" in {
          implicit val jsonWrites: OWrites[Street] = OWrites[Street] { street =>
            Json.obj("number" -> street.number, "name" -> street.name)
          }

          implicit val jsonReads: Reads[Street] = Reads[Street] { js =>
            for {
              number <- (js \ "number").validateOpt[Int]
              name <- (js \ "name").validate[String]
            } yield Street(number, name)
          }

          {
            // Resolved from jsonFormat
            val bsonWriter = implicitly[BSONDocumentWriter[Street]]

            bsonWriter.writeTry(street) must beSuccessfulTry(doc)
          } and {
            // Resolved from jsonFormat
            val bsonReader = implicitly[BSONDocumentReader[Street]]

            bsonReader.readTry(doc) must beSuccessfulTry(street)
          }
        }
      }
    }
  }
}

// Test fixtures
object HandlerUseCaseSpec {

  /*
   Note: Using BSON types in case class is not recommended,
   as it couples the case class with this specific persistence layer.
   */
  case class User(
      _id: BSONObjectID, // Rather use UUID or String
      username: String,
      role: String,
      created: BSONTimestamp, // Rather use Instance
      lastModified: BSONDateTime,
      sym: Option[BSONSymbol]) // Rather use String

  object User {
    implicit val bsonWriter: BSONDocumentWriter[User] = Macros.writer[User]

    implicit val bsonReader: BSONDocumentReader[User] = Macros.reader[User]
  }

  case class Street(
      number: Option[Int],
      name: String)
}
