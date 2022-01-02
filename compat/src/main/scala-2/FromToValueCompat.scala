package reactivemongo.play.json.compat

import play.api.libs.json.{ JsNull, JsObject }

import reactivemongo.api.bson.{
  BSONMaxKey,
  BSONMinKey,
  BSONNull,
  BSONUndefined
}

private[compat] trait FromValueCompat { _: FromValue =>

  /**
   * See [[https://github.com/mongodb/specifications/blob/master/source/extended-json.rst syntax]]:
   *
   * `{ "\$undefined": true }`
   */
  implicit final val fromUndefined: BSONUndefined => JsObject = _ => JsUndefined

  /**
   * See [[https://github.com/mongodb/specifications/blob/master/source/extended-json.rst syntax]]:
   *
   * `{ "\$maxKey": 1 }`
   */
  implicit final val fromMaxKey: BSONMaxKey => JsObject = _ => JsMaxKey

  /**
   * See [[https://github.com/mongodb/specifications/blob/master/source/extended-json.rst syntax]]:
   *
   * `{ "\$minKey": 1 }`
   */
  implicit final val fromMinKey: BSONMinKey => JsObject = _ => JsMinKey

  implicit val fromNull: BSONNull => JsNull.type = _ => JsNull
}

private[compat] trait ToValueCompat { _: ToValue =>
  implicit final val toNull: JsNull.type => BSONNull = _ => BSONNull
}
