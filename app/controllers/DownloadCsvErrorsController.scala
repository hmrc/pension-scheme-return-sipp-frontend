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

package controllers

import akka.NotUsed
import akka.stream.scaladsl.{Framing, Source}
import akka.util.ByteString
import config.Crypto
import controllers.DownloadCsvErrorsController._
import controllers.actions.IdentifyAndRequireData
import models.SchemeId.Srn
import models.csv.CsvRowState
import models.requests.LandOrConnectedPropertyRequest
import models.requests.raw.TangibleMoveablePropertyUpload.TangibleMoveablePropertyUpload
import models.{HeaderKeys, Journey, MemberDetailsUpload, UploadKey}
import play.api.Logger
import play.api.i18n.{I18nSupport, Messages}
import play.api.mvc.{AbstractController, Action, AnyContent, ControllerComponents}
import repositories.CsvRowStateSerialization.{read, IntLength}
import repositories.UploadRepository
import uk.gov.hmrc.crypto.{Decrypter, Encrypter}

import java.nio.{ByteBuffer, ByteOrder}
import javax.inject.Inject

class DownloadCsvErrorsController @Inject()(
  uploadRepository: UploadRepository,
  identifyAndRequireData: IdentifyAndRequireData,
  crypto: Crypto
)(cc: ControllerComponents)
    extends AbstractController(cc)
    with I18nSupport {

  val logger: Logger = Logger.apply(classOf[DownloadCsvErrorsController])
  implicit val cryptoEncDec: Encrypter with Decrypter = crypto.getCrypto
  private val lengthFieldFrame =
    Framing.lengthField(fieldLength = IntLength, maximumFrameLength = 256 * 1000, byteOrder = ByteOrder.BIG_ENDIAN)

  def downloadFile(srn: Srn, journey: Journey): Action[AnyContent] = identifyAndRequireData(srn) { implicit request =>
    val source: Source[String, NotUsed] = Source
      .fromPublisher(uploadRepository.streamUploadResult(UploadKey.fromRequest(srn, journey.uploadRedirectTag)))
      .map(ByteString.apply)
      .via(lengthFieldFrame)
      .map(_.toByteBuffer)
      .map(readBytes(_, journey))
      .map(_ + "\n")

    Ok.streamed(
      source.prepend(Source.single(headers(journey))),
      None,
      inline = false,
      Some(fileName(journey))
    )
  }
}

object DownloadCsvErrorsController {
  private def readBytes(
    bytes: ByteBuffer,
    journey: Journey
  )(implicit messages: Messages, crypto: Encrypter with Decrypter): String = journey match {
    case Journey.MemberDetails => read[MemberDetailsUpload](bytes).toCsvRow
    case Journey.InterestInLandOrProperty => read[MemberDetailsUpload](bytes).toCsvRow
    case Journey.ArmsLengthLandOrProperty => read[LandOrConnectedPropertyRequest.TransactionDetail](bytes).toCsvRow
    case Journey.TangibleMoveableProperty => read[TangibleMoveablePropertyUpload](bytes).toCsvRow
    case Journey.OutstandingLoans => ""
  }

  private def fileName(journey: Journey): String = journey match {
    case Journey.MemberDetails => "output-member-details.csv"
    case Journey.InterestInLandOrProperty => "output-interest-land-or-property.csv"
    case Journey.ArmsLengthLandOrProperty => "output-arms-length-land-or-property.csv"
    case Journey.TangibleMoveableProperty => "output-tangible-moveable-property.csv"
    case Journey.OutstandingLoans => "output-outstanding-loans.csv"
  }

  def headers(journey: Journey): String = {
    val (headers, helpers) = journey match {
      case Journey.MemberDetails =>
        HeaderKeys.headersForMemberDetails -> HeaderKeys.questionHelpersMemberDetails

      case Journey.InterestInLandOrProperty =>
        HeaderKeys.headersForInterestLandOrProperty -> HeaderKeys.questionHelpers

      case Journey.ArmsLengthLandOrProperty =>
        HeaderKeys.headersForArmsLength -> HeaderKeys.questionHelpers

      case Journey.TangibleMoveableProperty =>
        HeaderKeys.headersForTangibleMoveableProperty -> HeaderKeys.questionHelpersMoveableProperty

      case Journey.OutstandingLoans =>
        "" -> ""
    }

    (headers
      .split(";\n")
      .toList ++
      helpers
        .split(";\n")
        .toList)
      .mkString("\n")
  }

  implicit class CsvRowStateOps[T](val csvRowState: CsvRowState[T]) extends AnyVal {
    def toCsvRow(implicit messages: Messages): String =
      (csvRowState match {
        case CsvRowState.CsvRowValid(_, _, raw) => raw.toList
        case CsvRowState.CsvRowInvalid(_, errors, raw) => raw.toList ++ errors.map(m => Messages(m.message)).toList
      }).mkString(",")
  }
}
