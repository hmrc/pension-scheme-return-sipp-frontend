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

package models.requests

import models.DateRange
import play.api.libs.json.{Json, OFormat}

case class PsrSubmissionRequest(
  pstr: String,
  fbNumber: Option[String],
  periodStartDate: Option[String],
  psrVersion: Option[String],
  psaId: String,
  taxYear: DateRange,
  schemeName: Option[String],
  checkReturnDates: String
)

object PsrSubmissionRequest {
  implicit val formats: OFormat[PsrSubmissionRequest] = Json.format[PsrSubmissionRequest]

  case class PsrSubmittedResponse(
    emailSent: Boolean
  )

  object PsrSubmittedResponse {
    implicit val formats: OFormat[PsrSubmittedResponse] = Json.format[PsrSubmittedResponse]
  }
}
