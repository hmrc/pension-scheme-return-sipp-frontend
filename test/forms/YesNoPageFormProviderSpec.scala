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

class YesNoPageFormProviderSpec extends BooleanFieldBehaviours {
  private val fieldName = "value"

  private val requiredKey = "yesNoPage.error.required"
  private val invalidKey = "yesNoPage.error.invalid"

  private val formWithInvalidKey = YesNoPageFormProvider()(invalidKey)
  private val formWithTwoKeys = YesNoPageFormProvider()(requiredKey, invalidKey)

  ".value" - {

    behave.like(
      booleanField(
        formWithTwoKeys,
        fieldName,
        FormError(fieldName, invalidKey)
      )
    )

    behave.like(
      mandatoryField(
        formWithTwoKeys,
        fieldName,
        FormError(fieldName, requiredKey)
      )
    )
  }

  List(true, false).foreach { logical =>
    s"single keys as $logical" - {

      "bind true" in {
        val result = formWithInvalidKey.bind(Map(fieldName -> "true"))
        result.value.value mustBe true
        result.errors mustBe empty
      }

      "bind false" in {
        val result = formWithInvalidKey.bind(Map(fieldName -> "false"))
        result.value.value mustBe false
        result.errors mustBe empty
      }
    }
  }
}
