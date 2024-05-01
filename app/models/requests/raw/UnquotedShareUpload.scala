/*
 * Copyright 2023 HM Revenue & Customs
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

package models.requests.raw

import models.requests.raw.UnquotedShareRaw.RawTransactionDetail
import models.requests.raw.AssetConnectedPartyUpload.ShareCompanyDetails
import models.requests.raw.UnquotedShareUpload._
import play.api.libs.json.{Json, OFormat}

case class UnquotedShareUpload(
  row: Int,
  firstNameOfSchemeMember: String,
  lastNameOfSchemeMember: String,
  memberDateOfBirth: String,
  memberNino: Option[String],
  memberNoNinoReason: Option[String],
  countOfTransactions: String,
  shareCompanyDetails: ShareCompanyDetails,
  acquiredFromName: String,
  transactionDetail: TransactionDetail,
  isSharesDisposed: String,
  disposal: Disposal
)

object UnquotedShareUpload {

  case class TransactionDetail(
    totalCost: String,
    independentValuation: String,
    noOfSharesSold: Option[String],
    totalDividendsIncome: String,
    sharesDisposed: String
  )

  case class Disposal(
    disposedSharesAmt: Option[String],
    disposalConnectedParty: Option[String],
    purchaserName: Option[String],
    independentValuation: Option[String],
    noOfSharesHeld: Option[String]
  )

  def fromRaw(raw: RawTransactionDetail): UnquotedShareUpload =
    UnquotedShareUpload(
      row = raw.row,
      firstNameOfSchemeMember = raw.firstNameOfSchemeMember.value,
      lastNameOfSchemeMember = raw.lastNameOfSchemeMember.value,
      memberDateOfBirth = raw.memberDateOfBirth.value,
      memberNino = raw.memberNino.value,
      memberNoNinoReason = raw.memberNoNinoReason.value,
      countOfTransactions = raw.countOfTransactions.value,
      shareCompanyDetails = ShareCompanyDetails(
        companySharesName = Some(raw.shareCompanyDetails.companySharesName.value),
        companySharesCRN = raw.shareCompanyDetails.companySharesCRN.value,
        reasonNoCRN = raw.shareCompanyDetails.reasonNoCRN.value,
        sharesClass = raw.shareCompanyDetails.sharesClass.value,
        noOfShares = raw.shareCompanyDetails.noOfShares.value
      ),
      acquiredFromName = raw.acquiredFromName.value,
      TransactionDetail(
        raw.rawSharesTransactionDetail.totalCost.value,
        raw.rawSharesTransactionDetail.independentValuation.value,
        raw.rawSharesTransactionDetail.noOfIndependentValuationSharesSold.value,
        raw.rawSharesTransactionDetail.totalDividendsIncome.value,
        raw.rawSharesTransactionDetail.sharesDisposed.value
      ),
      raw.rawSharesTransactionDetail.sharesDisposed.value,
      Disposal(
        raw.rawDisposal.disposedSharesAmt.value,
        raw.rawDisposal.disposalConnectedParty.value,
        raw.rawDisposal.purchaserName.value,
        raw.rawDisposal.independentValuation.value,
        raw.rawDisposal.noOfSharesHeld.value
      )
    )

  implicit val formatDisposal: OFormat[Disposal] = Json.format[Disposal]
  implicit val formatShareCompanyDetails: OFormat[ShareCompanyDetails] = Json.format[ShareCompanyDetails]
  implicit val formaTransactionDetails: OFormat[TransactionDetail] = Json.format[TransactionDetail]
  implicit val formatAssetConnectedPartyUpload: OFormat[UnquotedShareUpload] =
    Json.format[UnquotedShareUpload]
}
