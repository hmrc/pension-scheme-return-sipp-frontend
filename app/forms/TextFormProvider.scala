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

import config.Constants.{postcodeCharsRegex, postcodeFormatRegex}
import forms.mappings.Mappings
import play.api.data.Form
import uk.gov.hmrc.domain.Nino

import javax.inject.Inject

class TextFormProvider @Inject()() {

  protected[forms] val yesNoRegex = "^(YES|NO)$"
  protected[forms] val yesNoMaxLength = 3
  protected[forms] val nameRegex = "^[a-zA-Z\\-' ]+$"
  protected[forms] val textAreaRegex = """^[a-zA-Z0-9\-'" \t\r\n,.@/]+$"""
  protected[forms] val textAreaMaxLength = 160

  protected[forms] val addressLineRegex = """^[a-zA-Z0-9\-'" \t\r\n]+$"""
  protected[forms] val addressLineAreaMaxLength = 35

  val formKey = "value"

  def apply(requiredKey: String): Form[String] =
    Form(
      formKey -> Mappings.text(requiredKey)
    )

  def textArea(
    requiredKey: String,
    tooLongKey: String,
    invalidCharactersKey: String,
    args: Any*
  ): Form[String] = Form(
    formKey -> Mappings.validatedText(
      requiredKey,
      List((textAreaRegex, invalidCharactersKey)),
      textAreaMaxLength,
      tooLongKey,
      args: _*
    )
  )

  def nino(
    requiredKey: String,
    invalidKey: String,
    args: Any*
  ): Form[Nino] =
    Form(
      formKey -> Mappings.nino(requiredKey, invalidKey, args: _*)
    )

  def name(
    requiredKey: String,
    tooLongKey: String,
    invalidCharactersKey: String,
    args: Any*
  ): Form[String] = Form(
    formKey -> Mappings.validatedText(
      requiredKey,
      List((nameRegex, invalidCharactersKey)),
      textAreaMaxLength,
      tooLongKey,
      args: _*
    )
  )

  def yesNo(
    requiredKey: String,
    tooLongKey: String,
    invalidCharactersKey: String,
    args: Any*
  ): Form[String] = Form(
    formKey -> Mappings.validatedText(
      requiredKey,
      List((yesNoRegex, invalidCharactersKey)),
      yesNoMaxLength,
      tooLongKey,
      args: _*
    )
  )

  def addressLine(
    requiredKey: String,
    tooLongKey: String,
    invalidCharactersKey: String,
    args: Any*
  ): Form[String] = Form(
    formKey -> Mappings.validatedText(
      requiredKey,
      List((addressLineRegex, invalidCharactersKey)),
      addressLineAreaMaxLength,
      tooLongKey,
      args: _*
    )
  )

  def postcode(
    requiredKey: String,
    invalidCharactersKey: String,
    invalidFormatKey: String,
    args: Any*
  ): Form[String] =
    Form(
      formKey -> Mappings.validatedText(
        requiredKey,
        List(
          (postcodeCharsRegex, invalidCharactersKey),
          (postcodeFormatRegex, invalidFormatKey)
        ),
        textAreaMaxLength,
        invalidFormatKey,
        args: _*
      )
    )

  def text(
    requiredKey: String,
    tooLongKey: String,
    invalidCharactersKey: String,
    args: Any*
  ): Form[String] = Form(
    formKey -> Mappings.validatedText(
      requiredKey,
      List((textAreaRegex, invalidCharactersKey)),
      textAreaMaxLength,
      tooLongKey,
      args: _*
    )
  )
}
