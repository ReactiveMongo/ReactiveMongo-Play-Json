package reactivemongo.play.json

/**
 * Implicit conversions for handler & value types between
 * `play.api.libs.json` and `reactivemongo.api.bson`,
 * by default using the [[https://docs.mongodb.com/manual/reference/mongodb-extended-json MongoDB Extended JSON]] syntax.
 *
 * {{{
 * import play.api.libs.json.JsValue
 * import reactivemongo.api.bson.BSONValue
 *
 * import reactivemongo.play.json.compat.ValueConverters._
 *
 * def foo(v: BSONValue): JsValue = v // ValueConverters.fromValue
 * }}}
 *
 * For more specific imports, see [[ValueConverters]] and handler converters.
 */
private[json] trait PackageImplicits
