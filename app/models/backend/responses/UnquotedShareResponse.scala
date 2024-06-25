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

package models.backend.responses

import cats.data.NonEmptyList
import models._
import models.requests.common.{SharesCompanyDetails, UnquotedShareDisposalDetail, UnquotedShareTransactionDetail, YesNo}
import models.requests.psr.ReportDetails
import play.api.libs.json._
import CustomFormats._

case class UnquotedShareResponse(
  reportDetails: ReportDetails,
  transactions: Option[NonEmptyList[UnquotedShareResponse.TransactionDetail]]
)

object UnquotedShareResponse {

  case class TransactionDetail(
    row: Int,
    nameDOB: NameDOB,
    nino: NinoType,
    shareCompanyDetails: SharesCompanyDetails,
    acquiredFromName: String,
    transactionDetail: UnquotedShareTransactionDetail,
    sharesDisposed: YesNo,
    sharesDisposalDetails: Option[UnquotedShareDisposalDetail],
    noOfSharesHeld: Int
  )

  implicit val formatTransactionDetails: OFormat[TransactionDetail] = Json.format[TransactionDetail]
  implicit val formatUnquotedShare: OFormat[UnquotedShareResponse] = Json.format[UnquotedShareResponse]
}
