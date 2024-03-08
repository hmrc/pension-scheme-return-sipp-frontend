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
import play.api.i18n.Messages
import uk.gov.hmrc.domain.Nino

import java.time.LocalDate
import java.util.concurrent.atomic.AtomicInteger
import javax.inject.Inject
import scala.concurrent.{ExecutionContext, Future}
import scala.math.Integral.Implicits.infixIntegralOps

class MemberDetailsUploadValidator @Inject()(
  validations: ValidationsService
)(implicit ec: ExecutionContext, mat: Materializer)
    extends UploadValidator {
  private val firstRowSink: Sink[List[ByteString], Future[List[String]]] =
    Sink.head[List[ByteString]].mapMaterializedValue(_.map(_.map(_.utf8String)))

  private val csvFrame: Flow[ByteString, List[ByteString], NotUsed] = {
    CsvParsing.lineScanner()
  }
  private val aToZ: List[Char] = ('a' to 'z').toList.map(_.toUpper)
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
        .drop(2) // drop csv header and 1st explanation row from our template
        .statefulMap[UploadInternalState, Upload](() => UploadInternalState.init)(
          (state, bs) => {
            counter.incrementAndGet()
            val parts = bs.map(_.utf8String)

            validateMemberDetailsRow(
              header,
              parts,
              state.row,
              validDateThreshold: Option[LocalDate],
              state.previousNinos
            ) match {
              case None => state.next() -> fileFormatError
              case Some((_, Valid(memberDetails))) =>
                state.next(memberDetails.nino.map(nino => Nino(nino.toUpperCase))) -> UploadSuccessMemberDetails(
                  List(memberDetails)
                )
              case Some((raw, Invalid(errs))) =>
                state.next() -> UploadErrorsMemberDetails(NonEmptyList.one(MemberDetailsUpload.fromRaw(raw)), errs)
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
          case (UploadErrorsMemberDetails(rawPrev, previous), UploadErrorsMemberDetails(raw, errs)) =>
            UploadErrorsMemberDetails(rawPrev ::: raw, previous ++ errs.toList)
          case (UploadErrorsMemberDetails(rawPrev, err), UploadSuccessMemberDetails(details)) =>
            UploadErrorsMemberDetails(rawPrev ++ details, err)
          case (UploadSuccessMemberDetails(detailsPrev), UploadErrorsMemberDetails(raw, err)) =>
            UploadErrorsMemberDetails(NonEmptyList.fromListUnsafe(detailsPrev) ::: raw, err)
          // success
          case (previous: UploadSuccessMemberDetails, current: UploadSuccessMemberDetails) =>
            UploadSuccessMemberDetails(previous.rows ++ current.rows)
          case (_, memberDetails: UploadSuccessMemberDetails) => memberDetails
        }
    } yield (validated, counter.get(), System.currentTimeMillis - startTime))
      .recover {
        case _: NoSuchElementException =>
          (fileFormatError, 0, System.currentTimeMillis - startTime)
      }
  }

  private def validateMemberDetailsRow(
    headerKeys: List[CsvHeaderKey],
    csvData: List[String],
    row: Int,
    validDateThreshold: Option[LocalDate],
    previousNinos: List[Nino]
  )(
    implicit messages: Messages
  ): Option[(RawMemberDetails, ValidatedNel[ValidationError, MemberDetailsUpload])] =
    for {
      raw <- readCSV(row, headerKeys, csvData)
      memberFullName = s"${raw.firstName} ${raw.lastName}"
      validatedNameDOB <- validations.validateNameDOB(
        raw.firstName,
        raw.lastName,
        raw.dateOfBirth,
        row,
        validDateThreshold
      )
      maybeValidatedNino = raw.nino.value.flatMap { nino =>
        validations.validateNinoWithDuplicationControl(
          raw.nino.as(nino.toUpperCase),
          memberFullName,
          previousNinos,
          row
        )
      }
      maybeValidatedNoNinoReason = raw.ninoReason.value.flatMap(
        reason => validations.validateNoNino(raw.ninoReason.as(reason), memberFullName, row)
      )
      validatedNinoOrNoNinoReason <- (maybeValidatedNino, maybeValidatedNoNinoReason) match {
        case (Some(validatedNino), None) => Some(Right(validatedNino))
        case (None, Some(validatedNoNinoReason)) => Some(Left(validatedNoNinoReason))
        case (_, _) =>
          Some(
            Left(
              ValidationError
                .fromCell(
                  row,
                  ValidationErrorType.NoNinoReason,
                  messages("noNINO.upload.error.required")
                )
                .invalidNel
            )
          )
      }
      validatedAddress <- validations.validateUKOrROWAddress(
        raw.isUK,
        raw.ukAddressLine1,
        raw.ukAddressLine2,
        raw.ukAddressLine3,
        raw.ukCity,
        raw.ukPostCode,
        raw.addressLine1,
        raw.addressLine2,
        raw.addressLine3,
        raw.addressLine4,
        raw.country,
        memberFullName,
        row
      )
    } yield (
      raw,
      (validatedAddress, validatedNameDOB, validatedNinoOrNoNinoReason.bisequence).mapN(
        (_, _, _) => MemberDetailsUpload.fromRaw(raw)
      )
    )

  private def readCSV(
    row: Int,
    headerKeys: List[CsvHeaderKey],
    csvData: List[String]
  ): Option[RawMemberDetails] =
    for {
      firstName <- getCSVValue(UploadKeys.firstName, headerKeys, csvData)
      lastName <- getCSVValue(UploadKeys.lastName, headerKeys, csvData)
      dob <- getCSVValue(UploadKeys.dateOfBirth, headerKeys, csvData)
      maybeNino <- getOptionalCSVValue(UploadKeys.nino, headerKeys, csvData)
      maybeNoNinoReason <- getOptionalCSVValue(UploadKeys.reasonForNoNino, headerKeys, csvData)
      isUKAddress <- getCSVValue(UploadKeys.isUKAddress, headerKeys, csvData)
      ukAddressLine1 <- getOptionalCSVValue(UploadKeys.ukAddressLine1, headerKeys, csvData)
      ukAddressLine2 <- getOptionalCSVValue(UploadKeys.ukAddressLine2, headerKeys, csvData)
      ukAddressLine3 <- getOptionalCSVValue(UploadKeys.ukAddressLine3, headerKeys, csvData)
      ukTownOrCity <- getOptionalCSVValue(UploadKeys.ukTownOrCity, headerKeys, csvData)
      ukPostcode <- getOptionalCSVValue(UploadKeys.ukPostcode, headerKeys, csvData)
      addressLine1 <- getOptionalCSVValue(UploadKeys.addressLine1, headerKeys, csvData)
      addressLine2 <- getOptionalCSVValue(UploadKeys.addressLine2, headerKeys, csvData)
      addressLine3 <- getOptionalCSVValue(UploadKeys.addressLine3, headerKeys, csvData)
      addressLine4 <- getOptionalCSVValue(UploadKeys.addressLine4, headerKeys, csvData)
      country <- getOptionalCSVValue(UploadKeys.country, headerKeys, csvData)
    } yield RawMemberDetails(
      row,
      firstName,
      lastName,
      dob,
      maybeNino,
      maybeNoNinoReason,
      isUKAddress,
      ukAddressLine1,
      ukAddressLine2,
      ukAddressLine3,
      ukTownOrCity,
      ukPostcode,
      addressLine1,
      addressLine2,
      addressLine3,
      addressLine4,
      country
    )

  // Replace missing csv value with blank string so form validation can return a `value required` instead of returning a format error
  private def getCSVValue(
    key: String,
    headerKeys: List[CsvHeaderKey],
    csvData: List[String]
  ): Option[CsvValue[String]] =
    getOptionalCSVValue(key, headerKeys, csvData) match {
      case Some(CsvValue(key, Some(value))) => Some(CsvValue(key, value))
      case Some(CsvValue(key, None)) => Some(CsvValue(key, ""))
      case _ => None
    }

  private def getOptionalCSVValue(
    key: String,
    headerKeys: List[CsvHeaderKey],
    csvData: List[String]
  ): Option[CsvValue[Option[String]]] =
    headerKeys
      .find(_.key.toLowerCase() == key.toLowerCase())
      .map(foundKey => CsvValue(foundKey, csvData.get(foundKey.index).flatMap(s => if (s.isEmpty) None else Some(s))))

  private def indexToCsvKey(index: Int): String =
    if (index == 0) aToZ.head.toString
    else {
      val (quotient, remainder) = index /% (aToZ.size)
      if (quotient == 0) aToZ(remainder).toString
      else indexToCsvKey(quotient - 1) + indexToCsvKey(remainder)
    }
}

object MemberDetailsUploadValidator {

  sealed trait UploadAddress

  case class UKAddress(
    line1: String,
    line2: Option[String],
    line3: Option[String],
    city: Option[String],
    postcode: String
  ) extends UploadAddress

  case class ROWAddress(
    line1: String,
    line2: Option[String],
    line3: Option[String],
    line4: Option[String],
    country: String
  ) extends UploadAddress

}
