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

package forms

import org.scalatest.wordspec.AnyWordSpec
import org.scalatest.matchers.must.Matchers
import play.api.data.{FormError, Forms}
import play.api.data.FieldMapping
import play.api.data.Forms.text

class StringFormProvider
    extends FormProvider[String, String]((errorKey: String, args: Seq[String]) =>
      text.verifying(s"$errorKey.failed", _.nonEmpty).asInstanceOf[FieldMapping[String]]
    )

object TestFormProvider extends StringFormProvider

class FormProviderSpec extends AnyWordSpec with Matchers {

  val testArgs = Seq("arg1", "arg2")

  "FormProvider" should {

    "bind single value form successfully" in {
      val form = TestFormProvider("errorKey")
      val result = form.bind(Map("value" -> "some input"))
      result.errors mustBe empty
      result.value mustBe Some("some input")
    }

    "fail single value form when input is empty" in {
      val form = TestFormProvider("errorKey")
      val result = form.bind(Map("value" -> ""))
      result.errors must contain(FormError("value", "errorKey.failed"))
    }

    "bind single value with args" in {
      val form = TestFormProvider("errorKey", testArgs)
      val result = form.bind(Map("value" -> "data"))
      result.errors mustBe empty
      result.value mustBe Some("data")
    }

    "bind tuple2 form" in {
      val form = TestFormProvider("error1", "error2")
      val result = form.bind(Map("value.1" -> "a", "value.2" -> "b"))
      result.errors mustBe empty
      result.value mustBe Some(("a", "b"))
    }

    "fail tuple2 form when one value is empty" in {
      val form = TestFormProvider("error1", "error2")
      val result = form.bind(Map("value.1" -> "", "value.2" -> "b"))
      result.errors must contain(FormError("value.1", "error1.failed"))
    }

    "bind tuple2 form with args" in {
      val form = TestFormProvider("error1", "error2", testArgs)
      val result = form.bind(Map("value.1" -> "x", "value.2" -> "y"))
      result.errors mustBe empty
      result.value mustBe Some(("x", "y"))
    }

    "bind tuple3 form" in {
      val form = TestFormProvider("e1", "e2", "e3")
      val result = form.bind(Map("value.1" -> "1", "value.2" -> "2", "value.3" -> "3"))
      result.errors mustBe empty
      result.value mustBe Some(("1", "2", "3"))
    }

    "fail tuple3 form if any input is missing" in {
      val form = TestFormProvider("e1", "e2", "e3")
      val result = form.bind(Map("value.1" -> "1", "value.2" -> "", "value.3" -> "3"))
      result.errors must contain(FormError("value.2", "e2.failed"))
    }

    "bind tuple3 with args" in {
      val form = TestFormProvider("e1", "e2", "e3", testArgs)
      val result = form.bind(Map("value.1" -> "a", "value.2" -> "b", "value.3" -> "c"))
      result.errors mustBe empty
      result.value mustBe Some(("a", "b", "c"))
    }
  }
}
