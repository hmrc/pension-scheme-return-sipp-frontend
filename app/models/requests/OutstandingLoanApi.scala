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
import models.*
import models.requests.common.YesNo
import models.requests.psr.ReportDetails
import play.api.libs.json.*
import CustomFormats.*
import controllers.DownloadCsvController.RowEncoder

import java.time.LocalDate

case class OutstandingLoanRequest(
  reportDetails: ReportDetails,
  transactions: Option[NonEmptyList[OutstandingLoanApi.TransactionDetail]]
)

case class OutstandingLoanResponse(
  transactions: List[OutstandingLoanApi.TransactionDetail]
)

object OutstandingLoanApi {

  case class TransactionDetail(
    row: Option[Int],
    nameDOB: NameDOB,
    nino: NinoType,
    loanRecipientName: String,
    dateOfLoan: LocalDate,
    amountOfLoan: Double,
    loanConnectedParty: YesNo,
    repayDate: LocalDate,
    interestRate: Double,
    loanSecurity: YesNo,
    capitalRepayments: Double,
    arrearsOutstandingPrYears: YesNo,
    outstandingYearEndAmount: Double,
    arrearsOutstandingPrYearsAmt: Option[Double],
    transactionCount: Option[Int] =
      None // In BE correcting with counting with transactions.length. Plan is get rid of from count totally with removing it from csv in the future
  )

  implicit val formatTransactionDetails: OFormat[TransactionDetail] = Json.format[TransactionDetail]
  implicit val formatOutstandingRequest: OFormat[OutstandingLoanRequest] = Json.format[OutstandingLoanRequest]
  implicit val formatOutstandingResponse: OFormat[OutstandingLoanResponse] = Json.format[OutstandingLoanResponse]

  implicit val outstandingLoanTrxDetailRowEncoder: RowEncoder[TransactionDetail] = { trx =>
    NonEmptyList.of(
      "",
      trx.nameDOB.firstName,
      trx.nameDOB.lastName,
      trx.nameDOB.dob.toString,
      trx.nino.nino.mkString,
      trx.nino.reasonNoNino.mkString,
      trx.transactionCount.map(_.toString).getOrElse(""),
      trx.loanRecipientName,
      trx.dateOfLoan.format(CSV_DATE_TIME),
      trx.amountOfLoan.toString,
      trx.loanConnectedParty.toString,
      trx.repayDate.format(CSV_DATE_TIME),
      trx.interestRate.toString,
      trx.loanSecurity.toString,
      trx.capitalRepayments.toString,
      trx.arrearsOutstandingPrYears.toString,
      trx.outstandingYearEndAmount.toString,
      trx.arrearsOutstandingPrYearsAmt.mkString
    )
  }
}
