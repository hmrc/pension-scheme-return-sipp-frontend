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

import cats.data.NonEmptyList
import controllers.actions._
import controllers.memberdetails.FileUploadErrorSummaryController.{viewModelErrors, viewModelFormatting}
import models.SchemeId.Srn
import models.ValidationError.ordering
import models.{Journey, Mode, UploadErrors, UploadFormatError, UploadKey, ValidationError}
import navigation.Navigator
import pages.memberdetails.MemberDetailsUploadErrorSummaryPage
import play.api.i18n._
import play.api.mvc._
import services.UploadService
import uk.gov.hmrc.play.bootstrap.frontend.controller.FrontendBaseController
import viewmodels.DisplayMessage._
import viewmodels.LabelSize
import viewmodels.implicits._
import viewmodels.models.{ContentPageViewModel, FormPageViewModel}
import views.html.ContentPageView

import javax.inject.{Inject, Named}
import scala.concurrent.ExecutionContext

class FileUploadErrorSummaryController @Inject()(
  override val messagesApi: MessagesApi,
  @Named("sipp") navigator: Navigator,
  identifyAndRequireData: IdentifyAndRequireData,
  uploadService: UploadService,
  val controllerComponents: MessagesControllerComponents,
  view: ContentPageView
)(implicit ec: ExecutionContext)
    extends FrontendBaseController
    with I18nSupport {

  def onPageLoad(srn: Srn, mode: Mode): Action[AnyContent] = identifyAndRequireData(srn).async { implicit request =>
    uploadService.getUploadResult(UploadKey.fromRequest(srn, Journey.MemberDetails.uploadRedirectTag)).map {
      case Some(UploadErrors(_, errors)) => Ok(view(viewModelErrors(srn, errors)))
      case Some(UploadFormatError(e)) => Ok(view(viewModelFormatting(srn, e)))
      case _ => Redirect(controllers.routes.JourneyRecoveryController.onPageLoad())
    }
  }

  def onSubmit(srn: Srn, mode: Mode, journey: Journey): Action[AnyContent] = identifyAndRequireData(srn) {
    implicit request =>
      Redirect(navigator.nextPage(MemberDetailsUploadErrorSummaryPage(srn, journey), mode, request.userAnswers))
  }
}

object FileUploadErrorSummaryController {

  private def errorSummary(errors: NonEmptyList[ValidationError]): TableMessage = {
    def toMessage(errors: NonEmptyList[ValidationError]) = CompoundMessage(
      ParagraphMessage(errors.head.message),
      ParagraphMessage(Message("uploadMemberDetails.table.message", errors.map(_.row).toList.mkString(",")))
    )

    val errorsAcc: List[InlineMessage] = errors.groupBy(_.errorType).foldLeft(List.empty[InlineMessage]) {
      case (acc, (_, errorMessages)) =>
        toMessage(errorMessages) :: acc
    }

    TableMessage(
      content = NonEmptyList.fromListUnsafe(errorsAcc),
      heading = Some(Message("site.error"))
    )
  }

  def viewModelErrors(srn: Srn, errors: NonEmptyList[ValidationError]): FormPageViewModel[ContentPageViewModel] =
    FormPageViewModel[ContentPageViewModel](
      title = "fileUploadErrorSummary.title",
      heading = "fileUploadErrorSummary.heading",
      description = Some(
        ParagraphMessage("fileUploadErrorSummary.paragraph") ++
          Heading2("fileUploadErrorSummary.heading2", LabelSize.Medium) ++
          errorSummary(errors) ++
          ParagraphMessage(
            "fileUploadErrorSummary.linkMessage.paragraph.start",
            DownloadLinkMessage(
              "fileUploadErrorSummary.linkMessage",
              routes.DownloadMemberDetailsErrorsController.downloadFile(srn).url
            ),
            "fileUploadErrorSummary.linkMessage.paragraph.end"
          ) ++
          ParagraphMessage(LinkMessage("downloadTemplateFile.hintMessage.print", "#print"))
      ),
      page = ContentPageViewModel(isLargeHeading = true),
      refresh = None,
      buttonText = "site.returnToFileUpload",
      onSubmit = routes.FileUploadErrorSummaryController.onSubmit(srn)
    )

  def viewModelFormatting(srn: Srn, error: ValidationError): FormPageViewModel[ContentPageViewModel] =
    FormPageViewModel[ContentPageViewModel](
      title = "fileUploadErrorSummary.title",
      heading = "fileUploadErrorSummary.heading",
      description = Some(
        ParagraphMessage("fileUploadErrorSummary.paragraph") ++
          Heading2("fileUploadErrorSummary.heading2", LabelSize.Medium) ++
          TableMessage(
            content = NonEmptyList.one(Message(error.message)),
            heading = Some(Message("site.error"))
          ) ++
          ParagraphMessage(LinkMessage("downloadTemplateFile.hintMessage.print", "#print"))
      ),
      page = ContentPageViewModel(isLargeHeading = true),
      refresh = None,
      buttonText = "site.returnToFileUpload",
      onSubmit = routes.FileUploadErrorSummaryController.onSubmit(srn)
    )
}
