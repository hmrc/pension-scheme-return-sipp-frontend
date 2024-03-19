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

package services.validation

import cats.syntax.either._
import cats.effect.IO
import connectors.UpscanDownloadStreamConnector
import models.SchemeId.Srn
import models.{Journey, PensionSchemeId, UploadKey, UploadStatus, UploadValidated, ValidationException}
import play.api.Logger
import play.api.i18n.Messages
import services.PendingFileActionService.{Complete, Pending, PendingState}
import services.{SchemeDetailsService, UploadService}
import uk.gov.hmrc.http.HeaderCarrier

import javax.inject.Inject
import scala.concurrent.ExecutionContext.Implicits.global
import scala.concurrent.Future

class ValidateUploadService @Inject()(
  uploadService: UploadService,
  schemeDetailsService: SchemeDetailsService,
  uploadValidatorForMemberDetails: MemberDetailsUploadValidator,
  landOrPropertyUploadValidatorFs2: LandOrPropertyUploadValidator,
  upscanDownloadStreamConnector: UpscanDownloadStreamConnector
) {

  private val logger: Logger = Logger(classOf[ValidateUploadService])
  def validateUpload(
    uploadKey: UploadKey,
    id: PensionSchemeId,
    srn: Srn,
    journey: Journey
  )(implicit headerCarrier: HeaderCarrier, messages: Messages): Future[PendingState] =
    streamingValidation(
      journey,
      uploadKey,
      id,
      srn
    )

  private def streamingValidation(
    journey: Journey,
    uploadKey: UploadKey,
    id: PensionSchemeId,
    srn: Srn
  )(implicit headerCarrier: HeaderCarrier, messages: Messages): Future[PendingState] = {
    val result = IO.fromFuture(IO(getUploadedFile(uploadKey))).flatMap {
      case Some(file) =>
        val validationResult = for {
          scheme <- IO.fromFuture(IO(schemeDetailsService.getMinimalSchemeDetails(id, srn)))
          validation <- journey match {
            case Journey.MemberDetails =>
              uploadValidatorForMemberDetails
                .validateUpload(
                  uploadKey,
                  upscanDownloadStreamConnector.stream(file.downloadUrl),
                  scheme.flatMap(_.windUpDate)
                )
            case _ =>
              landOrPropertyUploadValidatorFs2
                .validateUpload(
                  journey,
                  uploadKey,
                  upscanDownloadStreamConnector.stream(file.downloadUrl),
                  scheme.flatMap(_.windUpDate)
                )
          }
          result <- IO.fromFuture(IO(uploadService.setUploadValidationState(uploadKey, UploadValidated(validation))))
        } yield result

        validationResult
          .recoverWith(recoverValidation(journey, uploadKey))
          .unsafeRunAsync(_.leftMap(logger.error(s"Csv validation failed for journey, ${journey.name}", _)))(
            cats.effect.unsafe.implicits.global
          )

        IO.pure(Pending)

      case None => IO.pure(Complete(controllers.routes.JourneyRecoveryController.onPageLoad().url))
    }

    Future.successful(result.unsafeRunSync()(cats.effect.unsafe.implicits.global))
  }

  private def getUploadedFile(uploadKey: UploadKey): Future[Option[UploadStatus.Success]] =
    uploadService
      .getUploadStatus(uploadKey)
      .map {
        case Some(upload: UploadStatus.Success) => Some(upload)
        case _ => None
      }

  private def recoverValidation(journey: Journey, uploadKey: UploadKey): PartialFunction[Throwable, IO[Unit]] = {
    case throwable: Throwable =>
      logger.error(
        s"Validation failed with exception for journey, ${journey.name}, persisting ValidationException state.",
        throwable
      )
      IO.fromFuture(IO(uploadService.setUploadValidationState(uploadKey, ValidationException)))
  }
}
