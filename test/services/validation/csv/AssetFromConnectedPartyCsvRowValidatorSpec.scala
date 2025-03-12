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
import forms.{
  DatePageFormProvider,
  DoubleFormProvider,
  IntFormProvider,
  MoneyFormProvider,
  NameDOBFormProvider,
  TextFormProvider
}
import services.validation.AssetsFromConnectedPartyValidationsService
import utils.BaseSpec
import models.keys.AssetFromConnectedPartyKeys
import play.api.i18n.Messages
import play.api.test.Helpers.stubMessages
import models.csv.CsvRowState.CsvRowValid
import models.{Crn, CsvHeaderKey, NameDOB, NinoType}
import models.requests.AssetsFromConnectedPartyApi
import models.requests.common.{DisposalDetails, SharesCompanyDetails}
import models.requests.common.YesNo.Yes

import java.time.LocalDate

class AssetFromConnectedPartyCsvRowValidatorSpec extends BaseSpec {
  private val nameDobProvider = NameDOBFormProvider()
  private val textFormProvider = TextFormProvider()
  private val datePageFormProvider = DatePageFormProvider()
  private val moneyFormProvider = MoneyFormProvider()
  private val intFormProvider = IntFormProvider()
  private val doubleFormProvider = DoubleFormProvider()

  val service = AssetsFromConnectedPartyValidationsService(
    nameDobProvider,
    textFormProvider,
    datePageFormProvider,
    moneyFormProvider,
    intFormProvider,
    doubleFormProvider
  )
  val validator = AssetFromConnectedPartyCsvRowValidator(service)
  implicit val messages: Messages = stubMessages()
  val validationParams = CsvRowValidationParameters(Some(LocalDate.now().plusDays(15)))

  val headers =
    AssetFromConnectedPartyKeys.headers.zipWithIndex.map { case (key, i) => CsvHeaderKey(key, key, i) }.toList

  val validData = NonEmptyList.of(
    "name", // firstName
    "lastName", // lastName
    dateToString(LocalDate.now().minusYears(20)), // memberDateOfBirth
    "AB123456C", // memberNino
    "", // memberReasonNoNino
    "1", // countOfTransactions
    dateToString(LocalDate.now()), // dateOfAcquisition
    "desc", // descriptionOfAsset
    "Yes", // isAcquisitionOfShares
    "CompName", // companyNameShares
    "12345678", // companyCRNShares
    "", // companyNoCRNReasonShares
    "class", // companyClassShares
    "50", // companyNumberOfShares
    "AcquiredFrom", // acquiredFrom
    "100000", // totalCostOfAsset
    "Yes", // isIndependentValuation
    "Yes", // isFinanceAct
    "10000", // totalIncomeInTaxYear
    "Yes", // areAnyDisposalsYear
    "50000", // disposalsAmount
    "Purchasers1,Purchasers2", // namesOfPurchasers
    "Yes", // areConnectedPartiesPurchasers
    "Yes", // wasTransactionSupportedIndValuation
    "Yes", // hasFullyDisposedOf
    "Yes", // wasDisposalOfShares
    "0" // disposalOfSharesNumberHeld
  )

  "AssetFromConnectedPartyCsvRowValidatorSpec" - {
    "validate correct row" in {
      val actual = validator.validate(1, validData, headers, validationParams)
      val expected = CsvRowValid(
        1,
        AssetsFromConnectedPartyApi.TransactionDetail(
          row = 1.some,
          nameDOB = NameDOB("name", "lastName", LocalDate.now().minusYears(20)),
          nino = NinoType("AB123456C".some, None),
          acquisitionDate = LocalDate.now(),
          assetDescription = "desc",
          acquisitionOfShares = Yes,
          sharesCompanyDetails = SharesCompanyDetails("CompName", Crn("12345678").some, None, "class", 50).some,
          acquiredFromName = "AcquiredFrom",
          totalCost = 100000,
          independentValuation = Yes,
          tangibleSchedule29A = Yes,
          totalIncomeOrReceipts = 10000,
          isPropertyDisposed = Yes,
          disposalDetails = DisposalDetails(50000, "Purchasers1,Purchasers2", Yes, Yes, Yes).some,
          disposalOfShares = Yes.some,
          noOfSharesHeld = 0.some,
          transactionCount = None
        ),
        validData
      )
      actual mustBe expected
    }
  }
}
