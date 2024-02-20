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
import cats.data.ValidatedNel
import cats.implicits._
import models._
import models.requests.{LandConnectedProperty, YesNo}
import models.requests.common.{AddressDetails, RegistryDetails}
import play.api.i18n.Messages

import java.time.LocalDate
import java.util.concurrent.atomic.AtomicInteger
import javax.inject.Inject
import scala.concurrent.{ExecutionContext, Future}

class InterestLandOrPropertyUploadValidator @Inject()(
  validations: InterestLandOrPropertyValidationsService
)(implicit ec: ExecutionContext) extends Validator {
  private val firstRowSink: Sink[List[ByteString], Future[List[String]]] =
    Sink.head[List[ByteString]].mapMaterializedValue(_.map(_.map(_.utf8String)))

  private val csvFrame: Flow[ByteString, List[ByteString], NotUsed] = {
    CsvParsing.lineScanner()
  }

  private val fileFormatError = UploadFormatError(
    ValidationError(
      "File Format error",
      ValidationErrorType.Formatting,
      "Invalid file format, please format file as per provided template"
    )
  )

  def validateCSV(
    source: Source[ByteString, _],
    validDateThreshold: Option[LocalDate]
  )(implicit mat: Materializer, messages: Messages): Future[(Upload, Int, Long)] = {
    val startTime = System.currentTimeMillis
    val counter = new AtomicInteger()
    val csvFrames = source.via(csvFrame)
    (for {
      csvHeader <- csvFrames.runWith(firstRowSink)
      header = csvHeader.zipWithIndex
        .map { case (key, index) => CsvHeaderKey(key, indexToCsvKey(index), index)}
      validated <- csvFrames
        .drop(2) // drop csv header and process rows
        .statefulMap[UploadState, Upload](() => UploadState.init)(
          (state, bs) => {
            counter.incrementAndGet()
            val parts = bs.map(_.utf8String)

            validateInterestLandOrProperty(
              header.map(h => h.copy(key = h.key.trim)),
              parts.drop(0),
              state.row,
              validDateThreshold: Option[LocalDate]
            ) match {
              case None => state.next() -> fileFormatError
              case Some(Valid(landConnectedProperty)) =>
                state.next() -> UploadSuccessForLandConnectedProperty(List(landConnectedProperty))
              case Some(Invalid(errs)) => state.next() -> UploadErrors(errs)
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
          case (UploadErrors(previous), UploadErrors(errs)) => UploadErrors(previous ++ errs.toList)
          case (errs: UploadErrors, _) => errs
          case (_, errs: UploadErrors) => errs
          // success
          case (previous: UploadSuccessForLandConnectedProperty, current: UploadSuccessForLandConnectedProperty) =>
            UploadSuccessForLandConnectedProperty(previous.interestLandOrProperty ++ current.interestLandOrProperty)
          case (_, interestLandOrProperty: UploadSuccessForLandConnectedProperty) => interestLandOrProperty
        }
    } yield (validated, counter.get(), System.currentTimeMillis - startTime))
      .recover {
        case _: NoSuchElementException =>
          (fileFormatError, 0, System.currentTimeMillis - startTime)
      }
  }

  private def validateInterestLandOrProperty(
    headerKeys: List[CsvHeaderKey],
    csvData: List[String],
    row: Int,
    validDateThreshold: Option[LocalDate]
  )(implicit messages: Messages): Option[ValidatedNel[ValidationError, LandConnectedProperty.TransactionDetail]] =
    for {
      firstNameOfSchemeMember                       /*  B */ <- getCSVValue(UploadKeys.firstNameOfSchemeMember, headerKeys, csvData)
      lastNameOfSchemeMember                        /*  C */ <- getCSVValue(UploadKeys.lastNameOfSchemeMember, headerKeys, csvData)
      memberDateOfBirth                             /*  D */ <- getCSVValue(UploadKeys.memberDateOfBirth, headerKeys, csvData)
      countOfLandOrPropertyTransactions             /*  E */ <- getCSVValue(UploadKeys.countOfLandOrPropertyTransactions, headerKeys, csvData)
      acquisitionDate                               /*  F */ <- getCSVValue(UploadKeys.acquisitionDate, headerKeys, csvData)

      isLandOrPropertyInUK                          /*  G */ <- getCSVValue(UploadKeys.isLandOrPropertyInUK, headerKeys, csvData)
      landOrPropertyUkAddressLine1                  /*  H */ <- getOptionalCSVValue(UploadKeys.landOrPropertyUkAddressLine1, headerKeys, csvData)
      landOrPropertyUkAddressLine2                  /*  I */ <- getOptionalCSVValue(UploadKeys.landOrPropertyUkAddressLine2, headerKeys, csvData)
      landOrPropertyUkAddressLine3                  /*  J */ <- getOptionalCSVValue(UploadKeys.landOrPropertyUkAddressLine3, headerKeys, csvData)
      landOrPropertyUkTownOrCity                    /*  K */ <- getOptionalCSVValue(UploadKeys.landOrPropertyUkTownOrCity, headerKeys, csvData)
      landOrPropertyUkPostCode                      /*  L */ <- getOptionalCSVValue(UploadKeys.landOrPropertyUkPostCode, headerKeys, csvData)
      landOrPropertyAddressLine1                    /*  M */ <- getOptionalCSVValue(UploadKeys.landOrPropertyAddressLine1, headerKeys, csvData)
      landOrPropertyAddressLine2                    /*  N */ <- getOptionalCSVValue(UploadKeys.landOrPropertyAddressLine2, headerKeys, csvData)
      landOrPropertyAddressLine3                    /*  O */ <- getOptionalCSVValue(UploadKeys.landOrPropertyAddressLine3, headerKeys, csvData)
      landOrPropertyAddressLine4                    /*  P */ <- getOptionalCSVValue(UploadKeys.landOrPropertyAddressLine4, headerKeys, csvData)
      landOrPropertyCountry                         /*  Q */ <- getOptionalCSVValue(UploadKeys.landOrPropertyCountry, headerKeys, csvData)

      isThereLandRegistryReference                  /*  R */ <- getCSVValue(UploadKeys.isThereLandRegistryReference, headerKeys, csvData)
      noLandRegistryReference                       /*  S */ <- getOptionalCSVValue(UploadKeys.noLandRegistryReference, headerKeys, csvData)

      acquiredFromType                              /*  T */ <- getCSVValue(UploadKeys.acquiredFromType, headerKeys, csvData)
      acquirerNinoForIndividual                     /*  U */ <- getOptionalCSVValue(UploadKeys.acquirerNinoForIndividual, headerKeys, csvData)
      acquirerCrnForCompany                         /*  V */ <- getOptionalCSVValue(UploadKeys.acquirerCrnForCompany, headerKeys, csvData)
      acquirerUtrForPartnership                     /*  W */ <- getOptionalCSVValue(UploadKeys.acquirerUtrForPartnership, headerKeys, csvData)
      noIdOrAcquiredFromAnotherSource               /*  X */ <- getOptionalCSVValue(UploadKeys.noIdOrAcquiredFromAnotherSource, headerKeys, csvData)

      totalCostOfLandOrPropertyAcquired             /*  Y */ <- getCSVValue(UploadKeys.totalCostOfLandOrPropertyAcquired, headerKeys, csvData)
      isSupportedByAnIndependentValuation           /*  Z */ <- getCSVValue(UploadKeys.isSupportedByAnIndependentValuation, headerKeys, csvData)

      isPropertyHeldJointly                         /* AA */ <- getCSVValue(UploadKeys.isPropertyHeldJointly, headerKeys, csvData)
      howManyPersonsJointlyOwnProperty              /* AB */ <- getOptionalCSVValue(UploadKeys.howManyPersonsJointlyOwnProperty, headerKeys, csvData)
      firstPersonNameJointlyOwning                  /* AC */ <- getOptionalCSVValue(UploadKeys.firstPersonNameJointlyOwning   , headerKeys, csvData)
      firstPersonNinoJointlyOwning                  /* AD */ <- getOptionalCSVValue(UploadKeys.firstPersonNinoJointlyOwning   , headerKeys, csvData)
      firstPersonNoNinoJointlyOwning                /* AE */ <- getOptionalCSVValue(UploadKeys.firstPersonNoNinoJointlyOwning , headerKeys, csvData)
      secondPersonNameJointlyOwning                 /* AF */ <- getOptionalCSVValue(UploadKeys.secondPersonNameJointlyOwning  , headerKeys, csvData)
      secondPersonNinoJointlyOwning                 /* AG */ <- getOptionalCSVValue(UploadKeys.secondPersonNinoJointlyOwning  , headerKeys, csvData)
      secondPersonNoNinoJointlyOwning               /* AH */ <- getOptionalCSVValue(UploadKeys.secondPersonNoNinoJointlyOwning, headerKeys, csvData)
      thirdPersonNameJointlyOwning                  /* AI */ <- getOptionalCSVValue(UploadKeys.thirdPersonNameJointlyOwning   , headerKeys, csvData)
      thirdPersonNinoJointlyOwning                  /* AJ */ <- getOptionalCSVValue(UploadKeys.thirdPersonNinoJointlyOwning   , headerKeys, csvData)
      thirdPersonNoNinoJointlyOwning                /* AK */ <- getOptionalCSVValue(UploadKeys.thirdPersonNoNinoJointlyOwning , headerKeys, csvData)
      fourthPersonNameJointlyOwning                 /* AL */ <- getOptionalCSVValue(UploadKeys.fourthPersonNameJointlyOwning  , headerKeys, csvData)
      fourthPersonNinoJointlyOwning                 /* AM */ <- getOptionalCSVValue(UploadKeys.fourthPersonNinoJointlyOwning  , headerKeys, csvData)
      fourthPersonNoNinoJointlyOwning               /* AN */ <- getOptionalCSVValue(UploadKeys.fourthPersonNoNinoJointlyOwning, headerKeys, csvData)
      fifthPersonNameJointlyOwning                  /* AO */ <- getOptionalCSVValue(UploadKeys.fifthPersonNameJointlyOwning   , headerKeys, csvData)
      fifthPersonNinoJointlyOwning                  /* AP */ <- getOptionalCSVValue(UploadKeys.fifthPersonNinoJointlyOwning   , headerKeys, csvData)
      fifthPersonNoNinoJointlyOwning                /* AQ */ <- getOptionalCSVValue(UploadKeys.fifthPersonNoNinoJointlyOwning , headerKeys, csvData)

      isPropertyDefinedAsSchedule29a                /* AR */ <- getCSVValue(UploadKeys.isPropertyDefinedAsSchedule29a, headerKeys, csvData)

      isLeased                                      /* AS */ <- getCSVValue(UploadKeys.isLeased, headerKeys, csvData)
      firstLesseeName                               /* AT */ <- getOptionalCSVValue(UploadKeys.firstLesseeName, headerKeys, csvData)
      firstLesseeConnectedOrUnconnected             /* AU */ <- getOptionalCSVValue(UploadKeys.firstLesseeConnectedOrUnconnected, headerKeys, csvData)
      firstLesseeGrantedDate                        /* AV */ <- getOptionalCSVValue(UploadKeys.firstLesseeGrantedDate, headerKeys, csvData)
      firstLesseeAnnualAmount                       /* AW */ <- getOptionalCSVValue(UploadKeys.firstLesseeAnnualAmount, headerKeys, csvData)
      secondLesseeName                              /* AX */ <- getOptionalCSVValue(UploadKeys.secondLesseeName, headerKeys, csvData)
      secondLesseeConnectedOrUnconnected            /* AY */ <- getOptionalCSVValue(UploadKeys.secondLesseeConnectedOrUnconnected, headerKeys, csvData)
      secondLesseeGrantedDate                       /* AZ */ <- getOptionalCSVValue(UploadKeys.secondLesseeGrantedDate, headerKeys, csvData)
      secondLesseeAnnualAmount                      /* BA */ <- getOptionalCSVValue(UploadKeys.secondLesseeAnnualAmount, headerKeys, csvData)
      thirdLesseeName                               /* BB */ <- getOptionalCSVValue(UploadKeys.thirdLesseeName, headerKeys, csvData)
      thirdLesseeConnectedOrUnconnected             /* BC */ <- getOptionalCSVValue(UploadKeys.thirdLesseeConnectedOrUnconnected, headerKeys, csvData)
      thirdLesseeGrantedDate                        /* BD */ <- getOptionalCSVValue(UploadKeys.thirdLesseeGrantedDate, headerKeys, csvData)
      thirdLesseeAnnualAmount                       /* BE */ <- getOptionalCSVValue(UploadKeys.thirdLesseeAnnualAmount, headerKeys, csvData)
      fourthLesseeName                              /* BF */ <- getOptionalCSVValue(UploadKeys.fourthLesseeName, headerKeys, csvData)
      fourthLesseeConnectedOrUnconnected            /* BG */ <- getOptionalCSVValue(UploadKeys.fourthLesseeConnectedOrUnconnected, headerKeys, csvData)
      fourthLesseeGrantedDate                       /* BH */ <- getOptionalCSVValue(UploadKeys.fourthLesseeGrantedDate, headerKeys, csvData)
      fourthLesseeAnnualAmount                      /* BI */ <- getOptionalCSVValue(UploadKeys.fourthLesseeAnnualAmount, headerKeys, csvData)
      fifthLesseeName                               /* BJ */ <- getOptionalCSVValue(UploadKeys.fifthLesseeName, headerKeys, csvData)
      fifthLesseeConnectedOrUnconnected             /* BK */ <- getOptionalCSVValue(UploadKeys.fifthLesseeConnectedOrUnconnected, headerKeys, csvData)
      fifthLesseeGrantedDate                        /* BL */ <- getOptionalCSVValue(UploadKeys.fifthLesseeGrantedDate, headerKeys, csvData)
      fifthLesseeAnnualAmount                       /* BM */ <- getOptionalCSVValue(UploadKeys.fifthLesseeAnnualAmount, headerKeys, csvData)
      sixthLesseeName                               /* BN */ <- getOptionalCSVValue(UploadKeys.sixthLesseeName, headerKeys, csvData)
      sixthLesseeConnectedOrUnconnected             /* BO */ <- getOptionalCSVValue(UploadKeys.sixthLesseeConnectedOrUnconnected, headerKeys, csvData)
      sixthLesseeGrantedDate                        /* BP */ <- getOptionalCSVValue(UploadKeys.sixthLesseeGrantedDate, headerKeys, csvData)
      sixthLesseeAnnualAmount                       /* BQ */ <- getOptionalCSVValue(UploadKeys.sixthLesseeAnnualAmount, headerKeys, csvData)
      seventhLesseeName                             /* BR */ <- getOptionalCSVValue(UploadKeys.seventhLesseeName, headerKeys, csvData)
      seventhLesseeConnectedOrUnconnected           /* BS */ <- getOptionalCSVValue(UploadKeys.seventhLesseeConnectedOrUnconnected, headerKeys, csvData)
      seventhLesseeGrantedDate                      /* BT */ <- getOptionalCSVValue(UploadKeys.seventhLesseeGrantedDate, headerKeys, csvData)
      seventhLesseeAnnualAmount                     /* BU */ <- getOptionalCSVValue(UploadKeys.seventhLesseeAnnualAmount, headerKeys, csvData)
      eighthLesseeName                              /* BV */ <- getOptionalCSVValue(UploadKeys.eighthLesseeName, headerKeys, csvData)
      eighthLesseeConnectedOrUnconnected            /* BW */ <- getOptionalCSVValue(UploadKeys.eighthLesseeConnectedOrUnconnected, headerKeys, csvData)
      eighthLesseeGrantedDate                       /* BX */ <- getOptionalCSVValue(UploadKeys.eighthLesseeGrantedDate, headerKeys, csvData)
      eighthLesseeAnnualAmount                      /* BY */ <- getOptionalCSVValue(UploadKeys.eighthLesseeAnnualAmount, headerKeys, csvData)
      ninthLesseeName                               /* BZ */ <- getOptionalCSVValue(UploadKeys.ninthLesseeName, headerKeys, csvData)
      ninthLesseeConnectedOrUnconnected             /* CA */ <- getOptionalCSVValue(UploadKeys.ninthLesseeConnectedOrUnconnected, headerKeys, csvData)
      ninthLesseeGrantedDate                        /* CB */ <- getOptionalCSVValue(UploadKeys.ninthLesseeGrantedDate, headerKeys, csvData)
      ninthLesseeAnnualAmount                       /* CC */ <- getOptionalCSVValue(UploadKeys.ninthLesseeAnnualAmount, headerKeys, csvData)
      tenthLesseeName                               /* CD */ <- getOptionalCSVValue(UploadKeys.tenthLesseeName, headerKeys, csvData)
      tenthLesseeConnectedOrUnconnected             /* CE */ <- getOptionalCSVValue(UploadKeys.tenthLesseeConnectedOrUnconnected, headerKeys, csvData)
      tenthLesseeGrantedDate                        /* CF */ <- getOptionalCSVValue(UploadKeys.tenthLesseeGrantedDate, headerKeys, csvData)
      tenthLesseeAnnualAmount                       /* CG */ <- getOptionalCSVValue(UploadKeys.tenthLesseeAnnualAmount, headerKeys, csvData)

      totalAmountOfIncomeAndReceipts                /* CH */ <- getCSVValue(UploadKeys.totalAmountOfIncomeAndReceipts, headerKeys, csvData)

      wereAnyDisposalOnThisDuringTheYear            /* CI */ <- getCSVValue(UploadKeys.wereAnyDisposalOnThisDuringTheYear, headerKeys, csvData)
      totalSaleProceedIfAnyDisposal                 /* CJ */ <- getOptionalCSVValue(UploadKeys.totalSaleProceedIfAnyDisposal, headerKeys, csvData)
      firstPurchaserName                            /* CK */ <- getOptionalCSVValue(UploadKeys.firstPurchaserName, headerKeys, csvData)
      firstPurchaserConnectedOrUnconnected          /* CL */ <- getOptionalCSVValue(UploadKeys.firstPurchaserConnectedOrUnconnected, headerKeys, csvData)
      secondPurchaserName                           /* CM */ <- getOptionalCSVValue(UploadKeys.secondPurchaserName, headerKeys, csvData)
      secondPurchaserConnectedOrUnconnected         /* CN */ <- getOptionalCSVValue(UploadKeys.secondPurchaserConnectedOrUnconnected, headerKeys, csvData)
      thirdPurchaserName                            /* CO */ <- getOptionalCSVValue(UploadKeys.thirdPurchaserName, headerKeys, csvData)
      thirdPurchaserConnectedOrUnconnected          /* CP */ <- getOptionalCSVValue(UploadKeys.thirdPurchaserConnectedOrUnconnected, headerKeys, csvData)
      fourthPurchaserName                           /* CQ */ <- getOptionalCSVValue(UploadKeys.fourthPurchaserName, headerKeys, csvData)
      fourthPurchaserConnectedOrUnconnected         /* CR */ <- getOptionalCSVValue(UploadKeys.fourthPurchaserConnectedOrUnconnected, headerKeys, csvData)
      fifthPurchaserName                            /* CS */ <- getOptionalCSVValue(UploadKeys.fifthPurchaserName, headerKeys, csvData)
      fifthPurchaserConnectedOrUnconnected          /* CT */ <- getOptionalCSVValue(UploadKeys.fifthPurchaserConnectedOrUnconnected, headerKeys, csvData)
      sixthPurchaserName                            /* CU */ <- getOptionalCSVValue(UploadKeys.sixthPurchaserName, headerKeys, csvData)
      sixthPurchaserConnectedOrUnconnected          /* CV */ <- getOptionalCSVValue(UploadKeys.sixthPurchaserConnectedOrUnconnected, headerKeys, csvData)
      seventhPurchaserName                          /* CW */ <- getOptionalCSVValue(UploadKeys.seventhPurchaserName, headerKeys, csvData)
      seventhPurchaserConnectedOrUnconnected        /* CX */ <- getOptionalCSVValue(UploadKeys.seventhPurchaserConnectedOrUnconnected, headerKeys, csvData)
      eighthPurchaserName                           /* CY */ <- getOptionalCSVValue(UploadKeys.eighthPurchaserName, headerKeys, csvData)
      eighthPurchaserConnectedOrUnconnected         /* CZ */ <- getOptionalCSVValue(UploadKeys.eighthPurchaserConnectedOrUnconnected, headerKeys, csvData)
      ninthPurchaserName                            /* DA */ <- getOptionalCSVValue(UploadKeys.ninthPurchaserName, headerKeys, csvData)
      ninthPurchaserConnectedOrUnconnected          /* DB */ <- getOptionalCSVValue(UploadKeys.ninthPurchaserConnectedOrUnconnected, headerKeys, csvData)
      tenthPurchaserName                            /* DC */ <- getOptionalCSVValue(UploadKeys.tenthPurchaserName, headerKeys, csvData)
      tenthPurchaserConnectedOrUnconnected          /* DD */ <- getOptionalCSVValue(UploadKeys.tenthPurchaserConnectedOrUnconnected, headerKeys, csvData)
      isTransactionSupportedByIndependentValuation  /* DE */ <- getOptionalCSVValue(UploadKeys.isTransactionSupportedByIndependentValuation, headerKeys, csvData)
      hasLandOrPropertyFullyDisposedOf              /* DF */ <- getOptionalCSVValue(UploadKeys.hasLandOrPropertyFullyDisposedOf, headerKeys, csvData)

      memberFullNameDob = s"${firstNameOfSchemeMember.value} ${lastNameOfSchemeMember.value} ${memberDateOfBirth.value}"

      // Validations
      validatedNameDOB <- validations.validateNameDOB(
        firstName = firstNameOfSchemeMember,
        lastName = lastNameOfSchemeMember,
        dob = memberDateOfBirth,
        row = row,
        validDateThreshold = validDateThreshold
      )

      validatedAcquisitionDate <- validations.validateDate(
        date = acquisitionDate,
        key = "acquisitionDate",
        row = row,
        validDateThreshold = validDateThreshold
      )

      validatedAddress <- validations.validateUKOrROWAddress(
        isUKAddress = isLandOrPropertyInUK,
        ukAddressLine1 = landOrPropertyUkAddressLine1,
        ukAddressLine2 = landOrPropertyUkAddressLine2,
        ukAddressLine3 = landOrPropertyUkAddressLine3,
        ukTownOrCity = landOrPropertyUkTownOrCity,
        ukPostcode = landOrPropertyUkPostCode,
        addressLine1 = landOrPropertyAddressLine1,
        addressLine2 = landOrPropertyAddressLine2,
        addressLine3 = landOrPropertyAddressLine3,
        addressLine4 = landOrPropertyAddressLine4,
        country = landOrPropertyCountry,
        memberFullNameDob = memberFullNameDob,
        row
      )

      validatedIsThereARegistryReference <- validations.validateIsThereARegistryReference(
        isThereLandRegistryReference,
        noLandRegistryReference,
        memberFullNameDob,
        row
      )

      validatedAcquiredFrom <- validations.validateAcquiredFrom(
        acquiredFromType,
        acquirerNinoForIndividual,
        acquirerCrnForCompany,
        acquirerUtrForPartnership,
        noIdOrAcquiredFromAnotherSource,
        memberFullNameDob,
        row
      )

      validatedTotalCostOfLandOrPropertyAcquired <- validations.validatePrice(
        totalCostOfLandOrPropertyAcquired,
        "TotalCostOfLandOrPropertyAcquired",
        memberFullNameDob,
        row
      )

      validatedIsSupportedByAnIndependentValuation <- validations.validateYesNoQuestion(
        isSupportedByAnIndependentValuation,
        "isSupportedByAnIndependentValuation",
        memberFullNameDob,
        row
      )

      validatedJointlyHeld <- validations.validateJointlyHeldAll(
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
        memberFullNameDob,
        row
      )

      validatedIsPropertyDefinedAsSchedule29a <- validations.validateYesNoQuestion(
        isPropertyDefinedAsSchedule29a,
        "landOrProperty.isPropertyDefinedAsSchedule29a",
        memberFullNameDob,
        row,
      )

      validatedLessees <- validations.validateLeasedAll(
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
        memberFullNameDob,
        row
      )

      validatedTotalIncome <- validations.validatePrice(
        totalAmountOfIncomeAndReceipts,
        "totalAmountOfIncomeAndReceipts",
        memberFullNameDob,
        row
      )

      validatedDisposals <- validations.validateDisposals(
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
        hasLandOrPropertyFullyDisposedOf,
        memberFullNameDob,
        row
      )

    } yield (
      validatedNameDOB,
      validatedAcquisitionDate,
      validatedAddress,
      validatedIsThereARegistryReference,
      validatedAcquiredFrom,
      validatedTotalCostOfLandOrPropertyAcquired,
      validatedIsSupportedByAnIndependentValuation,
      validatedJointlyHeld,
      validatedIsPropertyDefinedAsSchedule29a,
      validatedLessees,
      validatedTotalIncome,
      validatedDisposals
      ).mapN (
      (nameDob,
       acquisitionDate,
       address,
       registryReferenceDetails,
       acquiredFromDetails,
       totalCostOfLandOrPropertyAcquired,
       isSupportedByAnIndependentValuation,
       jointlyHeld,
       isPropertyDefinedAsSchedule29a,
       lessees,
       totalIncome,
       disposals
      ) => {
         val addressDetails = AddressDetails.uploadAddressToRequestAddressDetails(address)
        LandConnectedProperty.TransactionDetail(
          nameDOB = nameDob,
          acquisitionDate = acquisitionDate,
          landOrPropertyinUK = addressDetails._1,
          addressDetails = addressDetails._2,
          registryDetails = registryReferenceDetails,
          acquiredFromName = "Dummy", //TODO There is an acquiredFrom and it is required!!
          acquiredFromType = acquiredFromDetails,
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
}
