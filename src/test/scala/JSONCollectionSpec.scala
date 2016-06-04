import scala.concurrent._, duration._

import reactivemongo.core.errors.DetailedDatabaseException

import org.specs2.concurrent.{ ExecutionEnv => EE }

class JSONCollectionSpec extends org.specs2.mutable.Specification {
  "JSON collection" title

  sequential

  import Common._
  import play.api.libs.json.{ JsObject, _ }
  import reactivemongo.play.json._
  import reactivemongo.play.json.collection.{ JSONCollection, JSONQueryBuilder }
  import reactivemongo.api.commands.WriteResult
  import reactivemongo.api.{ FailoverStrategy, ReadPreference }
  import reactivemongo.bson._

  case class User(
    _id: Option[BSONObjectID] = None, username: String, height: Double
  )

  implicit val userReads = Json.reads[User]
  implicit val userWrites = Json.writes[User]

  lazy val collectionName = "reactivemongo_test_users"
  lazy val bsonCollection = db(collectionName)
  lazy val collection = new JSONCollection(db, collectionName, new FailoverStrategy())

  "JSONCollection.save" should {
    "add object if there does not exist in database" in { implicit ee: EE =>
      // Check current document does not exist
      val query = BSONDocument("username" -> BSONString("John Doe"))
      bsonCollection.find(query).one[JsObject] must beNone.await(1, timeout)

      // Add document..
      collection.save(User(username = "John Doe", height = 12)).
        aka("save") must beLike[WriteResult] {
          case result => result.ok must beTrue
        }.await(1, timeout)

      // Check data in mongodb..
      bsonCollection.find(query).one[BSONDocument].
        aka("result") must beSome[BSONDocument].which { d =>
          d.get("_id") must beSome and (
            d.get("username") must beSome(BSONString("John Doe"))
          )
        }.await(1, timeout)
    }

    "update object there already exists in database" in { implicit ee: EE =>
      // Find saved object
      val fetched1 = Await.result(collection.find(Json.obj("username" -> "John Doe")).one[User], timeout)
      fetched1 must beSome[User].which { u =>
        u._id.isDefined must beTrue and (u.username must_== "John Doe")
      }

      // Update object..
      val newUser = fetched1.get.copy(username = "Jane Doe")
      val result = Await.result(collection.save(newUser), timeout)
      result.ok must beTrue

      // Check data in mongodb..
      val fetched2 = Await.result(bsonCollection.find(BSONDocument("username" -> BSONString("John Doe"))).one[BSONDocument], timeout)
      fetched2 must beNone

      val fetched3 = Await.result(bsonCollection.find(BSONDocument("username" -> BSONString("Jane Doe"))).one[BSONDocument], timeout)
      fetched3 must beSome[BSONDocument].which { d =>
        d.get("_id") must beSome(fetched1.get._id.get) and (
          d.get("username") must beSome(BSONString("Jane Doe"))
        )
      }
    }

    "add object if does not exist but its field `_id` is set" in {
      implicit ee: EE =>
        // Check current document does not exist
        val query = BSONDocument("username" -> BSONString("Robert Roe"))

        val id = BSONObjectID.generate

        // Add document..
        collection.save(User(
          _id = Some(id), username = "Robert Roe", height = 13
        )).map(_.ok) aka "saved" must beTrue.await(1, timeout) and {
          // Check data in mongodb..
          bsonCollection.find(query).one[BSONDocument].
            aka("result") must beSome[BSONDocument].which { d =>
              d.get("_id") must beSome(id) and (
                d.get("username") must beSome(BSONString("Robert Roe"))
              )
            }.await(1, timeout)
        }
    }
  }

  "JSONCollection.findAndModify" should {
    "be successful" in { implicit ee: EE =>
      val id = BSONObjectID.generate
      val updateOp = collection.updateModifier(
        User(
          _id = Some(id),
          username = "James Joyce", height = 1.264290338792695E+64
        ),
        fetchNewObject = false, upsert = true
      )

      collection.findAndModify(BSONDocument("_id" -> id), updateOp).
        map(_.result[BSONDocument]) must beNone.await(1, timeout)
    }
  }

  "JSONQueryBuilder.merge" should {
    "write an JsObject with mongo query only if there are not options defined" in { implicit ee: EE =>
      val builder = JSONQueryBuilder(
        collection = collection,
        failover = new FailoverStrategy(),
        queryOption = Option(Json.obj("username" -> "John Doe"))
      )

      builder.merge(ReadPreference.Primary).toString.
        aka("merged") must beEqualTo("""{"$query":{"username":"John Doe"},"$readPreference":{"mode":"primary"}}""")
    }

    "write an JsObject with only defined options" in { implicit ee: EE =>
      val builder1 = JSONQueryBuilder(
        collection = collection,
        failover = new FailoverStrategy(),
        queryOption = Option(Json.obj("username" -> "John Doe")),
        sortOption = Option(Json.obj("age" -> 1))
      )
      builder1.merge(ReadPreference.Primary).toString must beEqualTo("""{"$query":{"username":"John Doe"},"$orderby":{"age":1},"$readPreference":{"mode":"primary"}}""")

      val builder2 = builder1.copy(commentString = Option("get john doe users sorted by age"))
      builder2.merge(ReadPreference.Primary).toString must beEqualTo("""{"$query":{"username":"John Doe"},"$orderby":{"age":1},"$comment":"get john doe users sorted by age","$readPreference":{"mode":"primary"}}""")
    }
  }

  "JSON collection" should {
    "find with empty criteria document" in { implicit ee: EE =>
      collection.find(Json.obj()).sort(Json.obj("updated" -> -1)).
        cursor[JsObject]().collect[List]().
        aka("find with empty document") must not(throwA[Throwable]).
        await(1, timeout)
    }

    "find with selector and projection" in { implicit ee: EE =>
      collection.find(
        selector = Json.obj("username" -> "Jane Doe"),
        projection = Json.obj("_id" -> 0)
      ).cursor[JsObject]().headOption must beSome[JsObject].which { json =>
          Json.stringify(json) must beEqualTo(
            "{\"username\":\"Jane Doe\",\"height\":12}"
          )
        }.await(1, timeout)
    }

    "count all matching document" in { implicit ee: EE =>
      collection.count() aka "all" must beEqualTo(3).await(1, timeout) and (
        collection.count(Some(Json.obj("username" -> "Jane Doe"))).
        aka("with query") must beEqualTo(1).await(1, timeout)
      ) and (
          collection.count(limit = 1) aka "limited" must beEqualTo(1).
          await(1, timeout)
        )
    }
  }

  "JSON cursor" should {
    "return result as a JSON array" in { implicit ee: EE =>
      import reactivemongo.play.json.collection.JsCursor._

      collection.find(Json.obj()).cursor[JsObject]().jsArray().
        map(_.value.map { js => (js \ "username").as[String] }).
        aka("extracted JSON array") must beEqualTo(List(
          "Jane Doe", "Robert Roe", "James Joyce"
        )).await(1, timeout)
    }

    "fail on maxTimeout" in { implicit ee: EE =>
      val ndocs = 100000

      Await.ready(Future.sequence {
        for (i <- 1 to ndocs)
          yield collection.insert(Json.obj("doc" -> s"doc-$i"))
      }, DurationInt(timeout.toMillis.toInt / 1000 * ndocs).seconds)

      collection.find(Json.obj("doc" -> "docX")).maxTimeMs(1).
        cursor[JsValue]().collect[List](10).
        aka("cursor with max time") must throwA[DetailedDatabaseException].
        await(1, DurationInt(1).second)
    }
  }
}
