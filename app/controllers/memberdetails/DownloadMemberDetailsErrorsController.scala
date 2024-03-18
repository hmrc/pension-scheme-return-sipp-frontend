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

package controllers.memberdetails

import akka.NotUsed
import akka.stream.scaladsl.{Framing, Source}
import akka.util.ByteString
import config.Crypto
import controllers.actions.IdentifyAndRequireData
import controllers.landorproperty.StreamingDownloadLandOrPropertyErrorsController
import models.SchemeId.Srn
import models.csv.CsvRowState
import models.UploadKey
import models.Journey.MemberDetails
import play.api.Logger
import play.api.i18n.{I18nSupport, Messages}
import play.api.mvc.{AbstractController, Action, AnyContent, ControllerComponents}
import repositories.CsvRowStateSerialization.{read, IntLength}
import repositories.UploadRepository
import uk.gov.hmrc.crypto.{Decrypter, Encrypter}
import models.MemberDetailsUpload

import java.nio.ByteOrder
import javax.inject.Inject

class DownloadMemberDetailsErrorsController @Inject()(
  uploadRepository: UploadRepository,
  identifyAndRequireData: IdentifyAndRequireData,
  crypto: Crypto
)(cc: ControllerComponents)
    extends AbstractController(cc)
    with I18nSupport {

  val logger: Logger = Logger.apply(classOf[StreamingDownloadLandOrPropertyErrorsController])
  implicit val cryptoEncDec: Encrypter with Decrypter = crypto.getCrypto
  private val lengthFieldFrame =
    Framing.lengthField(fieldLength = IntLength, maximumFrameLength = 256 * 1000, byteOrder = ByteOrder.BIG_ENDIAN)

  def downloadFile(srn: Srn): Action[AnyContent] = identifyAndRequireData(srn) { implicit request =>
    val fileName = "output.csv"

    val allHeaders = Messages("memberDetails.upload.csv.headers")
      .split(";")
      .map(_.replace("\n", ""))
      .mkString(",") + "\n" +
      Messages("memberDetails.upload.csv.headers.explainer").split(";").map(_.replace("\n", "")).mkString(",") + "\n"

    val source: Source[String, NotUsed] = Source
      .fromPublisher(uploadRepository.streamUploadResult(UploadKey.fromRequest(srn, MemberDetails.uploadRedirectTag)))
      .map(ByteString.apply)
      .via(lengthFieldFrame)
      .map(_.toByteBuffer)
      .map(read[MemberDetailsUpload])
      .map(toCsvRow)
      .map(_ + "\n")

    Ok.streamed(
      source.prepend(Source.single(allHeaders)),
      None,
      inline = false,
      Some(fileName)
    )
  }

  private def toCsvRow[T](csvRowState: CsvRowState[T])(implicit messages: Messages): String =
    (csvRowState match {
      case CsvRowState.CsvRowValid(_, _, raw) => raw.toList
      case CsvRowState.CsvRowInvalid(_, errors, raw) => raw.toList ++ errors.map(m => Messages(m.message)).toList
    }).mkString(",")
}
