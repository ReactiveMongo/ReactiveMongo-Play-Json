package reactivemongo.play.json.compat

private[compat] trait PackageCompat {

  /** Compatibility alias for `play.api.libs.json.JsTrue` (DO NOT USE) */
  val JsTrue = play.api.libs.json.JsTrue

  /** Compatibility alias for `play.api.libs.json.JsFalse` (DO NOT USE) */
  val JsFalse = play.api.libs.json.JsFalse
}
