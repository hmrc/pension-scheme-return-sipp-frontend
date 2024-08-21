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

package models.keys

import cats.data.NonEmptyList

object OutstandingLoansKeys extends CommonKeys {
  val countOfPropertyTransactions =
    "How many separate loans were made or outstanding during the tax year and not reported in a previous return for this member?"
  val recipientNameOfLoan = "What is the name of the recipient of loan?"
  val dateOfOutstandingLoan = "What was the date of loan?"
  val amountOfLoan = "What is the amount of the loan?"
  val isLoanAssociatedWithConnectedParty = "Is the loan associated with a connected party?"
  val repaymentDate = "What is the repayment date of the loan?"
  val interestRate = "What is the Interest Rate for the loan?"
  val hasSecurity = "Has security been given for the loan?"
  val capitalAndInterestPaymentForYear =
    "In respect of this loan, what is the total amount of Capital repayments and interest payments have been received by the scheme during the year?"
  val anyArrears = "In respect of this loan, are there any arrears outstanding from previous years?"
  val arrearsOutstandingPrYearsAmt = "If arrears are outstanding from previous years, enter the amount"
  val outstandingAmount = "In respect of this loan, what is the amount outstanding at the year end?"

  val headers: NonEmptyList[String] = NonEmptyList.of(
    firstName,
    lastName,
    memberDateOfBirth,
    memberNino,
    memberReasonNoNino,
    countOfPropertyTransactions,
    recipientNameOfLoan,
    dateOfOutstandingLoan,
    amountOfLoan,
    isLoanAssociatedWithConnectedParty,
    repaymentDate,
    interestRate,
    hasSecurity,
    capitalAndInterestPaymentForYear,
    anyArrears,
    arrearsOutstandingPrYearsAmt,
    outstandingAmount
  )

  val helpers: NonEmptyList[String] = NonEmptyList.of(
    "Question help information. The information in this row will give you hints or tips to help you to complete the required questions in the cells above.",
    "Enter the first name of the scheme member. Mandatory question",
    "Enter the last name of the scheme member. Mandatory question",
    "Use the format DD-MM-YYYY. Mandatory question.",
    "Enter the individuals National Insurance Number. If you do not know this, add the reason why you do not have this in the next column. 9 characters in alphanumeric in the format for example: AA999999A. Mandatory question.",
    "Enter reason for not having the members National Insurance number. Maximum of 160 characters.",
    "Enter number. Mandatory question.",
    "Enter name. Maximum 160 characters. Mandatory question",
    "Use the format DD-MM-YYYY. Mandatory question",
    "Enter the total amount in GBP (pounds and pence). Enter amount as whole number and to two decimal places. Mandatory question.",
    "Enter YES or NO. Mandatory question.",
    "Use format DD-MM-YYYY. Mandatory question.",
    "Enter the % Enter percentage as whole number and to two decimal places. Mandatory question.",
    "Enter YES or NO. Mandatory question.",
    "Enter the amount in GBP (pounds and pence) Enter amount as whole number and to two decimal places",
    "Enter YES or NO. Mandatory question.",
    "Mandatory if there are arrears outstanding from previous years. Enter Enter the amount in GBP (pounds and pence). Enter amount as whole number and to two decimal places.",
    "Enter the amount in GBP (pounds and pence). Enter amount as whole number and to two decimal places. Mandatory question.",
    "ERRORS WITH DETAIL"
  )
}
