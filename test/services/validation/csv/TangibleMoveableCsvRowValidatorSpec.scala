/*
 * Copyright 2024 HM Revenue & Customs
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

package services.validation.csv

import cats.data.NonEmptyList
import cats.syntax.option.*
import forms.*
import models.csv.CsvRowState.CsvRowValid
import models.keys.TangibleKeys
import models.requests.common.CostOrMarketType.CostValue
import models.requests.TangibleMoveablePropertyApi
import models.requests.common.YesNo.Yes
import models.{CsvHeaderKey, NameDOB, NinoType}
import play.api.i18n.Messages
import play.api.test.Helpers.stubMessages
import services.validation.TangibleMoveablePropertyValidationsService
import utils.BaseSpec
import models.requests.common.DisposalDetails

import java.time.LocalDate

class TangibleMoveableCsvRowValidatorSpec extends BaseSpec {
  val nameDobProvider = NameDOBFormProvider()
  private val textFormProvider = TextFormProvider()
  private val datePageFormProvider = DatePageFormProvider()
  private val moneyFormProvider = MoneyFormProvider()
  private val intFormProvider = IntFormProvider()
  private val doubleFormProvider = DoubleFormProvider()

  implicit val messages: Messages = stubMessages()
  val service = TangibleMoveablePropertyValidationsService(
    nameDobProvider,
    textFormProvider,
    datePageFormProvider,
    moneyFormProvider,
    intFormProvider,
    doubleFormProvider
  )
  val validator = TangibleMoveableCsvRowValidator(service)

  val headers = TangibleKeys.headers.zipWithIndex.map { case (key, i) => CsvHeaderKey(key, key, i) }.toList
  val validData = NonEmptyList.of(
    "name", // firstName
    "lastName", // lastName
    dateToString(LocalDate.now().minusYears(20)), // memberDateOfBirth
    "AB123456C", // memberNino
    "", // memberReasonNoNino
    "desc", // descriptionOfAsset
    dateToString(LocalDate.now().minusYears(5)), // dateOfAcquisition
    "1000", // totalCostOfAsset
    "acquiredFrom", // acquiredFrom
    "Yes", // isIndependentValuation
    "100", // totalIncomeInTaxYear
    "2000", // totalCostOrMarketValue
    "Cost Value", // isTotalCostOrMarketValue
    "Yes", // areAnyDisposals
    "40", // disposalsAmount
    "Purchasers1,Purchasers2", // namesOfPurchasers
    "Yes", // areAnyPurchasersConnected
    "Yes", // wasTxSupportedIndValuation
    "Yes" // isAnyPartStillHeld
  )
  val validationParams = CsvRowValidationParameters(Some(LocalDate.now().plusDays(15)))

  "TangibleMoveableCsvRowValidator" - {
    "validate correct row" in {
      val actual = validator.validate(1, validData, headers, validationParams)
      val expected = CsvRowValid(
        1,
        TangibleMoveablePropertyApi.TransactionDetail(
          row = 1.some,
          nameDOB = NameDOB("name", "lastName", LocalDate.now().minusYears(20)),
          nino = NinoType("AB123456C".some, None),
          assetDescription = "desc",
          acquisitionDate = LocalDate.now().minusYears(5),
          totalCost = 1000,
          acquiredFromName = "acquiredFrom",
          independentValuation = Yes,
          totalIncomeOrReceipts = 100,
          costMarketValue = 2000,
          costOrMarket = CostValue,
          isPropertyDisposed = Yes,
          disposalDetails = DisposalDetails(40, "Purchasers1,Purchasers2", Yes, Yes, Yes).some
        ),
        validData
      )
      actual mustBe expected
    }
  }

}
