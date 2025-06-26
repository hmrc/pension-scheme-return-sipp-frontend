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

import models.Enumerable
import models.GenericFormMapper.{ConditionalRadioMapper, StringFieldMapper}
import org.scalatest.wordspec.AnyWordSpec
import org.scalatest.matchers.must.Matchers
import play.api.data.Mapping
import play.api.data.Forms._

sealed trait ExampleOption
object ExampleOption {
  case object Yes extends ExampleOption
  case object No extends ExampleOption

  val values: Seq[ExampleOption] = Seq(Yes, No)

  implicit val enumerable: Enumerable[ExampleOption] = Enumerable(values.map(v => v.toString -> v)*)
}

case class ConditionalInput(value: String)

object Mappers {
  
  implicit val stringFieldMapper: StringFieldMapper[ConditionalInput] =
    new StringFieldMapper[ConditionalInput] {
      override def to(a: String): ConditionalInput = ConditionalInput(a)
      override def from(b: ConditionalInput): Option[String] = Some(b.value)
    }
  
  implicit val conditionalRadioMapper: ConditionalRadioMapper[ConditionalInput, (String, Option[ConditionalInput])] =
    new ConditionalRadioMapper[ConditionalInput, (String, Option[ConditionalInput])] {
      override def to(in: (String, Option[ConditionalInput])): (String, Option[ConditionalInput]) = in
      override def from(out: (String, Option[ConditionalInput])): Option[(String, Option[ConditionalInput])] = Some(out)
    }
}

class RadioListFormProviderSpec extends AnyWordSpec with Matchers {

  import ExampleOption._
  import Mappers._

  val requiredKey = "example.error.required"

  "RadioListFormProvider" should {

    "bind a simple radio option" in {
      val form = new RadioListFormProvider().apply[ExampleOption](requiredKey)

      val result = form.bind(Map("value" -> "Yes"))
      result.errors mustBe empty
      result.value mustBe Some(Yes)
    }

    "fail to bind when missing radio input" in {
      val form = new RadioListFormProvider().apply[ExampleOption](requiredKey)

      val result = form.bind(Map.empty)
      result.errors.map(_.key) must contain("value")
    }

    "bind a conditional field when the correct value is selected" in {
      val form = new RadioListFormProvider().singleConditional[(String, Option[ConditionalInput]), ConditionalInput](
        requiredKey = "error.required",
        conditionalKey = "yes",
        conditionalMapping = text.verifying("error.required", _.nonEmpty).transform[ConditionalInput](ConditionalInput.apply, _.value)
      )

      val result = form.bind(Map("value" -> "yes", "isRequired" -> "some input"))

      result.errors mustBe empty
      result.value mustBe Some(("yes", Some(ConditionalInput("some input"))))
    }

    "fail conditional field validation when value is missing" in {
      val form = new RadioListFormProvider().singleConditional[(String, Option[ConditionalInput]), ConditionalInput](
        requiredKey = "error.required",
        conditionalKey = "yes",
        conditionalMapping = text.verifying("error.required", _.nonEmpty).transform[ConditionalInput](ConditionalInput.apply, _.value)
      )

      val result = form.bind(Map("value" -> "yes"))

      result.errors.map(_.key) must contain("isRequired")
    }

    "bind with conditionalM for multiple options with conditional field" in {
      val form = new RadioListFormProvider().conditionalM[(String, Option[ConditionalInput]), ConditionalInput](
        requiredKey = "error.required",
        conditionalMappings = List(
          "yes" -> Some(
            text
              .verifying("error.required", _.nonEmpty)
              .transform[ConditionalInput](ConditionalInput.apply, _.value)
          ),
          "maybe" -> None
        )
      )

      val result = form.bind(Map("value" -> "yes", "yes-conditional" -> "extra info"))

      result.errors mustBe empty
      result.value mustBe Some(("yes", Some(ConditionalInput("extra info"))))
    }

  }
}