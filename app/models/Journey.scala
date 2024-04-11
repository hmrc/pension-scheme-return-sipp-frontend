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

package models

import models.enumerations.TemplateFileType
import models.enumerations.TemplateFileType._
import play.api.mvc.JavascriptLiteral

sealed abstract class Journey(
  val name: String,
  val messagePrefix: String,
  val uploadRedirectTag: String,
  val templateFileType: TemplateFileType
)

object Journey {
  case object MemberDetails
      extends Journey(
        "memberDetails",
        "memberDetails",
        "upload-your-member-details",
        MemberDetailsTemplateFile
      )
  case object InterestInLandOrProperty
      extends Journey(
        "interestInLandOrProperty",
        "interestInLandOrProperty",
        "upload-interest-land-or-property",
        InterestLandOrPropertyTemplateFile
      )

  case object ArmsLengthLandOrProperty
      extends Journey(
        "armsLengthLandOrProperty",
        "armsLengthLandOrProperty",
        "upload-arms-length-land-or-property",
        ArmsLengthLandOrPropertyTemplateFile
      )

  case object TangibleMoveableProperty
      extends Journey(
        "tangibleMoveableProperty",
        "tangibleMoveableProperty",
        "upload-tangible-moveable-property",
        TangibleMoveablePropertyTemplateFile
      )

  case object OutstandingLoans
      extends Journey(
        "outstandingLoans",
        "outstandingLoans",
        "upload-outstanding-loans",
        OutstandingLoansTemplateFile
      )

  case object UnquotedShares
      extends Journey(
        "unquotedShares",
        "unquotedShares",
        "upload-unquoted-shares",
        UnquotedSharesTemplateFile
      )

  case object AssetFromConnectedParty
      extends Journey(
        "assetFromConnectedParty",
        "assetFromConnectedParty",
        "upload-asset-from-connected-party",
        AssetFromConnectedPartyTemplateFile
      )

  implicit val jsLiteral: JavascriptLiteral[Journey] = _.name
}
