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
import services.validation.MemberDetailsUploadValidator.{ROWAddress, UKAddress, UploadAddress}
import uk.gov.hmrc.domain.Nino

import java.time.LocalDate
import java.util.concurrent.atomic.AtomicInteger
import javax.inject.Inject
import scala.concurrent.{ExecutionContext, Future}
import scala.math.Integral.Implicits.infixIntegralOps

class MemberDetailsUploadValidator @Inject()(
  validations: ValidationsService
)(implicit ec: ExecutionContext) {
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
        .map { case (key, index) => CsvHeaderKey(key, indexToCsvKey(index), index) }
      validated <- csvFrames
        .drop(2) // drop csv header and 1st explanation row from our template
        .statefulMap[UploadState, Upload](() => UploadState.init)(
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
                state.next(memberDetails.nino.map(Nino(_))) -> UploadSuccess(List(memberDetails))
              case Some((raw, Invalid(errs))) =>
                state.next() -> UploadErrors(NonEmptyList.one(MemberDetailsUpload.fromRaw(raw)), errs)
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
          case (UploadErrors(rawPrev, previous), UploadErrors(raw, errs)) =>
            UploadErrors(rawPrev ::: raw, previous ++ errs.toList)
          case (UploadErrors(rawPrev, err), UploadSuccess(details)) => UploadErrors(rawPrev ++ details, err)
          case (UploadSuccess(detailsPrev), UploadErrors(raw, err)) => UploadErrors(raw ++ detailsPrev, err)
          // success
          case (previous: UploadSuccess, current: UploadSuccess) =>
            UploadSuccess(previous.memberDetails ++ current.memberDetails)
          case (_, memberDetails: UploadSuccess) => memberDetails
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
        validations.validateNino(raw.nino.as(nino), memberFullName, previousNinos, row)
      }
      maybeValidatedNoNinoReason = raw.ninoReason.value.flatMap(
        reason => validations.validateNoNino(raw.ninoReason.as(reason), memberFullName, row)
      )
      validatedNinoOrNoNinoReason <- (maybeValidatedNino, maybeValidatedNoNinoReason) match {
        case (Some(validatedNino), None) => Some(Right(validatedNino))
        case (None, Some(validatedNoNinoReason)) => Some(Left(validatedNoNinoReason))
        case (_, _) => None // fail if neither or both are present in csv
      }
      validatedAddress <- validateUKOrROWAddress(
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

  private def validateUKOrROWAddress(
    isUKAddress: CsvValue[String],
    ukAddressLine1: CsvValue[Option[String]],
    ukAddressLine2: CsvValue[Option[String]],
    ukAddressLine3: CsvValue[Option[String]],
    ukTownOrCity: CsvValue[Option[String]],
    ukPostcode: CsvValue[Option[String]],
    addressLine1: CsvValue[Option[String]],
    addressLine2: CsvValue[Option[String]],
    addressLine3: CsvValue[Option[String]],
    addressLine4: CsvValue[Option[String]],
    country: CsvValue[Option[String]],
    memberFullName: String,
    row: Int
  )(implicit messages: Messages): Option[ValidatedNel[ValidationError, UploadAddress]] =
    for {
      validatedIsUKAddress <- validations.validateIsUkAddress(isUKAddress, memberFullName, row)
      //uk address validations
      maybeUkValidatedAddressLine1 = ukAddressLine1.value.flatMap(
        line1 => validations.validateAddressLine(ukAddressLine1.as(line1), memberFullName, row)
      )
      maybeUkValidatedAddressLine2 = ukAddressLine2.value.flatMap(
        line2 => validations.validateAddressLine(ukAddressLine2.as(line2), memberFullName, row)
      )
      maybeUkValidatedAddressLine3 = ukAddressLine3.value.flatMap(
        line3 => validations.validateAddressLine(ukAddressLine3.as(line3), memberFullName, row)
      )
      maybeUkValidatedTownOrCity = ukTownOrCity.value.flatMap(
        line3 => validations.validateAddressLine(ukTownOrCity.as(line3), memberFullName, row)
      )
      maybeUkValidatedPostcode = ukPostcode.value.flatMap(
        code => validations.validateUkPostcode(ukPostcode.as(code), memberFullName, row)
      )
      //rest-of-world address validations
      maybeValidatedAddressLine1 = addressLine1.value.flatMap(
        line1 => validations.validateAddressLine(addressLine1.as(line1), memberFullName, row)
      )
      maybeValidatedAddressLine2 = addressLine2.value.flatMap(
        line2 => validations.validateAddressLine(addressLine2.as(line2), memberFullName, row)
      )
      maybeValidatedAddressLine3 = addressLine3.value.flatMap(
        line3 => validations.validateAddressLine(addressLine3.as(line3), memberFullName, row)
      )
      maybeValidatedAddressLine4 = addressLine4.value.flatMap(
        line4 => validations.validateAddressLine(addressLine4.as(line4), memberFullName, row)
      )
      maybeValidatedCountry = country.value.flatMap(
        c => validations.validateCountry(country.as(c), row)
      )
      validatedUkOrROWAddress <- (
        validatedIsUKAddress,
        maybeUkValidatedAddressLine1,
        maybeUkValidatedAddressLine2,
        maybeUkValidatedAddressLine3,
        maybeUkValidatedTownOrCity,
        maybeUkValidatedPostcode,
        maybeValidatedAddressLine1,
        maybeValidatedAddressLine2,
        maybeValidatedAddressLine3,
        maybeValidatedAddressLine4,
        maybeValidatedCountry
      ) match {
        case (Valid(isUKAddress), None, None, None, None, None, mLine1, mLine2, mLine3, mLine4, mCountry)
            if isUKAddress.toLowerCase == "no" =>
          (mLine1, mCountry) match {
            case (Some(line1), Some(country)) => //address line 1 and country are mandatory
              Some((line1, mLine2.sequence, mLine3.sequence, mLine4.sequence, country).mapN {
                (line1, line2, line3, line4, country) =>
                  ROWAddress(line1, line2, line3, line4, country)
              })
            case (None, Some(_)) =>
              Some(
                ValidationError
                  .fromCell(
                    row,
                    ValidationErrorType.AddressLine,
                    messages("address-line.upload.error.required")
                  )
                  .invalidNel
              )
            case (Some(_), None) =>
              Some(
                ValidationError
                  .fromCell(
                    row,
                    ValidationErrorType.Country,
                    messages("country.upload.error.required")
                  )
                  .invalidNel
              )
            case (_, _) => None //fail with formatting error
          }

        case (Valid(isUKAddress), mLine1, mLine2, mLine3, mCity, mPostcode, None, None, None, None, None)
            if isUKAddress.toLowerCase == "yes" =>
          (mLine1, mPostcode) match {
            case (Some(line1), Some(postcode)) => //address line 1 and postcode are mandatory
              Some((line1, mLine2.sequence, mLine3.sequence, mCity.sequence, postcode).mapN {
                (line1, line2, line3, city, postcode) =>
                  UKAddress(line1, line2, line3, city, postcode)
              })
            case (None, Some(_)) =>
              Some(
                ValidationError
                  .fromCell(
                    row,
                    ValidationErrorType.AddressLine,
                    messages("address-line.upload.error.required")
                  )
                  .invalidNel
              )
            case (Some(_), None) =>
              Some(
                ValidationError
                  .fromCell(
                    row,
                    ValidationErrorType.UKPostcode,
                    messages("postcode.upload.error.required")
                  )
                  .invalidNel
              )
            case (_, _) => None //fail with formatting error
          }

        case (e @ Invalid(_), _, _, _, _, _, _, _, _, _, _) => Some(e)

        case _ => None
      }
    } yield validatedUkOrROWAddress

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
