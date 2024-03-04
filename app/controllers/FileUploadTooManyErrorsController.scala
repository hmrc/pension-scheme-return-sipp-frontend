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

import controllers.FileUploadTooManyErrorsController.viewModel
import controllers.actions._
import models.SchemeId.Srn
import models.{Journey, Mode, UploadErrors, UploadKey}
import navigation.Navigator
import pages.FileUploadTooManyErrorsPage
import play.api.i18n._
import play.api.mvc._
import services.UploadService
import uk.gov.hmrc.play.bootstrap.frontend.controller.FrontendBaseController
import viewmodels.DisplayMessage._
import viewmodels.implicits._
import viewmodels.models.{ContentPageViewModel, FormPageViewModel}
import views.html.ContentPageView

import javax.inject.{Inject, Named}
import scala.concurrent.ExecutionContext

class FileUploadTooManyErrorsController @Inject()(
  override val messagesApi: MessagesApi,
  @Named("sipp") navigator: Navigator,
  identifyAndRequireData: IdentifyAndRequireData,
  uploadService: UploadService,
  val controllerComponents: MessagesControllerComponents,
  view: ContentPageView
)(implicit ec: ExecutionContext)
    extends FrontendBaseController
    with I18nSupport {

  def onPageLoad(srn: Srn, journey: Journey): Action[AnyContent] = identifyAndRequireData(srn).async {
    implicit request =>
      uploadService.getUploadResult(UploadKey.fromRequest(srn, journey.uploadRedirectTag)).map {
        case Some(_: UploadErrors) => Ok(view(viewModel(srn, journey)))
        case _ => Redirect(controllers.routes.JourneyRecoveryController.onPageLoad())
      }
  }

  def onSubmit(srn: Srn, journey: Journey, mode: Mode): Action[AnyContent] = identifyAndRequireData(srn) {
    implicit request =>
      Redirect(navigator.nextPage(FileUploadTooManyErrorsPage(srn, journey), mode, request.userAnswers))
  }
}

object FileUploadTooManyErrorsController {

  def viewModel(
    srn: Srn,
    journey: Journey
  ): FormPageViewModel[ContentPageViewModel] =
    FormPageViewModel[ContentPageViewModel](
      title = s"${journey.messagePrefix}.fileUploadTooManyErrors.title",
      heading = s"${journey.messagePrefix}.fileUploadTooManyErrors.heading",
      description = Some(
        ParagraphMessage("fileUploadTooManyErrors.paragraph") ++
          ParagraphMessage(Message("fileUploadTooManyErrors.paragraph2")) ++
          ListMessage.Bullet(Message("fileUploadTooManyErrors.bullet1")) ++
          ListMessage.Bullet(Message("fileUploadTooManyErrors.bullet2")) ++
          ListMessage.Bullet(Message("fileUploadTooManyErrors.bullet3")) ++
          ListMessage.Bullet(Message("fileUploadTooManyErrors.bullet4")) ++
          ParagraphMessage(
            "fileUploadTooManyErrors.paragraph3.part1",
            DownloadLinkMessage(
              "fileUploadTooManyErrors.paragraph3.part2",
              journey match {
                case Journey.MemberDetails =>
                  controllers.memberdetails.routes.DownloadMemberDetailsErrorsController
                    .downloadFile(srn)
                    .url //TODO make generic ?
                case Journey.InterestInLandOrProperty =>
                  controllers.landorproperty.routes.DownloadLandOrPropertyErrorsController.downloadFile(srn).url
                case Journey.ArmsLengthLandOrProperty | Journey.TangibleMoveableProperty =>
                  controllers.routes.JourneyRecoveryController.onPageLoad().url // not yet implemented
              }
            ),
            "fileUploadTooManyErrors.paragraph3.part3"
          ) ++
          ParagraphMessage("fileUploadTooManyErrors.paragraph4")
      ),
      page = ContentPageViewModel(isLargeHeading = true),
      refresh = None,
      buttonText = "site.returnToFileUpload",
      onSubmit = routes.FileUploadTooManyErrorsController.onSubmit(srn, journey)
    )
}
