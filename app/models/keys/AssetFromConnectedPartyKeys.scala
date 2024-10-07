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
  val countOfTransactions =
    "How many asset transactions were made during the tax year and not reported in a previous return for this member?" // G
  val dateOfAcquisition = "What was the date the asset was acquired?" // H
  val descriptionOfAsset = "Description of asset" // I
  val isAcquisitionOfShares = "Was this an acquisition of shares?" // J
  val companyNameShares = "If yes, What is the name of the company to which the shares relate?" // K
  val companyCRNShares = "What is the CRN of the company that has the shares?" // L
  val companyNoCRNReasonShares = "If no CRN, enter reason you do not have this" // M
  val companyClassShares = "What are the class of shares?" // N
  val companyNumberOfShares =
    "What are the total number of shares acquired or received/held in respect of this transaction?" // O
  val acquiredFrom = "Who was the asset acquired from?" // P
  val totalCostOfAsset = "What was the total cost of the asset acquired?" // Q
  val isIndependentValuation = "Is this transaction supported by an Independent Valuation" // R
  val isFinanceAct =
    "Is any part of the asset Tangible Moveable Property as defined by Schedule 29a Finance Act 2004?" // S
  val totalIncomeInTaxYear =
    "What is the total amount of income and receipts received in respect of the asset during tax year" // T
  val areAnyDisposalsYear = "During the year was any disposal of the asset made?" // U
  val disposalsAmount =
    "If disposals were made what is the total amount of consideration received from the sale or disposal of the asset?" // V
  val namesOfPurchasers = "Names of purchasers" // W
  val areConnectedPartiesPurchasers = "Are any of the purchasers connected parties?" // X
  val wasTransactionSupportedIndValuation = "Was the transaction supported by an independent valuation?" // Y
  val hasFullyDisposedOf = "Has the asset been fully disposed of?" // Z
  val wasDisposalOfShares = "Was there disposal of shares?" // AA
  val disposalOfSharesNumberHeld =
    "If there were disposals of shares what is the total number of shares now held" // AB

  val headers: NonEmptyList[String] = NonEmptyList.of(
    firstName,
    lastName,
    memberDateOfBirth,
    memberNino,
    memberReasonNoNino,
    countOfTransactions,
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
    "Question help information. This will give you hints or tips to help you to complete the required cells.",
    "Enter the first name of the scheme member. Hyphens are accepted.Mandatory question.",
    "Enter the last name of the scheme member. Hyphens are accepted.Mandatory question.",
    "Use the format DD-MM-YYYY. Mandatory question.",
    "Enter the individuals National Insurance Number. If you do not know this, add the reason why you do not have this in the next column. 9 characters in alphanumeric in the format for example: AA999999A. Mandatory question.",
    "If no National Insurance number for member, give reason",
    "Numerical. Mandatory question.",
    "Use the format DD-MM-YYY Mandatory question.",
    "Mandatory question. Max of 160 characters.",
    "Enter YES or NO. Mandatory question.",
    "Max of 160 characters",
    "Enter the Company Registration Number (CRN). If you do not know this, add the reason why you do not have this in the next column.",
    "Add the reason why you do not have the CRN. Maximum of 160 characters.",
    "Max of 160 characters. Mandatory question.",
    "Enter number. Mandatory question.",
    "Enter name or details. Max 160 characters. Mandatory Question",
    "Enter the total amount in GBP (in pounds and pence). Enter amount as whole number and to two decimal places. Mandatory question.",
    "Enter YES or NO. Mandatory question",
    "Enter YES or NO. Mandatory question",
    "Enter the total amount in GBP (pounds and pence). Mandatory question.",
    "Enter YES or NO. Mandatory question.",
    "Enter the total amount in GBP (pounds and pence)",
    "Enter name or details. Max 160 characters. ",
    "Enter YES or NO",
    "Enter YES or NO",
    "Enter YES or NO. Mandatory question if disposal of the asset was made.",
    "Enter YES or NO. Mandatory question if disposal of the asset was made.",
    "Min 1 - Max 7.  0-9,999,999  Min 1 - Max 7 Characters of 0-9. If the asset has been fully disposed of and there was disposal of shares, then the number of shares now held must equal zero. Mandatory question if disposal of the shares was made.",
    "ERRORS WITH DETAIL"
  )
}
