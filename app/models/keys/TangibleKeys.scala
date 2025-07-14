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

object TangibleKeys extends CommonKeys {
  val descriptionOfAsset = "Description of asset"
  val dateOfAcquisition = "What is the date the scheme acquired the Tangible Moveable Property?"
  val totalCostOfAsset = "What is the total cost of the Tangible Moveable Property at the date the scheme acquired it?"
  val acquiredFrom = "Who was the asset acquired from?"
  val isIndependentValuation = "Is this transaction supported by an Independent Valuation?"
  val totalIncomeInTaxYear =
    "What is the total amount of income and receipts received in respect of the Tangible Moveable property during tax year?"
  val totalCostOrMarketValue =
    "What is the market value as at 6 April, or the total cost value at the date the scheme first obtained the Tangible Moveable Property?"
  val isTotalCostOrMarketValue = "Is this the market value or cost value?"
  val areAnyDisposals = "During the year was any disposal of the tangible moveable property made?"
  val disposalsAmount =
    "If yes, there was disposal of tangible moveable property - what is the total amount of consideration received from the sale or disposal of the asset?"
  val namesOfPurchasers =
    "If disposals were made on this, what was the name of the purchasers of the Tangible Moveable property?"
  val areAnyPurchasersConnected = "Are any of the purchasers connected parties?"
  val wasTxSupportedIndValuation = "Was the transaction supported by an independent valuation?"
  val isAnyPartStillHeld = "Is any part of the asset still held?"

  val headers: NonEmptyList[String] = NonEmptyList.of(
    firstName,
    lastName,
    memberDateOfBirth,
    memberNino,
    memberReasonNoNino,
    descriptionOfAsset,
    dateOfAcquisition,
    totalCostOfAsset,
    acquiredFrom,
    isIndependentValuation,
    totalIncomeInTaxYear,
    totalCostOrMarketValue,
    isTotalCostOrMarketValue,
    areAnyDisposals,
    disposalsAmount,
    namesOfPurchasers,
    areAnyPurchasersConnected,
    wasTxSupportedIndValuation,
    isAnyPartStillHeld
  )

  val helpers: NonEmptyList[String] =
    NonEmptyList.of(
      "Question help information. The information in this row will give you hints or tips to help you to complete the required questions in the cells above. What you need to do: Enter your data from row 3 onwards. Do not remove any columns. Complete the questions per member marked horizontally across the columns. For all mandatory questions that don’t apply, please leave blank. For members that have multiple property assets, complete one row per asset and repeat the members first name, last name and date of birth for the required number of rows.",
      "Enter the first name of the scheme member. Hyphens are accepted. Should be letters A to Z. Names have a maximum of 35 characters. Mandatory question.",
      "Enter the last name of the scheme member. Hyphens are accepted. Should be letters A to Z. Names have a maximum of 35 characters. Mandatory question.",
      "Use the format DD/MM/YYYY. Mandatory question.",
      "Enter the individual's National Insurance Number. If you do not know this, add the reason why you do not have this in the next column. 9 characters in alphanumeric in the format for example: AA999999A. Mandatory question.",
      "Enter reason for not having the member's National Insurance number. Maximum of 160 characters.",
      "Min 1 character - Max 160 characters. Mandatory question",
      "Use format DD/MM/YYYY. Mandatory question if value entered for number of transactions made. Enter the date the Tangible Moveable Property was first obtained by the scheme",
      "Enter the total amount in GBP (in pounds and pence). Include all costs related to the transaction. If the Tangible Moveable Property was not an acquisition, provide the total market value. If the Tangible Moveable Property was acquired more than 6 years ago provide the market value at the year of the return. Mandatory question.",
      "Enter name[s]. If multiple names, separate each name with a plus sign (+), for example John Smith + Jane Doe + Joe Bloggs. Do not use commas. Should be letters A to Z. Hyphens are accepted. Maximum 160 characters. Mandatory question",
      "Enter YES or NO. Mandatory question",
      "Enter amount to two decimal places. Enter the total amount in GBP (pounds and pence). Enter the total amount received in respect of the relevant member for this tangible movable property. If multiple tangible movable properties for this member - enter each tangible movable property on a separate row.",
      "Enter amount in GBP (pounds and pence). Enter the market value at 6 April [of the tax year that you are submitting]. If you do not have this, enter the total cost value [as at the date the tangible moveable property was first obtained by the scheme]. Mandatory question",
      "Enter “Market Value” or “Cost Value”. Mandatory question.",
      "Enter YES or NO. Mandatory question",
      "Enter the total amount in GBP (pounds and pence). Mandatory question if there was disposal of tangible moveable property",
      "Enter names of purchaser[s]. If multiple names, separate each name with a plus sign (+), for example John Smith + Jane Doe + Joe Bloggs. Do not use commas. Should be letters A to Z. Hyphens are accepted. Maximum 160 characters. Mandatory question if there was disposal of tangible moveable property",
      "Enter YES or NO. Mandatory question if there was disposal of tangible moveable property",
      "Enter YES or NO. Mandatory question if there was disposal of tangible moveable property",
      "Enter YES or NO. Mandatory question if there was disposal of tangible moveable property",
      "ERRORS WITH DETAIL"
    )

}
