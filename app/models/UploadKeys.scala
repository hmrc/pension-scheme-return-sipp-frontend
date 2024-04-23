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
  val memberNationalInsuranceNumber = "Member National Insurance number"
  val memberReasonNoNino = "If no National Insurance number for member, give reason"

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

  val totalCostOfLandOrPropertyAcquired = "What is the total cost of the land or property acquired?"
  val isSupportedByAnIndependentValuation = "Is the transaction supported by an Independent Valuation?"

  val isPropertyHeldJointly = "Is the property held jointly?"
  val howManyPersonsJointlyOwnProperty = "How many persons jointly own the property?"

  val isPropertyDefinedAsSchedule29a =
    "Is any part of the land or property residential property as defined by schedule 29a Finance Act 2004?"

  val isLeased = "Is the land or property leased?"
  val lesseeCount = "How many lessees are there for the land or property?"
  val lesseeNames = "What are the names of the lessees?"
  val areAnyLesseesConnected = "Are any of the lessees a connected party?"
  val annualLeaseDate = "What date was the lease granted?"
  val annualLeaseAmount = "What is the annual lease amount?"

  val totalAmountOfIncomeAndReceipts =
    "What is the total amount of income and receipts in respect of the land or property during tax year"

  val wereAnyDisposalOnThisDuringTheYearInterest = "Were any disposals made on this?"
  val wasAnyDisposalOnThisDuringTheYearArmsLength = "Was any disposal of the land or property made during the year?"
  val totalSaleProceedIfAnyDisposal =
    "What was the total sale proceed of any land sold, or interest in land sold, or premiums received, on the disposal of a leasehold interest in land"
  val namesOfPurchasers = "If disposals were made on this, what are the names of the purchasers?"
  val areAnyPurchaserConnected = "Are any of the purchasers connected parties?"
  val isTransactionSupportedByIndependentValuation = "Is the transaction supported by an independent valuation"
  val hasLandOrPropertyFullyDisposedOf = "Has the land or property been fully disposed of?"

  //Tangible Moveable Property Keys
  val firstNameOfSchemeMemberTangible = "First name of scheme member"
  val lastNameOfSchemeMemberTangible = "Last name of scheme member"
  val memberDateOfBirthTangible = "Member date of birth"
  val countOfTangiblePropertyTransactions =
    "How many transactions of tangible moveable property were made during the tax year and not reported in a previous return for this member?"

  val descriptionOfAssetTangible = "Description of asset"
  val dateOfAcquisitionTangible = "What was the date of acquisiiton of the asset?"
  val totalCostOfAssetTangible = "What was the total cost of the asset acquired?"
  val acquiredFromTangible = "Who was the asset acquired from?"
  val acquiredFromTypeTangible = "Was the asset acquired from an individual or company or partnership or other?"
  val acquiredFromIndividualTangible =
    "If the asset was acquired from an individual enter the individuals National Insurance Number"
  val acquiredFromCompanyTangible = "If the asset was acquired from a company enter the CRN details"
  val acquiredFromPartnershipTangible = "If the asset was acquired from a partnership enter the UTR details"
  val acquiredFromReasonTangible =
    "Add the reason you do not have the individuals National Insurance number, the CRN or the UTR. Or if the land or property was acquired from another source, enter the details"
  val isIndependentEvaluationTangible = "Is this transaction supported by an Independent Valuation?"
  val totalIncomeInTaxYearTangible =
    "What is the total amount of income and receipts received in respect of the asset during tax year?"
  val isTotalCostOrMarketValueTangible = "Is the total cost value or market value of the asset?"
  val totalCostOrMarketValueTangible =
    "What is the total cost value or market value of the asset, as at 6 April [of the tax year that you are submitting]"
  val areAnyDisposalsTangible = "During the year was there any disposal of the tangible moveable property made?"
  val disposalsAmountTangible =
    "If yes, there was disposal of tangible moveable property - what is the total amount of consideration received from the sale or disposal of the asset?"
  val areJustDisposalsTangible = "Were any disposals made?"

  val disposalNameOfPurchaser1Tangible = "If disposals were made on this, what is the name of the purchaser?"
  val disposalTypeOfPurchaser1Tangible = "Is this purchaser a connected or unconnected party?"
  val disposalNameOfPurchaser2Tangible = "If there are other purchasers enter the name of the second purchaser"
  val disposalTypeOfPurchaser2Tangible = "Is this second purchaser a connected or unconnected party?"
  val disposalNameOfPurchaser3Tangible = "If there are other purchasers enter the name of the third purchaser"
  val disposalTypeOfPurchaser3Tangible = "Is the third purchaser a connected or unconnected party?"
  val disposalNameOfPurchaser4Tangible = "If there are other purchasers enter the name of the fourth purchaser"
  val disposalTypeOfPurchaser4Tangible = "Is the fourth purchaser a connected or unconnected party?"
  val disposalNameOfPurchaser5Tangible = "If there are other purchasers enter the name of the fifth purchaser"
  val disposalTypeOfPurchaser5Tangible = "Is the fifth purchaser a connected or unconnected party?"
  val disposalNameOfPurchaser6Tangible = "If there are other purchasers, enter the name of the sixth purchaser"
  val disposalTypeOfPurchaser6Tangible = "Is the sixth purchaser a connected or unconnected party?"
  val disposalNameOfPurchaser7Tangible = "If there are other purchasers, enter the name of the seventh purchaser"
  val disposalTypeOfPurchaser7Tangible = "Is the seventh purchaser a connected or unconnected party?"
  val disposalNameOfPurchaser8Tangible = "If there are other purchasers, enter the name of the eighth purchaser"
  val disposalTypeOfPurchaser8Tangible = "Is the eighth purchaser a connected or unconnected party?"
  val disposalNameOfPurchaser9Tangible = "If there are other purchasers, enter the name of the ninth purchaser"
  val disposalTypeOfPurchaser9Tangible = "Is the ninth purchaser a connected or unconnected party?"
  val disposalNameOfPurchaser10Tangible = "If there are other purchasers, enter the name of the tenth purchaser"
  val disposalTypeOfPurchaser10Tangible = "Is the tenth purchaser a connected or unconnected party?"

  val wasTransactionSupportedIndValuationTangible = "Was the transaction supported by an independent valuation?"
  val isAnyPartStillHeldTangible = "Is any part of the asset still held?"

  //Outstanding Loans Property Keys
  val firstNameOfSchemeMemberOutstanding = "First name of scheme member" // B
  val lastNameOfSchemeMemberOutstanding = "Last name of scheme member" // C
  val memberDateOfBirthOutstanding = "Member date of birth" // D
  val countOfOutstandingLoansPropertyTransactions =
    "How many separate loans were made or outstanding during the tax year and not reported in a previous return for this member?" // E
  val recipientNameOfOutstanding = "What is the name of the recipient of loan?" // F
  val acquiredFromTypeOutstanding = "Is the recipient of the loan an individual or company or partnership or other" // G
  val acquiredFromIndividualOutstanding =
    "If the recipient of the loan was an individual, enter the individuals National Insurance Number" // H
  val acquiredFromCompanyOutstanding = "If the recipient of the loan was a company, enter the CRN details" // I
  val acquiredFromPartnershipOutstanding = "If the recipient of the loan was a partnership, enter the UTR details" // J
  val acquiredFromReasonOutstanding =
    "Add the reason you do not have the individuals National Insurance number, the CRN or the UTR. Or if the loan was acquired from another source, enter the details" // K
  val dateOfOutstandingLoan = "What was the date of loan?" // L
  val amountOfOutstandingLoan = "What is the amount of the loan?" // M
  val isOutstandingLoanAssociatedWithConnectedParty = "Is the loan associated with a connected party?" // N
  val repaymentDateOfOutstandingLoan = "What is the repayment date of the loan?" // O
  val interestRateOfOutstandingLoan = "What is the Interest Rate for the loan?" // P
  val hasSecurityForOutstandingLoan = "Has security been given for the loan?" // Q
  val capitalPaymentOfOutstandingLoanForYear =
    "In respect of this loan, what Capital Repayments have been received by the scheme during the year?" // R
  val interestRateOfOutstandingLoanForYear =
    "In respect of this loan, what interest payments have been received by the scheme during the year?" // S
  val anyArrearsForOutstandingLoan =
    "In respect of this loan, are there any arrears outstanding from previous years" // T
  val outstandingAmountForOutstandingLoan =
    "In respect of this loan, what is the amount outstanding at the year end?" // U

}
