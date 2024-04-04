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
import cats.implicits._
import forms._
import models._
import models.requests.YesNo
import models.requests.YesNo.{No, Yes}
import models.requests.common._
import play.api.data.Form
import play.api.i18n.Messages

import javax.inject.Inject

class TangibleMoveablePropertyValidationsService @Inject()(
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

  private def acquiredFromTypeForm(memberFullDetails: String, key: String): Form[String] =
    textFormProvider.acquiredFromType(
      s"$key.upload.error.required",
      s"$key.upload.error.invalid",
      memberFullDetails
    )

  private def connectedOrUnconnectedTypeForm(memberFullDetails: String, key: String): Form[String] =
    textFormProvider.connectedOrUnconnectedType(
      s"$key.upload.error.required",
      s"$key.upload.error.invalid",
      memberFullDetails
    )

  private def marketValueOrCostValueTypeForm(memberFullDetails: String, key: String): Form[String] =
    textFormProvider.marketValueOrCostValueType(
      s"$key.upload.error.required",
      s"$key.upload.error.invalid",
      memberFullDetails
    )

  def validateAcquiredFromType(
    acquiredFromType: CsvValue[String],
    memberFullName: String,
    row: Int,
    key: String
  )(implicit messages: Messages): Option[ValidatedNel[ValidationError, String]] = {
    val boundForm = acquiredFromTypeForm(memberFullName, key)
      .bind(
        Map(
          textFormProvider.formKey -> acquiredFromType.value.toUpperCase
        )
      )

    formToResult(
      boundForm,
      row,
      errorTypeMapping = _ => ValidationErrorType.AcquiredFromType,
      cellMapping = _ => Some(acquiredFromType.key.cell)
    )
  }

  def validateConnectedOrUnconnected(
    connectedOrUnconnected: CsvValue[String],
    key: String,
    memberFullName: String,
    row: Int
  )(implicit messages: Messages): Option[ValidatedNel[ValidationError, String]] = {
    val boundForm = connectedOrUnconnectedTypeForm(memberFullName, key)
      .bind(
        Map(
          textFormProvider.formKey -> connectedOrUnconnected.value.toUpperCase
        )
      )

    formToResult(
      boundForm,
      row,
      errorTypeMapping = _ => ValidationErrorType.ConnectedUnconnectedType,
      cellMapping = _ => Some(connectedOrUnconnected.key.cell)
    )
  }

  def validateMarketValueOrCostValue(
    marketValueOrCostValue: CsvValue[String],
    key: String,
    memberFullName: String,
    row: Int
  )(implicit messages: Messages): Option[ValidatedNel[ValidationError, String]] = {
    val boundForm = marketValueOrCostValueTypeForm(memberFullName, key)
      .bind(
        Map(
          textFormProvider.formKey -> marketValueOrCostValue.value.toUpperCase
        )
      )

    formToResult(
      boundForm,
      row,
      errorTypeMapping = _ => ValidationErrorType.MarketOrCostType,
      cellMapping = _ => Some(marketValueOrCostValue.key.cell)
    )
  }

  def validateAcquiredFrom(
    acquiredFromType: CsvValue[String],
    acquirerNinoForIndividual: CsvValue[Option[String]],
    acquirerCrnForCompany: CsvValue[Option[String]],
    acquirerUtrForPartnership: CsvValue[Option[String]],
    whoAcquiredFromTypeReasonAsset: CsvValue[Option[String]],
    memberFullNameDob: String,
    row: Int
  )(implicit messages: Messages): Option[ValidatedNel[ValidationError, AcquiredFromType]] =
    for {

      validatedAcquiredFromType <- validateAcquiredFromType(
        acquiredFromType,
        memberFullNameDob,
        row,
        "tangibleMoveableProperty.acquiredFromType"
      )

      maybeNino = acquirerNinoForIndividual.value.flatMap(
        nino =>
          validateNino(
            acquirerNinoForIndividual.as(nino),
            memberFullNameDob,
            row,
            "tangibleMoveableProperty.acquirerNino"
          )
      )
      maybeCrn = acquirerCrnForCompany.value.flatMap(
        crn =>
          validateCrn(acquirerCrnForCompany.as(crn), memberFullNameDob, row, "tangibleMoveableProperty.acquirerCrn")
      )
      maybeUtr = acquirerUtrForPartnership.value.flatMap(
        utr =>
          validateUtr(acquirerUtrForPartnership.as(utr), memberFullNameDob, row, "tangibleMoveableProperty.acquirerUtr")
      )
      maybeOther = whoAcquiredFromTypeReasonAsset.value.flatMap(
        other =>
          validateFreeText(
            whoAcquiredFromTypeReasonAsset.as(other),
            "tangibleMoveableProperty.noWhoAcquiredFromTypeReason",
            memberFullNameDob,
            row
          )
      )

      validatedAcquiredFrom <- (
        validatedAcquiredFromType,
        maybeNino,
        maybeCrn,
        maybeUtr,
        maybeOther
      ) match {
        case (Valid(acquiredFromType), mNino, _, _, mOther) if acquiredFromType.toUpperCase == "INDIVIDUAL" =>
          (mNino, mOther) match {
            case (Some(nino), _) =>
              Some(nino.map { nino =>
                AcquiredFromType(
                  indivOrOrgType = IndOrOrgType(acquiredFromType.toUpperCase),
                  idNumber = Some(nino.value),
                  reasonNoIdNumber = None,
                  otherDescription = None
                )
              })

            case (None, Some(other)) =>
              Some(other.map { other =>
                AcquiredFromType(
                  indivOrOrgType = IndOrOrgType(acquiredFromType.toUpperCase),
                  idNumber = None,
                  reasonNoIdNumber = Some(other),
                  otherDescription = None
                )
              })

            case _ =>
              Some(
                ValidationError(
                  row,
                  ValidationErrorType.FreeText,
                  message = "tangibleMoveableProperty.acquirerNino.upload.error.required"
                ).invalidNel
              )
          }

        case (Valid(acquiredFromType), _, mCrn, _, mOther) if acquiredFromType.toUpperCase == "COMPANY" =>
          (mCrn, mOther) match {
            case (Some(crn), _) =>
              Some(crn.map { crn =>
                AcquiredFromType(
                  indivOrOrgType = IndOrOrgType(acquiredFromType.toUpperCase),
                  idNumber = Some(crn.value),
                  reasonNoIdNumber = None,
                  otherDescription = None
                )
              })
            case (None, Some(other)) =>
              Some(other.map { other =>
                AcquiredFromType(
                  indivOrOrgType = IndOrOrgType(acquiredFromType.toUpperCase),
                  idNumber = None,
                  reasonNoIdNumber = Some(other),
                  otherDescription = None
                )
              })

            case _ =>
              Some(
                ValidationError(
                  row,
                  ValidationErrorType.FreeText,
                  message = "tangibleMoveableProperty.acquirerCrn.upload.error.required"
                ).invalidNel
              )
          }

        case (Valid(acquiredFromType), _, _, mUtr, mOther) if acquiredFromType.toUpperCase == "PARTNERSHIP" =>
          (mUtr, mOther) match {
            case (Some(utr), _) =>
              Some(utr.map { utr =>
                AcquiredFromType(
                  indivOrOrgType = IndOrOrgType(acquiredFromType.toUpperCase),
                  idNumber = Some(utr.value),
                  reasonNoIdNumber = None,
                  otherDescription = None
                )
              })

            case (None, Some(other)) =>
              Some(other.map { other =>
                AcquiredFromType(
                  indivOrOrgType = IndOrOrgType(acquiredFromType.toUpperCase),
                  idNumber = None,
                  reasonNoIdNumber = Some(other),
                  otherDescription = None
                )
              })

            case _ =>
              Some(
                ValidationError(
                  row,
                  ValidationErrorType.FreeText,
                  message = "tangibleMoveableProperty.acquirerUtr.upload.error.required"
                ).invalidNel
              )
          }

        case (Valid(acquiredFromType), _, _, _, mOther) if acquiredFromType.toUpperCase == "OTHER" =>
          mOther match {
            case Some(other) =>
              Some(other.map { other =>
                AcquiredFromType(
                  indivOrOrgType = IndOrOrgType(acquiredFromType.toUpperCase),
                  idNumber = None,
                  reasonNoIdNumber = None,
                  otherDescription = Some(other)
                )
              })

            case _ =>
              Some(
                ValidationError(
                  row,
                  ValidationErrorType.FreeText,
                  message = "tangibleMoveableProperty.noWhoAcquiredFromTypeReason.upload.error.required"
                ).invalidNel
              )
          }

        case (e @ Invalid(_), _, _, _, _) => Some(e)

        case _ => None
      }
    } yield validatedAcquiredFrom

  def validatePurchaser(
    count: Int,
    purchaserName: CsvValue[Option[String]],
    purchaserConnectedOrUnconnected: CsvValue[Option[String]],
    memberFullNameDob: String,
    row: Int,
    isRequired: Boolean = false
  )(implicit messages: Messages): Option[ValidatedNel[ValidationError, Option[DispossalDetail.PurchaserDetail]]] =
    if (isRequired && purchaserName.value.isEmpty) {
      Some(
        ValidationError(
          row,
          ValidationErrorType.FreeText,
          message = "tangibleMoveableProperty.firstPurchaser.upload.error.required"
        ).invalidNel
      )
    } else if (purchaserName.value.isEmpty) {
      Some(None.validNel)
    } else {
      for {
        name <- validateFreeText(
          purchaserName.as(purchaserName.value.get),
          s"tangibleMoveableProperty.purchaserName.${count}",
          memberFullNameDob,
          row
        )
        maybeConnectedOrUnconnected = purchaserConnectedOrUnconnected.value.flatMap(
          c =>
            validateConnectedOrUnconnected(
              purchaserConnectedOrUnconnected.as(c),
              s"tangibleMoveableProperty.purchaserType.$count",
              memberFullNameDob,
              row
            )
        )

        purchaser <- (
          name,
          maybeConnectedOrUnconnected
        ) match {
          case (Valid(name), mCon) =>
            mCon match {
              case (Some(con)) =>
                con match {
                  case (e @ Invalid(_)) => Some(e)
                  case _ =>
                    Some(con.map { c =>
                      Some(
                        DispossalDetail.PurchaserDetail(
                          ConnectedOrUnconnectedType.uploadStringToRequestConnectedOrUnconnected(c),
                          name
                        )
                      )
                    })
                }
              case _ =>
                Some(
                  ValidationError(
                    row,
                    ValidationErrorType.FreeText,
                    message = s"tangibleMoveableProperty.purchaserType.$count.upload.error.required"
                  ).invalidNel
                )
            }
          case (e @ Invalid(_), _) => Some(e)
          case _ => None
        }
      } yield purchaser
    }

  def validateDisposals(
    wereAnyDisposalOnThisDuringTheYear: CsvValue[String],
    totalConsiderationAmountSaleIfAnyDisposal: CsvValue[Option[String]],
    wereAnyDisposals: CsvValue[String],
    purchasers: List[(CsvValue[Option[String]], CsvValue[Option[String]])],
    isTransactionSupportedByIndependentValuation: CsvValue[Option[String]],
    isAnyPartAssetStillHeld: CsvValue[Option[String]],
    memberFullNameDob: String,
    row: Int
  )(implicit messages: Messages): Option[ValidatedNel[ValidationError, (YesNo, Option[DispossalDetail])]] =
    for {
      validatedWereAnyDisposalOnThisDuringTheYear <- validateYesNoQuestion(
        wereAnyDisposalOnThisDuringTheYear,
        "tangibleMoveableProperty.wereAnyDisposalOnThisDuringTheYear",
        memberFullNameDob,
        row
      )

      validatedWereAnyDisposals <- validateYesNoQuestion(
        wereAnyDisposals,
        "tangibleMoveableProperty.wereAnyDisposals",
        memberFullNameDob,
        row
      )

      maybeDisposalAmount = totalConsiderationAmountSaleIfAnyDisposal.value.flatMap(
        p =>
          validatePrice(
            totalConsiderationAmountSaleIfAnyDisposal.as(p),
            s"tangibleMoveableProperty.totalConsiderationAmountSaleIfAnyDisposal",
            memberFullNameDob,
            row
          )
      )

      purchaserList = purchasers.zipWithIndex.map {
        case (p, i) =>
          validatePurchaser(i + 1, p._1, p._2, memberFullNameDob, row, isRequired = (i == 0))
      }

      maybeIsTransactionSupportedByIndependentValuation = isTransactionSupportedByIndependentValuation.value.flatMap(
        p =>
          validateYesNoQuestion(
            isTransactionSupportedByIndependentValuation.as(p),
            s"tangibleMoveableProperty.isTransactionSupportedByIndependentValuation",
            memberFullNameDob,
            row
          )
      )

      maybeIsAnyPartAssetStillHeld = isAnyPartAssetStillHeld.value.flatMap(
        p =>
          validateYesNoQuestion(
            isAnyPartAssetStillHeld.as(p),
            s"tangibleMoveableProperty.isAnyPartAssetStillHeld",
            memberFullNameDob,
            row
          )
      )

      disposalDetails <- (
        validatedWereAnyDisposals,
        validatedWereAnyDisposalOnThisDuringTheYear,
        maybeDisposalAmount,
        purchaserList,
        maybeIsTransactionSupportedByIndependentValuation,
        maybeIsAnyPartAssetStillHeld
      ) match {
        case (Valid(wereDisposals), disposalsYear, mAmount, mPurchasers, mIndependent, mFully)
            if wereDisposals.toUpperCase == "YES" =>
          (mAmount.sequence, mPurchasers.sequence, mIndependent, mFully) match {
            case (mAmount, mPeople, mDepend, mFully) =>
              (mAmount, mPeople, mDepend, mFully) match {
                case (mAmount, Some(people), Some(depend), Some(fully)) =>
                  people.sequence match {
                    case Invalid(errorList) =>
                      Some(Validated.invalid(errorList))
                    case Valid(details) =>
                      Some((mAmount, depend, fully, disposalsYear).mapN { (_amount, _depend, _fully, _) =>
                        (
                          Yes,
                          Some(
                            DispossalDetail(
                              disposedPropertyProceedsAmt = _amount.map(_.value),
                              independentValutionDisposal = YesNo.uploadYesNoToRequestYesNo(_depend),
                              propertyFullyDisposed = YesNo.uploadYesNoToRequestYesNo(_fully),
                              purchaserDetails = details.flatten
                            )
                          )
                        )
                      })
                  }
                case _ =>
                  val listEmpty = List.empty[Option[ValidationError]]
                  val purchaserErrors = mPurchasers.sequence match {
                    case Some(people) =>
                      people.sequence match {
                        case Invalid(errorList) =>
                          errorList.toList
                        case _ =>
                          List.empty
                      }
                    case _ =>
                      List.empty
                  }
                  val optAmount = if (disposalsYear.map(_.toUpperCase == "YES").getOrElse(false)) {
                    Some(
                      ValidationError(
                        row,
                        errorType = ValidationErrorType.Price,
                        "tangibleMoveableProperty.totalConsiderationAmountSaleIfAnyDisposal.upload.error.required"
                      )
                    )
                  } else {
                    None
                  }

                  val optDepend = if (mDepend.isEmpty) {
                    Some(
                      ValidationError(
                        row,
                        errorType = ValidationErrorType.YesNoQuestion,
                        "tangibleMoveableProperty.isTransactionSupportedByIndependentValuation.upload.error.required"
                      )
                    )
                  } else {
                    None
                  }

                  val optFullyDis = if (mFully.isEmpty) {
                    Some(
                      ValidationError(
                        row,
                        errorType = ValidationErrorType.YesNoQuestion,
                        "tangibleMoveableProperty.isAnyPartAssetStillHeld.upload.error.required"
                      )
                    )
                  } else {
                    None
                  }
                  val errors = listEmpty :+ purchaserErrors :+ optAmount :+ optDepend :+ optFullyDis
                  Some(Invalid(NonEmptyList.fromListUnsafe(errors.flatten)))
              }
            case _ => None
          }

        case (Valid(wereDisposals), _, _, _, _, _) if wereDisposals.toUpperCase == "NO" =>
          Some((No, None).validNel)

        case (e @ Invalid(_), _, _, _, _, _) => Some(e)

        case _ => None
      }
    } yield disposalDetails

}
