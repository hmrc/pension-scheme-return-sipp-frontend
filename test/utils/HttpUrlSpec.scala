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

package utils

import play.api.libs.json.{JsNumber, JsPath, JsString, JsSuccess, Json, JsonValidationError}
import utils.HttpUrl.*

import java.net.{URI, URL}

class HttpUrlSpec extends BaseSpec {
  private val fullUrl: String = "http://localhost?arg1=1&arg2=2"
  "HttpUrl" - {
    "makeUrl with valid URL" in {
      HttpUrl.makeUrl("http://localhost", Seq(("arg1", "1"), ("arg2", "2"))).toString mustBe URI
        .create(fullUrl)
        .toString
    }

    ".reads" - {

      s"bind correctly" in {
        Json.fromJson(JsString(fullUrl)) mustEqual JsSuccess(URI.create(fullUrl).toURL)
      }

      "must fail to bind for invalid values" in {
        Json.fromJson(JsString("invalid")).asEither.left.value must contain(
          JsPath -> Seq(JsonValidationError("error.expected.url"))
        )
      }

      "must fail to bind not String values" in {
        Json.fromJson(JsNumber(1)).asEither.left.value must contain(
          JsPath -> Seq(JsonValidationError("error.expected.url"))
        )
      }
    }

    ".writes" - {
      val httpUrl = HttpUrl.makeUrl("http://localhost", Seq(("arg1", "1"), ("arg2", "2")))
      s"write" in {
        Json.toJson(httpUrl) mustEqual JsString("http://localhost?arg1=1&arg2=2")
      }
    }

//    ".formats" - {
//
//      "must be found implicitly" in {
//        implicitly[Format[Foo]]
//      }
//    }
  }

}
