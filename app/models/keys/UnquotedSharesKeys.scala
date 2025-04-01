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
  val disposalPurchaserName = "If disposals were made on this, what was the name of the purchaser of the shares?"
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
    "Question help information. The information in this row will give you hints or tips to help you to complete the required questions in the cells above. What you need to do: Enter your data from row 3 onwards. Do not remove any columns. Complete the questions per member marked horizontally across the columns. For all mandatory questions that don’t apply, please leave blank. For members that have multiple shares, complete one row per share and repeat the members first name, last name and date of birth for the required number of rows.",
    "Enter the first name of the scheme member. Hyphens are accepted. Should be letters A to Z. Names have a maximum of 35 characters. Mandatory question.",
    "Enter the last name of the scheme member. Hyphens are accepted. Should be letters A to Z. Names have a maximum of 35 characters. Mandatory question.",
    "Use the format DD/MM/YYYY. Mandatory question.",
    "Enter the individual's National Insurance Number. If you do not know this, add the reason why you do not have this in the next column. 9 characters in alphanumeric in the format for example: AA999999A. Mandatory question.",
    "Enter reason for not having the member's National Insurance number. Maximum of 160 characters.",
    "Min 1 character - Max 160 characters. Mandatory question",
    "Enter Company Registration Number (CRN).",
    "If you do not know the CRN of company to which the shares relate, add the reason why you do not have this. Mandatory question if no CRN is given.",
    "Min 1 character - Max 160 characters. Mandatory question",
    "Enter total number of shares acquired. Mandatory question",
    "Enter name[s] If multiple names, separate each name with a plus sign (+), for example John Smith + Jane Doe + Joe Bloggs Do not use commas Should be letters A to Z. Hyphens are accepted. Maximum 160 characters Mandatory question",
    "Enter the total amount in GBP (pounds and pence). If the shares were not an acquisition, provide the total market value. If the shares were acquired more than 6 years ago provide the market value at the year of the return Mandatory question",
    "Enter YES or NO. Mandatory question",
    "Enter the total amount in GBP (pounds and pence). Enter the total amount received in respect of the relevant member for this share If multiple shares for this member - enter each share on a separate row",
    "Enter YES or NO. If NO, you do not need to complete any further questions in this section. Mandatory question",
    "Enter the total amount in GBP (pounds and pence). Mandatory question if there was any disposal of shares made during the tax year.",
    "Enter name of purchaser[s] If multiple names, separate each name with a plus sign (+), for example John Smith + Jane Doe + Joe Bloggs Do not use commas Should be letters A to Z. Hyphens are accepted. Maximum 160 characters Mandatory question if there was any disposal of shares made during the tax year.",
    "Enter YES or NO. Mandatory question if there was any disposal of shares made during the tax year.",
    "Enter YES or NO. Mandatory question if there was any disposal of shares made during the tax year.",
    "Enter characters 0-9. Mandatory question if there was any disposal of shares made during the tax year.",
    "Enter characters 0-9. Mandatory question if there was any disposal of shares made during the tax year.",
    "ERRORS WITH DETAIL"
  )
}
