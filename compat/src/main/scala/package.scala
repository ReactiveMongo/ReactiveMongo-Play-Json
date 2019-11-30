package reactivemongo.play.json

import play.api.libs.json.{ JsObject, JsString, JsValue }

/**
 * Implicit conversions for handler & value types between
 * `play.api.libs.json` and `reactivemongo.api.bson`.
 *
 * {{{
 * import reactivemongo.play.json.compat._
 * }}}
 *
 * For more specific imports, see [[ValueConverters]] and [[HandlerConverters]].
 */
package object compat extends PackageCompat
  with ValueConverters with HandlerConverters {

  /**
   * DSL for [[https://docs.mongodb.com/manual/reference/mongodb-extended-json MongoDB Extended JSON]] syntax (v2).
   *
   * {{{
   * import play.api.libs.json.Json
   * import reactivemongo.play.json.compat.dsl._
   *
   * Json.obj("int" -> int(1), "double" -> double(2.3D))
   * // {
   * //   "int": { "\$numberInt": "1" },
   * //   "double": { "\$numberDouble": "2.3" }
   * // }
   * }}}
   */
  object dsl {
    /**
     * Represents a [[scala.Int]] value using [[https://docs.mongodb.com/manual/reference/mongodb-extended-json MongoDB Extended JSON]] syntax (v2).
     *
     * {{{
     * import play.api.libs.json.Json
     * import reactivemongo.play.json.compat.dsl.int
     *
     * Json.obj("field" -> int(1))
     * // { "field": { "\$numberInt": "1" } }
     * }}}
     */
    @inline def int(i: Int): JsObject =
      JsObject(Map[String, JsValue](f"$$numberInt" -> JsString(i.toString)))

    /**
     * Represents a [[scala.Long]] value using [[https://docs.mongodb.com/manual/reference/mongodb-extended-json MongoDB Extended JSON]] syntax (v2).
     *
     * {{{
     * import play.api.libs.json.Json
     * import reactivemongo.play.json.compat.dsl.long
     *
     * Json.obj("field" -> long(2L))
     * // { "field": { "\$numberLong": "2" } }
     * }}}
     */
    @inline def long(l: Long): JsObject =
      JsObject(Map[String, JsValue](f"$$numberLong" -> JsString(l.toString)))

    /**
     * Represents a [[scala.Double]] value using [[https://docs.mongodb.com/manual/reference/mongodb-extended-json MongoDB Extended JSON]] syntax (v2).
     *
     * {{{
     * import play.api.libs.json.Json
     * import reactivemongo.play.json.compat.dsl.double
     *
     * Json.obj("field" -> double(3.4D))
     * // { "field": { "\$numberDouble": "3.4" } }
     * }}}
     */
    @inline def double(d: Double): JsObject = {
      val repr: String = {
        if (d.isNaN) "NaN"
        else if (d.isNegInfinity) "Infinity"
        else if (d.isInfinity) "-Infinity"
        else d.toString
      }

      JsObject(Map[String, JsValue](f"$$numberDouble" -> JsString(repr)))
    }

    /**
     * Represents a [[scala.BigDecimal]] value using [[https://docs.mongodb.com/manual/reference/mongodb-extended-json MongoDB Extended JSON]] syntax (v2).
     *
     * {{{
     * import play.api.libs.json.Json
     * import reactivemongo.play.json.compat.dsl.decimal
     *
     * Json.obj("field" -> decimal(BigDecimal("4")))
     * // { "field": { "\$numberDecimal": "4" } }
     * }}}
     */
    @inline def decimal(d: BigDecimal): JsObject =
      JsObject(Map[String, JsValue](f"$$numberDecimal" -> JsString(d.toString)))

    /**
     * Represents a symbol using [[https://docs.mongodb.com/manual/reference/mongodb-extended-json MongoDB Extended JSON]] syntax (v2).
     *
     * {{{
     * import play.api.libs.json.Json
     * import reactivemongo.play.json.compat.dsl.symbol
     *
     * Json.obj("field" -> symbol("sym_name"))
     * // { "field": { "\$symbol": "sym_name" } }
     * }}}
     */
    @inline def symbol(name: String): JsObject =
      JsObject(Map[String, JsValue](f"$$symbol" -> JsString(name.toString)))

  }
}
