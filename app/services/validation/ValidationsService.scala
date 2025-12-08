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

import cats.data.Validated.{Invalid, Valid}
import cats.data.{NonEmptyList, Validated, ValidatedNel}
import cats.implicits.*
import config.Constants
import forms.*
import forms.mappings.errors.{DateFormErrors, DoubleFormErrors, IntFormErrors, MoneyFormErrors}
import models.requests.common.YesNo
import models.*
import play.api.data.{Form, FormError}
import play.api.i18n.Messages
import uk.gov.hmrc.domain.Nino

import java.time.LocalDate
import java.time.format.{DateTimeFormatter, FormatStyle}
import javax.inject.Inject

class ValidationsService @Inject() (
  nameDOBFormProvider: NameDOBFormProvider,
  textFormProvider: TextFormProvider,
  datePageFormProvider: DatePageFormProvider,
  moneyFormProvider: MoneyFormProvider,
  intFormProvider: IntFormProvider,
  doubleFormProvider: DoubleFormProvider
) {

  private def noNinoForm(memberFullName: String): Form[String] =
    textFormProvider.textArea(
      "noNINO.upload.error.required",
      "noNINO.upload.error.length",
      "noNINO.upload.error.invalid",
      memberFullName
    )

  private def ninoForm(memberFullName: String, key: String): Form[Nino] =
    textFormProvider.nino(
      s"$key.upload.error.required",
      s"$key.upload.error.invalid",
      memberFullName
    )

  private def crnForm(memberFullName: String, key: String): Form[Crn] =
    textFormProvider.crn(
      s"$key.upload.error.required",
      s"$key.upload.error.invalid",
      memberFullName
    )

  private def utrForm(memberFullName: String, key: String): Form[Utr] =
    textFormProvider.utr(
      s"$key.upload.error.required",
      s"$key.upload.error.invalid",
      memberFullName
    )

  private def freeTextForm(memberFullName: String, key: String): Form[String] =
    textFormProvider.freeText(
      s"$key.upload.error.required",
      s"$key.upload.error.tooLong",
      s"$key.upload.error.newLine",
      memberFullName
    )

  private def isUkAddressForm(memberFullName: String): Form[String] =
    textFormProvider.yesNo(
      "isUK.upload.error.required",
      "isUK.upload.error.length",
      "isUK.upload.error.invalid",
      memberFullName
    )

  private def addressLineForm(memberFullName: String): Form[String] =
    textFormProvider.addressLine(
      "address-line.upload.error.required",
      "address-line.upload.error.length",
      "address-line.upload.error.invalid",
      memberFullName
    )

  private def townOrCityForm(memberFullName: String): Form[String] =
    textFormProvider.addressLine(
      "town-or-city.upload.error.required",
      "town-or-city.upload.error.invalid",
      "town-or-city.upload.error.invalid",
      memberFullName
    )

  private def countryForm(): Form[String] =
    textFormProvider.country(
      "country.upload.error.required",
      "country.upload.error.invalid"
    )

  private def postcodeForm(memberFullName: String): Form[String] =
    textFormProvider.postcode(
      "postcode.upload.error.required",
      "postcode.upload.error.invalid",
      "postcode.upload.error.invalid",
      memberFullName
    )

  private def yesNoQuestionForm(key: String, memberFullName: String): Form[String] =
    textFormProvider.yesNo(
      s"$key.upload.error.required",
      s"$key.upload.error.length",
      s"$key.upload.error.invalid",
      memberFullName
    )

  private def createErrorIfFieldEmpty(
    field: Option[ValidatedNel[ValidationError, String]],
    row: Int,
    errType: ValidationErrorType,
    msg: String
  ): Option[ValidationError] =
    if (field.isEmpty) Some(ValidationError(row = row, errorType = errType, msg))
    else None

  def validateNameDOB(
    firstName: CsvValue[String],
    lastName: CsvValue[String],
    dob: CsvValue[String],
    row: Int,
    validDateThreshold: Option[LocalDate]
  )(implicit messages: Messages): Option[ValidatedNel[ValidationError, NameDOB]] = {
    val dobDayKey = s"${nameDOBFormProvider.dateOfBirth}.day"
    val dobMonthKey = s"${nameDOBFormProvider.dateOfBirth}.month"
    val dobYearKey = s"${nameDOBFormProvider.dateOfBirth}.year"

    dob.value.split("[-/]").toList match {
      case day :: month :: year :: Nil =>
        val memberDetailsForm = {
          val dateThreshold: LocalDate = validDateThreshold.getOrElse(LocalDate.now())
          nameDOBFormProvider(
            "memberDetails.firstName.upload.error.required",
            "memberDetails.firstName.upload.error.invalid",
            "memberDetails.firstName.upload.error.length",
            "memberDetails.lastName.upload.error.required",
            "memberDetails.lastName.upload.error.invalid",
            "memberDetails.lastName.upload.error.length",
            DateFormErrors(
              "memberDetails.dateOfBirth.upload.error.required.all",
              "memberDetails.dateOfBirth.upload.error.required.day",
              "memberDetails.dateOfBirth.upload.error.required.month",
              "memberDetails.dateOfBirth.upload.error.required.year",
              "memberDetails.dateOfBirth.upload.error.required.two",
              "memberDetails.dateOfBirth.upload.error.invalid.date",
              "memberDetails.dateOfBirth.upload.error.invalid.characters",
              List(
                DateFormErrors
                  .failIfDateAfter(
                    dateThreshold,
                    messages(
                      "memberDetails.dateOfBirth.upload.error.future",
                      dateThreshold.format(DateTimeFormatter.ofLocalizedDate(FormatStyle.LONG))
                    )
                  ),
                DateFormErrors
                  .failIfDateBefore(
                    Constants.earliestDate,
                    messages(
                      "memberDetails.dateOfBirth.upload.error.after",
                      Constants.earliestDate.format(DateTimeFormatter.ofLocalizedDate(FormatStyle.LONG))
                    )
                  )
              )
            )
          )
        }.bind(
          Map(
            nameDOBFormProvider.firstName -> firstName.value,
            nameDOBFormProvider.lastName -> lastName.value,
            dobDayKey -> day,
            dobMonthKey -> month,
            dobYearKey -> year
          )
        )

        val errorTypeMapping: FormError => ValidationErrorType = _.key match {
          case nameDOBFormProvider.firstName => ValidationErrorType.FirstName
          case nameDOBFormProvider.lastName => ValidationErrorType.LastName
          case nameDOBFormProvider.dateOfBirth => ValidationErrorType.DateOfBirth
          case `dobDayKey` => ValidationErrorType.DateOfBirth
          case `dobMonthKey` => ValidationErrorType.DateOfBirth
          case `dobYearKey` => ValidationErrorType.DateOfBirth
        }

        val cellMapping: FormError => Option[String] = {
          case err if err.key == nameDOBFormProvider.firstName => Some(firstName.key.cell)
          case err if err.key == nameDOBFormProvider.lastName => Some(lastName.key.cell)
          case err if err.key == nameDOBFormProvider.dateOfBirth => Some(dob.key.cell)
          case err if err.key == dobDayKey => Some(dob.key.cell)
          case err if err.key == dobMonthKey => Some(dob.key.cell)
          case err if err.key == dobYearKey => Some(dob.key.cell)
          case _ => None
        }

        formToResult(memberDetailsForm, row, errorTypeMapping, cellMapping)

      case _ =>
        Some(
          ValidationError
            .fromCell(
              row,
              ValidationErrorType.DateOfBirth,
              messages("memberDetails.dateOfBirth.error.format")
            )
            .invalidNel
        )
    }
  }

  def validateNinoWithNoReason(
    nino: CsvValue[Option[String]],
    noNinoReason: CsvValue[Option[String]],
    memberFullName: String,
    row: Int
  )(implicit messages: Messages): Option[ValidatedNel[ValidationError, NinoType]] =
    (nino.value, noNinoReason.value) match {
      case (Some(n), _) =>
        validateNino(
          nino.as(n.toUpperCase.filterNot(_.isWhitespace)),
          memberFullName,
          row
        ).map {
          case Valid(n) => NinoType(Some(n.value), None).valid
          case e @ Invalid(_) => e
        }
      case (_, Some(reason)) =>
        validateFreeText(
          noNinoReason.as(reason),
          key = "nino.reason",
          memberFullName,
          row
        ).map {
          case Valid(reason) => NinoType(None, Some(reason)).valid
          case e @ Invalid(_) => e
        }
      case (None, None) =>
        Some(
          ValidationError
            .fromCell(
              row,
              ValidationErrorType.NoNinoReason,
              messages("nino.upload.error.required")
            )
            .invalidNel
        )
    }

  def validateNino(
    nino: CsvValue[String],
    memberFullName: String,
    row: Int,
    key: String = "nino"
  ): Option[ValidatedNel[ValidationError, Nino]] = {
    val boundForm = ninoForm(memberFullName, key)
      .bind(
        Map(
          textFormProvider.formKey -> nino.value
        )
      )

    formToResult(boundForm, row, _ => ValidationErrorType.NinoFormat, cellMapping = _ => Some(nino.key.cell))
  }

  def validateCrn(
    crn: CsvValue[String],
    memberFullName: String,
    row: Int,
    key: String = "crn"
  ): Option[ValidatedNel[ValidationError, Crn]] = {
    val boundForm = crnForm(memberFullName, key)
      .bind(
        Map(
          textFormProvider.formKey -> crn.value
        )
      )

    formToResult(boundForm, row, _ => ValidationErrorType.CrnFormat, cellMapping = _ => Some(crn.key.cell))
  }

  def validateUtr(
    utr: CsvValue[String],
    memberFullName: String,
    row: Int,
    key: String = "utr"
  ): Option[ValidatedNel[ValidationError, Utr]] = {
    val boundForm = utrForm(memberFullName, key)
      .bind(
        Map(
          textFormProvider.formKey -> utr.value
        )
      )

    formToResult(boundForm, row, _ => ValidationErrorType.UtrFormat, cellMapping = _ => Some(utr.key.cell))
  }

  def validateFreeText(
    text: CsvValue[String],
    key: String = "other",
    memberFullName: String,
    row: Int
  ): Option[ValidatedNel[ValidationError, String]] = {
    val boundForm = freeTextForm(memberFullName, key)
      .bind(
        Map(
          textFormProvider.formKey -> text.value
        )
      )

    formToResult(boundForm, row, _ => ValidationErrorType.FreeText, cellMapping = _ => Some(text.key.cell))
  }

  def validateNoNino(
    noNinoReason: CsvValue[String],
    memberFullName: String,
    row: Int
  ): Option[ValidatedNel[ValidationError, String]] = {
    val boundForm = noNinoForm(memberFullName)
      .bind(
        Map(
          textFormProvider.formKey -> noNinoReason.value
        )
      )

    formToResult(
      boundForm,
      row,
      errorTypeMapping = _ => ValidationErrorType.NoNinoReason,
      cellMapping = _ => Some(noNinoReason.key.cell)
    )
  }

  def validateAddressLine(
    inputAddressLine: CsvValue[String],
    memberFullName: String,
    row: Int
  ): Option[ValidatedNel[ValidationError, String]] = {
    val boundForm = addressLineForm(memberFullName)
      .bind(
        Map(
          textFormProvider.formKey -> inputAddressLine.value
        )
      )

    formToResult(
      boundForm,
      row,
      errorTypeMapping = _ => ValidationErrorType.AddressLine,
      cellMapping = _ => Some(inputAddressLine.key.cell)
    )
  }

  def validateTownOrCity(
    inputAddressLine: CsvValue[String],
    memberFullName: String,
    row: Int
  ): Option[ValidatedNel[ValidationError, String]] = {
    val boundForm = townOrCityForm(memberFullName)
      .bind(
        Map(
          textFormProvider.formKey -> inputAddressLine.value
        )
      )

    formToResult(
      boundForm,
      row,
      errorTypeMapping = _ => ValidationErrorType.TownOrCity,
      cellMapping = _ => Some(inputAddressLine.key.cell)
    )
  }

  def validateCountry(
    country: CsvValue[String],
    row: Int
  ): Option[ValidatedNel[ValidationError, String]] = {
    val boundForm = countryForm()
      .bind(
        Map(
          textFormProvider.formKey -> country.value.toLowerCase
        )
      )

    formToResult(
      boundForm,
      row,
      errorTypeMapping = _ => ValidationErrorType.Country,
      cellMapping = _ => Some(country.key.cell)
    )
  }

  def validateUkPostcode(
    postcode: CsvValue[String],
    memberFullName: String,
    row: Int
  ): Option[ValidatedNel[ValidationError, String]] = {
    val boundForm = postcodeForm(memberFullName)
      .bind(
        Map(
          textFormProvider.formKey -> postcode.value
        )
      )

    formToResult(
      boundForm,
      row,
      errorTypeMapping = _ => ValidationErrorType.UKPostcode,
      cellMapping = _ => Some(postcode.key.cell)
    )
  }

  def validateIsUkAddress(
    isUkAddress: CsvValue[String],
    memberFullName: String,
    row: Int
  ): Option[ValidatedNel[ValidationError, String]] = {
    val boundForm = isUkAddressForm(memberFullName)
      .bind(
        Map(
          textFormProvider.formKey -> isUkAddress.value
        )
      )

    formToResult(
      boundForm,
      row,
      errorTypeMapping = _ => ValidationErrorType.YesNoAddress,
      cellMapping = _ => Some(isUkAddress.key.cell)
    )
  }

  def validateDate(
    date: CsvValue[String],
    key: String,
    row: Int
  )(implicit messages: Messages): Option[ValidatedNel[ValidationError, LocalDate]] = {
    val splitRegex = if (date.value.contains("-")) "-" else "/"

    date.value.split(splitRegex).toList match {
      case day :: month :: year :: Nil =>
        val dateForm =
          datePageFormProvider(
            DateFormErrors(
              s"$key.upload.error.required.all",
              s"$key.upload.error.required.day",
              s"$key.upload.error.required.month",
              s"$key.upload.error.required.year",
              s"$key.upload.error.required.two",
              s"$key.upload.error.invalid.date",
              s"$key.upload.error.invalid.characters"
            )
          )
            .bind(
              Map(
                "value.day" -> day,
                "value.month" -> month,
                "value.year" -> year
              )
            )

        val errorTypeMapping: FormError => ValidationErrorType = _.key match {
          case "value" => ValidationErrorType.LocalDateFormat
          case "value.day" => ValidationErrorType.LocalDateFormat
          case "value.month" => ValidationErrorType.LocalDateFormat
          case "value.year" => ValidationErrorType.LocalDateFormat
        }

        val cellMapping: FormError => Option[String] = {
          case err if err.key == key => Some(date.key.cell)
          case err if err.key == "value.day" => Some(date.key.cell)
          case err if err.key == "value.month" => Some(date.key.cell)
          case err if err.key == "value.year" => Some(date.key.cell)
          case _ => Some(date.key.cell)
        }

        formToResult(
          dateForm,
          row,
          errorTypeMapping,
          cellMapping
        )

      case "" :: nNl =>
        Some(
          ValidationError
            .fromCell(
              row,
              ValidationErrorType.LocalDateFormat,
              messages(s"$key.upload.error.required.date")
            )
            .invalidNel
        )

      case _ =>
        Some(
          ValidationError
            .fromCell(
              row,
              ValidationErrorType.LocalDateFormat,
              messages(s"$key.upload.error.invalid.characters")
            )
            .invalidNel
        )
    }
  }

  def validateUKOrROWAddress(
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
  ): Option[ValidatedNel[ValidationError, UploadAddress]] =
    for {
      validatedIsUKAddress <- validateIsUkAddress(isUKAddress, memberFullName, row)
      // uk address validations
      maybeUkValidatedAddressLine1 = ukAddressLine1.value.flatMap(line1 =>
        validateAddressLine(ukAddressLine1.as(line1), memberFullName, row)
      )
      maybeUkValidatedAddressLine2 = ukAddressLine2.value.flatMap(line2 =>
        validateAddressLine(ukAddressLine2.as(line2), memberFullName, row)
      )
      maybeUkValidatedAddressLine3 = ukAddressLine3.value.flatMap(line3 =>
        validateAddressLine(ukAddressLine3.as(line3), memberFullName, row)
      )
      maybeUkValidatedTownOrCity = ukTownOrCity.value.flatMap(line3 =>
        validateTownOrCity(ukTownOrCity.as(line3), memberFullName, row)
      )
      maybeUkValidatedPostcode = ukPostcode.value.flatMap(code =>
        validateUkPostcode(ukPostcode.as(code), memberFullName, row)
      )
      // rest-of-world address validations
      maybeValidatedAddressLine1 = addressLine1.value.flatMap(line1 =>
        validateAddressLine(addressLine1.as(line1), memberFullName, row)
      )
      maybeValidatedAddressLine2 = addressLine2.value.flatMap(line2 =>
        validateAddressLine(addressLine2.as(line2), memberFullName, row)
      )
      maybeValidatedAddressLine3 = addressLine3.value.flatMap(line3 =>
        validateAddressLine(addressLine3.as(line3), memberFullName, row)
      )
      maybeValidatedAddressLine4 = addressLine4.value.flatMap(line4 =>
        validateAddressLine(addressLine4.as(line4), memberFullName, row)
      )
      maybeValidatedCountry = country.value.flatMap(c => validateCountry(country.as(c), row))
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
        case (Valid(isUKAddress), _, _, _, _, _, mLine1, mLine2, mLine3, cityOrTown, mCountry)
          if isUKAddress.toLowerCase == "no" =>
          (mLine1, mLine2, mCountry) match {
            case (
              Some(line1),
              Some(line2),
              Some(country)
              ) =>
              Some((line1, line2, mLine3.sequence, cityOrTown.sequence, country).mapN {
                (line1, line2, line3, cityOrTown, country) =>
                  ROWAddress(line1, line2, line3, cityOrTown, country)
              })
            case (eLine1, eLine2, eCountry) =>
              val listEmpty = List.empty[Option[ValidationError]]
              val errorList = listEmpty :+
                createErrorIfFieldEmpty(
                  eLine1,
                  row,
                  ValidationErrorType.AddressLine,
                  "address-line-non-uk.upload.error.required"
                ) :+
                createErrorIfFieldEmpty(
                  eLine2,
                  row,
                  ValidationErrorType.AddressLine,
                  "address-line-2-non-uk.upload.error.required"
                ) :+
                createErrorIfFieldEmpty(
                  eCountry,
                  row,
                  ValidationErrorType.Country,
                  "country.upload.error.required"
                )
              Some(Invalid(NonEmptyList.fromListUnsafe(errorList.flatten)))
          }

        case (Valid(isUKAddress), mLine1, mLine2, mLine3, mCity, mPostcode, _, _, _, _, _)
          if isUKAddress.toLowerCase == "yes" =>
          (mLine1, mCity, mPostcode) match {
            case (
              Some(line1),
              Some(city),
              Some(postcode)
              ) => // address line 1, city and postcode are mandatory
              Some((line1, mLine2.sequence, mLine3.sequence, city, postcode).mapN { (line1, line2, line3, city, postcode) =>
                UKAddress(line1, line2, line3, city, postcode)
              })
            case (eLine1, eCity, ePostcode) =>
              val listEmpty = List.empty[Option[ValidationError]]
              val errorList = listEmpty :+
                createErrorIfFieldEmpty(
                  eLine1,
                  row,
                  ValidationErrorType.AddressLine,
                  "address-line.upload.error.required"
                ) :+
                createErrorIfFieldEmpty(
                  eCity,
                  row,
                  ValidationErrorType.TownOrCity,
                  "town-or-city.upload.error.required"
                ) :+
                createErrorIfFieldEmpty(
                  ePostcode,
                  row,
                  ValidationErrorType.UKPostcode,
                  "postcode.upload.error.required"
                )
              Some(Invalid(NonEmptyList.fromListUnsafe(errorList.flatten)))
          }

        case (e@Invalid(_), _, _, _, _, _, _, _, _, _, _) => Some(e)

        case _ => None
      }
    } yield validatedUkOrROWAddress

  def validateYesNoQuestion(
    yesNoQuestion: CsvValue[String],
    key: String,
    memberFullName: String,
    row: Int
  ): Option[ValidatedNel[ValidationError, String]] = {
    val boundForm = yesNoQuestionForm(key, memberFullName)
      .bind(
        Map(
          textFormProvider.formKey -> yesNoQuestion.value
        )
      )

    formToResult(
      boundForm,
      row,
      errorTypeMapping = _ => ValidationErrorType.YesNoQuestion,
      cellMapping = _ => Some(yesNoQuestion.key.cell)
    )
  }

  def validateYesNoQuestionTyped(
    yesNoQuestion: CsvValue[String],
    key: String,
    memberFullName: String,
    row: Int
  ): Option[ValidatedNel[ValidationError, YesNo]] =
    validateYesNoQuestion(yesNoQuestion, key, memberFullName, row).map(_.map(YesNo.withNameInsensitive))

  def validatePrice(
    price: CsvValue[String],
    key: String,
    memberFullName: String,
    row: Int
  ): Option[ValidatedNel[ValidationError, Money]] = {
    val boundForm = moneyFormProvider(
      MoneyFormErrors(
        s"$key.upload.error.required",
        s"$key.upload.error.numericValueRequired",
        max = (999999999.99, s"$key.upload.error.tooBig"),
        min = (0, s"$key.upload.error.tooSmall")
      ),
      args = Seq(memberFullName)
    ).bind(
      Map(
        textFormProvider.formKey -> price.value
      )
    )

    formToResult(
      boundForm,
      row,
      errorTypeMapping = _ => ValidationErrorType.Price,
      cellMapping = _ => Some(price.key.cell)
    )
  }

  def validateCount(
    count: CsvValue[String],
    key: String,
    memberFullName: String,
    row: Int,
    minCount: Int = 1,
    maxCount: Int = 999999
  ): Option[ValidatedNel[ValidationError, Int]] = {
    val boundForm = intFormProvider(
      IntFormErrors(
        requiredKey = s"$key.upload.error.required",
        invalidKey = s"$key.upload.error.invalid",
        min = (minCount, s"$key.upload.error.tooSmall"),
        max = (maxCount, s"$key.upload.error.tooBig")
      ),
      args = Seq(memberFullName)
    ).bind(
      Map(
        textFormProvider.formKey -> count.value
      )
    )

    formToResult(
      boundForm,
      row,
      errorTypeMapping = _ => ValidationErrorType.Count,
      cellMapping = _ => Some(count.key.cell)
    )
  }

  def validatePercentage(
    percentage: CsvValue[String],
    key: String,
    memberFullName: String,
    row: Int
  ): Option[ValidatedNel[ValidationError, Double]] = {
    val boundForm = doubleFormProvider(
      DoubleFormErrors(
        requiredKey = s"$key.upload.error.required",
        invalidKey = s"$key.upload.error.invalid",
        min = (0.0, s"$key.upload.error.tooSmall"),
        max = (99.99, s"$key.upload.error.tooBig")
      ),
      args = Seq(memberFullName)
    ).bind(
      Map(
        textFormProvider.formKey -> percentage.value
      )
    )

    formToResult(
      boundForm,
      row,
      errorTypeMapping = _ => ValidationErrorType.Percentage,
      cellMapping = _ => Some(percentage.key.cell)
    )
  }

  protected def formToResult[A](
    form: Form[A],
    row: Int,
    errorTypeMapping: FormError => ValidationErrorType,
    cellMapping: FormError => Option[String]
  ): Option[Validated[NonEmptyList[ValidationError], A]] =
    form.fold(
      // unchecked is used as there will always be form errors here and theres no need to exhaustively pattern match and throw an unreachable exception
      hasErrors = form =>
        (form.errors: @unchecked) match {
          case head :: rest =>
            NonEmptyList
              .of[FormError](head, rest*)
              .map(err => cellMapping(err).as(ValidationError.fromCell(row, errorTypeMapping(err), err.message)))
              .sequence
              .map(_.distinct)
              .map(_.invalid)
        },
      success = _.valid.some
    )

}
