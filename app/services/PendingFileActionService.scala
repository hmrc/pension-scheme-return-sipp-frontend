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
import models.Journey.{InterestInLandOrProperty, MemberDetails}
import models.SchemeId.Srn
import models.UploadStatus.Failed
import models.requests.DataRequest
import models.{
  Journey,
  NormalMode,
  PensionSchemeId,
  UploadError,
  UploadFormatError,
  UploadKey,
  UploadStatus,
  UploadSuccess,
  UploadValidating,
  Uploaded
}
import navigation.Navigator
import pages.memberdetails.MemberDetailsUploadErrorPage
import play.api.Logger
import play.api.i18n.Messages
import services.PendingFileActionService.{Complete, Pending, PendingState}
import services.validation.MemberDetailsUploadValidator
import uk.gov.hmrc.http.HeaderCarrier
import uk.gov.hmrc.play.bootstrap.frontend.controller.FrontendHeaderCarrierProvider

import java.time.{Clock, Instant}
import javax.inject.{Inject, Named}
import scala.concurrent.ExecutionContext.Implicits.global
import scala.concurrent.Future
import scala.util.control.NonFatal

class PendingFileActionService @Inject()(
  @Named("sipp") navigator: Navigator,
  uploadService: UploadService,
  uploadValidator: MemberDetailsUploadValidator,
  schemeDetailsService: SchemeDetailsService,
  clock: Clock
)(implicit materializer: Materializer)
    extends FrontendHeaderCarrierProvider {

  private val logger: Logger = Logger(classOf[PendingFileActionService])

  def getUploadState(srn: Srn, journey: Journey)(implicit request: DataRequest[_]): Future[PendingState] = {
    val uploadKey = UploadKey.fromRequest(srn, journey.uploadRedirectTag)
    val failureUrl = controllers.routes.UploadFileController
      .onPageLoad(srn, journey)
      .url

    uploadService.getUploadStatus(uploadKey).map {
      case Some(success: UploadStatus.Success) =>
        val successUrl = controllers.routes.CheckFileNameController
          .onPageLoad(srn, journey, NormalMode)
          .url

        val redirectUrl = checkFileFormat(
          success,
          successUrl,
          failureUrl
        )

        Complete(redirectUrl)

      case Some(failed: UploadStatus.Failed) =>
        Complete(failureUrl + s"?${failed.asQueryParams}")

      case None => Complete(failureUrl)

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
                  .onPageLoad(srn, journey, NormalMode)
                  .url
              )
            )
          case Some(UploadValidating(_)) => Future.successful(Pending)
          case Some(Uploaded) =>
            uploadService
              .saveValidatedUpload(key, UploadValidating(Instant.now(clock)))
              .flatMap(_ => validate(key, request.pensionSchemeId, srn))
        }

      case _ =>
        Future.successful(
          Complete(
            controllers.routes.FileUploadSuccessController.onPageLoad(srn, journey, NormalMode).url
          )
        )
    }

  private def decideNextPage(srn: Srn, error: UploadError)(
    implicit request: DataRequest[_]
  ): String =
    navigator.nextPage(MemberDetailsUploadErrorPage(srn, error), NormalMode, request.userAnswers).url

  private def validate(
    uploadKey: UploadKey,
    id: PensionSchemeId,
    srn: Srn
  )(implicit headerCarrier: HeaderCarrier, messages: Messages): Future[PendingState] =
    getUploadedFile(uploadKey).flatMap {
      case None => Future.successful(Complete(controllers.routes.JourneyRecoveryController.onPageLoad().url))
      case Some(file) =>
        val _ = (for {
          source <- uploadService.stream(file.downloadUrl)
          scheme <- schemeDetailsService.getMinimalSchemeDetails(id, srn)
          validated <- uploadValidator.validateCSV(source._2, scheme.flatMap(_.windUpDate))
          _ <- uploadService.saveValidatedUpload(uploadKey, validated._1)
        } yield ()).recover {
          //this exception won't be propagated as we want to return Pending and not to wait for Validation process
          case NonFatal(e) => logger.error("Validations failed with error: ", e)
        }

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
