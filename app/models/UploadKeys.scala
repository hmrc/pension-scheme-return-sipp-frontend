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

package models

object UploadKeys {
  val firstName = "First name of scheme member"
  val lastName = "Last name of scheme member"
  val dateOfBirth = "Member date of birth"
  val isUKAddress = "Is the members address in the UK?"
  val ukAddressLine1 = "Enter the members UK address line 1"
  val ukAddressLine2 = "Enter members UK address line 2"
  val ukAddressLine3 = "Enter members UK address line 3"
  val ukTownOrCity = "Enter name of members UK town or city"
  val ukPostcode = "Enter members post code"
  val addressLine1 = "Enter the members non-UK address line 1"
  val addressLine2 = "Enter members non-UK address line 2"
  val addressLine3 = "Enter members non-UK address line 3"
  val addressLine4 = "Enter members non-UK address line 4"
  val country = "Enter members non-UK country"
  val nino = "Member National Insurance number"
  val reasonForNoNino = "If no National Insurance number for member, give reason"

  //Interest Land Or Property Keys
  val firstNameOfSchemeMember = "First name of scheme member"
  val lastNameOfSchemeMember = "Last name of scheme member"
  val memberDateOfBirth = "Member date of birth"
  val countOfInterestLandOrPropertyTransactions =
    "How many land or property transactions did the member make during the tax year and not reported in a previous return for this member?"
  val countOfArmsLengthLandOrPropertyTransactions =
    "How many land or property transactions were made during the tax year and not reported in a previous return for this member?"

  val acquisitionDate = "What is the date of acquisition?"
  val isLandOrPropertyInUK = "Is the land or property in the UK?"
  val landOrPropertyUkAddressLine1 = "Enter the UK address line 1 of the land or property"
  val landOrPropertyUkAddressLine2 = "Enter UK address line 2 of the land or property"
  val landOrPropertyUkAddressLine3 = "Enter UK address line 3 of the land or property"
  val landOrPropertyUkTownOrCity = "Enter name UK town or city of the land or property"
  val landOrPropertyUkPostCode = "Enter post code of the land or property"
  val landOrPropertyAddressLine1 = "Enter the non-UK address line 1 of the land or property"
  val landOrPropertyAddressLine2 = "Enter non-UK address line 2 of the land or property"
  val landOrPropertyAddressLine3 = "Enter non-UK address line 3 of the land or property"
  val landOrPropertyAddressLine4 = "Enter non-UK address line 4 of the land or property"
  val landOrPropertyCountry = "Enter non-UK country name of the land or property"
  val isThereLandRegistryReference = "Is there a land Registry reference in respect of the land or property?"
  val noLandRegistryReference = "If no land Registry reference, enter reason"
  val acquiredFromName = "Who was the land or property acquired from?"
  val acquiredTypeInterestLand =
    "Was the land or property acquired from an individual, a company, a partnership, or another source?"
  val acquiredTypeArmsLength =
    "Was the arms length land or property acquired from an individual, a company, or a partnership, or other source?"
  val acquirerNinoForIndividualInterest =
    "If the land or property was acquired from an individual, enter their National Insurance Number"
  val acquirerNinoForIndividualArmsLength =
    "If the arms length land or property acquired from an individual, enter the individuals National Insurance Number (NINO)"
  val acquirerCrnForCompanyInterest =
    "If the land or property acquired from a company, enter the Company Registration Number (CRN)"
  val acquirerCrnForCompanyArmsLength =
    "If the arms length land or property was acquired from a company, enter the Company Registration Number (CRN)"
  val acquirerUtrForPartnershipInterest = "If the land or property acquired from a partnership, enter the UTR"
  val noIdOrAcquiredFromAnotherSource = "Add the reason you do not have the individuals National Insurance number, the CRN or the UTR, or if the land or property was acquired from another source, enter the details"
  val acquirerUtrForPartnershipArmsLength =
    "If the arms length land or property was acquired from a partnership, enter the Unique Taxpayer Reference (UTR) details"
  val noIdOrAcquiredFromAnotherSourceInterest =
    "Add the reason you do not have the individuals National Insurance number, the CRN or the UTR, or if the land or property was acquired from another source, enter the details"
  val noIdOrAcquiredFromAnotherSourceArmsLength =
    "Add the reason you do not have the individuals National Insurance number, the CRN or the UTR, or if the land or property was acquired from another source, enter the details"

  val totalCostOfLandOrPropertyAcquired = "What is the total cost of the land or property acquired?"
  val isSupportedByAnIndependentValuation = "Is the transaction supported by an Independent Valuation?"

