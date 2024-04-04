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
import play.api.libs.json._

import java.time.LocalDate

case class OutstandingLoanRequest(
  noOfTransactions: Int,
  transactionDetails: Option[List[OutstandingLoanRequest.TransactionDetail]]
)

object OutstandingLoanRequest {

  case class TransactionDetail(
    row: Int,
    nameDOB: NameDOB,
    loanRecipientName: String,
    indivOrOrgIdentityDetails: AcquiredFromType,
    dateOfLoan: LocalDate,
    amountOfLoan: Double,
    loanConnectedParty: YesNo,
    repayDate: LocalDate,
    interestRate: Double,
    loanSecurity: YesNo,
    capitalRepayments: Double,
    interestPayments: Double,
    arrearsOutstandingPrYears: YesNo,
    outstandingYearEndAmount: Double
  )

  implicit def nonEmptyListFormat[T: Format]: Format[NonEmptyList[T]] = Format(
    Reads.list[T].flatMap { xs =>
      NonEmptyList.fromList(xs).fold[Reads[NonEmptyList[T]]](Reads.failed("The list is empty"))(Reads.pure(_))
    },
    Writes.list[T].contramap(_.toList)
  )

  implicit val formatTransactionDetails: OFormat[TransactionDetail] = Json.format[TransactionDetail]
  implicit val formatLandConnectedParty: OFormat[OutstandingLoanRequest] = Json.format[OutstandingLoanRequest]
}
