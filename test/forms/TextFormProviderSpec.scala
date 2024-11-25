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

import forms.behaviours.FieldBehaviours
import models.{Crn, Utr}
import org.scalacheck.Gen
import org.scalacheck.Gen.*
import play.api.data.Form
import uk.gov.hmrc.domain.Nino

class TextFormProviderSpec extends FieldBehaviours {

  private val formProvider = TextFormProvider()

  ".apply" - {
    val form: Form[String] = formProvider("required")

    behave.like(fieldThatBindsValidData(form, "value", nonEmptyAlphaString))
    behave.like(mandatoryField(form, "value", "required"))
    behave.like(trimmedField(form, "value", "     untrimmed value  "))
  }

  ".textarea" - {
    val form: Form[String] = formProvider.textArea(
      "required",
      "tooLong",
      "invalid"
    )

    val invalidTextGen = asciiPrintableStr.suchThat(_.trim.nonEmpty).suchThat(!_.matches(formProvider.textAreaRegex))

    behave.like(fieldThatBindsValidData(form, "value", nonEmptyAlphaString))
    behave.like(mandatoryField(form, "value", "required"))
    behave.like(invalidField(form, "value", "invalid", invalidTextGen))
    behave.like(textTooLongField(form, "value", "tooLong", formProvider.textAreaMaxLength))
    behave.like(trimmedField(form, "value", "     untrimmed value  "))

    "allow punctuation" - {
      behave.like(
        fieldThatBindsValidData(
          form,
          "value",
          "Hi, I'm a test on date 10-12-2010 with email test@email.com \n with a newline and \ttab"
        )
      )
    }
  }

  ".nino" - {
    val duplicates = Gen.listOfN(8, ninoGen).sample.value
    val invalidNinoGen = nonEmptyString.suchThat(!Nino.isValid(_))

    val ninoForm: Form[Nino] =
      formProvider.ninoWithDuplicateControl(
        "nino.error.required",
        "nino.error.invalid",
        duplicates,
        "nino.error.duplicate"
      )

    behave.like(mandatoryField(ninoForm, "value", "nino.error.required"))
    behave.like(invalidField(ninoForm, "value", "nino.error.invalid", invalidNinoGen))
  }

  ".crn" - {
    val crnForm: Form[Crn] =
      formProvider.crn("crn.error.required", "crn.error.invalid")

    behave.like(fieldThatBindsValidData(crnForm, "value", crnGen.map(_.value)))
    behave.like(mandatoryField(crnForm, "value", "crn.error.required"))
    behave.like(invalidField(crnForm, "value", "crn.error.invalid", Gen.oneOf(List("NOTAVALID"))))
  }

  ".utr" - {
    val utrForm: Form[Utr] =
      formProvider.utr("utr.error.required", "utr.error.invalid")

    behave.like(fieldThatBindsValidData(utrForm, "value", utrGen.map(_.value)))
    behave.like(mandatoryField(utrForm, "value", "utr.error.required"))
    behave.like(invalidField(utrForm, "value", "utr.error.invalid", Gen.oneOf(List("NOTAVALID"))))
  }

  ".yesNo" - {
    val yesNoForm: Form[String] =
      formProvider.yesNo("yesNo.error.required", "yesNo.error.tooLong", "yesNo.error.invalid")

    behave.like(fieldThatBindsValidData(yesNoForm, "value", yesNoGen.map(_.entryName.toUpperCase)))
    behave.like(mandatoryField(yesNoForm, "value", "yesNo.error.required"))
    behave.like(invalidField(yesNoForm, "value", "yesNo.error.invalid", Gen.oneOf(List("NOT_VALID"))))
  }

  ".acquiredFromType" - {
    val acquiredFrom: Form[String] =
      formProvider.acquiredFromType("acquiredFromType.error.required", "acquiredFromType.error.invalid")

    behave.like(fieldThatBindsValidData(acquiredFrom, "value", acquiredFromTypeGen))
    behave.like(mandatoryField(acquiredFrom, "value", "acquiredFromType.error.required"))
    behave.like(invalidField(acquiredFrom, "value", "acquiredFromType.error.invalid", Gen.oneOf(List("NOT_VALID"))))
  }

  ".connectedOrUnconnectedType" - {
    val connectedOrUnConnectedForm: Form[String] =
      formProvider.connectedOrUnconnectedType(
        "connectedOrUnconnectedType.error.required",
        "connectedOrUnconnectedType.error.invalid"
      )

    behave.like(fieldThatBindsValidData(connectedOrUnConnectedForm, "value", connectedOrUnconnectedTypeGen))
    behave.like(mandatoryField(connectedOrUnConnectedForm, "value", "connectedOrUnconnectedType.error.required"))
    behave.like(
      invalidField(
        connectedOrUnConnectedForm,
        "value",
        "connectedOrUnconnectedType.error.invalid",
        Gen.oneOf(List("NOT_VALID"))
      )
    )
  }

  ".name" - {
    val form: Form[String] = formProvider.name("required", "tooLong", "invalid")

    val invalidTextGen = stringLengthBetween(1, formProvider.nameMaxLength, asciiPrintableChar)
      .suchThat(!_.matches(formProvider.nameRegex))

    behave.like(fieldThatBindsValidData(form, "value", stringLengthBetween(1, formProvider.nameMaxLength, alphaChar)))
    behave.like(mandatoryField(form, "value", "required"))
    behave.like(invalidField(form, "value", "invalid", invalidTextGen))
    behave.like(textTooLongField(form, "value", "tooLong", formProvider.nameMaxLength))
    behave.like(trimmedField(form, "value", "     untrimmed value  "))

    "specific test value example" - {
      behave.like(fieldThatBindsValidData(form, "value", "Aa Z'z-z"))
      behave.like(invalidField(form, "value", "invalid", "John324324"))
    }
  }
}
