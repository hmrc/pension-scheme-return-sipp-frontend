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

package forms.mappings

import cats.implicits.toFunctorOps
import forms.mappings.errors.*
import models.{Crn, DateRange, Enumerable, GenericFormMapper, Money, SelectInput, Utr}
import play.api.data.Forms.of
import play.api.data.validation.{Constraint, Invalid, Valid}
import play.api.data.{FieldMapping, Mapping}
import uk.gov.hmrc.domain.Nino
import uk.gov.voa.play.form.Condition
import utils.Country

import java.time.LocalDate

trait Mappings extends Formatters with Constraints {

  def text(errorKey: String = "error.required", args: Seq[Any] = Seq.empty): Mapping[String] =
    of(stringFormatter(errorKey, args)).transform[String](_.trim, _.trim)

  def conditional[A](
    l: List[(Condition, Option[Mapping[A]])],
    prePopKey: Option[String]
  )(implicit ev: GenericFormMapper[String, A]): FieldMapping[Option[A]] =
    of(conditionalFormatter[A](l, prePopKey))

  def optionalText(): Mapping[String] =
    of(optionalStringFormatter()).transform[String](_.trim, _.trim)

  def int(
    requiredKey: String = "error.required",
    wholeNumberKey: String = "error.wholeNumber",
    nonNumericKey: String = "error.nonNumeric",
    max: (Int, String) = (Int.MaxValue, "error.tooLarge"),
    min: (Int, String) = (0, "error.tooSmall"),
    args: Seq[String] = Seq.empty
  ): FieldMapping[Int] =
    int(IntFormErrors(requiredKey, wholeNumberKey, nonNumericKey, max, min), args)

  def int(
    intFormErrors: IntFormErrors,
    args: Seq[String]
  ): FieldMapping[Int] =
    of(intFormatter(intFormErrors, args))

  def double(
    requiredKey: String = "error.required",
    nonNumericKey: String = "error.nonNumeric",
    max: (Double, String) = (Double.MaxValue, "error.tooLarge"),
    min: (Double, String) = (0d, "error.tooSmall"),
    args: Seq[String] = Seq.empty
  ): FieldMapping[Double] =
    of(doubleFormatter(requiredKey, nonNumericKey, max, min, args))

  def double(
    doubleFormErrors: DoubleFormErrors,
    args: Seq[String]
  ): FieldMapping[Double] =
    of(doubleFormatter(doubleFormErrors, args))

  def money(
    moneyFormErrors: MoneyFormErrors,
    args: Seq[String] = Seq.empty
  ): FieldMapping[Money] =
    of(moneyFormatter(moneyFormErrors, args))

  def boolean(
    requiredKey: String = "error.required",
    invalidKey: String = "error.boolean",
    args: Seq[String] = Seq.empty
  ): FieldMapping[Boolean] =
    of(booleanFormatter(requiredKey, invalidKey, args))

  val unit: FieldMapping[Unit] = of(unitFormatter)

  def enumerable[A](
    requiredKey: String = "error.required",
    invalidKey: String = "error.invalid",
    args: Seq[String] = Seq.empty
  )(implicit ev: Enumerable[A]): FieldMapping[A] =
    of(enumerableFormatter[A](requiredKey, invalidKey, args))

  def localDate(dateFormErrors: DateFormErrors, args: Seq[String] = Seq.empty): FieldMapping[LocalDate] =
    of(LocalDateFormatter(dateFormErrors, args))

  def dateRange(
    startDateErrors: DateFormErrors,
    endDateErrors: DateFormErrors,
    invalidRangeError: String,
    allowedRange: Option[DateRange],
    startDateAllowedDateRangeError: Option[String],
    endDateAllowedDateRangeError: Option[String],
    startDateDuplicateRangeError: Option[String],
    endDateDuplicateRangeError: Option[String],
    gapNotAllowedError: Option[String],
    duplicateRanges: List[DateRange]
  ): FieldMapping[DateRange] =
    of(
      DateRangeFormatter(
        startDateErrors,
        endDateErrors,
        invalidRangeError,
        allowedRange,
        startDateAllowedDateRangeError,
        endDateAllowedDateRangeError,
        startDateDuplicateRangeError,
        endDateDuplicateRangeError,
        gapNotAllowedError,
        duplicateRanges
      )
    )

