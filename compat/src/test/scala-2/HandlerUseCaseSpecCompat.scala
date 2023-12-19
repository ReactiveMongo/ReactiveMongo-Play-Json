import reactivemongo.play.TestUtils._

import org.specs2.matcher.TypecheckMatchers._

import _root_.reactivemongo.api.bson._
import reactivemongo.ExtendedJsonFixtures

trait HandlerUseCaseSpecCompat { _: HandlerUseCaseSpec =>
  "User (compatibility)" should {
    import HandlerUseCaseSpec.User
    import ExtendedJsonFixtures.boid

    "not be serializable on JSON without 'bson2json'" in {
      lazy val res = typecheck("""Json.toJson(User(
        boid,
        "lorem",
        "ipsum",
        created = BSONTimestamp(987654321L),
        lastModified = BSONDateTime(123456789L),
        sym = Some(BSONSymbol("foo"))
      ))""")

      {
        res must failWith(
          "(ambiguous implicit|No Json serializer found).*"
        )
      } or {
        res must failWith("Ambiguous given instances.*Writes.*")
      }
    }
  }
}
