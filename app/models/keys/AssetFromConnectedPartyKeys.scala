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

object AssetFromConnectedPartyKeys extends CommonKeys {

  val dateOfAcquisition = "What is the date the scheme acquired the asset?" // H
  val descriptionOfAsset = "Description of asset" // I
  val isAcquisitionOfShares = "Was this an acquisition of shares?" // J
  val companyNameShares = "If yes, what is the name of the company to which the shares relate?" // K
  val companyCRNShares = "What is the CRN of the company to which the shares relate?" // L
  val companyNoCRNReasonShares = "If no CRN of the company to which the shares relate, enter reason" // M
  val companyClassShares = "What are the class of shares?" // N
  val companyNumberOfShares =
    "What are the total number of shares acquired or received/held in respect of this transaction?" // O
  val acquiredFrom = "Who was the asset acquired from?" // P
  val totalCostOfAsset = "What is the total cost of the asset at the date the scheme acquired it?" // Q
  val isIndependentValuation = "Is this transaction supported by an Independent Valuation?" // R
  val isFinanceAct =
    "Is any part of the asset Tangible Moveable Property as defined by Schedule 29a Finance Act 2004?" // S
  val totalIncomeInTaxYear =
    "What is the total amount of income and receipts received in respect of the asset during tax year?" // T
  val areAnyDisposalsYear = "During the year was any disposal of the asset made?" // U
  val disposalsAmount =
    "If disposals were made what is the total amount of consideration received from the sale or disposal of the asset?" // V
  val namesOfPurchasers = "If disposals were made on this, what was the name of the purchasers of the asset?" // W
  val areConnectedPartiesPurchasers = "Are any of the purchasers connected parties?" // X
  val wasTransactionSupportedIndValuation = "Was the transaction supported by an independent valuation?" // Y
  val hasFullyDisposedOf = "Has the asset been fully disposed of?" // Z
  val wasDisposalOfShares = "Was there disposal of shares?" // AA
  val disposalOfSharesNumberHeld =
    "If there were disposals of shares what is the total number of shares now held?" // AB

  val headers: NonEmptyList[String] = NonEmptyList.of(
    firstName,
    lastName,
    memberDateOfBirth,
    memberNino,
    memberReasonNoNino,
    dateOfAcquisition,
    descriptionOfAsset,
    isAcquisitionOfShares,
    companyNameShares,
    companyCRNShares,
    companyNoCRNReasonShares,
    companyClassShares,
    companyNumberOfShares,
    acquiredFrom,
    totalCostOfAsset,
    isIndependentValuation,
    isFinanceAct,
    totalIncomeInTaxYear,
    areAnyDisposalsYear,
    disposalsAmount,
    namesOfPurchasers,
    areConnectedPartiesPurchasers,
    wasTransactionSupportedIndValuation,
    hasFullyDisposedOf,
    wasDisposalOfShares,
    disposalOfSharesNumberHeld
  )

  val helpers: NonEmptyList[String] = NonEmptyList.of(
    "Question help information. The information in this row will give you hints or tips to help you to complete the required questions in the cells above. What you need to do Enter your data from row 3 onwards. Do not remove any columns. Complete the questions per member marked horizontally across the columns. For all mandatory questions that don’t apply, please leave blank. For members that have multiple assets, complete one row per asset and repeat the members first name, last name and date of birth for the required number of rows.",
    "Enter the first name of the scheme member. Hyphens are accepted. Should be letters A to Z. Names have a maximum of 35 characters. Mandatory question.",
    "Enter the last name of the scheme member. Hyphens are accepted. Should be letters A to Z. Names have a maximum of 35 characters. Mandatory question.",
    "Use the format DD/MM/YYYY. Mandatory question.",
    "Enter the individual's National Insurance Number. If you do not know this, add the reason why you do not have this in the next column. 9 characters in alphanumeric in the format for example: AA999999A. Mandatory question.",
    "Enter reason for not having the member's National Insurance number. Maximum of 160 characters.",
    "Use format DD/MM/YYYY Mandatory question Enter the date that the asset was first obtained by the scheme",
    "Maximum of 160 characters. Mandatory question",
    "Enter YES or NO. Mandatory question",
    "Max of 160 characters Mandatory question if this was an acquisition of shares.",
    "Enter the Company Registration Number (CRN). If you do not know this, add the reason why you do not have this in the next column.",
    "If you do not know the CRN of company to which the shares relate, add the reason why you do not have this. Maximum of 160 characters. Mandatory question if no CRN is given.",
    "Max of 160 characters. Mandatory question if this was an acquisition of shares.",
    "Enter number. Mandatory question if this was an acquisition of shares.",
    "Enter name[s] If multiple names, separate each name with a plus sign (+), for example John Smith + Jane Doe + Joe Bloggs Do not use commas Should be letters A to Z. Hyphens are accepted. Maximum 160 characters Mandatory question",
    "Enter the total amount in GBP (in pounds and pence). Enter amount as whole number and to two decimal places. If the asset was not an acquisition, provide the total market value. If the asset was acquired more than 6 years ago provide the market value at the year of the return Mandatory question",
    "Enter YES or NO. Oandatory question",
    "Enter YES or NO. Oandatory question",
    "Enter the total amount in GBP (pounds and pence). Enter the total amount received in respect of the relevant member for this asset If multiple assets for this member - enter each asset on a separate row",
    "Enter YES or NO. Oandatory question",
    "Enter the total amount in GBP (pounds and pence) Mandatory question if disposal of the asset was made.",
    "Enter name of purchaser[s] If multiple names, separate each name with a plus sign (+), for example John Smith + Jane Doe + Joe Bloggs Do not use commas Should be letters A to Z. Hyphens are accepted. Maximum 160 characters Mandatory question if disposal of the asset was made.",
    "Enter YES or NO. Mandatory question if disposal of the asset was made.",
    "Enter YES or NO. Mandatory question if disposal of the asset was made.",
    "Enter YES or NO.",
    "Enter YES or NO. Mandatory question if disposal of the asset was made.",
    "0-9,999,999 Min 1 - Max 7 Characters of 0-9 If the asset has been fully disposed of and there was disposal of shares, then the number of shares now held must equal zero. Mandatory question if disposal of the shares was made.",
    "ERRORS WITH DETAIL"
  )
}
