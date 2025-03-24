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
import models.{CsvHeaderKey, NameDOB, NinoType}
import models.csv.CsvRowState.CsvRowValid
import models.keys.ArmsLengthKeys
import models.requests.LandOrConnectedPropertyApi
import models.requests.common.{AddressDetails, DisposalDetails, LesseeDetails, RegistryDetails}
import models.requests.common.YesNo.Yes
import services.validation.LandOrPropertyValidationsService
import play.api.i18n.Messages
import play.api.test.Helpers.stubMessages
import utils.BaseSpec

import java.time.LocalDate

class ArmsLengthLandOrPropertyCsvRowValidatorSpec extends BaseSpec {
  private val nameDobProvider = NameDOBFormProvider()
  private val textFormProvider = TextFormProvider()
  private val datePageFormProvider = DatePageFormProvider()
  private val moneyFormProvider = MoneyFormProvider()
  private val intFormProvider = IntFormProvider()
  private val doubleFormProvider = DoubleFormProvider()

  implicit val messages: Messages = stubMessages()
  val service = LandOrPropertyValidationsService(
    nameDobProvider,
    textFormProvider,
    datePageFormProvider,
    moneyFormProvider,
    intFormProvider,
    doubleFormProvider
  )
  val validator = ArmsLengthLandOrPropertyCsvRowValidator(service)

  val headers = ArmsLengthKeys.headers.zipWithIndex.map { case (key, i) => CsvHeaderKey(key, key, i) }.toList
  val validData = NonEmptyList.of(
    "Name", // firstName
    "lastName", // lastName
    dateToString(LocalDate.now().minusYears(20)), // memberDateOfBirth
    "AB123456C", // memberNino
    "", // memberReasonNoNino
    dateToString(LocalDate.now()), // acquisitionDate
    "Yes", // isLandOrPropertyInUK
    "Address line1", // landOrPropertyUkAddressLine1
    "Address line2", // landOrPropertyUkAddressLine2
    "Address line3", // landOrPropertyUkAddressLine3
    "London", // landOrPropertyUkTownOrCity
    "PO57CD", // landOrPropertyUkPostCode
    "", // landOrPropertyAddressLine1
    "", // landOrPropertyAddressLine2
    "", // landOrPropertyAddressLine3
    "", // landOrPropertyAddressLine4
    "UK", // landOrPropertyCountry
    "Yes", // isThereLandRegistryReference
    "1234", // landRegistryRefOrReason
    "AcquiredFromName", // acquiredFromName
    "70000", // totalCostOfLandOrPropertyAcquired
    "Yes", // isSupportedByAnIndependentValuation
    "Yes", // isPropertyHeldJointly
    "5", // howManyPersonsJointlyOwn
    "Yes", // isPropertyDefinedAsSchedule29a
    "Yes", // isLeased
    "5", // lesseeCount
    "Yes", // areAnyLesseesConnected
    dateToString(LocalDate.now().minusYears(1)), // annualLeaseDate
    "10000", // annualLeaseAmount
    "20000", // totalAmountOfIncomeAndReceipts
    "Yes", // wasAnyDisposalOnThisDuringTheYear
    "1000", // totalSaleProceedIfAnyDisposal
    "Purchaser1,Purchaser2", // namesOfPurchasers
    "Yes", // areAnyPurchaserConnected
    "Yes", // isTrxSupportedByIndependentValuation
    "Yes" // isFullyDisposed
  )
  val validationParams = CsvRowValidationParameters(Some(LocalDate.now().plusDays(15)))

  "ArmsLengthLandOrPropertyCsvRowValidator" - {
    "validate correct row" in {
      val actual = validator.validate(1, validData, headers, validationParams)
      val expected = CsvRowValid(
        1,
        LandOrConnectedPropertyApi.TransactionDetail(
          1.some,
          NameDOB("Name", "lastName", LocalDate.now().minusYears(20)),
          NinoType("AB123456C".some, None),
          LocalDate.now(),
          Yes,
          AddressDetails(
            "Address line1",
            "Address line2",
            "Address line3".some,
            "London".some,
            None,
            "PO57CD".some,
            "GB"
          ),
          RegistryDetails(Yes, "1234".some, None),
          "AcquiredFromName",
          70000,
          Yes,
          Yes,
          noOfPersons = 5.some,
          Yes,
          Yes,
          LesseeDetails(5, Yes, LocalDate.now().minusYears(1), 10000).some,
          20000,
          Yes,
          DisposalDetails(1000, "Purchaser1,Purchaser2", Yes, Yes, Yes).some
        ),
        validData
      )
      actual mustBe expected
    }
  }
}
