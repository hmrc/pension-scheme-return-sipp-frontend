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
      TemplateFileType.InterestLandOrPropertyTemplateFile.fileName mustBe "SIPP Interest in land or property-template.xlsx"
      TemplateFileType.ArmsLengthLandOrPropertyTemplateFile.fileName mustBe "SIPP Arms length land or property-template.xlsx"
      TemplateFileType.TangibleMoveablePropertyTemplateFile.fileName mustBe "SIPP Tangible moveable property-template.xlsx"
      TemplateFileType.OutstandingLoansTemplateFile.fileName mustBe "SIPP Outstanding loans-template.xlsx"
      TemplateFileType.UnquotedSharesTemplateFile.fileName mustBe "SIPP Unquoted shares-template.xlsx"
      TemplateFileType.AssetFromConnectedPartyTemplateFile.fileName mustBe "SIPP Asset from connected party-template.xlsx"
    }
  }
}
