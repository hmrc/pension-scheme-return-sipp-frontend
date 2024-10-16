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

import forms.mappings.Mappings
import forms.mappings.errors.DateFormErrors
import models.NameDOB
import play.api.data.Form
import play.api.data.Forms.mapping
import cats.syntax.option.*

import javax.inject.Inject

class NameDOBFormProvider @Inject() () extends Mappings {

  val nameMaxLength = 35
  val nameRegex = "^[a-zA-Z\\-' ]+$"

  val firstName = "firstName"
  val lastName = "lastName"
  val dateOfBirth = "dateOfBirth"

  def apply(
    firstNameRequired: String,
    firstNameInvalid: String,
    firstNameLength: String,
    lastNameRequired: String,
    lastNameInvalid: String,
    lastNameLength: String,
    dateFormErrors: DateFormErrors
  ): Form[NameDOB] =
    Form(
      mapping(
        firstName -> text(firstNameRequired).verifying(
          firstError[String](
            regexp(nameRegex, firstNameInvalid),
            failWhen(name => name.split("[- ]").length > 2, firstNameInvalid, firstNameInvalid),
            maxLength(nameMaxLength, firstNameLength)
          )
        ),
        lastName -> text(lastNameRequired).verifying(
          firstError[String](
            regexp(nameRegex, lastNameInvalid),
            failWhen(name => name.split("[- ]").length > 2, lastNameInvalid, lastNameInvalid),
            maxLength(nameMaxLength, lastNameLength)
          )
        ),
        dateOfBirth -> localDate(dateFormErrors)
      )(NameDOB.apply)(Tuple.fromProductTyped(_).some)
    )
}
