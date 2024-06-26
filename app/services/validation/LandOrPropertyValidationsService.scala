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
import cats.data.{NonEmptyList, ValidatedNel}
import cats.implicits._
import forms._
import models._
import models.requests.common.YesNo
import models.requests.common.YesNo.{No, Yes}
import models.requests.common._
import play.api.i18n.Messages

import javax.inject.Inject

class LandOrPropertyValidationsService @Inject()(
  nameDOBFormProvider: NameDOBFormProvider,
  textFormProvider: TextFormProvider,
  dateFormPageProvider: DatePageFormProvider,
  moneyFormProvider: MoneyFormProvider,
  intFormProvider: IntFormProvider,
  doubleFormProvider: DoubleFormProvider
) extends ValidationsService(
      nameDOBFormProvider,
      textFormProvider,
      dateFormPageProvider,
      moneyFormProvider,
      intFormProvider,
      doubleFormProvider
    ) {

  def validateIsThereARegistryReference(
    isThereARegistryReference: CsvValue[String],
    noLandRegistryReference: CsvValue[Option[String]],
    memberFullNameDob: String,
    row: Int
  ): Option[ValidatedNel[ValidationError, RegistryDetails]] =
    for {
      validatedIsThereARegistryReference <- validateYesNoQuestion(
        isThereARegistryReference,
        "landOrProperty.isThereARegistryReference",
        memberFullNameDob,
        row
      )
      maybeNoLandRegistryReference = noLandRegistryReference.value.flatMap(
        reason =>
          validateFreeText(
            noLandRegistryReference.as(reason),
            "landOrProperty.noLandRegistryReference",
            memberFullNameDob,
            row
          )
      )
      referenceDetails <- (
        validatedIsThereARegistryReference,
        maybeNoLandRegistryReference
      ) match {
        case (Valid(isThereARegistryReference), _) if isThereARegistryReference.toUpperCase == "YES" =>
          Some(RegistryDetails(Yes, None, None).valid)
        case (Valid(isThereARegistryReference), mNoLandRegistryReference)
            if isThereARegistryReference.toUpperCase == "NO" =>
          mNoLandRegistryReference match {
            case Some(noLandRegistryReference) =>
              Some(noLandRegistryReference.map { noRef =>
                RegistryDetails(No, None, Some(noRef))
              })
            case _ =>
              Some(
                ValidationError(
                  row,
                  errorType = ValidationErrorType.FreeText,
                  "landOrProperty.noLandRegistryReference.upload.error.required"
                ).invalidNel
              )
          }
        case (e @ Invalid(_), _) => Some(e)
        case _ => None
      }
    } yield referenceDetails

  def validateJointlyHeld(
    isPropertyHeldJointly: CsvValue[String],
    howManyPersonsJointlyOwnProperty: CsvValue[Option[String]],
    memberFullNameDob: String,
    row: Int
  ): Option[ValidatedNel[ValidationError, (YesNo, Option[Int])]] =
    for {
      validatedIsPropertyHeldJointly <- validateYesNoQuestion(
        isPropertyHeldJointly,
        "landOrProperty.isPropertyHeldJointly",
        memberFullNameDob,
        row
      )

      maybeCount = howManyPersonsJointlyOwnProperty.value.flatMap(
        count =>
          validateCount(
            howManyPersonsJointlyOwnProperty.as(count),
            "landOrProperty.personCount",
            memberFullNameDob,
            row
          )
      )

      jointlyHeld <- (
        validatedIsPropertyHeldJointly,
        maybeCount
      ) match {
        case (Valid(isPropertyHeldJointly), mCount) if isPropertyHeldJointly.toUpperCase == "YES" =>
          mCount match {
            case Some(count) =>
              (count) match {
                case Valid(c) => Some((Yes, Some(c)).validNel)
                case (e @ Invalid(_)) => Some(e)
              }
            case _ =>
              Some(
                ValidationError(
                  row,
                  errorType = ValidationErrorType.Count,
                  "landOrProperty.personCount.upload.error.required"
                ).invalidNel
              )
          }

        case (Valid(isPropertyHeldJointly), _) if isPropertyHeldJointly.toUpperCase == "NO" =>
          Some((No, None).validNel)

        case (e @ Invalid(_), _) => Some(e)

        case _ => None
      }
    } yield jointlyHeld

  def validateLease(
    isLeased: CsvValue[String],
    countOfLessees: CsvValue[Option[String]],
    namesOfLessees: CsvValue[Option[String]],
    anyOfLesseesConnected: CsvValue[Option[String]],
    leaseDate: CsvValue[Option[String]],
    annualLeaseAmount: CsvValue[Option[String]],
    isCountEntered: Boolean,
    memberFullNameDob: String,
    row: Int
  )(implicit messages: Messages): Option[ValidatedNel[ValidationError, (YesNo, Option[LesseeDetail])]] =
    for {
      validatedIsLeased <- validateYesNoQuestion(
        isLeased,
        "landOrProperty.isLeased",
        memberFullNameDob,
        row
      )

      maybeCount = countOfLessees.value.flatMap(
        count =>
          validateCount(
            countOfLessees.as(count),
            "landOrProperty.lesseePersonCount",
            memberFullNameDob,
            row,
            maxCount = 50
          )
      )

      maybeNames = namesOfLessees.value.flatMap(
        n =>
          validateFreeText(
            namesOfLessees.as(n),
            "landOrProperty.lesseePersonNames",
            memberFullNameDob,
            row
          )
      )

      maybeConnected = anyOfLesseesConnected.value.flatMap(
        yesNo =>
          validateYesNoQuestion(
            anyOfLesseesConnected.as(yesNo),
            "landOrProperty.anyLesseeConnected",
            memberFullNameDob,
            row
          )
      )

      maybeDate = leaseDate.value.flatMap(
        d =>
          validateDate(
            leaseDate.as(d),
            "landOrProperty.leaseDate",
            row
          )
      )

      maybeAmount = annualLeaseAmount.value.flatMap(
        amount =>
          validatePrice(
            annualLeaseAmount.as(amount),
            "landOrProperty.leaseAmount",
            memberFullNameDob,
            row
          )
      )

      lesseeDetails <- (
        validatedIsLeased,
        maybeCount,
        maybeNames,
        maybeConnected,
        maybeDate,
        maybeAmount
      ) match {
        case (Valid(isLeased), mCount, mNames, mConnected, mDate, mAmount) if isLeased.toUpperCase == "YES" =>
          (isCountEntered, mCount, mNames, mConnected, mDate, mAmount) match {
            case (countEntered, Some(count), _, Some(connected), Some(date), Some(amount)) if countEntered =>
              Some((count, connected, date, amount).mapN { (_count, _connected, _date, _amount) =>
                (
                  Yes,
                  Some(
                    LesseeDetail(
                      Some(_count),
                      None,
                      YesNo.withNameInsensitive(_connected),
                      _date,
                      _amount.value
                    )
                  )
                )
              })
            case (countEntered, _, Some(name), Some(connected), Some(date), Some(amount)) if !countEntered =>
              Some((name, connected, date, amount).mapN { (_name, _connected, _date, _amount) =>
                (
                  Yes,
                  Some(
                    LesseeDetail(
                      None,
                      Some(_name),
                      YesNo.withNameInsensitive(_connected),
                      _date,
                      _amount.value
                    )
                  )
                )
              })
            case _ =>
              val listEmpty = List.empty[Option[ValidationError]]
              val optCount = if (isCountEntered && mCount.isEmpty) {
                Some(
                  ValidationError(
                    row,
                    errorType = ValidationErrorType.Count,
                    "landOrProperty.lesseePersonCount.upload.error.required"
                  )
                )
              } else {
                None
              }

              val optName = if (!isCountEntered && mNames.isEmpty) {
                Some(
                  ValidationError(
                    row,
                    errorType = ValidationErrorType.FreeText,
                    "landOrProperty.lesseePersonNames.upload.error.required"
                  )
                )
              } else {
                None
              }

              val optConnected = if (mConnected.isEmpty) {
                Some(
                  ValidationError(
                    row,
                    errorType = ValidationErrorType.YesNoQuestion,
                    "landOrProperty.anyLesseeConnected.upload.error.required"
                  )
                )
              } else {
                None
              }

              val optDate = if (mDate.isEmpty) {
                Some(
                  ValidationError(
                    row,
                    errorType = ValidationErrorType.LocalDateFormat,
                    "landOrProperty.leaseDate.upload.error.required"
                  )
                )
              } else {
                None
              }

              val optAmount = if (mAmount.isEmpty) {
                Some(
                  ValidationError(
                    row,
                    errorType = ValidationErrorType.Price,
                    "landOrProperty.leaseAmount.upload.error.required"
                  )
                )
              } else {
                None
              }
              val errors = listEmpty :+ optCount :+ optName :+ optConnected :+ optDate :+ optAmount
              Some(Invalid(NonEmptyList.fromListUnsafe(errors.flatten)))
          }

        case (Valid(isLeased), _, _, _, _, _) if isLeased.toUpperCase == "NO" =>
          Some((No, None).validNel)

        case (e @ Invalid(_), _, _, _, _, _) => Some(e)

        case _ => None
      }
    } yield lesseeDetails

  def validateDisposals(
    wereAnyDisposalOnThisDuringTheYear: CsvValue[String],
    totalSaleProceedIfAnyDisposal: CsvValue[Option[String]],
    nameOfPurchasers: CsvValue[Option[String]],
    isAnyPurchaserConnected: CsvValue[Option[String]],
    isTransactionSupportedByIndependentValuation: CsvValue[Option[String]],
    hasLandOrPropertyFullyDisposedOf: CsvValue[Option[String]],
    memberFullNameDob: String,
    row: Int
  ): Option[ValidatedNel[ValidationError, (YesNo, Option[DisposalDetail])]] =
    for {
      validatedWereAnyDisposalOnThisDuringTheYear <- validateYesNoQuestion(
        wereAnyDisposalOnThisDuringTheYear,
        "landOrProperty.isAnyDisposalMade",
        memberFullNameDob,
        row
      )

      maybeDisposalAmount = totalSaleProceedIfAnyDisposal.value.flatMap(
        p =>
          validatePrice(
            totalSaleProceedIfAnyDisposal.as(p),
            s"landOrProperty.disposedAmount",
            memberFullNameDob,
            row
          )
      )

      maybeNames = nameOfPurchasers.value.flatMap(
        n =>
          validateFreeText(
            nameOfPurchasers.as(n),
            s"landOrProperty.disposedNames",
            memberFullNameDob,
            row
          )
      )

      maybeConnected = isAnyPurchaserConnected.value.flatMap(
        yN =>
          validateYesNoQuestion(
            isAnyPurchaserConnected.as(yN),
            s"landOrProperty.anyConnectedPurchaser",
            memberFullNameDob,
            row
          )
      )

      maybeIsTransactionSupportedByIndependentValuation = isTransactionSupportedByIndependentValuation.value.flatMap(
        p =>
          validateYesNoQuestion(
            totalSaleProceedIfAnyDisposal.as(p),
            s"landOrProperty.isTransactionSupported",
            memberFullNameDob,
            row
          )
      )

      maybeHasLandOrPropertyFullyDisposedOf = hasLandOrPropertyFullyDisposedOf.value.flatMap(
        p =>
          validateYesNoQuestion(
            hasLandOrPropertyFullyDisposedOf.as(p),
            s"landOrProperty.isFullyDisposedOf",
            memberFullNameDob,
            row
          )
      )

      disposalDetails <- (
        validatedWereAnyDisposalOnThisDuringTheYear,
        maybeDisposalAmount,
        maybeNames,
        maybeConnected,
        maybeIsTransactionSupportedByIndependentValuation,
        maybeHasLandOrPropertyFullyDisposedOf
      ) match {
        case (Valid(isLeased), mAmount, mNames, mConnected, mIndependent, mFully) if isLeased.toUpperCase == "YES" =>
          (mAmount, mNames, mConnected, mIndependent, mFully) match {
            case (Some(amount), Some(names), Some(connected), Some(independent), Some(fully)) =>
              Some((amount, names, connected, independent, fully).mapN {
                (_amount, _names, _connected, _independent, _fully) =>
                  (
                    Yes,
                    Some(
                      DisposalDetail(
                        _amount.value,
                        _names,
                        YesNo.withNameInsensitive(_connected),
                        YesNo.withNameInsensitive(_independent),
                        YesNo.withNameInsensitive(_fully)
                      )
                    )
                  )
              })
            case _ =>
              val listEmpty = List.empty[Option[ValidationError]]
              val optAmount = if (mAmount.isEmpty) {
                Some(
                  ValidationError(
                    row,
                    errorType = ValidationErrorType.Price,
                    "landOrProperty.disposedAmount.upload.error.required"
                  )
                )
              } else {
                None
              }

              val optNames = if (mNames.isEmpty) {
                Some(
                  ValidationError(
                    row,
                    errorType = ValidationErrorType.FreeText,
                    "landOrProperty.disposedNames.upload.error.required"
                  )
                )
              } else {
                None
              }

              val optConnected = if (mConnected.isEmpty) {
                Some(
                  ValidationError(
                    row,
                    errorType = ValidationErrorType.YesNoQuestion,
                    "landOrProperty.anyConnectedPurchaser.upload.error.required"
                  )
                )
              } else {
                None
              }

              val optIndependent = if (mIndependent.isEmpty) {
                Some(
                  ValidationError(
                    row,
                    errorType = ValidationErrorType.Price,
                    "landOrProperty.isTransactionSupported.upload.error.required"
                  )
                )
              } else {
                None
              }
              val optFully = if (mFully.isEmpty) {
                Some(
                  ValidationError(
                    row,
                    errorType = ValidationErrorType.Price,
                    "landOrProperty.isFullyDisposedOf.upload.error.required"
                  )
                )
              } else {
                None
              }

              val errors = listEmpty :+ optAmount :+ optNames :+ optConnected :+ optIndependent :+ optFully
              Some(Invalid(NonEmptyList.fromListUnsafe(errors.flatten)))
          }

        case (Valid(isLeased), _, _, _, _, _) if isLeased.toUpperCase == "NO" =>
          Some((No, None).validNel)

        case (e @ Invalid(_), _, _, _, _, _) => Some(e)

        case _ => None
      }
    } yield disposalDetails

}
