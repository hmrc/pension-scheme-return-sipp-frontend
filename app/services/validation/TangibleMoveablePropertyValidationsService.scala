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

import cats.data.ValidatedNel
import cats.implicits._
import forms._
import models._
import models.requests.common.YesNo.{No, Yes}
import models.requests.common._
import models.requests.raw.TangibleMoveablePropertyRaw.{RawAsset, RawDisposal}
import models.requests.raw.TangibleMoveablePropertyUpload.Asset
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
  ): Option[ValidatedNel[ValidationError, CostValueOrMarketValueType]] = {
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
    ).map(_.map(CostValueOrMarketValueType(_)))
  }

  def validateAsset(
    rawAsset: RawAsset,
    memberFullNameDob: String,
    line: Int
  )(implicit messages: Messages): Option[ValidatedNel[ValidationError, Asset]] =
    for {
      validateDescriptionOfAsset <- validateFreeText(
        rawAsset.descriptionOfAsset,
        "tangibleMoveableProperty.descriptionOfAsset",
        memberFullNameDob,
        line
      )

      validatedDateOfAcquisitionAsset <- validateDate(
        date = rawAsset.dateOfAcquisitionAsset,
        key = "tangibleMoveableProperty.dateOfAcquisitionAsset",
        row = line
      )

      validatedTotalCostAsset <- validatePrice(
        rawAsset.totalCostAsset,
        "tangibleMoveableProperty.totalCostAsset",
        memberFullNameDob,
        line
      )

      validatedAcquiredFrom <- validateFreeText(
        rawAsset.acquiredFrom,
        "tangibleMoveableProperty.whoAcquiredFromName",
        memberFullNameDob,
        line
      )

      validatedIsTxSupportedByIndependentValuation <- validateYesNoQuestionTyped(
        rawAsset.isTxSupportedByIndependentValuation,
        "tangibleMoveableProperty.isTxSupportedByIndependentValuation",
        memberFullNameDob,
        line
      )

      validatedTotalAmountIncomeReceiptsTaxYear <- validatePrice(
        rawAsset.totalAmountIncomeReceiptsTaxYear,
        "tangibleMoveableProperty.totalAmountIncomeReceiptsTaxYear",
        memberFullNameDob,
        line
      )

      validatedIsTotalCostValueOrMarketValue <- validateMarketValueOrCostValue(
        rawAsset.isTotalCostValueOrMarketValue,
        "tangibleMoveableProperty.isTotalCostValueOrMarketValue",
        memberFullNameDob,
        line
      )

      validatedTotalCostValueTaxYearAsset <- validatePrice(
        rawAsset.totalCostValueTaxYearAsset,
        "tangibleMoveableProperty.totalCostValueTaxYearAsset",
        memberFullNameDob,
        line
      )

      validatedAnyDisposalOnThisDuringTheYear <- validateYesNoQuestionTyped(
        rawAsset.rawDisposal.wereAnyDisposalOnThisDuringTheYear,
        "tangibleMoveableProperty.wereAnyDisposalOnThisDuringTheYear",
        memberFullNameDob,
        line
      )

      validatedDisposals <- validatedAnyDisposalOnThisDuringTheYear.fold(
        _ => none[DisposalDetail].validNel.some, {
          case Yes => validateDisposals(rawAsset.rawDisposal, memberFullNameDob, line).map(_.map(_.some))
          case No => none[DisposalDetail].validNel.some
        }
      )
    } yield (
      validateDescriptionOfAsset,
      validatedDateOfAcquisitionAsset,
      validatedTotalCostAsset,
      validatedAcquiredFrom,
      validatedIsTxSupportedByIndependentValuation,
      validatedTotalAmountIncomeReceiptsTaxYear,
      validatedIsTotalCostValueOrMarketValue,
      validatedTotalCostValueTaxYearAsset,
      validatedAnyDisposalOnThisDuringTheYear,
      validatedDisposals
    ).mapN(Asset.apply)

  private def validateDisposals(
    raw: RawDisposal,
    memberFullNameDob: String,
    row: Int
  ): Option[ValidatedNel[ValidationError, DisposalDetail]] =
    for {
      amount <- validatePrice(
        raw.totalConsiderationAmountSaleIfAnyDisposal.map(_.mkString),
        s"tangibleMoveableProperty.totalConsiderationAmountSaleIfAnyDisposal",
        memberFullNameDob,
        row
      ).map(_.map(_.value))

      purchasers <- validateFreeText(
        raw.purchaserNames.map(_.mkString),
        "tangibleMoveableProperty.namesOfPurchasers",
        memberFullNameDob,
        row
      )

      anyPurchasersConnected <- validateYesNoQuestionTyped(
        raw.areAnyPurchasersConnected.map(_.mkString),
        "tangibleMoveableProperty.areAnyPurchasersConnected",
        memberFullNameDob,
        row
      )

      indValuation <- validateYesNoQuestionTyped(
        raw.isTransactionSupportedByIndependentValuation.map(_.mkString),
        s"tangibleMoveableProperty.isTransactionSupportedByIndependentValuation",
        memberFullNameDob,
        row
      )

      fullyDisposed <- validateYesNoQuestionTyped(
        raw.isAnyPartAssetStillHeld.map(_.mkString),
        s"tangibleMoveableProperty.isAnyPartAssetStillHeld",
        memberFullNameDob,
        row
      ).map(_.map(_.negate))
    } yield (amount, purchasers, anyPurchasersConnected, indValuation, fullyDisposed).mapN(DisposalDetail.apply)

}
