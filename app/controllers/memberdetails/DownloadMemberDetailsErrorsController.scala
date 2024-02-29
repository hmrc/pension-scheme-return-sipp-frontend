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

import akka.stream.Materializer
import akka.stream.alpakka.csv.scaladsl.CsvFormatting
import akka.stream.scaladsl.{FileIO, Keep, Source}
import cats.data.NonEmptyList
import controllers.actions.IdentifyAndRequireData
import models.SchemeId.Srn
import models.{Journey, UploadErrorsMemberDetails, UploadFormatError, UploadKey}
import play.api.i18n.{I18nSupport, Messages}
import play.api.libs.Files.TemporaryFileCreator
import play.api.mvc.{AbstractController, Action, AnyContent, ControllerComponents}
import services.UploadService

import javax.inject.Inject
import scala.concurrent.{ExecutionContext, Future}

class DownloadMemberDetailsErrorsController @Inject()(
  uploadService: UploadService,
  identifyAndRequireData: IdentifyAndRequireData
)(cc: ControllerComponents)(
  implicit ec: ExecutionContext,
  temporaryFileCreator: TemporaryFileCreator,
  mat: Materializer
) extends AbstractController(cc)
    with I18nSupport {

  def downloadFile(srn: Srn): Action[AnyContent] = identifyAndRequireData(srn).async { implicit request =>
    uploadService.getUploadResult(UploadKey.fromRequest(srn, Journey.MemberDetails.uploadRedirectTag)).flatMap {
      case Some(UploadErrorsMemberDetails(unvalidated, errors)) =>
        val tempFile = temporaryFileCreator.create(suffix = "output.csv")
        val fileOutput = FileIO.toPath(tempFile.path)
        val groupedErr = errors.groupBy(_.row)

        val csvLines: NonEmptyList[List[String]] = unvalidated
          .map(
            raw =>
              List(
                raw.firstName,
                raw.lastName,
                raw.dateOfBirth,
                raw.nino.getOrElse(""),
                raw.ninoReason.getOrElse(""),
                raw.isUK,
                raw.ukAddressLine1.getOrElse(""),
                raw.ukAddressLine2.getOrElse(""),
                raw.ukAddressLine3.getOrElse(""),
                raw.ukCity.getOrElse(""),
                raw.ukPostCode.getOrElse(""),
                raw.addressLine1.getOrElse(""),
                raw.addressLine2.getOrElse(""),
                raw.addressLine3.getOrElse(""),
                raw.addressLine4.getOrElse(""),
                raw.country.getOrElse(""),
                groupedErr.get(raw.row).map(_.map(m => Messages(m.message)).toList.mkString(", ")).getOrElse("")
              )
          )

        val write = Source(
          List(Messages("memberDetails.upload.csv.headers").split(",").toList) ++ List(
            Messages("memberDetails.upload.csv.headers.explainer").split("&").toList
          ) ++ csvLines.toList
        ).via(CsvFormatting.format())
          .toMat(fileOutput)(Keep.right)

        write.run().map { _ =>
          Ok.sendFile(
            content = tempFile.toFile,
            fileName = _ => Option("output.csv")
          )
        }

      case Some(UploadFormatError(_)) =>
        Future.successful(Redirect(controllers.routes.JourneyRecoveryController.onPageLoad()))
      case _ => Future.successful(Redirect(controllers.routes.JourneyRecoveryController.onPageLoad()))
    }
  }
}
