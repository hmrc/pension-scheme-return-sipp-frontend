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

import play.api.mvc.JavascriptLiteral

sealed abstract class Journey(val name: String, val uploadRedirectTag: String)

object Journey {
  case object MemberDetails
      extends Journey(
        "memberDetails",
        "upload-your-member-details"
      )
  case object LandOrProperty
      extends Journey(
        "landOrProperty",
        "upload-interest-land-or-property"
      )

  implicit val jsLiteral: JavascriptLiteral[Journey] = {
    case MemberDetails => MemberDetails.name
    case LandOrProperty => LandOrProperty.name
  }
}
