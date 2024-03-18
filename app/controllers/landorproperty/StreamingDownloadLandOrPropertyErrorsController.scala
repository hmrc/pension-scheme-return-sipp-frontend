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

package controllers.landorproperty

import akka.NotUsed
import akka.stream.scaladsl.{Framing, Source}
import akka.util.ByteString
import config.Crypto
import controllers.actions.IdentifyAndRequireData
import models.Journey.InterestInLandOrProperty
import models.SchemeId.Srn
import models.csv.CsvRowState
import models.requests.LandOrConnectedPropertyRequest
import models.{HeaderKeys, Journey, UploadKey}
import play.api.Logger
import play.api.i18n.{I18nSupport, Messages}
import play.api.mvc.{AbstractController, Action, AnyContent, ControllerComponents}
import repositories.CsvRowStateSerialization._
import repositories.UploadRepository
import uk.gov.hmrc.crypto.{Decrypter, Encrypter}

import java.nio.ByteOrder
import javax.inject.Inject

class StreamingDownloadLandOrPropertyErrorsController @Inject()(
  uploadRepository: UploadRepository,
  identifyAndRequireData: IdentifyAndRequireData,
  crypto: Crypto
)(cc: ControllerComponents)
    extends AbstractController(cc)
    with I18nSupport {

  val logger = Logger.apply(classOf[StreamingDownloadLandOrPropertyErrorsController])
  implicit val cryptoEncDec: Encrypter with Decrypter = crypto.getCrypto

  def downloadFile(srn: Srn, journey: Journey): Action[AnyContent] = identifyAndRequireData(srn) { implicit request =>
    val source: Source[String, NotUsed] = Source
      .fromPublisher(uploadRepository.streamUploadResult(UploadKey.fromRequest(srn, journey.uploadRedirectTag)))
      .map(ByteString.apply)
      .via(
        Framing.lengthField(fieldLength = IntLength, maximumFrameLength = 256 * 1000, byteOrder = ByteOrder.BIG_ENDIAN)
      )
      .map(_.toByteBuffer)
      .map(
        read[LandOrConnectedPropertyRequest.TransactionDetail](_) match {
          case CsvRowState.CsvRowValid(_, _, raw) => raw.toList
          case CsvRowState.CsvRowInvalid(_, errors, raw) => raw.toList ++ errors.map(m => Messages(m.message)).toList
        }
      )
      .map(_.mkString(",") + "\n")

    val fileName = if (journey == InterestInLandOrProperty) {
      "output-interest-land-or-property.csv"
    } else {
      "output-arms-length-land-or-property.csv"
    }

    val headers =
      if (journey == InterestInLandOrProperty) {
        HeaderKeys.headersForInterestLandOrProperty
      } else {
        HeaderKeys.headersForArmsLength
      }

    val questionHelpers = HeaderKeys.questionHelpers

    val allHeaders = headers.split(";").map(_.replace("\n", "")).mkString(",") + "\n" +
      questionHelpers.split(";").map(_.replace("\n", "")).mkString(",") + "\n"

    Ok.streamed(
      source.prepend(Source.single(allHeaders)),
      None,
      inline = false,
      Some(fileName)
    )
  }
}
