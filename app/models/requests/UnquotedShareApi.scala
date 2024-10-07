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
import fs2.data.csv.RowEncoder
import models._
import models.requests.common.{SharesCompanyDetails, UnquotedShareDisposalDetail, YesNo}
import models.requests.psr.ReportDetails
import play.api.libs.json._
import CustomFormats._

case class UnquotedShareRequest(
  reportDetails: ReportDetails,
  transactions: Option[NonEmptyList[UnquotedShareApi.TransactionDetail]]
)

case class UnquotedShareResponse(
  transactions: List[UnquotedShareApi.TransactionDetail]
)

object UnquotedShareApi {

  case class TransactionDetail(
    row: Option[Int],
    nameDOB: NameDOB,
    nino: NinoType,
    sharesCompanyDetails: SharesCompanyDetails,
    acquiredFromName: String,
    totalCost: Double,
    independentValuation: YesNo,
    totalDividendsIncome: Double,
    sharesDisposed: YesNo,
    sharesDisposalDetails: Option[UnquotedShareDisposalDetail],
    transactionCount: Option[Int] =
      None // TODO -> Should not be needed! In Backend side we are counting with transactions.length
  )

  implicit val formatUnquotedTransactionDetails: OFormat[TransactionDetail] = Json.format[TransactionDetail]
  implicit val formatUnquotedRequest: OFormat[UnquotedShareRequest] = Json.format[UnquotedShareRequest]
  implicit val formatUnquotedResponse: OFormat[UnquotedShareResponse] = Json.format[UnquotedShareResponse]

  implicit val unquotedSharesTrxDetailRowEncoder: RowEncoder[TransactionDetail] = RowEncoder.instance { trx =>
    NonEmptyList.of(
      "",
      trx.nameDOB.firstName,
      trx.nameDOB.lastName,
      trx.nameDOB.dob.toString,
      trx.nino.nino.mkString,
      trx.nino.reasonNoNino.mkString,
      trx.transactionCount.map(_.toString).getOrElse(""),
      trx.sharesCompanyDetails.companySharesName,
      trx.sharesCompanyDetails.companySharesCRN.map(_.crn).mkString,
      trx.sharesCompanyDetails.reasonNoCRN.mkString,
      trx.sharesCompanyDetails.sharesClass,
      trx.sharesCompanyDetails.noOfShares.toString,
      trx.acquiredFromName,
      trx.totalCost.toString,
      trx.independentValuation.entryName,
      trx.totalDividendsIncome.toString,
      trx.sharesDisposed.entryName,
      trx.sharesDisposalDetails.map(_.disposedShareAmount).mkString,
      trx.sharesDisposalDetails.map(_.purchasersNames).mkString,
      trx.sharesDisposalDetails.map(_.disposalConnectedParty).mkString,
      trx.sharesDisposalDetails.map(_.independentValuationDisposal).mkString,
      trx.sharesDisposalDetails.map(_.noOfSharesSold).mkString,
      trx.sharesDisposalDetails.map(_.noOfSharesHeld).mkString
    )
  }

}
