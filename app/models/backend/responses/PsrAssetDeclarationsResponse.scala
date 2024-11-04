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

package models.backend.responses

import models.Journey
import models.requests.common.YesNo
import play.api.libs.json.{Json, OFormat}

case class PsrAssetDeclarationsResponse(
  armsLengthLandOrProperty: Option[YesNo],
  interestInLandOrProperty: Option[YesNo],
  tangibleMoveableProperty: Option[YesNo],
  outstandingLoans: Option[YesNo],
  unquotedShares: Option[YesNo],
  assetFromConnectedParty: Option[YesNo]
)

object PsrAssetDeclarationsResponse {
  implicit val format: OFormat[PsrAssetDeclarationsResponse] =
    Json.format[PsrAssetDeclarationsResponse]

  implicit class PsrAssetDeclarationsResponseExtensions(val response: PsrAssetDeclarationsResponse) extends AnyVal {
    def getPopulatedField(journey: Journey): Option[YesNo] = journey match {
      case Journey.InterestInLandOrProperty => response.interestInLandOrProperty
      case Journey.ArmsLengthLandOrProperty => response.armsLengthLandOrProperty
      case Journey.TangibleMoveableProperty => response.tangibleMoveableProperty
      case Journey.OutstandingLoans => response.outstandingLoans
      case Journey.UnquotedShares => response.unquotedShares
      case Journey.AssetFromConnectedParty => response.assetFromConnectedParty
    }
  }
}
