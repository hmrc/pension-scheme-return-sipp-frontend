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

package services.validation.csv

import cats.data.NonEmptyList
import cats.data.Validated.{Invalid, Valid}
import cats.implicits.*
import models.*
import models.csv.CsvRowState
import models.csv.CsvRowState.*
import models.requests.LandOrConnectedPropertyApi
import models.requests.common.AddressDetails
import models.requests.common.YesNo
import models.requests.raw.InterestInLandOrConnectedPropertyRaw.RawTransactionDetail
import models.requests.raw.InterestInLandOrConnectedPropertyRaw.RawTransactionDetail.Ops
import play.api.i18n.Messages
import services.validation.{LandOrPropertyValidationsService, Validator}
import models.keys.{InterestInLandKeys => Keys}

import javax.inject.Inject

class InterestInLandOrPropertyCsvRowValidator @Inject() (
  validations: LandOrPropertyValidationsService
) extends CsvRowValidator[LandOrConnectedPropertyApi.TransactionDetail]
    with Validator {
  override def validate(
    line: Int,
    data: NonEmptyList[String],
    headers: List[CsvHeaderKey],
    csvRowValidationParameters: CsvRowValidationParameters
  )(implicit messages: Messages): CsvRowState[LandOrConnectedPropertyApi.TransactionDetail] = {
    val validDateThreshold = csvRowValidationParameters.schemeWindUpDate

    (for {
      raw <- readCSV(line, headers, data.toList)
      memberFullNameDob =
        s"${raw.firstNameOfSchemeMember.value} ${raw.lastNameOfSchemeMember.value} ${raw.memberDateOfBirth.value}"

      // Validations
      validatedNameDOB <- validations.validateNameDOB(
        firstName = raw.firstNameOfSchemeMember,
        lastName = raw.lastNameOfSchemeMember,
        dob = raw.memberDateOfBirth,
        row = line,
        validDateThreshold = validDateThreshold
      )

      validatedNino <- validations.validateNinoWithNoReason(
        nino = raw.memberNino,
        noNinoReason = raw.memberReasonNoNino,
        memberFullName = memberFullNameDob,
        row = line
      )

      validatedAcquisitionDate <- validations.validateDate(
        date = raw.acquisitionDate,
        key = "landOrProperty.acquisitionDate",
        row = line
      )

      validatedAddress <- validations.validateUKOrROWAddress(
        isUKAddress = raw.rawAddressDetail.isLandOrPropertyInUK,
        ukAddressLine1 = raw.rawAddressDetail.landOrPropertyUkAddressLine1,
        ukAddressLine2 = raw.rawAddressDetail.landOrPropertyUkAddressLine2,
        ukAddressLine3 = raw.rawAddressDetail.landOrPropertyUkAddressLine3,
        ukTownOrCity = raw.rawAddressDetail.landOrPropertyUkTownOrCity,
        ukPostcode = raw.rawAddressDetail.landOrPropertyUkPostCode,
        addressLine1 = raw.rawAddressDetail.landOrPropertyAddressLine1,
        addressLine2 = raw.rawAddressDetail.landOrPropertyAddressLine2,
        addressLine3 = raw.rawAddressDetail.landOrPropertyAddressLine3,
        addressLine4 = raw.rawAddressDetail.landOrPropertyAddressLine4,
        country = raw.rawAddressDetail.landOrPropertyCountry,
        memberFullName = memberFullNameDob,
        line
      )

      validatedIsThereARegistryReference <- validations.validateIsThereARegistryReference(
        raw.isThereLandRegistryReference,
        raw.landRegistryReferenceOrReason,
        memberFullNameDob,
        line
      )

      validatedAcquiredFromName <- validations.validateFreeTextNoNewLines(
        raw.acquiredFromName,
        "landOrProperty.acquireFromName",
        memberFullNameDob,
        line
      )

      validatedTotalCostOfLandOrPropertyAcquired <- validations.validatePrice(
        raw.totalCostOfLandOrPropertyAcquired,
        "landOrProperty.totalCostOfLandOrPropertyAcquired",
        memberFullNameDob,
        line
      )

      validatedIsSupportedByAnIndependentValuation <- validations.validateYesNoQuestion(
        raw.isSupportedByAnIndependentValuation,
        "landOrProperty.supportedByAnIndependent",
        memberFullNameDob,
        line
      )

      validatedJointlyHeld <- validations.validateJointlyHeld(
        raw.rawJointlyHeld.isPropertyHeldJointly,
        raw.rawJointlyHeld.whatPercentageOfPropertyOwnedByMember,
        memberFullNameDob,
        line
      )

      validatedIsPropertyDefinedAsSchedule29a <- validations.validateYesNoQuestion(
        raw.isPropertyDefinedAsSchedule29a,
        "landOrProperty.isPropertyDefinedAsSchedule29a",
        memberFullNameDob,
        line
      )

      validatedLessees <- validations.validateLease(
        isLeased = raw.rawLeased.isLeased,
        numberOfLessees = raw.rawLeased.countOfLessees,
        anyLesseeConnectedParty = raw.rawLeased.anyLesseeConnectedParty,
        leaseDate = raw.rawLeased.leaseDate,
        annualLeaseAmount = raw.rawLeased.annualLeaseAmount,
        isCountEntered = true,
        memberFullNameDob = memberFullNameDob,
        row = line
      )

      validatedTotalIncome <- validations.validatePrice(
        raw.totalAmountOfIncomeAndReceipts,
        "landOrProperty.totalAmountOfIncomeAndReceipts",
        memberFullNameDob,
        line
      )

      validatedDisposals <- validations.validateDisposals(
        raw.rawDisposal.wereAnyDisposalOnThisDuringTheYear,
        raw.rawDisposal.totalSaleProceedIfAnyDisposal,
        raw.rawDisposal.nameOfPurchasers,
        raw.rawDisposal.isAnyPurchaserConnected,
        raw.rawDisposal.isTransactionSupportedByIndependentValuation,
        raw.rawDisposal.hasLandOrPropertyFullyDisposedOf,
        memberFullNameDob,
        line
      )

    } yield (
      raw,
      (
        validatedNameDOB,
        validatedNino,
        validatedAcquisitionDate,
        validatedAddress,
        validatedIsThereARegistryReference,
        validatedAcquiredFromName,
        validatedTotalCostOfLandOrPropertyAcquired,
        validatedIsSupportedByAnIndependentValuation,
        validatedJointlyHeld,
        validatedIsPropertyDefinedAsSchedule29a,
        validatedLessees,
        validatedTotalIncome,
        validatedDisposals
      ).mapN {
        (
          nameDob,
          nino,
          acquisitionDate,
          address,
          registryReferenceDetails,
          acquiredFromName,
          totalCostOfLandOrPropertyAcquired,
          isSupportedByAnIndependentValuation,
          jointlyHeld,
          isPropertyDefinedAsSchedule29a,
          lessees,
          totalIncome,
          disposals
        ) =>
          val addressDetails = AddressDetails.uploadAddressToRequestAddressDetails(address)
          LandOrConnectedPropertyApi.TransactionDetail(
            row = Some(line),
            nameDOB = nameDob,
            nino = nino,
            acquisitionDate = acquisitionDate,
            landOrPropertyInUK = addressDetails._1,
            addressDetails = addressDetails._2,
            registryDetails = registryReferenceDetails,
            acquiredFromName = acquiredFromName,
            totalCost = totalCostOfLandOrPropertyAcquired.value,
            independentValuation = YesNo.withNameInsensitive(isSupportedByAnIndependentValuation),
            jointlyHeld = jointlyHeld._1,
            noOfPersons = jointlyHeld._2,
            residentialSchedule29A = YesNo.withNameInsensitive(isPropertyDefinedAsSchedule29a),
            isLeased = lessees._1,
            lesseeDetails = lessees._2,
            totalIncomeOrReceipts = totalIncome.value,
            isPropertyDisposed = disposals._1,
            disposalDetails = disposals._2
          )
      }
    )) match {
      case None =>
        invalidFileFormat(line, data)
      case Some((raw, Valid(landConnectedProperty))) =>
        CsvRowValid(line, landConnectedProperty, raw.toNonEmptyList)
      case Some((raw, Invalid(errs))) =>
        CsvRowInvalid[LandOrConnectedPropertyApi.TransactionDetail](line, errs, raw.toNonEmptyList)
    }
  }

  private def readCSV(
    row: Int,
    headerKeys: List[CsvHeaderKey],
    csvData: List[String]
  ): Option[RawTransactionDetail] = {
    val csvValue = getCSVValue(_, headerKeys, csvData)
    val csvOptValue = getOptionalCSVValue(_, headerKeys, csvData)
    for {
      /*  B */
      firstNameOfSchemeMember <- csvValue(Keys.firstName)
      /*  C */
      lastNameOfSchemeMember <- csvValue(Keys.lastName)
      /*  D */
      memberDateOfBirth <- csvValue(Keys.memberDateOfBirth)
      /*  E */
      memberNino <- csvOptValue(Keys.memberNino)
      /*  F */
      memberReasonNoNino <- csvOptValue(Keys.memberReasonNoNino)
      /*  G */
      acquisitionDate <- csvValue(Keys.acquisitionDate)
      /*  H */
      isLandOrPropertyInUK <- csvValue(Keys.isLandOrPropertyInUK)
      /*  I */
      landOrPropertyUkAddressLine1 <- csvOptValue(Keys.landOrPropertyUkAddressLine1)
      /*  J */
      landOrPropertyUkAddressLine2 <- csvOptValue(Keys.landOrPropertyUkAddressLine2)
      /*  K */
      landOrPropertyUkAddressLine3 <- csvOptValue(Keys.landOrPropertyUkAddressLine3)
      /*  L */
      landOrPropertyUkTownOrCity <- csvOptValue(Keys.landOrPropertyUkTownOrCity)
      /*  M */
      landOrPropertyUkPostCode <- csvOptValue(Keys.landOrPropertyUkPostCode)
      /*  N */
      landOrPropertyAddressLine1 <- csvOptValue(Keys.landOrPropertyAddressLine1)
      /*  O */
      landOrPropertyAddressLine2 <- csvOptValue(Keys.landOrPropertyAddressLine2)
      /*  P */
      landOrPropertyAddressLine3 <- csvOptValue(Keys.landOrPropertyAddressLine3)
      /*  Q */
      landOrPropertyAddressLine4 <- csvOptValue(Keys.landOrPropertyAddressLine4)
      /*  R */
      landOrPropertyCountry <- csvOptValue(Keys.landOrPropertyCountry)
      /*  S */
      isThereLandRegistryReference <- csvValue(Keys.isThereLandRegistryReference)
      /*  T */
      landRegistryReferenceOrReason <- csvValue(Keys.landRegistryRefOrReason)
      /*  U */
      acquiredFromName <- csvValue(Keys.acquiredFromName)
      /*  V */
      totalCostOfLandOrPropertyAcquired <- csvValue(Keys.totalCostOfLandOrPropertyAcquired)
      /*  W */
      isSupportedByAnIndependentValuation <- csvValue(Keys.isSupportedByAnIndependentValuation)
      /*  X */
      isPropertyHeldJointly <- csvValue(Keys.isPropertyHeldJointly)
      /*  Y */
      whatPercentageOfPropertyOwnedByMember <- csvOptValue(Keys.whatPercentageOfPropertyOwnedByMember)
      /*  Z */
      isPropertyDefinedAsSchedule29a <- csvValue(Keys.isPropertyDefinedAsSchedule29a)
      /* AA */
      isLeased <- csvValue(Keys.isLeased)
      /* AB */
      lesseeCount <- csvOptValue(Keys.lesseeCount)
      /* AC */
      anyLesseeConnected <- csvOptValue(Keys.areAnyLesseesConnected)
      /* AD */
      leaseDate <- csvOptValue(Keys.annualLeaseDate)
      /* AE */
      leaseAnnualAmount <- csvOptValue(Keys.annualLeaseAmount)
      /* AF */
      totalAmountOfIncomeAndReceipts <- csvValue(Keys.totalAmountOfIncomeAndReceipts)
      /* AG */
      wereAnyDisposalOnThisDuringTheYear <- csvValue(Keys.wereAnyDisposalOnThisDuringTheYear)
      /* AH */
      totalSaleProceedIfAnyDisposal <- csvOptValue(Keys.totalSaleProceedIfAnyDisposal)
      /* AI */
      namesOfPurchaser <- csvOptValue(Keys.namesOfPurchasers)
      /* AJ */
      areAnyPurchaserConnected <- csvOptValue(Keys.areAnyPurchaserConnected)
      /* AK */
      isTransactionSupportedByIndependentValuation <- csvOptValue(Keys.isTransactionSupportedByIndependentValuation)
      /* AL */
      hasLandOrPropertyFullyDisposedOf <- csvOptValue(Keys.hasLandOrPropertyFullyDisposedOf)
    } yield RawTransactionDetail.create(
      row,
      firstNameOfSchemeMember,
      lastNameOfSchemeMember,
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
      landRegistryReferenceOrReason,
      acquiredFromName,
      totalCostOfLandOrPropertyAcquired,
      isSupportedByAnIndependentValuation,
      isPropertyHeldJointly,
      whatPercentageOfPropertyOwnedByMember,
      isPropertyDefinedAsSchedule29a,
      isLeased,
      lesseeCount,
      anyLesseeConnected,
      leaseDate,
      leaseAnnualAmount,
      totalAmountOfIncomeAndReceipts,
      wereAnyDisposalOnThisDuringTheYear,
      totalSaleProceedIfAnyDisposal,
      namesOfPurchaser,
      areAnyPurchaserConnected,
      isTransactionSupportedByIndependentValuation,
      hasLandOrPropertyFullyDisposedOf
    )
  }
}
