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

import cats.implicits.catsSyntaxOptionId
import com.softwaremill.quicklens._
import config.Constants
import forms._
import generators.Generators
import models.ValidationErrorType._
import models.requests.common.YesNo.{No, Yes}
import models.requests.common.CostValueOrMarketValueType.{CostValue, MarketValue}
import models.requests.common.DisposalDetail
import models.requests.raw.TangibleMoveablePropertyRaw.{RawAsset, RawDisposal}
import models.requests.raw.TangibleMoveablePropertyUpload.Asset
import models.{CsvHeaderKey, CsvValue, Money}
import org.scalatest.freespec.AnyFreeSpec
import org.scalatest.matchers.must.Matchers
import org.scalatestplus.scalacheck.ScalaCheckPropertyChecks
import play.api.i18n.Messages
import play.api.test.FakeRequest
import play.api.test.Helpers.stubMessagesApi
import utils.ValidationSpecUtils.{checkError, checkSuccess, genErr}

import java.time.LocalDate
import scala.util.Random

class TangibleMoveablePropertyValidationsServiceSpec
    extends AnyFreeSpec
    with ScalaCheckPropertyChecks
    with Generators
    with Matchers {

  private val nameDOBFormProvider = new NameDOBFormProvider {}
  private val textFormProvider = new TextFormProvider {}
  private val datePageFormProvider = new DatePageFormProvider {}
  private val moneyFormProvider = new MoneyFormProvider {}
  private val intFormProvider = new IntFormProvider {}
  private val doubleFormProvider = new DoubleFormProvider {}
  private val validator = new TangibleMoveablePropertyValidationsService(
    nameDOBFormProvider,
    textFormProvider,
    datePageFormProvider,
    moneyFormProvider,
    intFormProvider,
    doubleFormProvider
  )

  val row = 1
  val csvKey = CsvHeaderKey(key = "test", cell = "A", index = 1)
  val formKey = "key"
  val name = "fullName"
  val freeTextWith161Chars =
    "LoremipsumdolorsitametconsecteturadipisicingelitseddoeiusmodtemporincididuntutlaboreetdoloremagnaaliquaUtencoadminimveniamquisnostrudexercitationullamcolaborisnisiutaliquipexeacommodoconsequatDuisauteiruredolorinreprehenderitinvoluptatevelitessecillumdoloreeufugiatnullapariaturExcepteursintoccaecatcupidatatnonproidentsuntinculpaquiofficiadeseruntmollitanimidestlaboruma"

  implicit val messages: Messages = stubMessagesApi().preferred(FakeRequest())

  "tangibleMoveablePropertyValidationsServiceSpec" - {

    "validateMarketValueOrCostValue" - {
      "return required error if no MarketValueOrCostValue entered" in {
        val validation = validator.validateMarketValueOrCostValue(CsvValue(csvKey, ""), formKey, name, row)

        checkError(validation, List(genErr(MarketOrCostType, s"$formKey.upload.error.required")))
      }

      "return invalid error if MarketValueOrCostValue is not valid" in {
        val validation = validator.validateMarketValueOrCostValue(CsvValue(csvKey, "XXX"), formKey, name, row)

        checkError(validation, List(genErr(MarketOrCostType, s"$formKey.upload.error.invalid")))
      }

      "return successfully MarketValue Or CostValue" in {
        val table = Table(
          "string" -> "value",
          "MARKET VALUE" -> MarketValue,
          "Cost Value" -> CostValue
        )
        forEvery(table) {
          case (str, value) =>
            val validation = validator.validateMarketValueOrCostValue(CsvValue(csvKey, str), formKey, name, row)
            checkSuccess(validation, value)
        }
      }
    }

    "validateAsset" - {
      "validate correct RawAsset" in {
        checkSuccess(validator.validateAsset(rawAsset, "member", 1), asset)
      }

      "fail if description is empty" in {
        val raw = rawAsset.copy(descriptionOfAsset = csv(""))
        checkError(
          validator.validateAsset(raw, "member", 1),
          genErr(FreeText, "tangibleMoveableProperty.descriptionOfAsset.upload.error.required")
        )
      }

      "fail if description is too long" in {
        val raw = rawAsset.copy(descriptionOfAsset = csv(Random.nextString(Constants.maxTextAreaLength + 1)))
        checkError(
          validator.validateAsset(raw, "member", 1),
          genErr(FreeText, "tangibleMoveableProperty.descriptionOfAsset.upload.error.tooLong")
        )
      }

      "fail if dateOfAcquisition is empty" in {
        val raw = rawAsset.copy(dateOfAcquisitionAsset = csv(""))
        checkError(
          validator.validateAsset(raw, "member", 1),
          genErr(LocalDateFormat, "tangibleMoveableProperty.dateOfAcquisitionAsset.upload.error.required.date")
        )
      }

      "fail if totalCost is empty" in {
        val raw = rawAsset.copy(totalCostAsset = csv(""))
        checkError(
          validator.validateAsset(raw, "member", 1),
          genErr(Price, "tangibleMoveableProperty.totalCostAsset.upload.error.required")
        )
      }

      "fail if acquiredFrom is empty" in {
        val raw = rawAsset.copy(acquiredFrom = csv(""))
        checkError(
          validator.validateAsset(raw, "member", 1),
          genErr(FreeText, "tangibleMoveableProperty.whoAcquiredFromName.upload.error.required")
        )
      }

      "fail if isTxSupportedByIndependentValuation is empty" in {
        val raw = rawAsset.copy(isTxSupportedByIndependentValuation = csv(""))
        checkError(
          validator.validateAsset(raw, "member", 1),
          genErr(YesNoQuestion, "tangibleMoveableProperty.isTxSupportedByIndependentValuation.upload.error.required")
        )
      }

      "fail if totalAmountIncomeReceiptsTaxYear is empty" in {
        val raw = rawAsset.copy(totalAmountIncomeReceiptsTaxYear = csv(""))
        checkError(
          validator.validateAsset(raw, "member", 1),
          genErr(Price, "tangibleMoveableProperty.totalAmountIncomeReceiptsTaxYear.upload.error.required")
        )
      }

      "fail if isTotalCostValueOrMarketValue is empty" in {
        val raw = rawAsset.copy(isTotalCostValueOrMarketValue = csv(""))
        checkError(
          validator.validateAsset(raw, "member", 1),
          genErr(MarketOrCostType, "tangibleMoveableProperty.isTotalCostValueOrMarketValue.upload.error.required")
        )
      }

      "fail if totalCostValueTaxYearAsset is empty" in {
        val raw = rawAsset.copy(totalCostValueTaxYearAsset = csv(""))
        checkError(
          validator.validateAsset(raw, "member", 1),
          genErr(Price, "tangibleMoveableProperty.totalCostValueTaxYearAsset.upload.error.required")
        )
      }

      "fail if wereAnyDisposalOnThisDuringTheYear is empty" in {
        val raw = rawAsset.modify(_.rawDisposal.wereAnyDisposalOnThisDuringTheYear.value).setTo("")
        checkError(
          validator.validateAsset(raw, "member", 1),
          genErr(YesNoQuestion, "tangibleMoveableProperty.wereAnyDisposalOnThisDuringTheYear.upload.error.required")
        )
      }

      "disposalDetail" - {
        "not fail if totalConsiderationAmountSaleIfAnyDisposal is empty but wereAnyDisposalOnThisDuringTheYear is no" in {
          val raw = rawAsset
            .modify(_.rawDisposal.wereAnyDisposalOnThisDuringTheYear.value)
            .setTo("no")
            .modify(_.rawDisposal.totalConsiderationAmountSaleIfAnyDisposal.value.each)
            .setTo("")
          checkSuccess(
            validator.validateAsset(raw, "member", 1),
            asset.copy(wereAnyDisposalOnThisDuringTheYear = No, disposal = None)
          )
        }

        "fail if totalConsiderationAmountSaleIfAnyDisposal is empty" in {
          val raw = rawAsset.modify(_.rawDisposal.totalConsiderationAmountSaleIfAnyDisposal.value).setTo("".some)
          checkError(
            validator.validateAsset(raw, "member", 1),
            genErr(Price, "tangibleMoveableProperty.totalConsiderationAmountSaleIfAnyDisposal.upload.error.required")
          )
        }

        "fail if purchaserNames is empty" in {
          val raw = rawAsset.modify(_.rawDisposal.purchaserNames.value).setTo("".some)
          checkError(
            validator.validateAsset(raw, "member", 1),
            genErr(FreeText, "tangibleMoveableProperty.namesOfPurchasers.upload.error.required")
          )
        }

        "fail if areAnyPurchasersConnected is empty" in {
          val raw = rawAsset.modify(_.rawDisposal.areAnyPurchasersConnected.value).setTo("".some)
          checkError(
            validator.validateAsset(raw, "member", 1),
            genErr(YesNoQuestion, "tangibleMoveableProperty.areAnyPurchasersConnected.upload.error.required")
          )
        }

        "fail if isTransactionSupportedByIndependentValuation is empty" in {
          val raw = rawAsset.modify(_.rawDisposal.isTransactionSupportedByIndependentValuation.value).setTo("".some)
          checkError(
            validator.validateAsset(raw, "member", 1),
            genErr(
              YesNoQuestion,
              "tangibleMoveableProperty.isTransactionSupportedByIndependentValuation.upload.error.required"
            )
          )
        }

        "fail if isAnyPartAssetStillHeld is empty" in {
          val raw = rawAsset.modify(_.rawDisposal.isAnyPartAssetStillHeld.value).setTo("".some)
          checkError(
            validator.validateAsset(raw, "member", 1),
            genErr(YesNoQuestion, "tangibleMoveableProperty.isAnyPartAssetStillHeld.upload.error.required")
          )
        }
      }

      lazy val rawAsset = RawAsset(
        descriptionOfAsset = csv("description"),
        dateOfAcquisitionAsset = csv("15-05-2025"),
        totalCostAsset = csv("50.55"),
        acquiredFrom = csv("acq-from"),
        isTxSupportedByIndependentValuation = csv("yes"),
        totalAmountIncomeReceiptsTaxYear = csv("60.0"),
        isTotalCostValueOrMarketValue = csv("Cost Value"),
        totalCostValueTaxYearAsset = csv("55"),
        rawDisposal = RawDisposal(
          wereAnyDisposalOnThisDuringTheYear = csv("yes"),
          totalConsiderationAmountSaleIfAnyDisposal = csv("40".some),
          purchaserNames = csv("a, b, c".some),
          areAnyPurchasersConnected = csv("yes".some),
          isTransactionSupportedByIndependentValuation = csv("yes".some),
          isAnyPartAssetStillHeld = csv("no".some)
        )
      )

      lazy val asset = Asset(
        "description",
        LocalDate.of(2025, 5, 15),
        Money(50.55, "50.55"),
        "acq-from",
        Yes,
        Money(60, "60.00"),
        CostValue,
        Money(55, "55.00"),
        Yes,
        DisposalDetail(40, "a, b, c", Yes, Yes, Yes).some
      )

      def csv[A](a: A): CsvValue[A] = CsvValue(CsvHeaderKey(Random.nextString(50), "A", Random.nextInt()), a)
    }
  }
}
