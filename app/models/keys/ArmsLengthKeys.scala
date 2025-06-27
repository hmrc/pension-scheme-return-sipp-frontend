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

object ArmsLengthKeys extends CommonKeys {
  val acquisitionDate = "What is the date the scheme acquired the land or property?"
  val isLandOrPropertyInUK = "Is the land or property in the UK?"
  val landOrPropertyUkAddressLine1 = "Enter the UK address line 1 of the land or property"
  val landOrPropertyUkAddressLine2 = "Enter UK address line 2 of the land or property"
  val landOrPropertyUkAddressLine3 = "Enter UK address line 3 of the land or property"
  val landOrPropertyUkTownOrCity = "Enter name UK town or city of the land or property"
  val landOrPropertyUkPostCode = "Enter post code of the land or property"
  val landOrPropertyAddressLine1 = "Enter the non-UK address line 1 of the land or property"
  val landOrPropertyAddressLine2 = "Enter non-UK address line 2 of the land or property"
  val landOrPropertyAddressLine3 = "Enter non-UK address line 3 of the land or property"
  val landOrPropertyAddressLine4 = "Enter non-UK town or city of the land or property"
  val landOrPropertyCountry = "Enter non-UK country name of the land or property"
  val isThereLandRegistryReference = "Is there a Title Number in respect of the land or property?"
  val landRegistryRefOrReason = "If 'yes' enter Title number. If 'no' enter a reason for not having a Title Number."
  val acquiredFromName = "Who was the land or property acquired from?"
  val totalCostOfLandOrPropertyAcquired =
    "What is the total cost of the land or property at the date the scheme acquired it?"
  val isSupportedByAnIndependentValuation = "Is the transaction supported by an Independent Valuation?"
  val isPropertyHeldJointly = "Is the property held jointly?"
  val howManyPersonsJointlyOwn = "What percentage of the property does the member hold?"
  val isPropertyDefinedAsSchedule29a =
    "Is any part of the land or property residential property as defined by schedule 29a Finance Act 2004?"
  val isLeased = "Is the land or property leased?"
  val lesseeCount = "How many lessees are there for the land or property?"
  val areAnyLesseesConnected = "Are any of the lessees a connected party?"
  val annualLeaseDate = "What date was the lease granted?"
  val annualLeaseAmount = "What is the total annual lease amount due?"
  val totalAmountOfIncomeAndReceipts =
    "What is the total amount of income and receipts in respect of the land or property during tax year?"
  val wasAnyDisposalOnThisDuringTheYear = "Was any disposal of the land or property made during the year?"
  val totalSaleProceedIfAnyDisposal =
    "What was the total sale proceed of any land sold, or interest in land sold, or premiums received, on the disposal of a leasehold interest in land?"
  val namesOfPurchasers = "If disposals were made on this, what are the names of the purchasers?"
  val areAnyPurchaserConnected = "Are any of the purchasers connected parties?"
  val isTrxSupportedByIndependentValuation = "Is the transaction supported by an independent valuation?"
  val isFullyDisposed = "Has the land or property been fully disposed of?"

  val headers: NonEmptyList[String] = NonEmptyList.of(
    firstName,
    lastName,
    memberDateOfBirth,
    memberNino,
    memberReasonNoNino,
    acquisitionDate,
    isLandOrPropertyInUK,
    landOrPropertyUkAddressLine1,
    landOrPropertyUkAddressLine2,
    landOrPropertyUkAddressLine3,
    landOrPropertyUkTownOrCity,
    landOrPropertyUkPostCode,
    landOrPropertyAddressLine1,
    landOrPropertyAddressLine2,
    landOrPropertyAddressLine3,
    landOrPropertyAddressLine4,
    landOrPropertyCountry,
    isThereLandRegistryReference,
    landRegistryRefOrReason,
    acquiredFromName,
    totalCostOfLandOrPropertyAcquired,
    isSupportedByAnIndependentValuation,
    isPropertyHeldJointly,
    howManyPersonsJointlyOwn,
    isPropertyDefinedAsSchedule29a,
    isLeased,
    lesseeCount,
    areAnyLesseesConnected,
    annualLeaseDate,
    annualLeaseAmount,
    totalAmountOfIncomeAndReceipts,
    wasAnyDisposalOnThisDuringTheYear,
    totalSaleProceedIfAnyDisposal,
    namesOfPurchasers,
    areAnyPurchaserConnected,
    isTrxSupportedByIndependentValuation,
    isFullyDisposed
  )

