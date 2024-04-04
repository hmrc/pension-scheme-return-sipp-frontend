/*
 * Copyright 2024 HM Revenue & Customs
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

package services.validation.csv

import cats.data.NonEmptyList
import models.CsvHeaderKey
import models.Journey.InterestInLandOrProperty
import models.csv.CsvRowState
import models.requests.LandOrConnectedPropertyRequest
import play.api.i18n.Messages
import services.validation.LandOrPropertyValidationsService

import javax.inject.Inject

class InterestInLandOrPropertyCsvRowValidator @Inject()(
  landOrPropertyValidationsService: LandOrPropertyValidationsService
) extends CsvRowValidator[LandOrConnectedPropertyRequest.TransactionDetail] {
  override def validate(
    line: Int,
    values: NonEmptyList[String],
    headers: List[CsvHeaderKey],
    csvRowValidationParameters: CsvRowValidationParameters
  )(implicit messages: Messages): CsvRowState[LandOrConnectedPropertyRequest.TransactionDetail] =
    LandOrPropertyCsvRowValidator.validateJourney(
      InterestInLandOrProperty,
      line,
      values,
      headers,
      csvRowValidationParameters.schemeWindUpDate,
      landOrPropertyValidationsService
    )
}