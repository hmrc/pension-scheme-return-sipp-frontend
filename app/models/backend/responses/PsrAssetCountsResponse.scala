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
import play.api.libs.json.{Json, OFormat}

case class PsrAssetCountsResponse(
  interestInLandOrPropertyCount: Int,
  landArmsLengthCount: Int,
  assetsFromConnectedPartyCount: Int,
  tangibleMoveablePropertyCount: Int,
  outstandingLoansCount: Int,
  unquotedSharesCount: Int
)

object PsrAssetCountsResponse {
  implicit val formatPSRSubmissionResponse: OFormat[PsrAssetCountsResponse] =
    Json.format[PsrAssetCountsResponse]

  implicit class PsrAssetCountsResponseExtensions(val items: PsrAssetCountsResponse) extends AnyVal {
    def getPopulatedField(journey: Journey): Int = journey match {
      case Journey.InterestInLandOrProperty => items.interestInLandOrPropertyCount
      case Journey.ArmsLengthLandOrProperty => items.landArmsLengthCount
      case Journey.TangibleMoveableProperty => items.tangibleMoveablePropertyCount
      case Journey.OutstandingLoans => items.outstandingLoansCount
      case Journey.UnquotedShares => items.unquotedSharesCount
      case Journey.AssetFromConnectedParty => items.assetsFromConnectedPartyCount
    }
  }
}
