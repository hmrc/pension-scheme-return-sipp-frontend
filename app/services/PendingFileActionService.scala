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

import models.Journey.{LandOrProperty, MemberDetails}
import models.SchemeId.Srn
import models.UploadStatus.Failed
import models.{Journey, NormalMode, UploadKey, UploadStatus}
import models.requests.DataRequest
import pages.memberdetails.CheckMemberDetailsFilePage
import services.PendingFileActionService.{Complete, Pending, PendingState}

import javax.inject.Inject
import scala.concurrent.Future
import scala.concurrent.ExecutionContext.Implicits.global

class PendingFileActionService @Inject()(
  uploadService: UploadService
) {
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
  def getValidationState(srn: Srn, journey: Journey)(implicit request: DataRequest[_]): PendingState =
    journey match {
      case MemberDetails =>
        if (request.userAnswers.get(CheckMemberDetailsFilePage(srn)).nonEmpty) {
          Complete(
            controllers.routes.FileUploadSuccessController.onPageLoad(srn, journey.uploadRedirectTag, NormalMode).url
          )
        } else {
          Pending
        }
      case LandOrProperty =>
        Complete(
          controllers.routes.FileUploadSuccessController.onPageLoad(srn, journey.uploadRedirectTag, NormalMode).url
        )
    }
}

object PendingFileActionService {
  sealed trait PendingState
  case object Pending extends PendingState
  case class Complete(url: String) extends PendingState
}
