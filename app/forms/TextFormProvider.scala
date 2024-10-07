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

import config.Constants.postcodeFormatRegex
import forms.mappings.Mappings
import models.{Crn, Utr}
import play.api.data.Form
import uk.gov.hmrc.domain.Nino
import utils.Country

import javax.inject.Inject

class TextFormProvider @Inject() () {

  protected[forms] val yesNoRegex = "(?i)^(yes|no)$"
  protected[forms] val nameRegex = "^[a-zA-Z\\-' ]+$"
  protected[forms] val nameMaxLength = 35
  protected[forms] val textAreaRegex = """^[a-zA-Z0-9\-'" \t\r\n,.@/]+$"""
  protected[forms] val textAreaMaxLength = 160
  protected[forms] val acquiredFromType = "(?i)^(INDIVIDUAL|COMPANY|PARTNERSHIP|OTHER)$"
  protected[forms] val connectedOrUnconnectedType = "(?i)^(CONNECTED|UNCONNECTED)$"
  protected[forms] val marketValueOrCostValueType = "(?i)^(MARKET VALUE|COST VALUE)$"

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

  def ninoWithDuplicateControl(
    requiredKey: String,
    invalidKey: String,
    duplicates: List[Nino],
    duplicateKey: String,
    args: Any*
  ): Form[Nino] =
    Form(
      formKey -> Mappings.ninoNoDuplicates(requiredKey, invalidKey, duplicates, duplicateKey, args: _*)
    )

  def nino(
    requiredKey: String,
    invalidKey: String,
    args: Any*
  ): Form[Nino] =
    Form(
      formKey -> Mappings.nino(requiredKey, invalidKey, args: _*)
    )

  def crn(
    requiredKey: String,
    invalidKey: String,
    args: Any*
  ): Form[Crn] =
    Form(
      formKey -> Mappings.crn(requiredKey, invalidKey, invalidKey, args: _*)
    )

  def utr(
    requiredKey: String,
    invalidKey: String,
    args: Any*
  ): Form[Utr] =
    Form(
      formKey -> Mappings.utr(requiredKey, invalidKey, args: _*)
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
      nameMaxLength,
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
      textAreaMaxLength,
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

  def country(
    requiredKey: String,
    invalidCharactersKey: String
  ): Form[String] = Form(
    formKey -> Mappings.selectCountry(
      Country.countries,
      requiredKey,
      invalidCharactersKey
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
          (postcodeFormatRegex, invalidFormatKey)
        ),
        textAreaMaxLength,
        invalidFormatKey,
        args: _*
      )
    )

  def acquiredFromType(
    requiredKey: String,
    invalidType: String,
    args: Any*
  ): Form[String] =
    Form(
      formKey -> Mappings.validatedText(
        requiredKey,
        List(
          (acquiredFromType, invalidType)
        ),
        textAreaMaxLength,
        invalidType,
        args: _*
      )
    )

  def connectedOrUnconnectedType(
    requiredKey: String,
    invalidType: String,
    args: Any*
  ): Form[String] =
    Form(
      formKey -> Mappings.validatedText(
        requiredKey,
        List(
          (connectedOrUnconnectedType, invalidType)
        ),
        textAreaMaxLength,
        invalidType,
        args: _*
      )
    )

  def marketValueOrCostValueType(
    requiredKey: String,
    invalidType: String,
    args: Any*
  ): Form[String] =
    Form(
      formKey -> Mappings.validatedText(
        requiredKey,
        List(
          (marketValueOrCostValueType, invalidType)
        ),
        textAreaMaxLength,
        invalidType,
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

  def freeText(
    requiredKey: String,
    tooLongKey: String,
    args: Any*
  ): Form[String] = Form(
    formKey -> Mappings.validatedText(
      requiredKey,
      List.empty,
      textAreaMaxLength,
      tooLongKey,
      args: _*
    )
  )
}
