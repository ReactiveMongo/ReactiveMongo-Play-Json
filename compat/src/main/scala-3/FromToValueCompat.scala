package reactivemongo.play.json.compat

import scala.language.implicitConversions

import play.api.libs.json.{ JsNull, JsObject }

import reactivemongo.api.bson.{
  BSONMaxKey,
  BSONMinKey,
  BSONNull,
  BSONUndefined
}

private[compat] trait FromValueCompat { _self: FromValue =>

  /**
   * See [[https://github.com/mongodb/specifications/blob/master/source/extended-json.rst syntax]]:
   *
   * `{ "\$undefined": true }`
   */
  implicit final def fromUndefined(_undef: BSONUndefined): JsObject =
    JsUndefined

  /**
   * See [[https://github.com/mongodb/specifications/blob/master/source/extended-json.rst syntax]]:
   *
   * `{ "\$maxKey": 1 }`
   */
  implicit final def fromMaxKey(_max: BSONMaxKey): JsObject = JsMaxKey

  /**
   * See [[https://github.com/mongodb/specifications/blob/master/source/extended-json.rst syntax]]:
   *
   * `{ "\$minKey": 1 }`
   */
  implicit final def fromMinKey(_min: BSONMinKey): JsObject = JsMinKey

  implicit def fromNull(_null: BSONNull): JsNull.type = JsNull
}

private[compat] trait ToValueCompat { _self: ToValue & FromValueAPI =>
  implicit final def toNull(_null: JsNull.type): BSONNull = BSONNull
}
