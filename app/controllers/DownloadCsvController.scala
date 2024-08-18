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

import cats.data.NonEmptyList
import cats.effect.IO
import cats.effect.unsafe.implicits.global
import config.Crypto
import connectors.PSRConnector
import controllers.DownloadCsvController._
import controllers.actions.IdentifyAndRequireData
import fs2.data.csv._
import fs2.{Chunk, Stream}
import models.Journey._
import models.SchemeId.Srn
import models.csv.CsvRowState
import models.keys.{
  ArmsLengthKeys,
  AssetFromConnectedPartyKeys,
  InterestInLandKeys,
  OutstandingLoansKeys,
  TangibleKeys,
  UnquotedSharesKeys
}
import models.{Journey, UploadKey}
import org.apache.pekko.NotUsed
import org.apache.pekko.stream.scaladsl.{Framing, Source}
import org.apache.pekko.stream.{Materializer, OverflowStrategy}
import org.apache.pekko.util.ByteString
import play.api.Logger
import play.api.i18n.{I18nSupport, Messages}
import play.api.libs.json.JsValue
import play.api.mvc.{AbstractController, Action, AnyContent, ControllerComponents}
import repositories.CsvRowStateSerialization.{read, IntLength}
import repositories.UploadRepository
import uk.gov.hmrc.crypto.{Decrypter, Encrypter}
import uk.gov.hmrc.play.bootstrap.frontend.controller.FrontendHeaderCarrierProvider

import java.nio.{ByteBuffer, ByteOrder}
import javax.inject.Inject
import scala.concurrent.ExecutionContext

class DownloadCsvController @Inject()(
  uploadRepository: UploadRepository,
  identifyAndRequireData: IdentifyAndRequireData,
  psrConnector: PSRConnector,
  crypto: Crypto
)(cc: ControllerComponents)(implicit ec: ExecutionContext, materializer: Materializer)
    extends AbstractController(cc)
    with I18nSupport
    with FrontendHeaderCarrierProvider {

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
      source.prepend(Source.single(getHeadersAndHelpersCombined(journey) + newLine)),
      None,
      inline = false,
      Some(fileName(journey))
    )
  }

  def downloadEtmpFile(
    srn: Srn,
    journey: Journey,
    optFbNumber: Option[String],
    optPeriodStartDate: Option[String],
    optPsrVersion: Option[String]
  ): Action[AnyContent] = identifyAndRequireData(srn).async { implicit request =>
    val pstr = request.schemeDetails.pstr
    val (headers, helpers) = getHeadersAndHelpers(journey)

    def toCsv[T: RowEncoder](list: List[T]) = {
      val (queue, source) = Source.queue[String](10, OverflowStrategy.backpressure).preMaterialize()
      val headersAndHelpers: fs2.Pipe[IO, NonEmptyList[String], NonEmptyList[String]] =
        stream =>
          stream.cons(
            Chunk(NonEmptyList.fromListUnsafe("" +: headers.toList), NonEmptyList.fromListUnsafe(helpers.init))
          )
      val pipe = lowlevel
        .encode[IO, T]
        .andThen(lowlevel.writeWithoutHeaders)
        .andThen(headersAndHelpers)
        .andThen(lowlevel.toStrings[IO]())
      Stream
        .emits[IO, T](list)
        .through(pipe)
        .evalMap(row => IO.fromFuture(IO(queue.offer(row))))
        .onFinalize(IO(queue.complete()))
        .compile
        .drain
        .unsafeToFuture()
      source
    }

    val encoded = journey match {
      case Journey.InterestInLandOrProperty =>
        psrConnector
          .getLandOrConnectedProperty(pstr, optFbNumber, optPeriodStartDate, optPsrVersion)
          .map(res => toCsv(res.transactions))
      case Journey.ArmsLengthLandOrProperty =>
        psrConnector
          .getLandArmsLength(pstr, optFbNumber, optPeriodStartDate, optPsrVersion)
          .map(res => toCsv(res.transactions))
      case Journey.TangibleMoveableProperty =>
        psrConnector
          .getTangibleMoveableProperty(pstr, optFbNumber, optPeriodStartDate, optPsrVersion)
          .map(res => toCsv(res.transactions))
      case Journey.OutstandingLoans =>
        psrConnector
          .getOutstandingLoans(pstr, optFbNumber, optPeriodStartDate, optPsrVersion)
          .map(res => toCsv(res.transactions))
      case Journey.UnquotedShares =>
        psrConnector
          .getUnquotedShares(pstr, optFbNumber, optPeriodStartDate, optPsrVersion)
          .map(res => toCsv(res.transactions))
      case Journey.AssetFromConnectedParty =>
        psrConnector
          .getAssetsFromConnectedParty(pstr, optFbNumber, optPeriodStartDate, optPsrVersion)
          .map(res => toCsv(res.transactions))
    }

    encoded.map { csvSource =>
      Ok.streamed(content = csvSource, contentLength = None, inline = false, fileName = Some(fileName(journey)))
    }
  }
}

object DownloadCsvController {
  private val newLine = "\n"

  private def readBytes(
    bytes: ByteBuffer
  )(implicit messages: Messages, crypto: Encrypter with Decrypter): String =
    read[JsValue](bytes).toCsvRow

  private def fileName(journey: Journey): String = journey match {
    case InterestInLandOrProperty => "output-interest-land-or-property.csv"
    case ArmsLengthLandOrProperty => "output-arms-length-land-or-property.csv"
    case TangibleMoveableProperty => "output-tangible-moveable-property.csv"
    case OutstandingLoans => "output-outstanding-loans.csv"
    case UnquotedShares => "output-unquoted-shares.csv"
    case AssetFromConnectedParty => "output-asset-from-connected-party.csv"
  }

  private def getHeadersAndHelpers(journey: Journey): (NonEmptyList[String], NonEmptyList[String]) =
    journey match {
      case InterestInLandOrProperty => InterestInLandKeys.headers -> InterestInLandKeys.helpers
      case ArmsLengthLandOrProperty => ArmsLengthKeys.headers -> ArmsLengthKeys.helpers
      case TangibleMoveableProperty => TangibleKeys.headers -> TangibleKeys.helpers
      case OutstandingLoans => OutstandingLoansKeys.headers -> OutstandingLoansKeys.helpers
      case UnquotedShares => UnquotedSharesKeys.headers -> UnquotedSharesKeys.helpers
      case AssetFromConnectedParty => AssetFromConnectedPartyKeys.headers -> AssetFromConnectedPartyKeys.helpers
    }

  private def getHeadersAndHelpersCombined(journey: Journey) = {
    val (headers, helpers) = getHeadersAndHelpers(journey)
    val headersLine = headers.toList.map("\"" + _ + "\"").mkString(",")
    val helpersLine = helpers.toList.map("\"" + _ + "\"").mkString(",")

    headersLine + newLine + helpersLine
  }

  implicit class CsvRowStateOps(val csvRowState: CsvRowState[JsValue]) extends AnyVal {
    def toCsvRow(implicit messages: Messages): String = {
      val row = (csvRowState match {
        case CsvRowState.CsvRowValid(_, _, raw) => raw.toList.map(str => s""""$str"""")
        case CsvRowState.CsvRowInvalid(_, errors, raw) =>
          raw.toList
            .map(str => s""""$str"""")
            .appended(
              s""""${errors.map(m => Messages(m.message)).toList.mkString(",")}""""
            )

      }).mkString(",")

      "," + row //add first empty column
    }
  }
}
