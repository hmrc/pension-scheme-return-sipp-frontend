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
  val dateOfAcquisition = "What was the date of acquisition of the asset?"
  val totalCostOfAsset = "What was the total cost of the asset acquired?"
  val acquiredFrom = "Who was the asset acquired from?"
  val isIndependentValuation = "Is this transaction supported by an Independent Valuation?"
  val totalIncomeInTaxYear =
    "What is the total amount of income and receipts received in respect of the asset during tax year?"
  val isTotalCostOrMarketValue = "Is the total cost value or market value of the asset?"
  val totalCostOrMarketValue =
    "What is the total cost value or market value of the asset, as at 6 April [of the tax year that you are submitting]"
  val areAnyDisposals = "During the year was there any disposal of the tangible moveable property made?"
  val disposalsAmount =
    "If yes, there was disposal of tangible moveable property - what is the total amount of consideration received from the sale or disposal of the asset?"
  val namesOfPurchasers = "Names of purchasers"
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
    isTotalCostOrMarketValue,
    totalCostOrMarketValue,
    areAnyDisposals,
    disposalsAmount,
    namesOfPurchasers,
    areAnyPurchasersConnected,
    wasTxSupportedIndValuation,
    isAnyPartStillHeld
  )

  val helpers: NonEmptyList[String] =
    NonEmptyList.of(
      "Question help information. The information in this row will give you hints or tips to help you to complete the required questions in the cells above.",
      "Enter the first name of the scheme member. Mandatory question. Should be letters A to Z. Hyphens are accepted.",
      "Enter the last name of the scheme member. Mandatory question. Should be letters A to Z. Hyphens are accepted.",
      "Use the format DD-MM-YYYY. Mandatory question.",
      "Enter the individuals National Insurance Number. If you do not know this, add the reason why you do not have this in the next column. 9 characters in alphanumeric in the format for example: AA999999A. Mandatory question.",
      "Enter reason for not having the members National Insurance number. Maximum of 160 characters.",
      "Min 1 character - Max 160 characters. Mandatory question.",
      "Use format DD-MM-YYYY Mandatory question",
      "Enter the total amount in GBP (in pounds and pence). Include stamp duty and other costs related to the transaction. If the land or property was not an acquisition, provide the total value.",
      "Enter name. Min 1 character - Max 160 characters. Mandatory question",
      "Enter YES or NO. Mandatory question.",
      "Enter the total amount in GBP (pounds and pence). Enter amount as whole number and to two decimal places. Mandatory question.",
      "Enter if cost value or market value. As at 6 April [of the tax year that you are submitting]. Mandatory question",
      "Enter cost value or market value. Then enter amount in GBP (pounds and pence). Mandatory question",
      "Enter YES or NO. Mandatory question.",
      "Enter the total amount in GBP (pounds and pence).",
      "Enter the name of all purchasers. Should be letters A to Z. Hyphens are accepted.",
      "Enter yes or no.",
      "Enter yes or no. Mandatory question",
      "Enter yes or no. Mandatory question if disposals made.",
      "ERRORS WITH DETAIL"
    )

}