  val isPropertyHeldJointly = "Is the property held jointly?"
  val howManyPersonsJointlyOwnProperty = "If the property is held jointly, how many persons jointly own it?"
  val firstPersonNameJointlyOwning = "Enter the name of the first joint owner"
  val firstPersonNinoJointlyOwning = "National Insurance Number of person or entity jointly owning the property"
  val firstPersonNoNinoJointlyOwning = "If no National Insurance number for the first joint owner, enter the reason"
  val secondPersonNameJointlyOwning = "Enter the name of the second joint owner"
  val secondPersonNinoJointlyOwning = "National Insurance Number of second person or entity jointly owning the property"
  val secondPersonNoNinoJointlyOwning = "If no National Insurance number for the second joint owner, enter the reason"
  val thirdPersonNameJointlyOwning = "Enter the name of the third joint owner"
  val thirdPersonNinoJointlyOwning = "National Insurance Number of third person or entity jointly owning the property"
  val thirdPersonNoNinoJointlyOwning = "If no National Insurance number for the third joint owner, enter the reason"
  val fourthPersonNameJointlyOwning = "Enter the name of the fourth joint owner"
  val fourthPersonNinoJointlyOwning = "National Insurance Number of fourth person or entity jointly owning the property"
  val fourthPersonNoNinoJointlyOwning = "If no National Insurance number for the fourth joint owner, enter the reason"
  val fifthPersonNameJointlyOwning = "Enter the name of the fifth joint owner"
  val fifthPersonNinoJointlyOwning = "National Insurance Number of fifth person or entity jointly owning the property"
  val fifthPersonNoNinoJointlyOwning = "If no National Insurance number for the fifth joint owner, enter the reason"

  val isPropertyDefinedAsSchedule29a =
    "Is any part of the land or property residential property as defined by schedule 29a Finance Act 2004?"

  val isLeased = "Is the land or property leased?"
  val firstLesseeName = "If the land or property is leased, enter the name of the first lessee"
  val firstLesseeConnectedOrUnconnected = "Is the first lessee a connected or unconnected party?"
  val firstLesseeGrantedDate =
    "If the land or property linked to the first lessee is leased, what was the date that the lease was granted?"
  val firstLesseeAnnualAmount =
    "If the land or property linked to the first lessee is leased, what is the annual lease amount for the first lessee?"
  val secondLesseeName = "If the land or property leased, enter the name of the second lessee"
  val secondLesseeConnectedOrUnconnected = "Is the second lessee a connected or unconnected party?"
  val secondLesseeGrantedDate =
    "If the land or property linked to the second lessee is leased, what was the date that the lease was granted?"
  val secondLesseeAnnualAmount =
    "If the land or property linked to the second lessee is leased, what is the annual lease amount?"
  val thirdLesseeName = "If the land or property is leased, enter the name of the third lessee"
  val thirdLesseeConnectedOrUnconnected = "Is the third lessee a connected or unconnected party?"
  val thirdLesseeGrantedDate =
    "If the land or property linked to the third lessee is leased, what was the date that the lease was granted?"
  val thirdLesseeAnnualAmount =
    "If the land or property linked to the third lessee is leased, what is the annual lease amount?"
  val fourthLesseeName = "If the land or property is leased, enter the name of the fourth lessee"
  val fourthLesseeConnectedOrUnconnected = "Is the fourth lessee a connected or unconnected party?"
  val fourthLesseeGrantedDate =
    "If the land or property linked to the fourth lessee is leased, what was the date that the lease was granted?"
  val fourthLesseeAnnualAmount =
    "If the land or property linked to the fourth lessee is leased, what is the annual lease amount?"
  val fifthLesseeName = "If the land or property is leased, enter the name of the fifth lessee"
  val fifthLesseeConnectedOrUnconnected = "Is the fifth lessee a connected or unconnected party?"
  val fifthLesseeGrantedDate =
    "If the land or property linked to the fifth lessee is leased, what was the date that the lease was granted?"
  val fifthLesseeAnnualAmount =
    "If the land or property linked to the fifth lessee is leased, what is the annual lease amount?"
  val sixthLesseeName = "If the land or property is leased, enter the name of the sixth lessee"
  val sixthLesseeConnectedOrUnconnected = "Is the sixth lessee a connected or unconnected party?"
  val sixthLesseeGrantedDate =
    "If the land or property linked to the sixth lessee is leased, what was the date that the lease was granted?"
  val sixthLesseeAnnualAmount =
    "If the land or property linked to the sixth lessee is leased, what is the annual lease amount?"
  val seventhLesseeName = "If the land or property is leased, enter the name of the seventh lessee"
  val seventhLesseeConnectedOrUnconnected = "Is the seventh lessee a connected or unconnected party?"
  val seventhLesseeGrantedDate =
    "If the land or property linked to the seventh lessee is leased, what was the date that the lease was granted?"
  val seventhLesseeAnnualAmount =
    "If the land or property linked to the seventh lessee is leased, what is the annual lease amount?"
  val eighthLesseeName = "If the land or property is leased, enter the name of the eighth lessee"
  val eighthLesseeConnectedOrUnconnected = "Is the eighth lessee a connected or unconnected party?"
  val eighthLesseeGrantedDate =
    "If the land or property linked to the eighth lessee is leased, what was the date that the lease was granted?"
  val eighthLesseeAnnualAmount =
    "If the land or property linked to the eighth lessee is leased, what is the annual lease amount?"
  val ninthLesseeName = "If the land or property is leased, enter the name of the ninth lessee"
  val ninthLesseeConnectedOrUnconnected = "Is the ninth lessee a connected or unconnected party?"
  val ninthLesseeGrantedDate =
    "If the land or property linked to the ninth lessee is leased, what was the date that the lease was granted?"
  val ninthLesseeAnnualAmount =
    "If the land or property linked to the ninth lessee is leased, what is the annual lease amount?"
  val tenthLesseeName = "If the land or property is leased, enter the name of the tenth lessee"
  val tenthLesseeConnectedOrUnconnected = "Is the tenth lessee a connected or unconnected party?"
  val tenthLesseeGrantedDate =
    "If the land or property linked to the tenth lessee is leased, what was the date that the lease was granted?"
  val tenthLesseeAnnualAmount =
    "If the land or property linked to the tenth lessee is leased, what is the annual lease amount?"

