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

package models.backend.responses

import cats.data.NonEmptyList
import models.CustomFormats._
import models.requests.{
  AssetsFromConnectedPartyApi,
  LandOrConnectedPropertyApi,
  OutstandingLoanApi,
  TangibleMoveablePropertyApi,
  UnquotedShareApi
}
import models.requests.psr.ReportDetails
import play.api.libs.json.{Json, OFormat}

case class PSRSubmissionResponse(
  details: ReportDetails,
  accountingPeriodDetails: Option[AccountingPeriodDetails],
  landConnectedParty: Option[NonEmptyList[LandOrConnectedPropertyApi.TransactionDetail]],
  landArmsLength: Option[NonEmptyList[LandOrConnectedPropertyApi.TransactionDetail]],
  otherAssetsConnectedParty: Option[NonEmptyList[AssetsFromConnectedPartyApi.TransactionDetail]],
  tangibleProperty: Option[NonEmptyList[TangibleMoveablePropertyApi.TransactionDetail]],
  loanOutstanding: Option[NonEmptyList[OutstandingLoanApi.TransactionDetail]],
  unquotedShares: Option[NonEmptyList[UnquotedShareApi.TransactionDetail]],
  versions: Versions
)

object PSRSubmissionResponse {
  implicit val formatPSRSubmissionResponse: OFormat[PSRSubmissionResponse] = Json.format[PSRSubmissionResponse]
}
