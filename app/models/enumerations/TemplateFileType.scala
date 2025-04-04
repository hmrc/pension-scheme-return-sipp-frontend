/*
 * Copyright 2024 HM Revenue & Customs
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package models.enumerations

import models.Enumerable
import play.api.mvc.JavascriptLiteral
import utils.WithName

sealed trait TemplateFileType {
  val name: String
  val key: String
  val fileName: String
}
object TemplateFileType extends Enumerable.Implicits {
  case object InterestLandOrPropertyTemplateFile
      extends WithName("InterestLandOrPropertyTemplateFile")
      with TemplateFileType {
    override val key: String = "InterestLandOrPropertyTemplateFile"
    override val fileName: String = "TEMPLATE - SIPP Interest in land or property from a connected party.xlsx"
  }
  case object ArmsLengthLandOrPropertyTemplateFile
      extends WithName("ArmsLengthLandOrPropertyTemplateFile")
      with TemplateFileType {
    override val key: String = "ArmsLengthLandOrPropertyTemplateFile"
    override val fileName: String = "TEMPLATE - SIPP Arms length land or property.xlsx"
  }

  case object TangibleMoveablePropertyTemplateFile
      extends WithName("TangibleMoveablePropertyTemplateFile")
      with TemplateFileType {
    override val key: String = "TangibleMoveablePropertyTemplateFile"
    override val fileName: String =
      "TEMPLATE - SIPP Tangible moveable property acquired from an arm's length party.xlsx"
  }

  case object OutstandingLoansTemplateFile extends WithName("OutstandingLoansTemplateFile") with TemplateFileType {
    override val key: String = "OutstandingLoansTemplateFile"
    override val fileName: String = "TEMPLATE - SIPP Outstanding loans made to someone else.xlsx"
  }

  case object UnquotedSharesTemplateFile extends WithName("UnquotedSharesTemplateFile") with TemplateFileType {
    override val key: String = "UnquotedSharesTemplateFile"
    override val fileName: String = "TEMPLATE - SIPP Unquoted shares acquired from an arm's length party.xlsx"
  }

  case object AssetFromConnectedPartyTemplateFile
      extends WithName("AssetFromConnectedPartyTemplateFile")
      with TemplateFileType {
    override val key: String = "AssetFromConnectedPartyTemplateFile"
    override val fileName: String =
      "TEMPLATE - SIPP Any asset other than land or property acquired from a connected party.xlsx"
  }

  case object Unknown extends WithName("unknown") with TemplateFileType {
    override val key: String = "unknown"
    override val fileName: String = "unknown"
  }

  val values: List[TemplateFileType] =
    List(
      InterestLandOrPropertyTemplateFile,
      ArmsLengthLandOrPropertyTemplateFile,
      TangibleMoveablePropertyTemplateFile,
      OutstandingLoansTemplateFile,
      UnquotedSharesTemplateFile,
      AssetFromConnectedPartyTemplateFile
    )

  def withNameWithDefault(name: String): TemplateFileType =
    values.find(_.toString.toLowerCase() == name.toLowerCase()).getOrElse(Unknown)

  implicit val enumerable: Enumerable[TemplateFileType] = Enumerable(values.map(v => (v.toString, v))*)

  implicit val jsLiteral: JavascriptLiteral[TemplateFileType] = (value: TemplateFileType) => value.name
}
