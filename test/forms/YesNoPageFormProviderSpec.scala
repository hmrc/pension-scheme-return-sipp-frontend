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

package forms

import forms.behaviours.BooleanFieldBehaviours
import play.api.data.FormError
import play.api.data.Forms.text

class YesNoPageFormProviderSpec extends BooleanFieldBehaviours {

  val requiredKey = "yesNoPage.error.required"
  val invalidKey = "yesNoPage.error.invalid"

  val form = YesNoPageFormProvider()(requiredKey, invalidKey)

  ".value" - {

    val fieldName = "value"

    behave.like(
      booleanField(
        form,
        fieldName,
        FormError(fieldName, invalidKey)
      )
    )

    behave.like(
      mandatoryField(
        form,
        fieldName,
        FormError(fieldName, requiredKey)
      )
    )

    "bind conditional yes" in {
      val form = new YesNoPageFormProvider().conditionalYes("test", text)

      val result = form.bind(
        Map("value" -> "true", "value.yes" -> "some input")
      )

      result.errors mustBe empty
      result.value mustBe Some(Right("some input"))
    }

    "bind conditional no" in {
      val form = new YesNoPageFormProvider().conditionalYes("test", text)

      val result = form.bind(
        Map("value" -> "false")
      )

      result.errors mustBe empty
      result.value mustBe Some(Left(()))
    }

    "fail to bind when yes selected but no input provided" in {
      val form = new YesNoPageFormProvider().conditionalYes("test", text)

      val result = form.bind(
        Map("value" -> "true") // Missing value.yes
      )

      result.errors must not be empty
      result.errors.head.key mustBe "value.yes"
    }


    "bind conditional yes with valid yes input" in {
      val form = new YesNoPageFormProvider().conditional(
        requiredKey = "test.required",
        mappingNo = text.verifying("no.error.required", _.nonEmpty),
        mappingYes = text.verifying("yes.error.required", _.nonEmpty)
      )

      val result = form.bind(
        Map("value" -> "true", "value.yes" -> "yes answer")
      )

      result.errors mustBe empty
      result.value mustBe Some(Right("yes answer"))
    }

    "bind conditional no with valid no input" in {
      val form = new YesNoPageFormProvider().conditional(
        requiredKey = "test.required",
        mappingNo = text.verifying("no.error.required", _.nonEmpty),
        mappingYes = text.verifying("yes.error.required", _.nonEmpty)
      )

      val result = form.bind(
        Map("value" -> "false", "value.no" -> "no answer")
      )

      result.errors mustBe empty
      result.value mustBe Some(Left("no answer"))
    }

    "fail to bind when yes selected but yes value missing" in {
      val form = new YesNoPageFormProvider().conditional(
        requiredKey = "test.required",
        mappingNo = text.verifying("no.error.required", _.nonEmpty),
        mappingYes = text.verifying("yes.error.required", _.nonEmpty)
      )

      val result = form.bind(
        Map("value" -> "true")
      )

      result.errors must not be empty
      result.errors.head.key mustBe "value.yes"
    }

    "fail to bind when no selected but no value missing" in {
      val form = new YesNoPageFormProvider().conditional(
        requiredKey = "test.required",
        mappingNo = text.verifying("no.error.required", _.nonEmpty),
        mappingYes = text.verifying("yes.error.required", _.nonEmpty)
      )

      val result = form.bind(
        Map("value" -> "false")
      )

      result.errors must not be empty
      result.errors.head.key mustBe "value.no"
    }

  }
}
