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

import models.backend.responses.PSRSubmissionResponse

case class SchemeDetailsItems(
  isLandOrPropertyInterestPopulated: Boolean,
  isLandOrPropertyArmsLengthPopulated: Boolean,
  isTangiblePropertyPopulated: Boolean,
  isSharesPopulated: Boolean,
  isAssetsPopulated: Boolean,
  isLoansPopulated: Boolean
)

object SchemeDetailsItems {
  def fromPSRSubmission(submissionResponse: PSRSubmissionResponse): SchemeDetailsItems = SchemeDetailsItems(
    isLandOrPropertyInterestPopulated = submissionResponse.landConnectedParty.nonEmpty,
    isLandOrPropertyArmsLengthPopulated = submissionResponse.landArmsLength.nonEmpty,
    isTangiblePropertyPopulated = submissionResponse.tangibleProperty.nonEmpty,
    isSharesPopulated = submissionResponse.unquotedShares.nonEmpty,
    isAssetsPopulated = submissionResponse.otherAssetsConnectedParty.nonEmpty,
    isLoansPopulated = submissionResponse.loanOutstanding.nonEmpty
  )

  implicit class SchemeDetailsItemsExtensions(val items: SchemeDetailsItems) extends AnyVal {
    def getPopulatedField(journey: Journey): Boolean = journey match {
      case Journey.InterestInLandOrProperty => items.isLandOrPropertyInterestPopulated
      case Journey.ArmsLengthLandOrProperty => items.isLandOrPropertyArmsLengthPopulated
      case Journey.TangibleMoveableProperty => items.isTangiblePropertyPopulated
      case Journey.OutstandingLoans => items.isLoansPopulated
      case Journey.UnquotedShares => items.isSharesPopulated
      case Journey.AssetFromConnectedParty => items.isAssetsPopulated
    }
  }
}
