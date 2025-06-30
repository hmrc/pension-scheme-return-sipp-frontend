/*
 * Copyright 2025 HM Revenue & Customs
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

package repositories

import cats.data.NonEmptyList
import config.Crypto
import models.{ValidationError, ValidationErrorType}
import models.csv.CsvRowState.{CsvRowInvalid, CsvRowValid}
import org.scalatest.OptionValues
import org.scalatest.freespec.AnyFreeSpec
import org.scalatest.matchers.must.Matchers
import org.scalatestplus.mockito.MockitoSugar
import play.api.inject.bind
import play.api.inject.guice.{GuiceApplicationBuilder, GuiceableModule}
import play.api.libs.json.*
import uk.gov.hmrc.crypto.{Decrypter, Encrypter}

import java.nio.charset.StandardCharsets

class CsvRowStateSerializationSpec extends AnyFreeSpec with Matchers with OptionValues with MockitoSugar {
  private val app = GuiceApplicationBuilder()
    .overrides(
      List[GuiceableModule](
        bind[Crypto].toInstance(Crypto.noop).eagerly()
      )*
    )
    .configure("play.filters.csp.nonce.enabled" -> false)

  private implicit val crypto: Encrypter & Decrypter = app.injector().instanceOf[Crypto].getCrypto
  private val raw = "dummy"

  "CsvRowStateSerialization" - {
    ".writes" - {
      "valid" in {
        val expectedJson: JsValue = Json.parse(s"""
             |{
             |  "line" : 1,
             |  "validated" : { },
             |  "raw" : [ "dummy" ]
             |}
             |""".stripMargin)

        val csvRowValid = CsvRowValid(1, Json.obj(), NonEmptyList.one(raw))
        val byteBuffer = CsvRowStateSerialization().write(csvRowValid)
        val decoded = StandardCharsets.UTF_8.decode(byteBuffer).toString
        formatQuotes(decoded) mustBe expectedJson.toString
      }
      "invalid" in {
        val expectedJson: JsValue = Json.parse(s"""
             |{
             |  "line" : 1,
             |  "errors" : [ {
             |    "row" : 1,
             |    "errorType" : "FirstName",
             |    "message" : "error A1"
             |  } ],
             |  "raw" : [ "dummy" ]
             |}
             |""".stripMargin)

        val validationError = ValidationError(1, ValidationErrorType.FirstName, "error A1")
        val csvRowInvalid = CsvRowInvalid[JsObject](1, NonEmptyList.one(validationError), NonEmptyList.one(raw))
        val byteBuffer = CsvRowStateSerialization().write(csvRowInvalid)
        val decoded = StandardCharsets.UTF_8.decode(byteBuffer).toString
        formatQuotes(decoded) mustBe expectedJson.toString
      }
    }
  }

  /**
   * Remove length of buffer and format quotes
   * @param decoded
   *   string to format
   * @return
   *   string with removed buffer length and quotes formatted
   */
  private def formatQuotes(decoded: String) =
    decoded.substring(CsvRowStateSerialization.IntLength + 1, decoded.length - 1).replace("\\\"", "\"")
}
