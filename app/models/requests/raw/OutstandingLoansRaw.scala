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
import play.api.libs.json._

object OutstandingLoansRaw {

  case class RawAcquiredFrom(
    acquiredFromType: CsvValue[String],
    acquirerNinoForIndividual: CsvValue[Option[String]],
    acquirerCrnForCompany: CsvValue[Option[String]],
    acquirerUtrForPartnership: CsvValue[Option[String]],
    whoAcquiredFromTypeReasonAsset: CsvValue[Option[String]]
  )

  case class RawTransactionDetail(
    row: Int,
    firstNameOfSchemeMember: CsvValue[String],
    lastNameOfSchemeMember: CsvValue[String],
    memberDateOfBirth: CsvValue[String],
    countOfTransactions: CsvValue[String],
    rawAsset: RawAsset
  )

  case class RawAsset(
    loanRecipientName: CsvValue[String],
    acquiredFrom: RawAcquiredFrom,
    dateOfLoan: CsvValue[String],
    amountOfLoan: CsvValue[String],
    loanConnectedParty: CsvValue[String],
    repayDate: CsvValue[String],
    interestRate: CsvValue[String],
    loanSecurity: CsvValue[String],
    capitalRepayments: CsvValue[String],
    interestPayments: CsvValue[String],
    arrearsOutstandingPrYears: CsvValue[String],
    outstandingYearEndAmount: CsvValue[String]
  )

  object RawTransactionDetail {
    def create(
      row: Int,
      /*  B */ firstNameOfSchemeMember: CsvValue[String],
      /*  C */ lastNameOfSchemeMember: CsvValue[String],
      /*  D */ memberDateOfBirth: CsvValue[String],
      /*  E */ countOfTransactions: CsvValue[String],
      /*  F */ recipientName: CsvValue[String],
      /*  G */ acquiredFromType: CsvValue[String],
      /*  H */ acquiredFromIndividual: CsvValue[Option[String]],
      /*  I */ acquiredFromCompany: CsvValue[Option[String]],
      /*  J */ acquiredFromPartnership: CsvValue[Option[String]],
      /*  K */ acquiredFromReason: CsvValue[Option[String]],
      /*  L */ dateOfLoan: CsvValue[String],
      /*  M */ amountOfLoan: CsvValue[String],
      /*  N */ isAssociated: CsvValue[String],
      /*  O */ repaymentDate: CsvValue[String],
      /*  P */ interestRate: CsvValue[String],
      /*  Q */ hasSecurity: CsvValue[String],
      /*  R */ capitalPayment: CsvValue[String],
      /*  S */ interestRateForYear: CsvValue[String],
      /*  T */ anyArrears: CsvValue[String],
      /*  U */ outstandingAmount: CsvValue[String]
    ): RawTransactionDetail = RawTransactionDetail(
      row = row,
      firstNameOfSchemeMember = firstNameOfSchemeMember,
      lastNameOfSchemeMember = lastNameOfSchemeMember,
      memberDateOfBirth = memberDateOfBirth,
      countOfTransactions = countOfTransactions,
      RawAsset(
        loanRecipientName = recipientName,
        acquiredFrom = RawAcquiredFrom(
          acquiredFromType = acquiredFromType,
          acquirerNinoForIndividual = acquiredFromIndividual,
          acquirerCrnForCompany = acquiredFromCompany,
          acquirerUtrForPartnership = acquiredFromPartnership,
          whoAcquiredFromTypeReasonAsset = acquiredFromReason
        ),
        dateOfLoan = dateOfLoan,
        amountOfLoan = amountOfLoan,
        loanConnectedParty = isAssociated,
        repayDate = repaymentDate,
        interestRate = interestRate,
        loanSecurity = hasSecurity,
        capitalRepayments = capitalPayment,
        interestPayments = interestRateForYear,
        arrearsOutstandingPrYears = anyArrears,
        outstandingYearEndAmount = outstandingAmount
      )
    )

    implicit class Ops(val raw: RawTransactionDetail) extends AnyVal {
      def toNonEmptyList: NonEmptyList[String] =
        NonEmptyList.of(
          raw.firstNameOfSchemeMember.value,
          raw.lastNameOfSchemeMember.value,
          raw.memberDateOfBirth.value,
          raw.countOfTransactions.value,
          raw.rawAsset.loanRecipientName.value,
          raw.rawAsset.acquiredFrom.acquiredFromType.value,
          raw.rawAsset.acquiredFrom.acquirerNinoForIndividual.value.getOrElse(""),
          raw.rawAsset.acquiredFrom.acquirerCrnForCompany.value.getOrElse(""),
          raw.rawAsset.acquiredFrom.acquirerUtrForPartnership.value.getOrElse(""),
          raw.rawAsset.acquiredFrom.whoAcquiredFromTypeReasonAsset.value.getOrElse(""),
          raw.rawAsset.dateOfLoan.value,
          raw.rawAsset.amountOfLoan.value,
          raw.rawAsset.loanConnectedParty.value,
          raw.rawAsset.repayDate.value,
          raw.rawAsset.interestRate.value,
          raw.rawAsset.loanSecurity.value,
          raw.rawAsset.capitalRepayments.value,
          raw.rawAsset.interestPayments.value,
          raw.rawAsset.arrearsOutstandingPrYears.value,
          raw.rawAsset.outstandingYearEndAmount.value
        )
    }

  }

  implicit val formatRawAcquiredFrom: OFormat[RawAcquiredFrom] = Json.format[RawAcquiredFrom]
  implicit val formatRawAsset: OFormat[RawAsset] = Json.format[RawAsset]
  implicit val formatTransactionRawDetails: OFormat[RawTransactionDetail] = Json.format[RawTransactionDetail]
}
