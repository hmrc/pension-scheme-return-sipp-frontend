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

package services.validation

import forms.*
import org.scalatest.freespec.AnyFreeSpec
import org.scalatest.matchers.must.Matchers
import org.scalatestplus.scalacheck.ScalaCheckPropertyChecks
import models.requests.common.YesNo.{No, Yes}
import models.{Crn, CsvHeaderKey, CsvValue, ValidationError, ValidationErrorType}
import models.requests.common.UnquotedShareDisposalDetail
import models.requests.common.UnquotedShareTransactionDetail
import models.requests.common.SharesCompanyDetails
import cats.syntax.all.*
import play.api.test.FakeRequest
import play.api.test.Helpers.stubMessagesApi
import play.api.i18n.Messages

class UnquotedSharesValidationsServiceSpec extends AnyFreeSpec with ScalaCheckPropertyChecks with Matchers {

  val nameDobProvider = NameDOBFormProvider()
  private val textFormProvider = TextFormProvider()
  private val datePageFormProvider = DatePageFormProvider()
  private val moneyFormProvider = MoneyFormProvider()
  private val intFormProvider = IntFormProvider()
  private val doubleFormProvider = DoubleFormProvider()

  val service = UnquotedSharesValidationsService(
    nameDobProvider,
    textFormProvider,
    datePageFormProvider,
    moneyFormProvider,
    intFormProvider,
    doubleFormProvider
  )

  val csvKey: CsvHeaderKey = CsvHeaderKey(key = "test", cell = "A", index = 1)
  val formKey = "key"
  val name = "fullName"

  implicit val messages: Messages = stubMessagesApi().preferred(FakeRequest())

  "UnquotedSharesValidationsServiceSpec" - {
    "validateShareCompanyDetails" - {
      "return share company details for correct input - with CRN" in {
        val result = service.validateShareCompanyDetails(
          companySharesName = CsvValue(csvKey, "Name"),
          companySharesCRN = CsvValue(csvKey, "12345678".some),
          reasonNoCRN = CsvValue(csvKey, "".some),
          sharesClass = CsvValue(csvKey, "class".some),
          noOfSharesHeld = CsvValue(csvKey, "104".some),
          memberFullNameDob = name,
          row = 1
        )
        result mustEqual SharesCompanyDetails("Name", Crn("12345678").some, None, "class", 104).valid.some
      }

      "return share company details for correct input - without CRN" in {
        val result = service.validateShareCompanyDetails(
          companySharesName = CsvValue(csvKey, "Name"),
          companySharesCRN = CsvValue(csvKey, None),
          reasonNoCRN = CsvValue(csvKey, "no crn".some),
          sharesClass = CsvValue(csvKey, "class".some),
          noOfSharesHeld = CsvValue(csvKey, "104".some),
          memberFullNameDob = name,
          row = 1
        )
        result mustEqual SharesCompanyDetails("Name", None, Some("no crn"), "class", 104).valid.some
      }

      "fail when both CRN and reasonNoCRN don't exist" in {
        val result = service.validateShareCompanyDetails(
          companySharesName = CsvValue(csvKey, "Name"),
          companySharesCRN = CsvValue(csvKey, None),
          reasonNoCRN = CsvValue(csvKey, "".some),
          sharesClass = CsvValue(csvKey, "class".some),
          noOfSharesHeld = CsvValue(csvKey, "104".some),
          memberFullNameDob = name,
          row = 1
        )
        result mustEqual ValidationError(
          1,
          ValidationErrorType.FreeText,
          "unquotedShares.reasonNoCRN.upload.error.required"
        ).invalidNel.some
      }
    }

    "validateDisposals" - {
      "return disposals details when there's disposal details" in {
        val result = service.validateDisposals(
          wereAnyDisposalOnThisDuringTheYear = CsvValue(csvKey, "YES"),
          disposedSharesAmt = CsvValue(csvKey, Some("100")),
          purchaserNames = CsvValue(csvKey, Some("test")),
          disposalConnectedParty = CsvValue(csvKey, Some("Yes")),
          independentValuation = CsvValue(csvKey, Some("Yes")),
          noOfSharesSold = CsvValue(csvKey, Some("10")),
          noOfSharesHeld = CsvValue(csvKey, Some("0")),
          memberFullNameDob = name,
          1
        )

        result mustEqual (Yes, UnquotedShareDisposalDetail(100, "test", Yes, Yes, 10, 0).some).valid.some
      }

      "return empty result when there's no disposal" in {
        val result = service.validateDisposals(
          wereAnyDisposalOnThisDuringTheYear = CsvValue(csvKey, "No"),
          disposedSharesAmt = CsvValue(csvKey, Some("")),
          purchaserNames = CsvValue(csvKey, Some("")),
          disposalConnectedParty = CsvValue(csvKey, Some("")),
          independentValuation = CsvValue(csvKey, Some("")),
          noOfSharesSold = CsvValue(csvKey, Some("")),
          noOfSharesHeld = CsvValue(csvKey, Some("")),
          memberFullNameDob = name,
          1
        )

        result mustEqual (No, None).valid.some
      }
    }

    "validateShareTransaction" - {
      "returns valid data for correct input" in {
        val result = service.validateShareTransaction(
          CsvValue(csvKey, "19"),
          CsvValue(csvKey, "Yes"),
          CsvValue(csvKey, "120"),
          name,
          1
        )
        result mustEqual UnquotedShareTransactionDetail(19, Yes, 120).valid.some
      }
    }
  }
}
