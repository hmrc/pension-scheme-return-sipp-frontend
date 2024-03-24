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
import models._
import models.requests.raw.TangibleMoveablePropertyRaw.RawTransactionDetail
import models.requests.raw.TangibleMoveablePropertyUpload.{fromRaw, TangibleMoveablePropertyUpload}
import play.api.i18n.Messages

import java.time.LocalDate
import java.util.concurrent.atomic.AtomicInteger
import javax.inject.Inject
import scala.concurrent.{ExecutionContext, Future}

class TangibleMoveableUploadValidator @Inject()(validations: TangibleMoveablePropertyValidationsService)(
  implicit ec: ExecutionContext,
  materializer: Materializer
) extends Validator
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
              state.row,
              parts.drop(0),
              header.map(h => h.copy(key = h.key.trim)),
              validDateThreshold: Option[LocalDate]
            ) match {
              case None => state.next() -> fileFormatError
              case Some((raw, Valid(tangibleProperty))) =>
                state.next() -> UploadSuccessTangibleMoveableProperty(List(raw), List(tangibleProperty))
              case Some((raw, Invalid(errs))) =>
                state.next() -> UploadErrorsTangibleMoveableProperty(NonEmptyList.one(raw), errs)
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
              UploadErrorsTangibleMoveableProperty(rawPrev, previous),
              UploadErrorsTangibleMoveableProperty(raw, errs)
              ) =>
            UploadErrorsTangibleMoveableProperty(rawPrev ::: raw, previous ++ errs.toList)
          case (
              UploadErrorsTangibleMoveableProperty(rawPrev, err),
              UploadSuccessTangibleMoveableProperty(detailsRaw, _)
              ) =>
            UploadErrorsTangibleMoveableProperty(rawPrev ++ detailsRaw, err)
          case (
              UploadSuccessTangibleMoveableProperty(detailsPrevRaw, _),
              UploadErrorsTangibleMoveableProperty(raw, err)
              ) =>
            UploadErrorsTangibleMoveableProperty(NonEmptyList.fromListUnsafe(detailsPrevRaw) ::: raw, err)
          // success
          case (previous: UploadSuccessTangibleMoveableProperty, current: UploadSuccessTangibleMoveableProperty) =>
            UploadSuccessTangibleMoveableProperty(
              previous.tangibleMoveablePropertyRaw ++ current.tangibleMoveablePropertyRaw,
              previous.rows ++ current.rows
            )
          case (_, success: UploadSuccessTangibleMoveableProperty) => success
        }
    } yield (validated, counter.get(), System.currentTimeMillis - startTime))
      .recover {
        case _: NoSuchElementException =>
          (fileFormatError, 0, System.currentTimeMillis - startTime)
      }
  }

  private def validate(
    row: Int,
    data: List[String],
    headerKeys: List[CsvHeaderKey],
    validDateThreshold: Option[LocalDate]
  )(
    implicit messages: Messages
  ): Option[(RawTransactionDetail, ValidatedNel[ValidationError, TangibleMoveablePropertyUpload])] = {
    (for {
      raw <- readCSV(row, headerKeys, data)
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
        raw.countOfTangiblePropertyTransactions,
        key = "tangibleMoveableProperty.transactionCount",
        memberFullName = memberFullNameDob,
        row = row,
        maxCount = 50
      )

      /*  F */
      validateDescriptionOfAsset <- validations.validateFreeText(
        raw.rawAsset.descriptionOfAsset,
        "tangibleMoveableProperty.descriptionOfAsset",
        memberFullNameDob,
        row
      )

      /*  G */
      validatedDateOfAcquisitionAsset <- validations.validateDate(
        date = raw.rawAsset.dateOfAcquisitionAsset,
        key = "tangibleMoveableProperty.dateOfAcquisitionAsset",
        row = row,
        validDateThreshold = validDateThreshold
      )

      /*  H */
      validatedTotalCostAsset <- validations.validatePrice(
        raw.rawAsset.totalCostAsset,
        "tangibleMoveableProperty.totalCostAsset",
        memberFullNameDob,
        row
      )

      /*  I ... N */
      validatedWhoAcquiredFromName <- validations.validateFreeText(
        raw.rawAsset.rawAcquiredFromType.whoAcquiredFromName,
        "tangibleMoveableProperty.whoAcquiredFromName",
        memberFullNameDob,
        row
      )

      validatedAcquiredFromType <- validations.validateAcquiredFrom(
        raw.rawAsset.rawAcquiredFromType.acquiredFromType,
        raw.rawAsset.rawAcquiredFromType.acquirerNinoForIndividual,
        raw.rawAsset.rawAcquiredFromType.acquirerCrnForCompany,
        raw.rawAsset.rawAcquiredFromType.acquirerUtrForPartnership,
        raw.rawAsset.rawAcquiredFromType.whoAcquiredFromTypeReasonAsset,
        memberFullNameDob,
        row
      )

      /*  O */
      validatedIsTxSupportedByIndependentValuation <- validations.validateYesNoQuestion(
        raw.rawAsset.isTxSupportedByIndependentValuation,
        "tangibleMoveableProperty.isTxSupportedByIndependentValuation",
        memberFullNameDob,
        row
      )

      /*  P */
      validatedTotalAmountIncomeReceiptsTaxYear <- validations.validatePrice(
        raw.rawAsset.isTxSupportedByIndependentValuation,
        "tangibleMoveableProperty.totalAmountIncomeReceiptsTaxYear",
        memberFullNameDob,
        row
      )

      /*  Q */
      validatedIsTotalCostValueOrMarketValue <- validations.validatePrice(
        raw.rawAsset.isTotalCostValueOrMarketValue,
        "tangibleMoveableProperty.isTotalCostValueOrMarketValue",
        memberFullNameDob,
        row
      )

      /*  R */
      validatedTotalCostValueTaxYearAsset <- validations.validatePrice(
        raw.rawAsset.totalCostValueTaxYearAsset,
        "tangibleMoveableProperty.totalCostValueTaxYearAsset",
        memberFullNameDob,
        row
      )

      validatedDisposals <- validations.validateDisposals(
        raw.rawAsset.rawDisposal.wereAnyDisposalOnThisDuringTheYear,
        raw.rawAsset.rawDisposal.totalConsiderationAmountSaleIfAnyDisposal,
        raw.rawAsset.rawDisposal.wereAnyDisposals,
        List(
          (raw.rawAsset.rawDisposal.first.name, raw.rawAsset.rawDisposal.first.connection),
          (raw.rawAsset.rawDisposal.second.name, raw.rawAsset.rawDisposal.second.connection),
          (raw.rawAsset.rawDisposal.third.name, raw.rawAsset.rawDisposal.third.connection),
          (raw.rawAsset.rawDisposal.fourth.name, raw.rawAsset.rawDisposal.fourth.connection),
          (raw.rawAsset.rawDisposal.fifth.name, raw.rawAsset.rawDisposal.fifth.connection),
          (raw.rawAsset.rawDisposal.sixth.name, raw.rawAsset.rawDisposal.sixth.connection),
          (raw.rawAsset.rawDisposal.seventh.name, raw.rawAsset.rawDisposal.seventh.connection),
          (raw.rawAsset.rawDisposal.eighth.name, raw.rawAsset.rawDisposal.eighth.connection),
          (raw.rawAsset.rawDisposal.ninth.name, raw.rawAsset.rawDisposal.ninth.connection),
          (raw.rawAsset.rawDisposal.tenth.name, raw.rawAsset.rawDisposal.tenth.connection)
        ),
        raw.rawAsset.rawDisposal.isTransactionSupportedByIndependentValuation,
        raw.rawAsset.rawDisposal.isAnyPartAssetStillHeld,
        memberFullNameDob,
        row
      )

    } yield (
      raw,
      (
        validatedNameDOB,
        validatePropertyCount,
        validateDescriptionOfAsset,
        validatedDateOfAcquisitionAsset,
        validatedTotalCostAsset,
        validatedWhoAcquiredFromName,
        validatedAcquiredFromType,
        validatedIsTxSupportedByIndependentValuation,
        validatedTotalAmountIncomeReceiptsTaxYear,
        validatedTotalCostValueTaxYearAsset,
        validatedIsTotalCostValueOrMarketValue,
        validatedDisposals
      ).mapN(
        (
          _,
          _,
          _,
          _,
          _,
          _,
          _,
          _,
          _,
          _,
          _,
          _
        ) => {
          fromRaw(raw)
        }
      )
    ))
  }

  private def readCSV(
    row: Int,
    headerKeys: List[CsvHeaderKey],
    csvData: List[String]
  ): Option[RawTransactionDetail] =
    for {
      /*  B */
      firstNameOfSchemeMemberTangible <- getCSVValue(UploadKeys.firstNameOfSchemeMemberTangible, headerKeys, csvData)
      /*  C */
      lastNameOfSchemeMemberTangible <- getCSVValue(UploadKeys.lastNameOfSchemeMemberTangible, headerKeys, csvData)
      /*  D */
      memberDateOfBirthTangible <- getCSVValue(UploadKeys.memberDateOfBirthTangible, headerKeys, csvData)
      /*  E */
      countOfTangiblePropertyTransactions <- getCSVValue(
        UploadKeys.countOfTangiblePropertyTransactions,
        headerKeys,
        csvData
      )
      /*  F */
      descriptionOfAssetTangible <- getCSVValue(UploadKeys.descriptionOfAssetTangible, headerKeys, csvData)
      /*  G */
      dateOfAcquisitionTangible <- getCSVValue(UploadKeys.dateOfAcquisitionTangible, headerKeys, csvData)
      /*  H */
      totalCostOfAssetTangible <- getCSVValue(UploadKeys.totalCostOfAssetTangible, headerKeys, csvData)
      /*  I */
      acquiredFromTangible <- getCSVValue(UploadKeys.acquiredFromTangible, headerKeys, csvData)
      /*  J */
      acquiredFromTypeTangible <- getCSVValue(UploadKeys.acquiredFromTypeTangible, headerKeys, csvData)
      /*  K */
      acquiredFromIndividualTangible <- getOptionalCSVValue(
        UploadKeys.acquiredFromIndividualTangible,
        headerKeys,
        csvData
      )
      /*  L */
      acquiredFromCompanyTangible <- getOptionalCSVValue(UploadKeys.acquiredFromCompanyTangible, headerKeys, csvData)
      /*  M */
      acquiredFromPartnershipTangible <- getOptionalCSVValue(
        UploadKeys.acquiredFromPartnershipTangible,
        headerKeys,
        csvData
      )
      /*  N */
      acquiredFromReasonTangible <- getOptionalCSVValue(UploadKeys.acquiredFromReasonTangible, headerKeys, csvData)
      /*  O */
      isIndependentEvaluationTangible <- getCSVValue(UploadKeys.isIndependentEvaluationTangible, headerKeys, csvData)
      /*  P */
      totalIncomeInTaxYearTangible <- getCSVValue(UploadKeys.totalIncomeInTaxYearTangible, headerKeys, csvData)
      /*  Q */
      isTotalCostOrMarketValueTangible <- getCSVValue(UploadKeys.isTotalCostOrMarketValueTangible, headerKeys, csvData)
      /*  R */
      totalCostOrMarketValueTangible <- getCSVValue(UploadKeys.totalCostOrMarketValueTangible, headerKeys, csvData)
      /*  S */
      areAnyDisposalsTangible <- getCSVValue(UploadKeys.areAnyDisposalsTangible, headerKeys, csvData)
      /*  T */
      disposalsAmountTangible <- getOptionalCSVValue(UploadKeys.disposalsAmountTangible, headerKeys, csvData)
      /*  U */
      areJustDisposalsTangible <- getCSVValue(UploadKeys.areJustDisposalsTangible, headerKeys, csvData)
      /*  V ... O */
      disposalNameOfPurchaser1Tangible <- getOptionalCSVValue(
        UploadKeys.disposalNameOfPurchaser1Tangible,
        headerKeys,
        csvData
      )
      disposalTypeOfPurchaser1Tangible <- getOptionalCSVValue(
        UploadKeys.disposalTypeOfPurchaser1Tangible,
        headerKeys,
        csvData
      )
      disposalNameOfPurchaser2Tangible <- getOptionalCSVValue(
        UploadKeys.disposalNameOfPurchaser2Tangible,
        headerKeys,
        csvData
      )
      disposalTypeOfPurchaser2Tangible <- getOptionalCSVValue(
        UploadKeys.disposalTypeOfPurchaser2Tangible,
        headerKeys,
        csvData
      )
      disposalNameOfPurchaser3Tangible <- getOptionalCSVValue(
        UploadKeys.disposalNameOfPurchaser3Tangible,
        headerKeys,
        csvData
      )
      disposalTypeOfPurchaser3Tangible <- getOptionalCSVValue(
        UploadKeys.disposalTypeOfPurchaser3Tangible,
        headerKeys,
        csvData
      )
      disposalNameOfPurchaser4Tangible <- getOptionalCSVValue(
        UploadKeys.disposalNameOfPurchaser4Tangible,
        headerKeys,
        csvData
      )
      disposalTypeOfPurchaser4Tangible <- getOptionalCSVValue(
        UploadKeys.disposalTypeOfPurchaser4Tangible,
        headerKeys,
        csvData
      )
      disposalNameOfPurchaser5Tangible <- getOptionalCSVValue(
        UploadKeys.disposalNameOfPurchaser5Tangible,
        headerKeys,
        csvData
      )
      disposalTypeOfPurchaser5Tangible <- getOptionalCSVValue(
        UploadKeys.disposalTypeOfPurchaser5Tangible,
        headerKeys,
        csvData
      )
      disposalNameOfPurchaser6Tangible <- getOptionalCSVValue(
        UploadKeys.disposalNameOfPurchaser6Tangible,
        headerKeys,
        csvData
      )
      disposalTypeOfPurchaser6Tangible <- getOptionalCSVValue(
        UploadKeys.disposalTypeOfPurchaser6Tangible,
        headerKeys,
        csvData
      )
      disposalNameOfPurchaser7Tangible <- getOptionalCSVValue(
        UploadKeys.disposalNameOfPurchaser7Tangible,
        headerKeys,
        csvData
      )
      disposalTypeOfPurchaser7Tangible <- getOptionalCSVValue(
        UploadKeys.disposalTypeOfPurchaser7Tangible,
        headerKeys,
        csvData
      )
      disposalNameOfPurchaser8Tangible <- getOptionalCSVValue(
        UploadKeys.disposalNameOfPurchaser8Tangible,
        headerKeys,
        csvData
      )
      disposalTypeOfPurchaser8Tangible <- getOptionalCSVValue(
        UploadKeys.disposalTypeOfPurchaser8Tangible,
        headerKeys,
        csvData
      )
      disposalNameOfPurchaser9Tangible <- getOptionalCSVValue(
        UploadKeys.disposalNameOfPurchaser9Tangible,
        headerKeys,
        csvData
      )
      disposalTypeOfPurchaser9Tangible <- getOptionalCSVValue(
        UploadKeys.disposalTypeOfPurchaser9Tangible,
        headerKeys,
        csvData
      )
      disposalNameOfPurchaser10Tangible <- getOptionalCSVValue(
        UploadKeys.disposalNameOfPurchaser10Tangible,
        headerKeys,
        csvData
      )
      disposalTypeOfPurchaser10Tangible <- getOptionalCSVValue(
        UploadKeys.disposalTypeOfPurchaser10Tangible,
        headerKeys,
        csvData
      )
      /*  AP */
      wasTransactionSupportedIndValuationTangible <- getOptionalCSVValue(
        UploadKeys.wasTransactionSupportedIndValuationTangible,
        headerKeys,
        csvData
      )
      /*  AQ */
      isAnyPartStillHeldTangible <- getOptionalCSVValue(UploadKeys.isAnyPartStillHeldTangible, headerKeys, csvData)
    } yield RawTransactionDetail.create(
      row,
      firstNameOfSchemeMemberTangible,
      lastNameOfSchemeMemberTangible,
      memberDateOfBirthTangible,
      countOfTangiblePropertyTransactions,
      descriptionOfAssetTangible,
      dateOfAcquisitionTangible,
      totalCostOfAssetTangible,
      acquiredFromTangible,
      acquiredFromTypeTangible,
      acquiredFromIndividualTangible,
      acquiredFromCompanyTangible,
      acquiredFromPartnershipTangible,
      acquiredFromReasonTangible,
      isIndependentEvaluationTangible,
      totalIncomeInTaxYearTangible,
      isTotalCostOrMarketValueTangible,
      totalCostOrMarketValueTangible,
      areAnyDisposalsTangible,
      disposalsAmountTangible,
      areJustDisposalsTangible,
      disposalNameOfPurchaser1Tangible,
      disposalTypeOfPurchaser1Tangible,
      disposalNameOfPurchaser2Tangible,
      disposalTypeOfPurchaser2Tangible,
      disposalNameOfPurchaser3Tangible,
      disposalTypeOfPurchaser3Tangible,
      disposalNameOfPurchaser4Tangible,
      disposalTypeOfPurchaser4Tangible,
      disposalNameOfPurchaser5Tangible,
      disposalTypeOfPurchaser5Tangible,
      disposalNameOfPurchaser6Tangible,
      disposalTypeOfPurchaser6Tangible,
      disposalNameOfPurchaser7Tangible,
      disposalTypeOfPurchaser7Tangible,
      disposalNameOfPurchaser8Tangible,
      disposalTypeOfPurchaser8Tangible,
      disposalNameOfPurchaser9Tangible,
      disposalTypeOfPurchaser9Tangible,
      disposalNameOfPurchaser10Tangible,
      disposalTypeOfPurchaser10Tangible,
      wasTransactionSupportedIndValuationTangible,
      isAnyPartStillHeldTangible
    )

}
