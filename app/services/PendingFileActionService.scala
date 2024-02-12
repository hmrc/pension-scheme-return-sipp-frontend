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

package services

import akka.stream.Materializer
import models.Journey.MemberDetails
import models.SchemeId.Srn
import models.{Journey, NormalMode, UploadError, UploadKey, UploadStatus, UploadSuccess, UploadValidating}
import models.requests.DataRequest
import play.api.i18n.Messages
import services.PendingFileActionService.{Complete, Pending, PendingState}
import services.validation.MemberDetailsUploadValidator
import uk.gov.hmrc.http.HeaderCarrier
import uk.gov.hmrc.play.bootstrap.frontend.controller.FrontendHeaderCarrierProvider

import java.time.{Clock, Instant}
import javax.inject.Inject
import scala.concurrent.Future
import scala.concurrent.ExecutionContext.Implicits.global

class PendingFileActionService @Inject()(
  uploadService: UploadService,
  uploadValidator: MemberDetailsUploadValidator,
  clock: Clock
)(implicit materializer: Materializer)
    extends FrontendHeaderCarrierProvider {
  def getUploadState(srn: Srn, journey: Journey)(implicit request: DataRequest[_]): Future[PendingState] = {
    val uploadKey = UploadKey.fromRequest(srn, journey.uploadRedirectTag)

    uploadService.getUploadStatus(uploadKey).map {
      case Some(_: UploadStatus.Success) =>
        val redirectUrl = journey match {
          case MemberDetails =>
            controllers.memberdetails.routes.CheckMemberDetailsFileController.onPageLoad(srn, NormalMode).url
          case _ => controllers.routes.JourneyRecoveryController.onPageLoad().url
        }

        Complete(redirectUrl)

      case Some(_: UploadStatus.Failed) | None =>
        val redirectUrl = journey match {
          case MemberDetails => controllers.routes.UploadMemberDetailsController.onPageLoad(srn).url
          case _ => controllers.routes.JourneyRecoveryController.onPageLoad().url
        }

        Complete(redirectUrl)

      case Some(_) => Pending
    }
  }

  def getValidationState(
    srn: Srn,
    journey: Journey
  )(implicit request: DataRequest[_], messages: Messages): Future[PendingState] =
    journey match {
      case MemberDetails =>
        val key = UploadKey.fromRequest(srn, journey.uploadRedirectTag)
        uploadService.getUploadResult(key).flatMap {
          case Some(_: UploadError) =>
            Future.successful(Complete(controllers.routes.JourneyRecoveryController.onPageLoad().url))
          case Some(_: UploadSuccess) =>
            Future.successful(
              Complete(
                controllers.routes.FileUploadSuccessController
                  .onPageLoad(srn, journey.uploadRedirectTag, NormalMode)
                  .url
              )
            )
          case Some(UploadValidating(_)) => Future.successful(Pending)
          case _ =>
            uploadService
              .saveValidatedUpload(key, UploadValidating(Instant.now(clock)))
              .flatMap(_ => validate(key))
        }
      case _ => Future.successful(Complete(controllers.routes.JourneyRecoveryController.onPageLoad().url))
    }

  private def validate(
    uploadKey: UploadKey
  )(implicit headerCarrier: HeaderCarrier, messages: Messages): Future[PendingState] =
    getUploadedFile(uploadKey).flatMap {
      case None => Future.successful(Complete(controllers.routes.JourneyRecoveryController.onPageLoad().url))
      case Some(file) =>
        val _ = for {
          source <- uploadService.stream(file.downloadUrl)
          validated <- uploadValidator.validateCSV(source._2, None)
          _ <- uploadService.saveValidatedUpload(uploadKey, validated._1)
        } yield ()

        Future.successful(Pending)
    }

  private def getUploadedFile(uploadKey: UploadKey): Future[Option[UploadStatus.Success]] =
    uploadService
      .getUploadStatus(uploadKey)
      .map {
        case Some(upload: UploadStatus.Success) => Some(upload)
        case _ => None
      }
}

object PendingFileActionService {
  sealed trait PendingState
  case object Pending extends PendingState
  case class Complete(url: String) extends PendingState
}
