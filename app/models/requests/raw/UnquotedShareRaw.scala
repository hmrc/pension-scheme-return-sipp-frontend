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

import cats.data.NonEmptyList
import models.CsvValue
import play.api.libs.json.*

object UnquotedShareRaw {

  case class RawShareCompanyDetails(
    companySharesName: CsvValue[String],
    companySharesCRN: CsvValue[Option[String]],
    reasonNoCRN: CsvValue[Option[String]],
    sharesClass: CsvValue[Option[String]],
    noOfShares: CsvValue[Option[String]]
  )

  case class RawShareTransactionDetail(
    totalCost: CsvValue[String],
    independentValuation: CsvValue[String],
    totalDividendsIncome: CsvValue[String],
    sharesDisposed: CsvValue[String]
  )

  case class RawDisposal(
    disposedSharesAmt: CsvValue[Option[String]],
    disposalConnectedParty: CsvValue[Option[String]],
    purchaserName: CsvValue[Option[String]],
    independentValuation: CsvValue[Option[String]],
    noOfSharesSold: CsvValue[Option[String]],
    noOfSharesHeld: CsvValue[Option[String]]
  )

  case class RawTransactionDetail(
    row: Int,
    firstNameOfSchemeMember: CsvValue[String],
    lastNameOfSchemeMember: CsvValue[String],
    memberDateOfBirth: CsvValue[String],
    memberNino: CsvValue[Option[String]],
    memberNoNinoReason: CsvValue[Option[String]],
    shareCompanyDetails: RawShareCompanyDetails,
    acquiredFromName: CsvValue[String],
    rawSharesTransactionDetail: RawShareTransactionDetail,
    rawDisposal: RawDisposal
  )

  object RawTransactionDetail {
    def create(
      row: Int,
      firstNameOfSchemeMemberAssetConnectedParty: CsvValue[String],
      lastNameOfSchemeMemberAssetConnectedParty: CsvValue[String],
      memberDateOfBirthAssetConnectedParty: CsvValue[String],
      memberNinoAssetConnectedParty: CsvValue[Option[String]],
      memberNinoReasonAssetConnectedParty: CsvValue[Option[String]],
      companySharesName: CsvValue[String],
      companySharesCRN: CsvValue[Option[String]],
      reasonNoCRN: CsvValue[Option[String]],
      sharesClass: CsvValue[Option[String]],
      noOfShares: CsvValue[Option[String]],
      acquiredFromName: CsvValue[String],
      totalCost: CsvValue[String],
      independentValuationTransaction: CsvValue[String],
      totalDividendsIncome: CsvValue[String],
      sharesDisposed: CsvValue[String],
      disposedSharesAmt: CsvValue[Option[String]],
      disposalConnectedParty: CsvValue[Option[String]],
      purchaserName: CsvValue[Option[String]],
      independentValuationDisposal: CsvValue[Option[String]],
      noOfSharesSold: CsvValue[Option[String]],
      noOfSharesHeld: CsvValue[Option[String]]
    ): RawTransactionDetail = RawTransactionDetail(
      row,
      firstNameOfSchemeMemberAssetConnectedParty,
      lastNameOfSchemeMemberAssetConnectedParty,
      memberDateOfBirthAssetConnectedParty,
      memberNinoAssetConnectedParty,
      memberNinoReasonAssetConnectedParty,
      RawShareCompanyDetails(
        companySharesName,
        companySharesCRN,
        reasonNoCRN,
        sharesClass,
        noOfShares
      ),
      acquiredFromName,
      RawShareTransactionDetail(
        totalCost,
        independentValuationTransaction,
        totalDividendsIncome,
        sharesDisposed
      ),
      RawDisposal(
        disposedSharesAmt,
        disposalConnectedParty,
        purchaserName,
        independentValuationDisposal,
        noOfSharesSold = noOfSharesSold,
        noOfSharesHeld = noOfSharesHeld
      )
    )

    implicit class Ops(val raw: RawTransactionDetail) extends AnyVal {
      def toNonEmptyList: NonEmptyList[String] =
        NonEmptyList.of(
          raw.firstNameOfSchemeMember.value,
          raw.lastNameOfSchemeMember.value,
          raw.memberDateOfBirth.value,
          raw.memberNino.value.getOrElse(""),
          raw.memberNoNinoReason.value.getOrElse(""),
          raw.shareCompanyDetails.companySharesName.value,
          raw.shareCompanyDetails.companySharesCRN.value.getOrElse(""),
          raw.shareCompanyDetails.reasonNoCRN.value.getOrElse(""),
          raw.shareCompanyDetails.sharesClass.value.getOrElse(""),
          raw.shareCompanyDetails.noOfShares.value.getOrElse(""),
          raw.acquiredFromName.value,
          raw.rawSharesTransactionDetail.totalCost.value,
          raw.rawSharesTransactionDetail.independentValuation.value,
          raw.rawSharesTransactionDetail.totalDividendsIncome.value,
          raw.rawSharesTransactionDetail.sharesDisposed.value,
          raw.rawDisposal.disposedSharesAmt.value.getOrElse(""),
          raw.rawDisposal.purchaserName.value.getOrElse(""),
          raw.rawDisposal.disposalConnectedParty.value.getOrElse(""),
          raw.rawDisposal.independentValuation.value.getOrElse(""),
          raw.rawDisposal.noOfSharesSold.value.getOrElse(""),
          raw.rawDisposal.noOfSharesHeld.value.getOrElse("")
        )
    }
  }

  implicit val formatRawShareCompanyDetails: OFormat[RawShareCompanyDetails] = Json.format[RawShareCompanyDetails]
  implicit val formatShareTransactionRawDetails: OFormat[RawShareTransactionDetail] =
    Json.format[RawShareTransactionDetail]
  implicit val formatRawDisposal: OFormat[RawDisposal] = Json.format[RawDisposal]
  implicit val formatTransactionRawDetails: OFormat[RawTransactionDetail] = Json.format[RawTransactionDetail]
}
