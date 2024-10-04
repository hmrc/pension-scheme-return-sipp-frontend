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

import forms._
import generators.Generators
import models.ValidationErrorType._
import models.requests.common.CostOrMarketType.{CostValue, MarketValue}
import models.requests.common.{DisposalDetails, YesNo}
import models.{CsvHeaderKey, CsvValue}
import org.scalatest.freespec.AnyFreeSpec
import org.scalatest.matchers.must.Matchers
import org.scalatestplus.scalacheck.ScalaCheckPropertyChecks
import play.api.i18n.Messages
import play.api.test.FakeRequest
import play.api.test.Helpers.stubMessagesApi
import utils.ValidationSpecUtils.{checkError, checkSuccess, genErr}

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
          "Market Value" -> MarketValue,
          "Cost Value" -> CostValue
        )
        forEvery(table) { case (str, value) =>
          val validation = validator.validateMarketValueOrCostValue(CsvValue(csvKey, str), formKey, name, row)
          checkSuccess(validation, value)
        }
      }
    }

    "validateDisposals" - {
      // ERROR TESTS
      "get errors for validateDisposals" - {
        "return required error if wereAnyDisposalOnThisDuringTheYear not entered" in {
          val validation = validator.validateDisposals(
            wereAnyDisposalOnThisDuringTheYear = CsvValue(csvKey, ""),
            totalSaleProceedIfAnyDisposal = CsvValue(csvKey, None),
            nameOfPurchasers = CsvValue(csvKey, None),
            isAnyPurchaserConnected = CsvValue(csvKey, None),
            isTransactionSupportedByIndependentValuation = CsvValue(csvKey, None),
            hasLandOrPropertyFullyDisposedOf = CsvValue(csvKey, None),
            memberFullNameDob = name,
            row = row
          )

          checkError(
            validation,
            List(
              genErr(YesNoQuestion, "tangibleMoveableProperty.wereAnyDisposalOnThisDuringTheYear.upload.error.required")
            )
          )
        }

        "return invalid error if wereAnyDisposalOnThisDuringTheYear is not valid" in {
          val validation = validator.validateDisposals(
            wereAnyDisposalOnThisDuringTheYear = CsvValue(csvKey, "ADS"),
            totalSaleProceedIfAnyDisposal = CsvValue(csvKey, None),
            nameOfPurchasers = CsvValue(csvKey, None),
            isAnyPurchaserConnected = CsvValue(csvKey, None),
            isTransactionSupportedByIndependentValuation = CsvValue(csvKey, None),
            hasLandOrPropertyFullyDisposedOf = CsvValue(csvKey, None),
            memberFullNameDob = name,
            row = row
          )

          checkError(
            validation,
            List(
              genErr(YesNoQuestion, "tangibleMoveableProperty.wereAnyDisposalOnThisDuringTheYear.upload.error.invalid")
            )
          )
        }

        "return all required errors if wereAnyDisposalOnThisDuringTheYear is YES but others not entered" in {
          val validation = validator.validateDisposals(
            wereAnyDisposalOnThisDuringTheYear = CsvValue(csvKey, "YES"),
            totalSaleProceedIfAnyDisposal = CsvValue(csvKey, None),
            nameOfPurchasers = CsvValue(csvKey, None),
            isAnyPurchaserConnected = CsvValue(csvKey, None),
            isTransactionSupportedByIndependentValuation = CsvValue(csvKey, None),
            hasLandOrPropertyFullyDisposedOf = CsvValue(csvKey, None),
            memberFullNameDob = name,
            row = row
          )

          checkError(
            validation,
            List(
              genErr(Price, "tangibleMoveableProperty.totalConsiderationAmountSaleIfAnyDisposal.upload.error.required"),
              genErr(FreeText, "tangibleMoveableProperty.namesOfPurchasers.upload.error.required"),
              genErr(YesNoQuestion, "tangibleMoveableProperty.areAnyPurchasersConnected.upload.error.required"),
              genErr(
                Price,
                "tangibleMoveableProperty.isTransactionSupportedByIndependentValuation.upload.error.required"
              ),
              genErr(Price, "tangibleMoveableProperty.isAnyPartAssetStillHeld.upload.error.required")
            )
          )
        }

        "return numericValueRequired error for totalSaleProceedIfAnyDisposal" in {
          val validation = validator.validateDisposals(
            wereAnyDisposalOnThisDuringTheYear = CsvValue(csvKey, "Yes"),
            totalSaleProceedIfAnyDisposal = CsvValue(csvKey, Some("ASD")),
            nameOfPurchasers = CsvValue(csvKey, Some("Name 1, Name 2")),
            isAnyPurchaserConnected = CsvValue(csvKey, Some("No")),
            isTransactionSupportedByIndependentValuation = CsvValue(csvKey, Some("No")),
            hasLandOrPropertyFullyDisposedOf = CsvValue(csvKey, Some("Yes")),
            memberFullNameDob = name,
            row = row
          )

          checkError(
            validation,
            List(
              genErr(
                Price,
                "tangibleMoveableProperty.totalConsiderationAmountSaleIfAnyDisposal.upload.error.numericValueRequired"
              )
            )
          )
        }

        "return required error for nameOfPurchasers if not entered" in {
          val validation = validator.validateDisposals(
            wereAnyDisposalOnThisDuringTheYear = CsvValue(csvKey, "Yes"),
            totalSaleProceedIfAnyDisposal = CsvValue(csvKey, Some("123.2")),
            nameOfPurchasers = CsvValue(csvKey, Some("")),
            isAnyPurchaserConnected = CsvValue(csvKey, Some("No")),
            isTransactionSupportedByIndependentValuation = CsvValue(csvKey, Some("No")),
            hasLandOrPropertyFullyDisposedOf = CsvValue(csvKey, Some("Yes")),
            memberFullNameDob = name,
            row = row
          )

          checkError(
            validation,
            List(
              genErr(FreeText, "tangibleMoveableProperty.namesOfPurchasers.upload.error.required")
            )
          )
        }

        "return tooLong error for nameOfPurchasers if entered more than 160 chars" in {
          val validation = validator.validateDisposals(
            wereAnyDisposalOnThisDuringTheYear = CsvValue(csvKey, "Yes"),
            totalSaleProceedIfAnyDisposal = CsvValue(csvKey, Some("123.2")),
            nameOfPurchasers = CsvValue(csvKey, Some(freeTextWith161Chars)),
            isAnyPurchaserConnected = CsvValue(csvKey, Some("No")),
            isTransactionSupportedByIndependentValuation = CsvValue(csvKey, Some("No")),
            hasLandOrPropertyFullyDisposedOf = CsvValue(csvKey, Some("Yes")),
            memberFullNameDob = name,
            row = row
          )

          checkError(
            validation,
            List(
              genErr(FreeText, "tangibleMoveableProperty.namesOfPurchasers.upload.error.tooLong")
            )
          )
        }

        "return invalid error for isAnyPurchaserConnected if wrong" in {
          val validation = validator.validateDisposals(
            wereAnyDisposalOnThisDuringTheYear = CsvValue(csvKey, "Yes"),
            totalSaleProceedIfAnyDisposal = CsvValue(csvKey, Some("123.2")),
            nameOfPurchasers = CsvValue(csvKey, Some("Name 1, Name 2")),
            isAnyPurchaserConnected = CsvValue(csvKey, Some("ASD")),
            isTransactionSupportedByIndependentValuation = CsvValue(csvKey, Some("No")),
            hasLandOrPropertyFullyDisposedOf = CsvValue(csvKey, Some("Yes")),
            memberFullNameDob = name,
            row = row
          )

          checkError(
            validation,
            List(
              genErr(YesNoQuestion, "tangibleMoveableProperty.areAnyPurchasersConnected.upload.error.invalid")
            )
          )
        }

        "return invalid error for isTransactionSupportedByIndependentValuation if wrong" in {
          val validation = validator.validateDisposals(
            wereAnyDisposalOnThisDuringTheYear = CsvValue(csvKey, "Yes"),
            totalSaleProceedIfAnyDisposal = CsvValue(csvKey, Some("123.2")),
            nameOfPurchasers = CsvValue(csvKey, Some("Name 1, Name 2")),
            isAnyPurchaserConnected = CsvValue(csvKey, Some("Yes")),
            isTransactionSupportedByIndependentValuation = CsvValue(csvKey, Some("X")),
            hasLandOrPropertyFullyDisposedOf = CsvValue(csvKey, Some("Yes")),
            memberFullNameDob = name,
            row = row
          )

          checkError(
            validation,
            List(
              genErr(
                YesNoQuestion,
                "tangibleMoveableProperty.isTransactionSupportedByIndependentValuation.upload.error.invalid"
              )
            )
          )
        }

        "return invalid error for hasLandOrPropertyFullyDisposedOf if wrong" in {
          val validation = validator.validateDisposals(
            wereAnyDisposalOnThisDuringTheYear = CsvValue(csvKey, "Yes"),
            totalSaleProceedIfAnyDisposal = CsvValue(csvKey, Some("123.2")),
            nameOfPurchasers = CsvValue(csvKey, Some("Name 1, Name 2")),
            isAnyPurchaserConnected = CsvValue(csvKey, Some("Yes")),
            isTransactionSupportedByIndependentValuation = CsvValue(csvKey, Some("yes")),
            hasLandOrPropertyFullyDisposedOf = CsvValue(csvKey, Some("nx")),
            memberFullNameDob = name,
            row = row
          )

          checkError(
            validation,
            List(
              genErr(YesNoQuestion, "tangibleMoveableProperty.isAnyPartAssetStillHeld.upload.error.invalid")
            )
          )
        }

        "return multiple errors" in {
          val validation = validator.validateDisposals(
            wereAnyDisposalOnThisDuringTheYear = CsvValue(csvKey, "Yes"),
            totalSaleProceedIfAnyDisposal = CsvValue(csvKey, Some("123.2")),
            nameOfPurchasers = CsvValue(csvKey, Some("Name 1, Name 2")),
            isAnyPurchaserConnected = CsvValue(csvKey, Some("")),
            isTransactionSupportedByIndependentValuation = CsvValue(csvKey, Some("a")),
            hasLandOrPropertyFullyDisposedOf = CsvValue(csvKey, Some("nx")),
            memberFullNameDob = name,
            row = row
          )

          checkError(
            validation,
            List(
              genErr(YesNoQuestion, "tangibleMoveableProperty.areAnyPurchasersConnected.upload.error.required"),
              genErr(
                YesNoQuestion,
                "tangibleMoveableProperty.isTransactionSupportedByIndependentValuation.upload.error.invalid"
              ),
              genErr(YesNoQuestion, "tangibleMoveableProperty.isAnyPartAssetStillHeld.upload.error.invalid")
            )
          )
        }
      }

      // SUCCESS TESTS
      "get success results for validateDisposals" - {
        "return successfully Yes, None if wereAnyDisposalOnThisDuringTheYear entered as NO" in {
          val validation = validator.validateDisposals(
            wereAnyDisposalOnThisDuringTheYear = CsvValue(csvKey, "No"),
            totalSaleProceedIfAnyDisposal = CsvValue(csvKey, None),
            nameOfPurchasers = CsvValue(csvKey, None),
            isAnyPurchaserConnected = CsvValue(csvKey, None),
            isTransactionSupportedByIndependentValuation = CsvValue(csvKey, None),
            hasLandOrPropertyFullyDisposedOf = CsvValue(csvKey, None),
            memberFullNameDob = name,
            row = row
          )

          checkSuccess(
            validation,
            (YesNo.No, None)
          )
        }

        "return successfully DisposalDetails if wereAnyDisposalOnThisDuringTheYear Yes and all details entered correctly" in {
          val validation = validator.validateDisposals(
            wereAnyDisposalOnThisDuringTheYear = CsvValue(csvKey, "Yes"),
            totalSaleProceedIfAnyDisposal = CsvValue(csvKey, Some("123.22")),
            nameOfPurchasers = CsvValue(csvKey, Some("Name 1, Name 2")),
            isAnyPurchaserConnected = CsvValue(csvKey, Some("No")),
            isTransactionSupportedByIndependentValuation = CsvValue(csvKey, Some("No")),
            hasLandOrPropertyFullyDisposedOf = CsvValue(csvKey, Some("Yes")),
            memberFullNameDob = name,
            row = row
          )

          checkSuccess(
            validation,
            (
              YesNo.Yes,
              Some(
                DisposalDetails(
                  disposedPropertyProceedsAmt = 123.22,
                  purchasersNames = "Name 1, Name 2",
                  anyPurchaserConnectedParty = YesNo.No,
                  independentValuationDisposal = YesNo.No,
                  propertyFullyDisposed = YesNo.Yes
                )
              )
            )
          )
        }
      }
    }
  }
}
