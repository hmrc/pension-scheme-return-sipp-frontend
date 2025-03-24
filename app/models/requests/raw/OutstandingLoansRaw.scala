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

object OutstandingLoansRaw {

  case class RawTransactionDetail(
    row: Int,
    firstNameOfSchemeMember: CsvValue[String],
    lastNameOfSchemeMember: CsvValue[String],
    memberNino: CsvValue[Option[String]],
    memberReasonNoNino: CsvValue[Option[String]],
    memberDateOfBirth: CsvValue[String],
    rawAsset: RawAsset
  )

  case class RawAsset(
    loanRecipientName: CsvValue[String],
    dateOfLoan: CsvValue[String],
    amountOfLoan: CsvValue[String],
    loanConnectedParty: CsvValue[String],
    repayDate: CsvValue[String],
    interestRate: CsvValue[String],
    loanSecurity: CsvValue[String],
    capitalRepayments: CsvValue[String],
    arrearsOutstandingPrYears: CsvValue[String],
    outstandingYearEndAmount: CsvValue[String],
    arrearsOutstandingPrYearsAmt: CsvValue[Option[String]]
  )

  object RawTransactionDetail {
    def create(
      row: Int,
      /*  B */ firstNameOfSchemeMember: CsvValue[String],
      /*  C */ lastNameOfSchemeMember: CsvValue[String],
      /*  D */ memberDateOfBirth: CsvValue[String],
      /*  E */ memberNino: CsvValue[Option[String]],
      /*  F */ memberReasonNoNino: CsvValue[Option[String]],
      /*  G */ recipientName: CsvValue[String],
      /*  H */ dateOfLoan: CsvValue[String],
      /*  I */ amountOfLoan: CsvValue[String],
      /*  J */ isAssociated: CsvValue[String],
      /*  K */ repaymentDate: CsvValue[String],
      /*  L */ interestRate: CsvValue[String],
      /*  M */ hasSecurity: CsvValue[String],
      /*  N */ capitalPayment: CsvValue[String],
      /*  O */ anyArrears: CsvValue[String],
      /*  P */ outstandingAmount: CsvValue[String],
      /*  Q */ arrearsOutstandingPrYearsAmt: CsvValue[Option[String]]
    ): RawTransactionDetail = RawTransactionDetail(
      row = row,
      firstNameOfSchemeMember = firstNameOfSchemeMember,
      lastNameOfSchemeMember = lastNameOfSchemeMember,
      memberNino = memberNino,
      memberReasonNoNino = memberReasonNoNino,
      memberDateOfBirth = memberDateOfBirth,
      RawAsset(
        loanRecipientName = recipientName,
        dateOfLoan = dateOfLoan,
        amountOfLoan = amountOfLoan,
        loanConnectedParty = isAssociated,
        repayDate = repaymentDate,
        interestRate = interestRate,
        loanSecurity = hasSecurity,
        capitalRepayments = capitalPayment,
        arrearsOutstandingPrYears = anyArrears,
        outstandingYearEndAmount = outstandingAmount,
        arrearsOutstandingPrYearsAmt = arrearsOutstandingPrYearsAmt
      )
    )

    implicit class Ops(val raw: RawTransactionDetail) extends AnyVal {
      def toNonEmptyList: NonEmptyList[String] =
        NonEmptyList.of(
          raw.firstNameOfSchemeMember.value,
          raw.lastNameOfSchemeMember.value,
          raw.memberDateOfBirth.value,
          raw.memberNino.value.getOrElse(""),
          raw.memberReasonNoNino.value.getOrElse(""),
          raw.rawAsset.loanRecipientName.value,
          raw.rawAsset.dateOfLoan.value,
          raw.rawAsset.amountOfLoan.value,
          raw.rawAsset.loanConnectedParty.value,
          raw.rawAsset.repayDate.value,
          raw.rawAsset.interestRate.value,
          raw.rawAsset.loanSecurity.value,
          raw.rawAsset.capitalRepayments.value,
          raw.rawAsset.arrearsOutstandingPrYears.value,
          raw.rawAsset.arrearsOutstandingPrYearsAmt.value.getOrElse(""),
          raw.rawAsset.outstandingYearEndAmount.value
        )
    }

  }

  implicit val formatRawAsset: OFormat[RawAsset] = Json.format[RawAsset]
  implicit val formatTransactionRawDetails: OFormat[RawTransactionDetail] = Json.format[RawTransactionDetail]
}
