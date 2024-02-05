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

import cats.syntax.option._
import controllers.PendingFileActionController._
import controllers.UploadMemberDetailsController.redirectTag
import controllers.actions.IdentifyAndRequireData
import models.FileAction._
import models.Journey._
import models.SchemeId.Srn
import models.requests.DataRequest
import models.{NormalMode, UploadKey, UploadStatus}
import pages.memberdetails.CheckMemberDetailsFilePage
import play.api.libs.json.{Format, Json}
import play.api.mvc.{Action, AnyContent, MessagesControllerComponents}
import services.UploadService
import uk.gov.hmrc.play.bootstrap.frontend.controller.FrontendController

import javax.inject.Inject
import scala.concurrent.{ExecutionContext, Future}

class PendingFileActionController @Inject()(
  mcc: MessagesControllerComponents,
  identifyAndRequireData: IdentifyAndRequireData,
  uploadService: UploadService
)(implicit ec: ExecutionContext)
    extends FrontendController(mcc) {

  def pollPendingState(srn: Srn, action: String, page: String): Action[AnyContent] = identifyAndRequireData(srn).async {
    implicit request =>
      action match {
        case VALIDATING =>
          Future(Ok(Json.toJson(getValidationState(srn, page))))

        case UPLOADING =>
          getUploadState(srn, page)
            .map(pendingState => Json.toJson(pendingState))
            .map(Ok(_))
      }
  }

  private def getUploadState(srn: Srn, page: String)(implicit request: DataRequest[_]): Future[PendingState] = {
    val tag = page match {
      case MEMBER_DETAILS => redirectTag
      case _ => ""
    }

    val uploadKey = UploadKey.fromRequest(srn, tag)

    uploadService.getUploadStatus(uploadKey).map {
      case Some(_: UploadStatus.Success) =>
        val redirectUrl = page match {
          case MEMBER_DETAILS =>
            controllers.memberdetails.routes.CheckMemberDetailsFileController.onPageLoad(srn, NormalMode).url
          case _ => controllers.routes.JourneyRecoveryController.onPageLoad().url
        }

        PendingState(redirectUrl)

      case Some(_: UploadStatus.Failed) | None =>
        val redirectUrl = page match {
          case MEMBER_DETAILS => controllers.routes.UploadMemberDetailsController.onPageLoad(srn).url
          case _ => controllers.routes.JourneyRecoveryController.onPageLoad().url
        }

        PendingState(redirectUrl)

      case Some(_) => PendingState()
    }
  }

  private def getValidationState(srn: Srn, page: String)(implicit request: DataRequest[_]): PendingState =
    page match {
      case MEMBER_DETAILS =>
        if (request.userAnswers.get(CheckMemberDetailsFilePage(srn)).nonEmpty) {
          PendingState(
            controllers.routes.TaskListController.onPageLoad(srn).url
          )
        } else {
          PendingState()
        }
      case _ => PendingState(controllers.routes.JourneyRecoveryController.onPageLoad().url)
    }
}

object PendingFileActionController {
  case class PendingState(
    isPending: Boolean,
    redirectUrl: Option[String]
  )

  object PendingState {
    implicit val format: Format[PendingState] = Json.format

    def apply(url: String): PendingState = PendingState(isPending = false, url.some)
    def apply(): PendingState = PendingState(isPending = true, None)
  }
}
