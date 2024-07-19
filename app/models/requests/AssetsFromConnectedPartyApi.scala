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

package models.requests

import cats.data.NonEmptyList
import models._
import models.requests.common._
import models.requests.psr.ReportDetails
import play.api.libs.json._
import CustomFormats._
import fs2.data.csv.RowEncoder

import java.time.LocalDate

case class AssetsFromConnectedPartyRequest(
  reportDetails: ReportDetails,
  transactions: Option[NonEmptyList[AssetsFromConnectedPartyApi.TransactionDetail]]
)

case class AssetsFromConnectedPartyResponse(
  transactions: List[AssetsFromConnectedPartyApi.TransactionDetail]
)

object AssetsFromConnectedPartyApi {

  case class TransactionDetail(
    row: Option[Int],
    nameDOB: NameDOB,
    nino: NinoType,
    acquisitionDate: LocalDate,
    assetDescription: String,
    acquisitionOfShares: YesNo,
    sharesCompanyDetails: Option[SharesCompanyDetails],
    acquiredFromName: String,
    totalCost: Double,
    independentValuation: YesNo,
    tangibleSchedule29A: YesNo,
    totalIncomeOrReceipts: Double,
    isPropertyDisposed: YesNo,
    disposalDetails: Option[DisposalDetails],
    disposalOfShares: Option[YesNo],
    noOfSharesHeld: Option[Int],
    transactionCount: Option[Int] = None // TODO -> Should not be needed! In Backend side we are counting with transactions.length
  )

  implicit val formatTransactionDetails: OFormat[TransactionDetail] = Json.format[TransactionDetail]
  implicit val formatAssetsFromConnectedRequest: OFormat[AssetsFromConnectedPartyRequest] =
    Json.format[AssetsFromConnectedPartyRequest]
  implicit val formatAssetsFromConnectedResponse: OFormat[AssetsFromConnectedPartyResponse] =
    Json.format[AssetsFromConnectedPartyResponse]

  // Last order is little confusing but it is like that in Excel
  implicit val assetsTrxDetailRowEncoder: RowEncoder[TransactionDetail] = RowEncoder.instance { trx =>
    NonEmptyList.of(
      "",
      trx.nameDOB.firstName,
      trx.nameDOB.lastName,
      trx.nameDOB.dob.toString,
      trx.nino.nino.mkString,
      trx.nino.reasonNoNino.mkString,
      trx.transactionCount.map(_.toString).getOrElse(""),
      trx.acquisitionDate.format(CSV_DATE_TIME),
      trx.assetDescription,
      trx.acquisitionOfShares.toString,
      trx.sharesCompanyDetails.map(_.companySharesName).getOrElse(""),
      trx.sharesCompanyDetails.flatMap(_.companySharesCRN.map(_.crn)).getOrElse(""),
      trx.sharesCompanyDetails.flatMap(_.reasonNoCRN).getOrElse(""),
      trx.sharesCompanyDetails.map(_.sharesClass).getOrElse(""),
      trx.sharesCompanyDetails.map(_.noOfShares.toString).getOrElse(""),
      trx.acquiredFromName,
      trx.totalCost.toString,
      trx.independentValuation.toString,
      trx.tangibleSchedule29A.toString,
      trx.totalIncomeOrReceipts.toString,
      trx.isPropertyDisposed.toString,
      trx.disposalDetails.map(_.disposedPropertyProceedsAmt.toString).getOrElse(""),
      trx.disposalDetails.map(_.purchasersNames).getOrElse(""),
      trx.disposalDetails.map(_.anyPurchaserConnectedParty.toString).getOrElse(""),
      trx.disposalDetails.map(_.independentValuationDisposal.toString).getOrElse(""),
      trx.disposalOfShares.mkString,
      trx.noOfSharesHeld.map(_.toString).getOrElse(""),
      trx.disposalDetails.map(_.propertyFullyDisposed.toString).getOrElse("")
    )
  }
}
