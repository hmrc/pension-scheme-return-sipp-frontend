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
import models.Journey.{LandOrProperty, MemberDetails}
import models.SchemeId.Srn
import models.UploadStatus.Failed
import models.requests.DataRequest
import models.{Journey, NormalMode, UploadError, UploadFormatError, UploadKey, UploadStatus, UploadSuccess, UploadValidating, Uploaded}
import navigation.Navigator
import pages.memberdetails.MemberDetailsUploadErrorPage
import play.api.i18n.Messages
import services.PendingFileActionService.{Complete, Pending, PendingState}
import services.validation.MemberDetailsUploadValidator
import uk.gov.hmrc.http.HeaderCarrier
import uk.gov.hmrc.play.bootstrap.frontend.controller.FrontendHeaderCarrierProvider

import java.time.{Clock, Instant}
import javax.inject.{Inject, Named}
import scala.concurrent.ExecutionContext.Implicits.global
import scala.concurrent.Future

class PendingFileActionService @Inject()(
  @Named("sipp") navigator: Navigator,
  uploadService: UploadService,
  uploadValidator: MemberDetailsUploadValidator,
  clock: Clock
)(implicit materializer: Materializer)
    extends FrontendHeaderCarrierProvider {
  def getUploadState(srn: Srn, journey: Journey)(implicit request: DataRequest[_]): Future[PendingState] = {
    val uploadKey = UploadKey.fromRequest(srn, journey.uploadRedirectTag)

    uploadService.getUploadStatus(uploadKey).map {
      case Some(success: UploadStatus.Success) =>
        val redirectUrl = journey match {
          case MemberDetails =>
            checkFileFormat(
              success,
              controllers.memberdetails.routes.CheckMemberDetailsFileController.onPageLoad(srn, NormalMode).url,
              controllers.routes.UploadFileController.onPageLoad(srn, journey).url
            )

          case LandOrProperty =>
            checkFileFormat(
              success,
              controllers.landorproperty.routes.CheckInterestLandOrPropertyFileController
                .onPageLoad(srn, NormalMode)
                .url,
              controllers.routes.UploadFileController.onPageLoad(srn, journey).url
            )
        }

        Complete(redirectUrl)

      case Some(failed: UploadStatus.Failed) =>
        Complete(controllers.routes.UploadFileController.onPageLoad(srn, journey).url + s"?${failed.asQueryParams}")

      case None =>
        val redirectUrl = journey match {
          case MemberDetails =>
            controllers.routes.UploadFileController.onPageLoad(srn, journey).url
          case _ => controllers.routes.JourneyRecoveryController.onPageLoad().url
        }
        Complete(redirectUrl)

      case _ => Pending
    }
  }

  private def checkFileFormat(success: UploadStatus.Success, successUrl: String, failureUrl: String) =
    if (success.name.endsWith(".csv")) {
      successUrl
    } else {
      failureUrl + s"?${Failed.incorrectFileFormatQueryParam}"
    }

  def getValidationState(
    srn: Srn,
    journey: Journey
  )(implicit request: DataRequest[_], messages: Messages): Future[PendingState] =
    journey match {
      case MemberDetails =>
        val key = UploadKey.fromRequest(srn, journey.uploadRedirectTag)
        uploadService.getUploadResult(key).flatMap {
          case Some(error: UploadError) =>
            Future.successful(Complete(error match {
              case e: UploadFormatError => decideNextPage(srn, e)
              case errors: UploadError => decideNextPage(srn, errors)
              case _ => controllers.routes.JourneyRecoveryController.onPageLoad().url
            }))
          case Some(_: UploadSuccess) =>
            Future.successful(
              Complete(
                controllers.routes.FileUploadSuccessController
                  .onPageLoad(srn, journey.uploadRedirectTag, NormalMode)
                  .url
              )
            )
          case Some(UploadValidating(_)) => Future.successful(Pending)
          case Some(Uploaded) =>
            uploadService
              .saveValidatedUpload(key, UploadValidating(Instant.now(clock)))
              .flatMap(_ => validate(key))
        }

      case LandOrProperty =>
        Future.successful(
          Complete(
            controllers.routes.FileUploadSuccessController.onPageLoad(srn, journey.uploadRedirectTag, NormalMode).url
          )
        )
    }

  private def decideNextPage(srn: Srn, error: UploadError)(
    implicit request: DataRequest[_]
  ): String =
    navigator.nextPage(MemberDetailsUploadErrorPage(srn, error), NormalMode, request.userAnswers).url

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