  val totalAmountOfIncomeAndReceipts =
    "What is the total amount of income and receipts in respect of the land or property during tax year"

  val wereAnyDisposalOnThisDuringTheYear = "Were any disposals made on this?"
  val totalSaleProceedIfAnyDisposal =
    "What was the total sale proceed of any land sold, or interest in land sold, or premiums received, on the disposal of a leasehold interest in land"
  val firstPurchaserName = "If disposals were made on this, what is the name of the purchaser?"
  val firstPurchaserConnectedOrUnconnected = "Is the purchaser a connected or unconnected party?"
  val secondPurchaserName = "If there are other purchasers, enter the name of the second purchaser"
  val secondPurchaserConnectedOrUnconnected = "Is this second purchaser a connected or unconnected party?"
  val thirdPurchaserName = "If there are other purchasers, enter the name of the third purchaser"
  val thirdPurchaserConnectedOrUnconnected = "Is the third purchaser a connected or unconnected party?"
  val fourthPurchaserName = "If there are other purchasers, enter the name of the fourth purchaser"
  val fourthPurchaserConnectedOrUnconnected = "Is the fourth purchaser a connected or unconnected party?"
  val fifthPurchaserName = "If there are other purchasers, enter the name of the fifth purchaser"
  val fifthPurchaserConnectedOrUnconnected = "Is the fifth purchaser a connected or unconnected party?"
  val sixthPurchaserName = "If there are other purchasers, enter the name of the sixth purchaser"
  val sixthPurchaserConnectedOrUnconnected = "Is the sixth purchaser a connected or unconnected party?"
  val seventhPurchaserName = "If there are other purchasers, enter the name of the seventh purchaser"
  val seventhPurchaserConnectedOrUnconnected = "Is the seventh purchaser a connected or unconnected party?"
  val eighthPurchaserName = "If there are other purchasers, enter the name of the eighth purchaser"
  val eighthPurchaserConnectedOrUnconnected = "Is the eighth purchaser a connected or unconnected party?"
  val ninthPurchaserName = "If there are other purchasers, enter the name of the ninth purchaser"
  val ninthPurchaserConnectedOrUnconnected = "Is the ninth purchaser a connected or unconnected party?"
  val tenthPurchaserName = "If there are other purchasers, enter the name of the tenth purchaser"
  val tenthPurchaserConnectedOrUnconnected = "Is the tenth purchaser a connected or unconnected party?"
  val isTransactionSupportedByIndependentValuation = "Is the transaction supported by an independent valuation"
  val hasLandOrPropertyFullyDisposedOf = "Has the land or property been fully disposed of?"
}