  val helpers: NonEmptyList[String] = NonEmptyList.of(
    "Question help information. The information in this row will give you hints or tips to help you to complete the required questions in the cells above. What you need to do Enter your data from row 3 onwards. Do not remove any columns. Complete the questions per member marked horizontally across the columns. For all mandatory questions that don’t apply, please leave blank. For members that have multiple property transactions, complete one row per property and repeat the members first name, last name and date of birth for the required number of rows.",
    "Enter the first name of the scheme member. Hyphens are accepted. Should be letters A to Z. Names have a maximum of 35 characters. Mandatory question.",
    "Enter the last name of the scheme member. Hyphens are accepted. Should be letters A to Z. Names have a maximum of 35 characters. Mandatory question.",
    "Use the format DD/MM/YYYY. Mandatory question.",
    "Enter the individual's National Insurance Number. If you do not know this, add the reason why you do not have this in the next column. 9 characters in alphanumeric in the format for example: AA999999A. Mandatory question.",
    "Enter reason for not having the member's National Insurance number. Maximum of 160 characters.",
    "Use the format DD/MM/YYYY This is a mandatory question. Enter the date that the land or property was first obtained by the scheme",
    "Enter YES or NO. Mandatory question",
    "Enter UK Address Line 1. Should be letters A to Z, numbers 0 to 9. Hyphens and speech marks are accepted. Maximum number of characters is 35. Mandatory question for UK address.",
    "Enter UK address Line 2. Should be letters A to Z, numbers 0 to 9. Hyphens and speech marks are accepted. Maximum number of characters is 35. Mandatory question for UK address.",
    "Enter UK address Line 3. This is an optional field. Should be letters A to Z, numbers 0 to 9. Hyphens and speech marks are accepted. Maximum number of characters is 35.",
    "Enter the UK town or city. Should be letters A to Z, numbers 0 to 9. Hyphens and speech marks are accepted. Maximum number of characters is 35. Mandatory question for UK address.",
    "Enter UK post code. Mandatory question for UK address.",
    "Enter non-UK address Line 1. Should be letters A to Z, numbers 0 to 9. Hyphens and speech marks are accepted. Maximum number of characters is 35. Mandatory question if non-UK address. ",
    "Enter non-UK address Line 2. This is an optional field. Should be letters A to Z, numbers 0 to 9. Hyphens and speech marks are accepted. Maximum number of characters is 35.",
    "Enter the non-UK address Line 3. This is an optional field. Should be letters A to Z, numbers 0 to 9. Hyphens and speech marks are accepted. Maximum number of characters is 35.",
    "Enter the non-UK town or city. This is an optional field. Should be letters A to Z, numbers 0 to 9. Hyphens and speech marks are accepted. Maximum number of characters is 35.",
    "Enter the name of the non-UK country Should be letters A to Z, numbers 0 to 9. Hyphens and speech marks are accepted. Mandatory question if non-UK address.",
    "Enter YES or NO. Mandatory question if any interest in land or property transactions have taken place within the tax year. Where there is a Title number, HMRC expect this to be provided.",
    "Max of 160 characters. Mandatory question",
    "Enter name[s] If multiple names, separate each name with a plus sign (+), for example John Smith + Jane Doe + Joe Bloggs Do not use commas Should be letters A to Z. Hyphens are accepted. Maximum 160 characters Mandatory question",
    "Enter the total amount in GBP (in pounds and pence). Include stamp duty and other costs related to the transaction. If the land or property was not an acquisition, provide the total market value. Mandatory question. If the land or property was acquired more than 6 years ago provide the market value at the year of the return",
    "Enter YES or NO Mandatory question.",
    "Enter YES or NO. Mandatory question.",
    "Enter the percentage of the property the member holds. ",
    "Enter YES or NO. Mandatory question",
    "Enter YES or NO. Mandatory question.",
    "Enter number of lessees using characters of 0-9.",
    "Enter YES or NO. ",
    "Use the format DD/MM/YYYY. If multiple lessees, enter the earliest date an existing lease was granted.",
    "Enter the total annual lease amount due in GBP (pounds and pence). If multiple lessees, enter the total amount due",
    "Enter the total amount in GBP (pounds and pence). Enter the total amount received in respect of the relevant member for this land or property If multiple properties for this member - enter each property on a separate row",
    "Enter YES or NO Mandatory field.",
    "Enter the total amount in GBP (pounds and pence). Mandatory if disposals were made.",
    "Enter names of purchaser[s] If multiple names, separate each name with a plus sign (+), for example John Smith + Jane Doe + Joe Bloggs Do not use commas Should be letters A to Z. Hyphens are accepted. Maximum 160 characters Mandatory if any disposal of the land or property was made during the year.",
    "Enter YES or NO. Mandatory if any disposal of the land or property was made during the year",
    "Enter YES or NO. Mandatory if any disposal of the land or property was made during the year",
    "Enter YES or NO Mandatory if any disposal of the land or property was made during the year",
    "ERRORS WITH DETAIL"
  )
}
