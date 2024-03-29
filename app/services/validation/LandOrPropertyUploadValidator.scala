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

package services.validation

import akka.NotUsed
import akka.stream.Materializer
import akka.stream.alpakka.csv.scaladsl.CsvParsing
import akka.stream.scaladsl.{Flow, Sink, Source}
import akka.util.ByteString
import cats.data.Validated.{Invalid, Valid}
import cats.data.{NonEmptyList, ValidatedNel}
import cats.implicits._
import models.requests.common.AddressDetails
import models.requests.raw.LandOrConnectedPropertyRaw.RawTransactionDetail
import models.requests.{LandOrConnectedPropertyRequest, YesNo}
import models.{UploadKeys, _}
import play.api.i18n.Messages

import java.time.LocalDate
import java.util.concurrent.atomic.AtomicInteger
import scala.concurrent.{ExecutionContext, Future}

class LandOrPropertyUploadValidator(
  validations: LandOrPropertyValidationsService,
  journey: Journey
)(implicit ec: ExecutionContext, materializer: Materializer)
    extends Validator
    with UploadValidator {
  private val firstRowSink: Sink[List[ByteString], Future[List[String]]] =
    Sink.head[List[ByteString]].mapMaterializedValue(_.map(_.map(_.utf8String)))

  private val csvFrame: Flow[ByteString, List[ByteString], NotUsed] = {
    CsvParsing.lineScanner()
  }

  private val fileFormatError = UploadFormatError(
    ValidationError(
      0,
      ValidationErrorType.Formatting,
      "Invalid file format, please format file as per provided template"
    )
  )

  def validateUpload(
    source: Source[ByteString, _],
    validDateThreshold: Option[LocalDate]
  )(implicit messages: Messages): Future[(Upload, Int, Long)] = {
    val startTime = System.currentTimeMillis
    val counter = new AtomicInteger()
    val csvFrames = source.via(csvFrame)
    (for {
      csvHeader <- csvFrames.runWith(firstRowSink)
      header = csvHeader.zipWithIndex
        .map { case (key, index) => CsvHeaderKey(key, indexToCsvKey(index), index) }
      validated <- csvFrames
        .drop(2) // drop csv header and process rows
        .statefulMap[UploadInternalState, Upload](() => UploadInternalState.init)(
          (state, bs) => {
            counter.incrementAndGet()
            val parts = bs.map(_.utf8String)

            validate(
              journey,
              header.map(h => h.copy(key = h.key.trim)),
              parts.drop(0),
              state.row,
              validDateThreshold: Option[LocalDate]
            ) match {
              case None => state.next() -> fileFormatError
              case Some((raw, Valid(landConnectedProperty))) =>
                state.next() -> UploadSuccessLandConnectedProperty(List(raw), List(landConnectedProperty))
              case Some((raw, Invalid(errs))) =>
                state.next() -> UploadErrorsLandConnectedProperty(NonEmptyList.one(raw), errs)
            }
          },
          _ => None
        )
        .takeWhile({
          case _: UploadFormatError => false
          case _ => true
        }, inclusive = true)
        .runReduce[Upload] {
          // format and max row errors
          case (_, e: UploadFormatError) => e
          case (e: UploadFormatError, _) => e
          // errors
          case (
              UploadErrorsLandConnectedProperty(rawPrev, previous),
              UploadErrorsLandConnectedProperty(raw, errs)
              ) =>
            UploadErrorsLandConnectedProperty(rawPrev ::: raw, previous ++ errs.toList)
          case (
              UploadErrorsLandConnectedProperty(rawPrev, err),
              UploadSuccessLandConnectedProperty(detailsRaw, _)
              ) =>
            UploadErrorsLandConnectedProperty(rawPrev ++ detailsRaw, err)
          case (
              UploadSuccessLandConnectedProperty(detailsPrevRaw, _),
              UploadErrorsLandConnectedProperty(raw, err)
              ) =>
            UploadErrorsLandConnectedProperty(NonEmptyList.fromListUnsafe(detailsPrevRaw) ::: raw, err)
          // success
          case (previous: UploadSuccessLandConnectedProperty, current: UploadSuccessLandConnectedProperty) =>
            UploadSuccessLandConnectedProperty(
              previous.interestLandOrPropertyRaw ++ current.interestLandOrPropertyRaw,
              previous.rows ++ current.rows
            )
          case (_, success: UploadSuccessLandConnectedProperty) => success
        }
    } yield (validated, counter.get(), System.currentTimeMillis - startTime))
      .recover {
        case _: NoSuchElementException =>
          (fileFormatError, 0, System.currentTimeMillis - startTime)
      }
  }

  private def validate(
    journey: Journey,
    headerKeys: List[CsvHeaderKey],
    csvData: List[String],
    row: Int,
    validDateThreshold: Option[LocalDate]
  )(
    implicit messages: Messages
  ): Option[(RawTransactionDetail, ValidatedNel[ValidationError, LandOrConnectedPropertyRequest.TransactionDetail])] =
    for {
      raw <- readCSV(journey, row, headerKeys, csvData)
      memberFullNameDob = s"${raw.firstNameOfSchemeMember.value} ${raw.lastNameOfSchemeMember.value} ${raw.memberDateOfBirth.value}"

      // Validations
      validatedNameDOB <- validations.validateNameDOB(
        firstName = raw.firstNameOfSchemeMember,
        lastName = raw.lastNameOfSchemeMember,
        dob = raw.memberDateOfBirth,
        row = row,
        validDateThreshold = validDateThreshold
      )

      validatePropertyCount <- validations.validateCount(
        raw.countOfLandOrPropertyTransactions,
        key = "landOrProperty.transactionCount",
        memberFullName = memberFullNameDob,
        row = row,
        maxCount = 50
      )

      validatedAcquisitionDate <- validations.validateDate(
        date = raw.acquisitionDate,
        key = "landOrProperty.acquisitionDate",
        row = row,
        validDateThreshold = validDateThreshold
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
        row
      )

      validatedIsThereARegistryReference <- validations.validateIsThereARegistryReference(
        raw.isThereLandRegistryReference,
        raw.noLandRegistryReference,
        memberFullNameDob,
        row
      )

      validatedAcquiredFromName <- validations.validateFreeText(
        raw.acquiredFromName,
        "landOrProperty.acquireFromName",
        memberFullNameDob,
        row
      )

      validatedAcquiredFromType <- validations.validateAcquiredFrom(
        raw.rawAcquiredFromType.acquiredFromType,
        raw.rawAcquiredFromType.acquirerNinoForIndividual,
        raw.rawAcquiredFromType.acquirerCrnForCompany,
        raw.rawAcquiredFromType.acquirerUtrForPartnership,
        raw.rawAcquiredFromType.noIdOrAcquiredFromAnotherSource,
        memberFullNameDob,
        row
      )

      validatedTotalCostOfLandOrPropertyAcquired <- validations.validatePrice(
        raw.totalCostOfLandOrPropertyAcquired,
        "landOrProperty.totalCostOfLandOrPropertyAcquired",
        memberFullNameDob,
        row
      )

      validatedIsSupportedByAnIndependentValuation <- validations.validateYesNoQuestion(
        raw.isSupportedByAnIndependentValuation,
        "landOrProperty.supportedByAnIndependent",
        memberFullNameDob,
        row
      )

      validatedJointlyHeld <- validations.validateJointlyHeldAll(
        raw.rawJointlyHeld.isPropertyHeldJointly,
        raw.rawJointlyHeld.howManyPersonsJointlyOwnProperty,
        List(
          (
            raw.rawJointlyHeld.firstPersonNameJointlyOwning,
            raw.rawJointlyHeld.firstPersonNinoJointlyOwning,
            raw.rawJointlyHeld.firstPersonNoNinoJointlyOwning
          ),
          (
            raw.rawJointlyHeld.secondPersonNameJointlyOwning,
            raw.rawJointlyHeld.secondPersonNinoJointlyOwning,
            raw.rawJointlyHeld.secondPersonNoNinoJointlyOwning
          ),
          (
            raw.rawJointlyHeld.thirdPersonNameJointlyOwning,
            raw.rawJointlyHeld.thirdPersonNinoJointlyOwning,
            raw.rawJointlyHeld.thirdPersonNoNinoJointlyOwning
          ),
          (
            raw.rawJointlyHeld.fourthPersonNameJointlyOwning,
            raw.rawJointlyHeld.fourthPersonNinoJointlyOwning,
            raw.rawJointlyHeld.fourthPersonNoNinoJointlyOwning
          ),
          (
            raw.rawJointlyHeld.fifthPersonNameJointlyOwning,
            raw.rawJointlyHeld.fifthPersonNinoJointlyOwning,
            raw.rawJointlyHeld.fifthPersonNoNinoJointlyOwning
          )
        ),
        memberFullNameDob,
        row
      )

      validatedIsPropertyDefinedAsSchedule29a <- validations.validateYesNoQuestion(
        raw.isPropertyDefinedAsSchedule29a,
        "landOrProperty.isPropertyDefinedAsSchedule29a",
        memberFullNameDob,
        row
      )

      validatedLessees <- validations.validateLeasedAll(
        raw.rawLeased.isLeased,
        List(
          (
            raw.rawLeased.first.name,
            raw.rawLeased.first.connection,
            raw.rawLeased.first.grantedDate,
            raw.rawLeased.first.annualAmount
          ),
          (
            raw.rawLeased.second.name,
            raw.rawLeased.second.connection,
            raw.rawLeased.second.grantedDate,
            raw.rawLeased.second.annualAmount
          ),
          (
            raw.rawLeased.third.name,
            raw.rawLeased.third.connection,
            raw.rawLeased.third.grantedDate,
            raw.rawLeased.third.annualAmount
          ),
          (
            raw.rawLeased.fourth.name,
            raw.rawLeased.fourth.connection,
            raw.rawLeased.fourth.grantedDate,
            raw.rawLeased.fourth.annualAmount
          ),
          (
            raw.rawLeased.fifth.name,
            raw.rawLeased.fifth.connection,
            raw.rawLeased.fifth.grantedDate,
            raw.rawLeased.fifth.annualAmount
          ),
          (
            raw.rawLeased.sixth.name,
            raw.rawLeased.sixth.connection,
            raw.rawLeased.sixth.grantedDate,
            raw.rawLeased.sixth.annualAmount
          ),
          (
            raw.rawLeased.seventh.name,
            raw.rawLeased.seventh.connection,
            raw.rawLeased.seventh.grantedDate,
            raw.rawLeased.seventh.annualAmount
          ),
          (
            raw.rawLeased.eighth.name,
            raw.rawLeased.eighth.connection,
            raw.rawLeased.eighth.grantedDate,
            raw.rawLeased.eighth.annualAmount
          ),
          (
            raw.rawLeased.ninth.name,
            raw.rawLeased.ninth.connection,
            raw.rawLeased.ninth.grantedDate,
            raw.rawLeased.ninth.annualAmount
          ),
          (
            raw.rawLeased.tenth.name,
            raw.rawLeased.tenth.connection,
            raw.rawLeased.tenth.grantedDate,
            raw.rawLeased.tenth.annualAmount
          )
        ),
        memberFullNameDob,
        row
      )

      validatedTotalIncome <- validations.validatePrice(
        raw.totalAmountOfIncomeAndReceipts,
        "landOrProperty.totalAmountOfIncomeAndReceipts",
        memberFullNameDob,
        row
      )

      validatedDisposals <- validations.validateDisposals(
        raw.rawDisposal.wereAnyDisposalOnThisDuringTheYear,
        raw.rawDisposal.totalSaleProceedIfAnyDisposal,
        List(
          (raw.rawDisposal.first.name, raw.rawDisposal.first.connection),
          (raw.rawDisposal.second.name, raw.rawDisposal.second.connection),
          (raw.rawDisposal.third.name, raw.rawDisposal.third.connection),
          (raw.rawDisposal.fourth.name, raw.rawDisposal.fourth.connection),
          (raw.rawDisposal.fifth.name, raw.rawDisposal.fifth.connection),
          (raw.rawDisposal.sixth.name, raw.rawDisposal.sixth.connection),
          (raw.rawDisposal.seventh.name, raw.rawDisposal.seventh.connection),
          (raw.rawDisposal.eighth.name, raw.rawDisposal.eighth.connection),
          (raw.rawDisposal.ninth.name, raw.rawDisposal.ninth.connection),
          (raw.rawDisposal.tenth.name, raw.rawDisposal.tenth.connection)
        ),
        raw.rawDisposal.isTransactionSupportedByIndependentValuation,
        raw.rawDisposal.hasLandOrPropertyFullyDisposedOf,
        memberFullNameDob,
        row
      )

      validatedDuplicatedNinoNumbers <- validations.validateDuplicatedNinoNumbers(
        List(
          raw.rawAcquiredFromType.acquirerNinoForIndividual,
          raw.rawJointlyHeld.firstPersonNinoJointlyOwning,
          raw.rawJointlyHeld.secondPersonNinoJointlyOwning,
          raw.rawJointlyHeld.thirdPersonNinoJointlyOwning,
          raw.rawJointlyHeld.fourthPersonNinoJointlyOwning,
          raw.rawJointlyHeld.fifthPersonNinoJointlyOwning
        ),
        row
      )

    } yield (
      raw,
      (
        validatedNameDOB,
        validatedAcquisitionDate,
        validatePropertyCount,
        validatedAddress,
        validatedIsThereARegistryReference,
        validatedAcquiredFromName,
        validatedAcquiredFromType,
        validatedTotalCostOfLandOrPropertyAcquired,
        validatedIsSupportedByAnIndependentValuation,
        validatedJointlyHeld,
        validatedIsPropertyDefinedAsSchedule29a,
        validatedLessees,
        validatedTotalIncome,
        validatedDisposals,
        validatedDuplicatedNinoNumbers
      ).mapN(
        (
          nameDob,
          acquisitionDate,
          totalPropertyCount, // TODO Check that property count!
          address,
          registryReferenceDetails,
          acquiredFromName,
          acquiredFromTypeDetails,
          totalCostOfLandOrPropertyAcquired,
          isSupportedByAnIndependentValuation,
          jointlyHeld,
          isPropertyDefinedAsSchedule29a,
          lessees,
          totalIncome,
          disposals,
          _
        ) => {
          val addressDetails = AddressDetails.uploadAddressToRequestAddressDetails(address)
          LandOrConnectedPropertyRequest.TransactionDetail(
            row = row,
            nameDOB = nameDob,
            acquisitionDate = acquisitionDate,
            landOrPropertyinUK = addressDetails._1,
            addressDetails = addressDetails._2,
            registryDetails = registryReferenceDetails,
            acquiredFromName = acquiredFromName,
            acquiredFromType = acquiredFromTypeDetails,
            totalCost = totalCostOfLandOrPropertyAcquired.value,
            independentValution = YesNo.uploadYesNoToRequestYesNo(isSupportedByAnIndependentValuation),
            jointlyHeld = jointlyHeld._1,
            noOfPersons = jointlyHeld._2,
            jointPropertyPersonDetails = jointlyHeld._3,
            residentialSchedule29A = YesNo.uploadYesNoToRequestYesNo(isPropertyDefinedAsSchedule29a),
            isLeased = lessees._1,
            lesseeDetails = lessees._2,
            totalIncomeOrReceipts = totalIncome.value,
            isPropertyDisposed = disposals._1,
            disposalDetails = disposals._2
          )
        }
      )
    )

  private def readCSV(
    journey: Journey,
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
      keyCountOfLandOrProperty = if (journey == Journey.InterestInLandOrProperty) {
        UploadKeys.countOfInterestLandOrPropertyTransactions
      } else {
        UploadKeys.countOfArmsLengthLandOrPropertyTransactions
      }
      countOfLandOrPropertyTransactions <- getCSVValue(
        keyCountOfLandOrProperty,
        headerKeys,
        csvData
      )

      /*  F */
      acquisitionDate <- getCSVValue(UploadKeys.acquisitionDate, headerKeys, csvData)

      /*  G */
      isLandOrPropertyInUK <- getCSVValue(UploadKeys.isLandOrPropertyInUK, headerKeys, csvData)
      /*  H */
      landOrPropertyUkAddressLine1 <- getOptionalCSVValue(UploadKeys.landOrPropertyUkAddressLine1, headerKeys, csvData)
      /*  I */
      landOrPropertyUkAddressLine2 <- getOptionalCSVValue(UploadKeys.landOrPropertyUkAddressLine2, headerKeys, csvData)
      /*  J */
      landOrPropertyUkAddressLine3 <- getOptionalCSVValue(UploadKeys.landOrPropertyUkAddressLine3, headerKeys, csvData)
      /*  K */
      landOrPropertyUkTownOrCity <- getOptionalCSVValue(UploadKeys.landOrPropertyUkTownOrCity, headerKeys, csvData)
      /*  L */
      landOrPropertyUkPostCode <- getOptionalCSVValue(UploadKeys.landOrPropertyUkPostCode, headerKeys, csvData)
      /*  M */
      landOrPropertyAddressLine1 <- getOptionalCSVValue(UploadKeys.landOrPropertyAddressLine1, headerKeys, csvData)
      /*  N */
      landOrPropertyAddressLine2 <- getOptionalCSVValue(UploadKeys.landOrPropertyAddressLine2, headerKeys, csvData)
      /*  O */
      landOrPropertyAddressLine3 <- getOptionalCSVValue(UploadKeys.landOrPropertyAddressLine3, headerKeys, csvData)
      /*  P */
      landOrPropertyAddressLine4 <- getOptionalCSVValue(UploadKeys.landOrPropertyAddressLine4, headerKeys, csvData)
      /*  Q */
      landOrPropertyCountry <- getOptionalCSVValue(UploadKeys.landOrPropertyCountry, headerKeys, csvData)

      /*  R */
      isThereLandRegistryReference <- getCSVValue(UploadKeys.isThereLandRegistryReference, headerKeys, csvData)
      /*  S */
      noLandRegistryReference <- getOptionalCSVValue(UploadKeys.noLandRegistryReference, headerKeys, csvData)

      /*  T */
      acquiredFromName <- getCSVValue(UploadKeys.acquiredFromName, headerKeys, csvData)
      /*  U */
      keyAcquiredFromType = if (journey == Journey.InterestInLandOrProperty) {
        UploadKeys.acquiredTypeInterestLand
      } else {
        UploadKeys.acquiredTypeArmsLength
      }
      acquiredFromType <- getCSVValue(keyAcquiredFromType, headerKeys, csvData)
      /*  V */
      keyAcquirerNinoForIndividual = if (journey == Journey.InterestInLandOrProperty) {
        UploadKeys.acquirerNinoForIndividualInterest
      } else {
        UploadKeys.acquirerNinoForIndividualArmsLength
      }
      acquirerNinoForIndividual <- getOptionalCSVValue(keyAcquirerNinoForIndividual, headerKeys, csvData)
      /*  W */
      keyAcquirerCrnForCompany = if (journey == Journey.InterestInLandOrProperty) {
        UploadKeys.acquirerCrnForCompanyInterest
      } else {
        UploadKeys.acquirerCrnForCompanyArmsLength
      }
      acquirerCrnForCompany <- getOptionalCSVValue(keyAcquirerCrnForCompany, headerKeys, csvData)
      /*  X */
      keyAcquirerUtrForPartnership = if (journey == Journey.InterestInLandOrProperty) {
        UploadKeys.acquirerUtrForPartnershipInterest
      } else {
        UploadKeys.acquirerUtrForPartnershipArmsLength
      }
      acquirerUtrForPartnership <- getOptionalCSVValue(keyAcquirerUtrForPartnership, headerKeys, csvData)
      /*  Y */
      noIdOrAcquiredFromAnotherSource <- getOptionalCSVValue(
        UploadKeys.noIdOrAcquiredFromAnotherSource,
        headerKeys,
        csvData
      )

      /*  Z */
      totalCostOfLandOrPropertyAcquired <- getCSVValue(
        UploadKeys.totalCostOfLandOrPropertyAcquired,
        headerKeys,
        csvData
      )
      /* AA */
      isSupportedByAnIndependentValuation <- getCSVValue(
        UploadKeys.isSupportedByAnIndependentValuation,
        headerKeys,
        csvData
      )

      /* AB */
      isPropertyHeldJointly <- getCSVValue(UploadKeys.isPropertyHeldJointly, headerKeys, csvData)
      /* AC */
      howManyPersonsJointlyOwnProperty <- getOptionalCSVValue(
        UploadKeys.howManyPersonsJointlyOwnProperty,
        headerKeys,
        csvData
      )
      /* AD */
      firstPersonNameJointlyOwning <- getOptionalCSVValue(UploadKeys.firstPersonNameJointlyOwning, headerKeys, csvData)
      /* AE */
      firstPersonNinoJointlyOwning <- getOptionalCSVValue(UploadKeys.firstPersonNinoJointlyOwning, headerKeys, csvData)
      /* AF */
      firstPersonNoNinoJointlyOwning <- getOptionalCSVValue(
        UploadKeys.firstPersonNoNinoJointlyOwning,
        headerKeys,
        csvData
      )
      /* AG */
      secondPersonNameJointlyOwning <- getOptionalCSVValue(
        UploadKeys.secondPersonNameJointlyOwning,
        headerKeys,
        csvData
      )
      /* AH */
      secondPersonNinoJointlyOwning <- getOptionalCSVValue(
        UploadKeys.secondPersonNinoJointlyOwning,
        headerKeys,
        csvData
      )
      /* AI */
      secondPersonNoNinoJointlyOwning <- getOptionalCSVValue(
        UploadKeys.secondPersonNoNinoJointlyOwning,
        headerKeys,
        csvData
      )
      /* AJ */
      thirdPersonNameJointlyOwning <- getOptionalCSVValue(UploadKeys.thirdPersonNameJointlyOwning, headerKeys, csvData)
      /* AK */
      thirdPersonNinoJointlyOwning <- getOptionalCSVValue(UploadKeys.thirdPersonNinoJointlyOwning, headerKeys, csvData)
      /* AL */
      thirdPersonNoNinoJointlyOwning <- getOptionalCSVValue(
        UploadKeys.thirdPersonNoNinoJointlyOwning,
        headerKeys,
        csvData
      )
      /* AM */
      fourthPersonNameJointlyOwning <- getOptionalCSVValue(
        UploadKeys.fourthPersonNameJointlyOwning,
        headerKeys,
        csvData
      )
      /* AN */
      fourthPersonNinoJointlyOwning <- getOptionalCSVValue(
        UploadKeys.fourthPersonNinoJointlyOwning,
        headerKeys,
        csvData
      )
      /* AO */
      fourthPersonNoNinoJointlyOwning <- getOptionalCSVValue(
        UploadKeys.fourthPersonNoNinoJointlyOwning,
        headerKeys,
        csvData
      )
      /* AP */
      fifthPersonNameJointlyOwning <- getOptionalCSVValue(UploadKeys.fifthPersonNameJointlyOwning, headerKeys, csvData)
      /* AQ */
      fifthPersonNinoJointlyOwning <- getOptionalCSVValue(UploadKeys.fifthPersonNinoJointlyOwning, headerKeys, csvData)
      /* AR */
      fifthPersonNoNinoJointlyOwning <- getOptionalCSVValue(
        UploadKeys.fifthPersonNoNinoJointlyOwning,
        headerKeys,
        csvData
      )

      /* AS */
      isPropertyDefinedAsSchedule29a <- getCSVValue(UploadKeys.isPropertyDefinedAsSchedule29a, headerKeys, csvData)

      /* AT */
      isLeased <- getCSVValue(UploadKeys.isLeased, headerKeys, csvData)
      /* AU */
      firstLesseeName <- getOptionalCSVValue(UploadKeys.firstLesseeName, headerKeys, csvData)
      /* AV */
      firstLesseeConnectedOrUnconnected <- getOptionalCSVValue(
        UploadKeys.firstLesseeConnectedOrUnconnected,
        headerKeys,
        csvData
      )
      /* AW */
      firstLesseeGrantedDate <- getOptionalCSVValue(UploadKeys.firstLesseeGrantedDate, headerKeys, csvData)
      /* AX */
      firstLesseeAnnualAmount <- getOptionalCSVValue(UploadKeys.firstLesseeAnnualAmount, headerKeys, csvData)
      /* AY */
      secondLesseeName <- getOptionalCSVValue(UploadKeys.secondLesseeName, headerKeys, csvData)
      /* AZ */
      secondLesseeConnectedOrUnconnected <- getOptionalCSVValue(
        UploadKeys.secondLesseeConnectedOrUnconnected,
        headerKeys,
        csvData
      )
      /* BA */
      secondLesseeGrantedDate <- getOptionalCSVValue(UploadKeys.secondLesseeGrantedDate, headerKeys, csvData)
      /* BB */
      secondLesseeAnnualAmount <- getOptionalCSVValue(UploadKeys.secondLesseeAnnualAmount, headerKeys, csvData)
      /* BC */
      thirdLesseeName <- getOptionalCSVValue(UploadKeys.thirdLesseeName, headerKeys, csvData)
      /* BD */
      thirdLesseeConnectedOrUnconnected <- getOptionalCSVValue(
        UploadKeys.thirdLesseeConnectedOrUnconnected,
        headerKeys,
        csvData
      )
      /* BE */
      thirdLesseeGrantedDate <- getOptionalCSVValue(UploadKeys.thirdLesseeGrantedDate, headerKeys, csvData)
      /* BF */
      thirdLesseeAnnualAmount <- getOptionalCSVValue(UploadKeys.thirdLesseeAnnualAmount, headerKeys, csvData)
      /* BG */
      fourthLesseeName <- getOptionalCSVValue(UploadKeys.fourthLesseeName, headerKeys, csvData)
      /* BH */
      fourthLesseeConnectedOrUnconnected <- getOptionalCSVValue(
        UploadKeys.fourthLesseeConnectedOrUnconnected,
        headerKeys,
        csvData
      )
      /* BI */
      fourthLesseeGrantedDate <- getOptionalCSVValue(UploadKeys.fourthLesseeGrantedDate, headerKeys, csvData)
      /* BJ */
      fourthLesseeAnnualAmount <- getOptionalCSVValue(UploadKeys.fourthLesseeAnnualAmount, headerKeys, csvData)
      /* BK */
      fifthLesseeName <- getOptionalCSVValue(UploadKeys.fifthLesseeName, headerKeys, csvData)
      /* BL */
      fifthLesseeConnectedOrUnconnected <- getOptionalCSVValue(
        UploadKeys.fifthLesseeConnectedOrUnconnected,
        headerKeys,
        csvData
      )
      /* BM */
      fifthLesseeGrantedDate <- getOptionalCSVValue(UploadKeys.fifthLesseeGrantedDate, headerKeys, csvData)
      /* BN */
      fifthLesseeAnnualAmount <- getOptionalCSVValue(UploadKeys.fifthLesseeAnnualAmount, headerKeys, csvData)
      /* BO */
      sixthLesseeName <- getOptionalCSVValue(UploadKeys.sixthLesseeName, headerKeys, csvData)
      /* BP */
      sixthLesseeConnectedOrUnconnected <- getOptionalCSVValue(
        UploadKeys.sixthLesseeConnectedOrUnconnected,
        headerKeys,
        csvData
      )
      /* BQ */
      sixthLesseeGrantedDate <- getOptionalCSVValue(UploadKeys.sixthLesseeGrantedDate, headerKeys, csvData)
      /* BR */
      sixthLesseeAnnualAmount <- getOptionalCSVValue(UploadKeys.sixthLesseeAnnualAmount, headerKeys, csvData)
      /* BS */
      seventhLesseeName <- getOptionalCSVValue(UploadKeys.seventhLesseeName, headerKeys, csvData)
      /* BT */
      seventhLesseeConnectedOrUnconnected <- getOptionalCSVValue(
        UploadKeys.seventhLesseeConnectedOrUnconnected,
        headerKeys,
        csvData
      )
      /* BU */
      seventhLesseeGrantedDate <- getOptionalCSVValue(UploadKeys.seventhLesseeGrantedDate, headerKeys, csvData)
      /* BV */
      seventhLesseeAnnualAmount <- getOptionalCSVValue(UploadKeys.seventhLesseeAnnualAmount, headerKeys, csvData)
      /* BW */
      eighthLesseeName <- getOptionalCSVValue(UploadKeys.eighthLesseeName, headerKeys, csvData)
      /* BX */
      eighthLesseeConnectedOrUnconnected <- getOptionalCSVValue(
        UploadKeys.eighthLesseeConnectedOrUnconnected,
        headerKeys,
        csvData
      )
      /* BY */
      eighthLesseeGrantedDate <- getOptionalCSVValue(UploadKeys.eighthLesseeGrantedDate, headerKeys, csvData)
      /* BZ */
      eighthLesseeAnnualAmount <- getOptionalCSVValue(UploadKeys.eighthLesseeAnnualAmount, headerKeys, csvData)
      /* CA */
      ninthLesseeName <- getOptionalCSVValue(UploadKeys.ninthLesseeName, headerKeys, csvData)
      /* CB */
      ninthLesseeConnectedOrUnconnected <- getOptionalCSVValue(
        UploadKeys.ninthLesseeConnectedOrUnconnected,
        headerKeys,
        csvData
      )
      /* CC */
      ninthLesseeGrantedDate <- getOptionalCSVValue(UploadKeys.ninthLesseeGrantedDate, headerKeys, csvData)
      /* CD */
      ninthLesseeAnnualAmount <- getOptionalCSVValue(UploadKeys.ninthLesseeAnnualAmount, headerKeys, csvData)
      /* CE */
      tenthLesseeName <- getOptionalCSVValue(UploadKeys.tenthLesseeName, headerKeys, csvData)
      /* CF */
      tenthLesseeConnectedOrUnconnected <- getOptionalCSVValue(
        UploadKeys.tenthLesseeConnectedOrUnconnected,
        headerKeys,
        csvData
      )
      /* CG */
      tenthLesseeGrantedDate <- getOptionalCSVValue(UploadKeys.tenthLesseeGrantedDate, headerKeys, csvData)
      /* CH */
      tenthLesseeAnnualAmount <- getOptionalCSVValue(UploadKeys.tenthLesseeAnnualAmount, headerKeys, csvData)

      /* CI */
      totalAmountOfIncomeAndReceipts <- getCSVValue(UploadKeys.totalAmountOfIncomeAndReceipts, headerKeys, csvData)

      /* CJ */
      wereAnyDisposalOnThisDuringTheYear <- getCSVValue(
        UploadKeys.wereAnyDisposalOnThisDuringTheYear,
        headerKeys,
        csvData
      )
      /* CK */
      totalSaleProceedIfAnyDisposal <- getOptionalCSVValue(
        UploadKeys.totalSaleProceedIfAnyDisposal,
        headerKeys,
        csvData
      )
      /* CL */
      firstPurchaserName <- getOptionalCSVValue(UploadKeys.firstPurchaserName, headerKeys, csvData)
      /* CM */
      firstPurchaserConnectedOrUnconnected <- getOptionalCSVValue(
        UploadKeys.firstPurchaserConnectedOrUnconnected,
        headerKeys,
        csvData
      )
      /* CN */
      secondPurchaserName <- getOptionalCSVValue(UploadKeys.secondPurchaserName, headerKeys, csvData)
      /* CO */
      secondPurchaserConnectedOrUnconnected <- getOptionalCSVValue(
        UploadKeys.secondPurchaserConnectedOrUnconnected,
        headerKeys,
        csvData
      )
      /* CP */
      thirdPurchaserName <- getOptionalCSVValue(UploadKeys.thirdPurchaserName, headerKeys, csvData)
      /* CQ */
      thirdPurchaserConnectedOrUnconnected <- getOptionalCSVValue(
        UploadKeys.thirdPurchaserConnectedOrUnconnected,
        headerKeys,
        csvData
      )
      /* CR */
      fourthPurchaserName <- getOptionalCSVValue(UploadKeys.fourthPurchaserName, headerKeys, csvData)
      /* CS */
      fourthPurchaserConnectedOrUnconnected <- getOptionalCSVValue(
        UploadKeys.fourthPurchaserConnectedOrUnconnected,
        headerKeys,
        csvData
      )
      /* CT */
      fifthPurchaserName <- getOptionalCSVValue(UploadKeys.fifthPurchaserName, headerKeys, csvData)
      /* CU */
      fifthPurchaserConnectedOrUnconnected <- getOptionalCSVValue(
        UploadKeys.fifthPurchaserConnectedOrUnconnected,
        headerKeys,
        csvData
      )
      /* CV */
      sixthPurchaserName <- getOptionalCSVValue(UploadKeys.sixthPurchaserName, headerKeys, csvData)
      /* CW */
      sixthPurchaserConnectedOrUnconnected <- getOptionalCSVValue(
        UploadKeys.sixthPurchaserConnectedOrUnconnected,
        headerKeys,
        csvData
      )
      /* CX */
      seventhPurchaserName <- getOptionalCSVValue(UploadKeys.seventhPurchaserName, headerKeys, csvData)
      /* CY */
      seventhPurchaserConnectedOrUnconnected <- getOptionalCSVValue(
        UploadKeys.seventhPurchaserConnectedOrUnconnected,
        headerKeys,
        csvData
      )
      /* CZ */
      eighthPurchaserName <- getOptionalCSVValue(UploadKeys.eighthPurchaserName, headerKeys, csvData)
      /* DA */
      eighthPurchaserConnectedOrUnconnected <- getOptionalCSVValue(
        UploadKeys.eighthPurchaserConnectedOrUnconnected,
        headerKeys,
        csvData
      )
      /* DB */
      ninthPurchaserName <- getOptionalCSVValue(UploadKeys.ninthPurchaserName, headerKeys, csvData)
      /* DC */
      ninthPurchaserConnectedOrUnconnected <- getOptionalCSVValue(
        UploadKeys.ninthPurchaserConnectedOrUnconnected,
        headerKeys,
        csvData
      )
      /* DD */
      tenthPurchaserName <- getOptionalCSVValue(UploadKeys.tenthPurchaserName, headerKeys, csvData)
      /* DE */
      tenthPurchaserConnectedOrUnconnected <- getOptionalCSVValue(
        UploadKeys.tenthPurchaserConnectedOrUnconnected,
        headerKeys,
        csvData
      )
      /* DF */
      isTransactionSupportedByIndependentValuation <- getOptionalCSVValue(
        UploadKeys.isTransactionSupportedByIndependentValuation,
        headerKeys,
        csvData
      )
      /* DG */
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
      acquiredFromType,
      acquirerNinoForIndividual,
      acquirerCrnForCompany,
      acquirerUtrForPartnership,
      noIdOrAcquiredFromAnotherSource,
      totalCostOfLandOrPropertyAcquired,
      isSupportedByAnIndependentValuation,
      isPropertyHeldJointly,
      howManyPersonsJointlyOwnProperty,
      firstPersonNameJointlyOwning,
      firstPersonNinoJointlyOwning,
      firstPersonNoNinoJointlyOwning,
      secondPersonNameJointlyOwning,
      secondPersonNinoJointlyOwning,
      secondPersonNoNinoJointlyOwning,
      thirdPersonNameJointlyOwning,
      thirdPersonNinoJointlyOwning,
      thirdPersonNoNinoJointlyOwning,
      fourthPersonNameJointlyOwning,
      fourthPersonNinoJointlyOwning,
      fourthPersonNoNinoJointlyOwning,
      fifthPersonNameJointlyOwning,
      fifthPersonNinoJointlyOwning,
      fifthPersonNoNinoJointlyOwning,
      isPropertyDefinedAsSchedule29a,
      isLeased,
      firstLesseeName,
      firstLesseeConnectedOrUnconnected,
      firstLesseeGrantedDate,
      firstLesseeAnnualAmount,
      secondLesseeName,
      secondLesseeConnectedOrUnconnected,
      secondLesseeGrantedDate,
      secondLesseeAnnualAmount,
      thirdLesseeName,
      thirdLesseeConnectedOrUnconnected,
      thirdLesseeGrantedDate,
      thirdLesseeAnnualAmount,
      fourthLesseeName,
      fourthLesseeConnectedOrUnconnected,
      fourthLesseeGrantedDate,
      fourthLesseeAnnualAmount,
      fifthLesseeName,
      fifthLesseeConnectedOrUnconnected,
      fifthLesseeGrantedDate,
      fifthLesseeAnnualAmount,
      sixthLesseeName,
      sixthLesseeConnectedOrUnconnected,
      sixthLesseeGrantedDate,
      sixthLesseeAnnualAmount,
      seventhLesseeName,
      seventhLesseeConnectedOrUnconnected,
      seventhLesseeGrantedDate,
      seventhLesseeAnnualAmount,
      eighthLesseeName,
      eighthLesseeConnectedOrUnconnected,
      eighthLesseeGrantedDate,
      eighthLesseeAnnualAmount,
      ninthLesseeName,
      ninthLesseeConnectedOrUnconnected,
      ninthLesseeGrantedDate,
      ninthLesseeAnnualAmount,
      tenthLesseeName,
      tenthLesseeConnectedOrUnconnected,
      tenthLesseeGrantedDate,
      tenthLesseeAnnualAmount,
      totalAmountOfIncomeAndReceipts,
      wereAnyDisposalOnThisDuringTheYear,
      totalSaleProceedIfAnyDisposal,
      firstPurchaserName,
      firstPurchaserConnectedOrUnconnected,
      secondPurchaserName,
      secondPurchaserConnectedOrUnconnected,
      thirdPurchaserName,
      thirdPurchaserConnectedOrUnconnected,
      fourthPurchaserName,
      fourthPurchaserConnectedOrUnconnected,
      fifthPurchaserName,
      fifthPurchaserConnectedOrUnconnected,
      sixthPurchaserName,
      sixthPurchaserConnectedOrUnconnected,
      seventhPurchaserName,
      seventhPurchaserConnectedOrUnconnected,
      eighthPurchaserName,
      eighthPurchaserConnectedOrUnconnected,
      ninthPurchaserName,
      ninthPurchaserConnectedOrUnconnected,
      tenthPurchaserName,
      tenthPurchaserConnectedOrUnconnected,
      isTransactionSupportedByIndependentValuation,
      hasLandOrPropertyFullyDisposedOf
    )
}
