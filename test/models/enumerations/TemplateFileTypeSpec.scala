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

import org.scalatestplus.scalacheck.ScalaCheckPropertyChecks
import utils.BaseSpec

class TemplateFileTypeSpec extends BaseSpec with ScalaCheckPropertyChecks {
  "TemplateFileTypeSpec" - {

    "return correct file names for given enums" in {
      TemplateFileType.InterestLandOrPropertyTemplateFile.fileName mustBe "TEMPLATE - SIPP Interest in land or property from a connected party.xlsx"
      TemplateFileType.ArmsLengthLandOrPropertyTemplateFile.fileName mustBe "TEMPLATE - SIPP Arms length land or property.xlsx"
      TemplateFileType.TangibleMoveablePropertyTemplateFile.fileName mustBe "TEMPLATE - SIPP Tangible moveable property acquired from an arm's length party.xlsx"
      TemplateFileType.OutstandingLoansTemplateFile.fileName mustBe "TEMPLATE - SIPP Outstanding loans made to someone else.xlsx"
      TemplateFileType.UnquotedSharesTemplateFile.fileName mustBe "TEMPLATE - SIPP Unquoted shares acquired from an arm's length party.xlsx"
      TemplateFileType.AssetFromConnectedPartyTemplateFile.fileName mustBe "TEMPLATE - SIPP Any asset other than land or property acquired from a connected party.xlsx"
    }
  }
}
