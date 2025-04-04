$if(directory.empty)$
package pages
$else$
package pages.$directory$
$endif$

import play.api.libs.json.JsPath
import models.SchemeId.Srn
import models.Money
import pages.QuestionPage
$if(!index.empty)$
import config.RefinedTypes.*
import utils.RefinedUtils.arrayIndex
$endif$

$if(index.empty)$
case class $className;format="cap"$Page(srn: Srn) extends QuestionPage[String] {
  $else$
  case class $className;format="cap"$Page(srn: Srn, index: $index$) extends QuestionPage[String] {
    $endif$

    $if(index.empty)$
    override def path: JsPath = JsPath \ toString
    $else$
    override def path: JsPath = JsPath \ toString \ index.arrayIndex.toString
    $endif$

    override def toString: String = "$className;format="decap"$"
  }