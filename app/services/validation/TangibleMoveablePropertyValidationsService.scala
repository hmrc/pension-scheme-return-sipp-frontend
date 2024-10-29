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
import cats.syntax.all.*
import forms.*
import models.*
import models.requests.common.YesNo.{No, Yes}
import models.requests.common.*
import play.api.data.Form

import javax.inject.Inject

class TangibleMoveablePropertyValidationsService @Inject() (
  nameDOBFormProvider: NameDOBFormProvider,
  textFormProvider: TextFormProvider,
  datePageFormProvider: DatePageFormProvider,
  moneyFormProvider: MoneyFormProvider,
  intFormProvider: IntFormProvider,
  doubleFormProvider: DoubleFormProvider
) extends ValidationsService(
      nameDOBFormProvider,
      textFormProvider,
      datePageFormProvider,
      moneyFormProvider,
      intFormProvider,
      doubleFormProvider
    ) {

  private def marketValueOrCostValueTypeForm(memberFullDetails: String, key: String): Form[String] =
    textFormProvider.marketValueOrCostValueType(
      s"$key.upload.error.required",
      s"$key.upload.error.invalid",
      memberFullDetails
    )

  def validateMarketValueOrCostValue(
    marketValueOrCostValue: CsvValue[String],
    key: String,
    memberFullName: String,
    row: Int
  ): Option[ValidatedNel[ValidationError, CostOrMarketType]] = {
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
    ).map(_.map(CostOrMarketType.withNameInsensitive))
  }

  def validateDisposals(
    wereAnyDisposalOnThisDuringTheYear: CsvValue[String],
    totalSaleProceedIfAnyDisposal: CsvValue[Option[String]],
    nameOfPurchasers: CsvValue[Option[String]],
    isAnyPurchaserConnected: CsvValue[Option[String]],
    isTransactionSupportedByIndependentValuation: CsvValue[Option[String]],
    hasLandOrPropertyFullyDisposedOf: CsvValue[Option[String]],
    memberFullNameDob: String,
    row: Int
  ): Option[ValidatedNel[ValidationError, (YesNo, Option[DisposalDetails])]] =
    for {
      validatedWereAnyDisposalOnThisDuringTheYear <- validateYesNoQuestionTyped(
        wereAnyDisposalOnThisDuringTheYear,
        "tangibleMoveableProperty.wereAnyDisposalOnThisDuringTheYear",
        memberFullNameDob,
        row
      )

      maybeDisposalAmount = totalSaleProceedIfAnyDisposal.value.flatMap(p =>
        validatePrice(
          totalSaleProceedIfAnyDisposal.as(p),
          s"tangibleMoveableProperty.totalConsiderationAmountSaleIfAnyDisposal",
          memberFullNameDob,
          row
        )
      )

      maybeNames = nameOfPurchasers.value.flatMap(n =>
        validateFreeText(
          nameOfPurchasers.as(n),
          s"tangibleMoveableProperty.namesOfPurchasers",
          memberFullNameDob,
          row
        )
      )

      maybeConnected = isAnyPurchaserConnected.value.flatMap(yN =>
        validateYesNoQuestion(
          isAnyPurchaserConnected.as(yN),
          s"tangibleMoveableProperty.areAnyPurchasersConnected",
          memberFullNameDob,
          row
        )
      )

      maybeIsTransactionSupportedByIndependentValuation = isTransactionSupportedByIndependentValuation.value.flatMap(
        p =>
          validateYesNoQuestion(
            totalSaleProceedIfAnyDisposal.as(p),
            s"tangibleMoveableProperty.isTransactionSupportedByIndependentValuation",
            memberFullNameDob,
            row
          )
      )

      maybeHasLandOrPropertyFullyDisposedOf = hasLandOrPropertyFullyDisposedOf.value.flatMap(p =>
        validateYesNoQuestion(
          hasLandOrPropertyFullyDisposedOf.as(p),
          s"tangibleMoveableProperty.isAnyPartAssetStillHeld",
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
        case (Valid(isLeased), mAmount, mNames, mConnected, mIndependent, mFully) if isLeased == Yes =>
          (mAmount, mNames, mConnected, mIndependent, mFully) match {
            case (Some(amount), Some(names), Some(connected), Some(independent), Some(fully)) =>
              Some((amount, names, connected, independent, fully).mapN {
                (_amount, _names, _connected, _independent, _fully) =>
                  (
                    isLeased,
                    Some(
                      DisposalDetails(
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
              def checkRequiredV = checkRequired(row, "tangibleMoveableProperty")
              import ValidationErrorType.{Price, FreeText, YesNoQuestion}
              mergeErrors(
                checkRequiredV(mAmount, "totalConsiderationAmountSaleIfAnyDisposal", Price),
                checkRequiredV(mNames, "namesOfPurchasers", FreeText),
                checkRequiredV(mConnected, "areAnyPurchasersConnected", YesNoQuestion),
                checkRequiredV(mIndependent, "isTransactionSupportedByIndependentValuation", YesNoQuestion),
                checkRequiredV(mFully, "isAnyPartAssetStillHeld", YesNoQuestion),
              )
          }

        case (Valid(isLeased), _, _, _, _, _) if isLeased == No =>
          Some((isLeased, None).validNel)

        case (e @ Invalid(_), _, _, _, _, _) => Some(e)

        case _ => None
      }
    } yield disposalDetails

}
