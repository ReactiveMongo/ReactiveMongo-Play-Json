package reactivemongo.play.json.collection

import play.api.libs.json.JsObject

import reactivemongo.api.{ Collection, FailoverStrategy, QueryOpts }
import reactivemongo.api.collections.GenericQueryBuilder

import reactivemongo.play.json.JSONSerializationPack

@SerialVersionUID(1)
@SuppressWarnings(Array("FinalModifierOnCaseClass"))
case class JSONQueryBuilder(
    @transient collection: Collection,
    failover: FailoverStrategy,
    queryOption: Option[JsObject] = None,
    sortOption: Option[JsObject] = None,
    projectionOption: Option[JsObject] = None,
    hintOption: Option[JsObject] = None,
    explainFlag: Boolean = false,
    snapshotFlag: Boolean = false,
    commentString: Option[String] = None,
    options: QueryOpts = QueryOpts(),
    maxTimeMsOption: Option[Long] = None
) extends GenericQueryBuilder[JSONSerializationPack.type] {
  type Self = JSONQueryBuilder

  @transient val pack = JSONSerializationPack

  def copy(queryOption: Option[JsObject], sortOption: Option[JsObject], projectionOption: Option[JsObject], hintOption: Option[JsObject], explainFlag: Boolean, snapshotFlag: Boolean, commentString: Option[String], options: QueryOpts, failover: FailoverStrategy, maxTimeMsOption: Option[Long]): JSONQueryBuilder = JSONQueryBuilder(collection, failover, queryOption, sortOption, projectionOption, hintOption, explainFlag, snapshotFlag, commentString, options, maxTimeMsOption)

}
