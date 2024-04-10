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
import controllers.DownloadCsvController._
import controllers.actions.IdentifyAndRequireData
import models.Journey._
import models.SchemeId.Srn
import models.csv.CsvRowState
import models.{HeaderKeys, Journey, UploadKey}
import play.api.Logger
import play.api.i18n.{I18nSupport, Messages}
import play.api.libs.json.JsValue
import play.api.mvc.{AbstractController, Action, AnyContent, ControllerComponents}
import repositories.CsvRowStateSerialization.{IntLength, read}
import repositories.UploadRepository
import uk.gov.hmrc.crypto.{Decrypter, Encrypter}

import java.nio.{ByteBuffer, ByteOrder}
import javax.inject.Inject

class DownloadCsvController @Inject()(
  uploadRepository: UploadRepository,
  identifyAndRequireData: IdentifyAndRequireData,
  crypto: Crypto
)(cc: ControllerComponents)
    extends AbstractController(cc)
    with I18nSupport {

  val logger: Logger = Logger.apply(classOf[DownloadCsvController])
  implicit val cryptoEncDec: Encrypter with Decrypter = crypto.getCrypto
  private val lengthFieldFrame =
    Framing.lengthField(fieldLength = IntLength, maximumFrameLength = 256 * 1000, byteOrder = ByteOrder.BIG_ENDIAN)

  def downloadFile(srn: Srn, journey: Journey): Action[AnyContent] = identifyAndRequireData(srn) { implicit request =>
    val source: Source[String, NotUsed] = Source
      .fromPublisher(uploadRepository.streamUploadResult(UploadKey.fromRequest(srn, journey.uploadRedirectTag)))
      .map(ByteString.apply)
      .via(lengthFieldFrame)
      .map(_.toByteBuffer)
      .map(readBytes)
      .map(_ + newLine)

    Ok.streamed(
      source.prepend(Source.single(headers(journey) + newLine)),
      None,
      inline = false,
      Some(fileName(journey))
    )
  }
}

object DownloadCsvController {
  private val newLine = "\n"

  private def readBytes(
    bytes: ByteBuffer
  )(implicit messages: Messages, crypto: Encrypter with Decrypter): String =
    read[JsValue](bytes).toCsvRow

  private def fileName(journey: Journey): String = journey match {
    case MemberDetails => "output-member-details.csv"
    case InterestInLandOrProperty => "output-interest-land-or-property.csv"
    case ArmsLengthLandOrProperty => "output-arms-length-land-or-property.csv"
    case TangibleMoveableProperty => "output-tangible-moveable-property.csv"
    case OutstandingLoans => "output-outstanding-loans.csv"
    case UnquotedShares => "output-unquoted-shares.csv"
  }

  def headers(journey: Journey): String = {
    val (headers, helpers) = journey match {
      case MemberDetails =>
        HeaderKeys.headersForMemberDetails -> HeaderKeys.questionHelpersMemberDetails

      case InterestInLandOrProperty =>
        HeaderKeys.headersForInterestLandOrProperty -> HeaderKeys.questionHelpers

      case ArmsLengthLandOrProperty =>
        HeaderKeys.headersForArmsLength -> HeaderKeys.questionHelpers

      case TangibleMoveableProperty =>
        HeaderKeys.headersForTangibleMoveableProperty -> HeaderKeys.questionHelpersMoveableProperty

      case OutstandingLoans =>
        HeaderKeys.headersForOutstandingLoans -> HeaderKeys.questionHelpersForOutstandingLoans

      case _ =>
        "" -> ""
    }

    toCsvHeaderRow(headers) + newLine + toCsvHeaderRow(helpers)
  }

  private def toCsvHeaderRow(values: String): String =
    values
      .split(";\n")
      .toList
      .map("\"" + _ + "\"")
      .mkString(",")

  implicit class CsvRowStateOps(val csvRowState: CsvRowState[JsValue]) extends AnyVal {
    def toCsvRow(implicit messages: Messages): String = {
      val row = (csvRowState match {
        case CsvRowState.CsvRowValid(_, _, raw) => raw.toList.map(str => s"\"$str\"")
        case CsvRowState.CsvRowInvalid(_, errors, raw) =>
          raw.toList.map(str => s"\"$str\"").appended(
            s""""${errors.map(m => Messages(m.message)).toList.mkString(",")}""""
          )

      }).mkString(",")

      "," + row //add first empty column
    }
  }
}
