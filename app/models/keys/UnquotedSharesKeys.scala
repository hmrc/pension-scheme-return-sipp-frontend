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

object UnquotedSharesKeys extends CommonKeys {
  val companyName = "What is the name of the company to which the shares relate?"
  val companyCRN = "What is the CRN of the company to which the shares relate?"
  val companyNoCRNReason = "If no CRN of the company to which the shares relate, enter reason"
  val companyClassOfShares = "What are the class of shares acquired?"
  val companyNumberOfShares = "What are the total number of shares acquired?"
  val acquiredFrom = "Who were the shares acquired from?"
  val totalCost = "What was the total cost of shares acquired, or subscribed for?"
  val transactionIndependentValuation =
    "In respect of of these shares, was this transaction supported by an independent valuation?"
  val totalDividends =
    "What was the total amount of dividends or other income received during the year?"
  val disposalMade = "Was any disposal of shares made during the tax year?"
  val totalSaleValue =
    "If disposal of shares were made during the tax year - what is the total amount of consideration received from the sale of shares?"
  val disposalPurchaserName = "What was the name of the purchaser of the shares?"
  val disposalConnectedParty = "Was the disposal made to a connected party or connected parties?"
  val disposalIndependentValuation = "Was the transaction supported by an independent valuation?"
  val noOfSharesSold = "What was the number of shares sold?"
  val noOfSharesHeld = "What is the total number of shares now held?"

  val headers: NonEmptyList[String] = NonEmptyList.of(
    firstName,
    lastName,
    memberDateOfBirth,
    memberNino,
    memberReasonNoNino,
    companyName,
    companyCRN,
    companyNoCRNReason,
    companyClassOfShares,
    companyNumberOfShares,
    acquiredFrom,
    totalCost,
    transactionIndependentValuation,
    totalDividends,
    disposalMade,
    totalSaleValue,
    disposalPurchaserName,
    disposalConnectedParty,
    disposalIndependentValuation,
    noOfSharesSold,
    noOfSharesHeld
  )

  val helpers: NonEmptyList[String] = NonEmptyList.of(
    "Question help information. The information in this row will give you hints or tips to help you to complete the required questions in the cells above.",
    "Enter the first name of the scheme member. Mandatory question.",
    "Enter the last name of the scheme member. Mandatory question.",
    "Use the format DD-MM-YYYY. Mandatory question.",
    "Enter the individuals National Insurance Number.  If you do not know this, add the reason why you do not have this in the next column. 9 characters in alphanumeric in the format for example: AA999999A.  Mandatory question.",
    "Enter reason for not having the members National Insurance number. Maximum of 160 characters.",
    "Min 1 character - Max 160 characters. Mandatory question.",
    "Enter Company Registration Number (CRN). If you do not know this, add the reason why you do not have this.",
    "If you do not know CRN of company to which the shares relate, add the reason why you do not have this.",
    "Min 1 character - Max 160 characters",
    "Enter total number of shares acquired.",
    "Enter name or details. Max 160 characters. Mandatory Question.",
    "Enter the total amount in GBP (pounds and pence)",
    "Enter YES or NO",
    "Enter the total amount in GBP (pounds and pence)",
    "Enter YES or NO. If NO, you do not need to complete any further questions in this section.",
    "Enter the total amount in GBP (pounds and pence)",
    "Enter name. Max 160 characters",
    "Enter YES or NO.",
    "Enter YES or NO",
    "Maximum of 50. Enter characters 0-9.",
    "Enter characters 0-9",
    "ERRORS WITH DETAIL"
  )
}
