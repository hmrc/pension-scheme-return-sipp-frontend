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
import cats.implicits._
import models._
import models.csv.CsvRowState
import models.csv.CsvRowState._
import models.requests.{LandOrConnectedPropertyRequest, YesNo}
import models.requests.common.AddressDetails
import models.requests.raw.ArmsLengthLandOrConnectedPropertyRaw.RawTransactionDetail
import models.requests.raw.ArmsLengthLandOrConnectedPropertyRaw.RawTransactionDetail.Ops
import play.api.i18n.Messages
import services.validation.{LandOrPropertyValidationsService, Validator}

import javax.inject.Inject

class ArmsLengthLandOrPropertyCsvRowValidator @Inject()(
  validations: LandOrPropertyValidationsService
) extends CsvRowValidator[LandOrConnectedPropertyRequest.TransactionDetail]
    with Validator {
  override def validate(
    line: Int,
    data: NonEmptyList[String],
    headers: List[CsvHeaderKey],
    csvRowValidationParameters: CsvRowValidationParameters
  )(implicit messages: Messages): CsvRowState[LandOrConnectedPropertyRequest.TransactionDetail] = {
    val validDateThreshold = csvRowValidationParameters.schemeWindUpDate

    (for {
      raw <- readCSV(line, headers, data.toList)
      memberFullNameDob = s"${raw.firstNameOfSchemeMember.value} ${raw.lastNameOfSchemeMember.value} ${raw.memberDateOfBirth.value}"

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

      validatePropertyCount <- validations.validateCount(
        raw.countOfLandOrPropertyTransactions,
        key = "landOrProperty.transactionCount",
        memberFullName = memberFullNameDob,
        row = line,
        maxCount = 50
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
        raw.noLandRegistryReference,
        memberFullNameDob,
        line
      )

      validatedAcquiredFromName <- validations.validateFreeText(
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
        raw.rawJointlyHeld.howManyPersonsJointlyOwnProperty,
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
        raw.rawLeased.isLeased,
        raw.rawLeased.namesOfLessees,
        raw.rawLeased.namesOfLessees,
        raw.rawLeased.anyOfLesseesConnected,
        raw.rawLeased.leaseDate,
        raw.rawLeased.annualLeaseAmount,
        isCountEntered = false,
        memberFullNameDob,
        line
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
        validatePropertyCount,
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
      ).mapN(
        (
          nameDob,
          nino,
          totalPropertyCount, // TODO Check that property count!
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
        ) => {
          val addressDetails = AddressDetails.uploadAddressToRequestAddressDetails(address)
          LandOrConnectedPropertyRequest.TransactionDetail(
            row = line,
            nameDOB = nameDob,
            nino = nino,
            acquisitionDate = acquisitionDate,
            landOrPropertyinUK = addressDetails._1,
            addressDetails = addressDetails._2,
            registryDetails = registryReferenceDetails,
            acquiredFromName = acquiredFromName,
            totalCost = totalCostOfLandOrPropertyAcquired.value,
            independentValuation = YesNo.uploadYesNoToRequestYesNo(isSupportedByAnIndependentValuation),
            jointlyHeld = jointlyHeld._1,
            noOfPersons = jointlyHeld._2,
            residentialSchedule29A = YesNo.uploadYesNoToRequestYesNo(isPropertyDefinedAsSchedule29a),
            isLeased = lessees._1,
            lesseeDetails = lessees._2,
            totalIncomeOrReceipts = totalIncome.value,
            isPropertyDisposed = disposals._1,
            disposalDetails = disposals._2
          )
        }
      )
    )) match {
      case None =>
        invalidFileFormat(line, data)
      case Some((raw, Valid(landConnectedProperty))) =>
        CsvRowValid(line, landConnectedProperty, raw.toNonEmptyList)
      case Some((raw, Invalid(errs))) =>
        CsvRowInvalid[LandOrConnectedPropertyRequest.TransactionDetail](line, errs, raw.toNonEmptyList)
    }
  }

  private def readCSV(
    row: Int,
    headerKeys: List[CsvHeaderKey],
    csvData: List[String]
  ): Option[RawTransactionDetail] =
    for {
      /*  B */
      firstNameOfSchemeMember <- getCSVValue(UploadKeys.firstNameOfSchemeMember, headerKeys, csvData)
      /*  C */
      lastNameOfSchemeMember <- getCSVValue(UploadKeys.lastNameOfSchemeMember, headerKeys, csvData)
      /*  D */
      memberDateOfBirth <- getCSVValue(UploadKeys.memberDateOfBirth, headerKeys, csvData)
      /*  E */
      memberNino <- getOptionalCSVValue(UploadKeys.memberNationalInsuranceNumber, headerKeys, csvData)
      /*  F */
      memberReasonNoNino <- getOptionalCSVValue(UploadKeys.memberReasonNoNino, headerKeys, csvData)

      /*  G */
      countOfLandOrPropertyTransactions <- getCSVValue(
        UploadKeys.countOfArmsLengthLandOrPropertyTransactions,
        headerKeys,
        csvData
      )

      /*  H */
      acquisitionDate <- getCSVValue(UploadKeys.acquisitionDate, headerKeys, csvData)

      /*  I */
      isLandOrPropertyInUK <- getCSVValue(UploadKeys.isLandOrPropertyInUK, headerKeys, csvData)
      /*  J */
      landOrPropertyUkAddressLine1 <- getOptionalCSVValue(UploadKeys.landOrPropertyUkAddressLine1, headerKeys, csvData)
      /*  K */
      landOrPropertyUkAddressLine2 <- getOptionalCSVValue(UploadKeys.landOrPropertyUkAddressLine2, headerKeys, csvData)
      /*  L */
      landOrPropertyUkAddressLine3 <- getOptionalCSVValue(UploadKeys.landOrPropertyUkAddressLine3, headerKeys, csvData)
      /*  M */
      landOrPropertyUkTownOrCity <- getOptionalCSVValue(UploadKeys.landOrPropertyUkTownOrCity, headerKeys, csvData)
      /*  N */
      landOrPropertyUkPostCode <- getOptionalCSVValue(UploadKeys.landOrPropertyUkPostCode, headerKeys, csvData)
      /*  O */
      landOrPropertyAddressLine1 <- getOptionalCSVValue(UploadKeys.landOrPropertyAddressLine1, headerKeys, csvData)
      /*  P */
      landOrPropertyAddressLine2 <- getOptionalCSVValue(UploadKeys.landOrPropertyAddressLine2, headerKeys, csvData)
      /*  Q */
      landOrPropertyAddressLine3 <- getOptionalCSVValue(UploadKeys.landOrPropertyAddressLine3, headerKeys, csvData)
      /*  R */
      landOrPropertyAddressLine4 <- getOptionalCSVValue(UploadKeys.landOrPropertyAddressLine4, headerKeys, csvData)
      /*  S */
      landOrPropertyCountry <- getOptionalCSVValue(UploadKeys.landOrPropertyCountry, headerKeys, csvData)

      /*  T */
      isThereLandRegistryReference <- getCSVValue(UploadKeys.isThereLandRegistryReference, headerKeys, csvData)
      /*  U */
      noLandRegistryReference <- getOptionalCSVValue(UploadKeys.noLandRegistryReference, headerKeys, csvData)

      /*  V */
      acquiredFromName <- getCSVValue(UploadKeys.acquiredFromName, headerKeys, csvData)

      /*  W */
      totalCostOfLandOrPropertyAcquired <- getCSVValue(
        UploadKeys.totalCostOfLandOrPropertyAcquired,
        headerKeys,
        csvData
      )
      /*  X */
      isSupportedByAnIndependentValuation <- getCSVValue(
        UploadKeys.isSupportedByAnIndependentValuation,
        headerKeys,
        csvData
      )

      /*  Y */
      isPropertyHeldJointly <- getCSVValue(UploadKeys.isPropertyHeldJointly, headerKeys, csvData)
      /*  Z */
      howManyPersonsJointlyOwnProperty <- getOptionalCSVValue(
        UploadKeys.howManyPersonsJointlyOwnProperty,
        headerKeys,
        csvData
      )

      /* AA */
      isPropertyDefinedAsSchedule29a <- getCSVValue(UploadKeys.isPropertyDefinedAsSchedule29a, headerKeys, csvData)

      /* AB */
      isLeased <- getCSVValue(UploadKeys.isLeased, headerKeys, csvData)
      /* AC */
      lesseeNames <- getOptionalCSVValue(UploadKeys.lesseeNames, headerKeys, csvData)
      /* AD */
      anyLesseeConnected <- getOptionalCSVValue(UploadKeys.areAnyLesseesConnected, headerKeys, csvData)
      /* AE */
      leaseDate <- getOptionalCSVValue(UploadKeys.annualLeaseDate, headerKeys, csvData)
      /* AF */
      leaseAnnualAmount <- getOptionalCSVValue(UploadKeys.annualLeaseAmount, headerKeys, csvData)

      /* AG */
      totalAmountOfIncomeAndReceipts <- getCSVValue(UploadKeys.totalAmountOfIncomeAndReceipts, headerKeys, csvData)

      /* AH */
      wereAnyDisposalOnThisDuringTheYear <- getCSVValue(
        UploadKeys.wasAnyDisposalOnThisDuringTheYearArmsLength,
        headerKeys,
        csvData
      )
      /* AI */
      totalSaleProceedIfAnyDisposal <- getOptionalCSVValue(
        UploadKeys.totalSaleProceedIfAnyDisposal,
        headerKeys,
        csvData
      )
      /* AJ */
      namesOfPurchaser <- getOptionalCSVValue(
        UploadKeys.namesOfPurchasers,
        headerKeys,
        csvData
      )
      /* AK */
      areAnyPurchaserConnected <- getOptionalCSVValue(
        UploadKeys.areAnyPurchaserConnected,
        headerKeys,
        csvData
      )

      /* AL */
      isTransactionSupportedByIndependentValuation <- getOptionalCSVValue(
        UploadKeys.isTransactionSupportedByIndependentValuation,
        headerKeys,
        csvData
      )
      /* AM */
      hasLandOrPropertyFullyDisposedOf <- getOptionalCSVValue(
        UploadKeys.hasLandOrPropertyFullyDisposedOf,
        headerKeys,
        csvData
      )
    } yield RawTransactionDetail.create(
      row,
      firstNameOfSchemeMember,
      lastNameOfSchemeMember,
      memberDateOfBirth,
      memberNino,
      memberReasonNoNino,
      countOfLandOrPropertyTransactions,
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
      noLandRegistryReference,
      acquiredFromName,
      totalCostOfLandOrPropertyAcquired,
      isSupportedByAnIndependentValuation,
      isPropertyHeldJointly,
      howManyPersonsJointlyOwnProperty,
      isPropertyDefinedAsSchedule29a,
      isLeased,
      lesseeNames,
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
