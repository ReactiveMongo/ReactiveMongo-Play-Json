package reactivemongo.play.json.compat

import scala.language.implicitConversions

import play.api.libs.json.{ JsFalse => F, JsTrue => T }

import reactivemongo.api.bson.BSONBoolean

private[compat] trait ExtendedJsonCompat {

  private val _fbool = BSONBoolean(false)

  implicit final def toFalse(_f: F.type): BSONBoolean = {
    val _ = _f // Avoid unused
    _fbool
  }

  private val _tbool = BSONBoolean(true)

  implicit final def toTrue(_t: T.type): BSONBoolean = {
    val _ = _t // Avoid unused
    _tbool
  }
}