  def verify[A](errorKey: String, pred: A => Boolean, args: Any*): Constraint[A] =
    Constraint[A] { (a: A) =>
      if (pred(a)) Valid
      else Invalid(errorKey, args*)
    }

  def validatedText(
    requiredKey: String,
    regexChecks: List[(Regex, String)],
    maxLength: Int,
    maxLengthErrorKey: String,
    args: Any*
  ): Mapping[String] =
    regexChecks
      .foldLeft(text(requiredKey, args.toList)) { case (mapping, (regex, key)) =>
        mapping.verifying(verify[String](key, _.matches(regex), args*))
      }
      .verifying(verify[String](maxLengthErrorKey, _.length <= maxLength, args*))

  def nino(
    requiredKey: String,
    invalidKey: String,
    args: Any*
  ): Mapping[Nino] =
    text(requiredKey, args.toList)
      .verifying(verify[String](invalidKey, s => Nino.isValid(s.toUpperCase), args*))
      .transform[Nino](s => Nino(s.toUpperCase), _.nino.toUpperCase)

  def ninoNoDuplicates(
    requiredKey: String,
    invalidKey: String,
    duplicates: List[Nino],
    duplicateKey: String,
    args: Any*
  ): Mapping[Nino] =
    text(requiredKey, args.toList)
      .verifying(verify[String](invalidKey, s => Nino.isValid(s.toUpperCase), args*))
      .verifying(verify[String](duplicateKey, !duplicates.map(_.nino).contains(_), args*))
      .transform[Nino](s => Nino(s.toUpperCase), _.nino.toUpperCase)

  def utr(
    requiredKey: String,
    invalidKey: String,
    args: Any*
  ): Mapping[Utr] =
    text(requiredKey, args.toList)
      .verifying(verify[String](invalidKey, s => Utr.isValid(s.toUpperCase), args*))
      .transform[Utr](s => Utr(s.toUpperCase), _.utr.toUpperCase)

  def crn(requiredKey: String, invalidKey: String, minMaxLengthErrorKey: String, args: Any*): Mapping[Crn] =
    text(requiredKey, args.toList)
      .verifying(verify[String](invalidKey, s => Crn.isValid(s.toUpperCase), args*))
      .verifying(verify[String](minMaxLengthErrorKey, s => Crn.isLengthInRange(s.toUpperCase), args*))
      .transform[Crn](s => Crn(s.toUpperCase), _.crn.toUpperCase)

  private def country(countryOptions: Seq[SelectInput], errorKey: String): Constraint[String] =
    Constraint { input =>
      countryOptions
        .find(_.label.equalsIgnoreCase(input))
        .as(Valid)
        .getOrElse(Invalid(errorKey))
    }

  def selectCountry(countryOptions: Seq[SelectInput], requiredKey: String, invalidKey: String): Mapping[String] =
    text(requiredKey)
      .verifying(country(countryOptions, invalidKey))
      .transform[String](
        input => Country.getCountryCode(input).getOrElse(input),
        countryCode => Country.getCountry(countryCode).getOrElse(countryCode)
      )

  def psaId(
    requiredKey: String,
    regexChecks: List[(Regex, String)],
    maxLength: Int,
    maxLengthErrorKey: String,
    authorisingPSAID: Option[String],
    noMatchKey: String,
    args: Any*
  ): Mapping[String] =
    regexChecks
      .foldLeft(text(requiredKey, args.toList)) { case (mapping, (regex, key)) =>
        mapping.verifying(verify[String](key, _.matches(regex), args*))
      }
      .verifying(isEqual(authorisingPSAID, noMatchKey))
      .verifying(verify[String](maxLengthErrorKey, _.length <= maxLength, args*))
}

object Mappings extends Mappings
