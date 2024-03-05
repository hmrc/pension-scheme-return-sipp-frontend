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

import cats.data.NonEmptyList
import controllers.FileUploadErrorSummaryController.{viewModelErrors, viewModelFormatting}
import controllers.actions._
import models.SchemeId.Srn
import models.{Journey, Mode, UploadErrors, UploadFormatError, UploadKey, ValidationError}
import navigation.Navigator
import pages.UploadErrorSummaryPage
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

  def onPageLoad(srn: Srn, journey: Journey): Action[AnyContent] = identifyAndRequireData(srn).async {
    implicit request =>
      uploadService
        .getUploadResult(UploadKey.fromRequest(srn, journey.uploadRedirectTag))
        .map {
          case Some(uploadErrors: UploadErrors) => Ok(view(viewModelErrors(srn, journey, uploadErrors.errors)))
          case Some(UploadFormatError(e)) => Ok(view(viewModelFormatting(srn, journey, e)))
          case _ => Redirect(controllers.routes.JourneyRecoveryController.onPageLoad())
        }
  }

  def onSubmit(srn: Srn, journey: Journey, mode: Mode): Action[AnyContent] = identifyAndRequireData(srn) {
    implicit request =>
      Redirect(navigator.nextPage(UploadErrorSummaryPage(srn, journey), mode, request.userAnswers))
  }
}

object FileUploadErrorSummaryController {

  private def errorSummary(errors: NonEmptyList[ValidationError]): TableMessage = {
    def toMessage(errors: NonEmptyList[ValidationError]) = CompoundMessage(
      ParagraphMessage(errors.head.message),
      ParagraphMessage(Message("fileUploadErrorSummary.table.message", errors.map(_.row).toList.mkString(",")))
    )

    val errorsAcc: List[InlineMessage] =
      errors.groupBy(_.message).foldLeft(List.empty[InlineMessage]) { // TODO Group BY!!!!!!!
        case (acc, (_, errorMessages)) =>
          toMessage(errorMessages) :: acc
      }

    TableMessage(
      content = NonEmptyList.fromListUnsafe(errorsAcc),
      heading = Some(Message("site.error"))
    )
  }

  def viewModelErrors(
    srn: Srn,
    journey: Journey,
    errors: NonEmptyList[ValidationError]
  ): FormPageViewModel[ContentPageViewModel] =
    FormPageViewModel[ContentPageViewModel](
      title = s"${journey.messagePrefix}.fileUploadErrorSummary.title",
      heading = s"${journey.messagePrefix}.fileUploadErrorSummary.heading",
      description = Some(
        ParagraphMessage("fileUploadErrorSummary.paragraph") ++
          Heading2("fileUploadErrorSummary.heading2", LabelSize.Medium) ++
          errorSummary(errors) ++
          ParagraphMessage(
            "fileUploadErrorSummary.linkMessage.paragraph.start",
            DownloadLinkMessage(
              "fileUploadErrorSummary.linkMessage",
              journey match {
                case Journey.MemberDetails =>
                  controllers.memberdetails.routes.DownloadMemberDetailsErrorsController
                    .downloadFile(srn)
                    .url //TODO make generic ?
                case Journey.InterestInLandOrProperty =>
                  controllers.landorproperty.routes.DownloadLandOrPropertyErrorsController.downloadFile(srn, Journey.InterestInLandOrProperty).url
                case Journey.ArmsLengthLandOrProperty =>
                  controllers.landorproperty.routes.DownloadLandOrPropertyErrorsController.downloadFile(srn, Journey.ArmsLengthLandOrProperty).url
              }
            ),
            "fileUploadErrorSummary.linkMessage.paragraph.end"
          ) ++
          ParagraphMessage(LinkMessage("download.template.file.hintMessage.print", "#print"))
      ),
      page = ContentPageViewModel(isLargeHeading = true),
      refresh = None,
      buttonText = "site.returnToFileUpload",
      onSubmit = routes.FileUploadErrorSummaryController.onSubmit(srn, journey)
    )

  def viewModelFormatting(srn: Srn, journey: Journey, error: ValidationError): FormPageViewModel[ContentPageViewModel] =
    FormPageViewModel[ContentPageViewModel](
      title = s"${journey.messagePrefix}.fileUploadErrorSummary.title",
      heading = s"${journey.messagePrefix}.fileUploadErrorSummary.heading",
      description = Some(
        ParagraphMessage("fileUploadErrorSummary.paragraph") ++
          Heading2("fileUploadErrorSummary.heading2", LabelSize.Medium) ++
          TableMessage(
            content = NonEmptyList.one(Message(error.message)),
            heading = Some(Message("site.error"))
          ) ++
          ParagraphMessage(LinkMessage("download.template.file.hintMessage.print", "#print"))
      ),
      page = ContentPageViewModel(isLargeHeading = true),
      refresh = None,
      buttonText = "site.returnToFileUpload",
      onSubmit = routes.FileUploadErrorSummaryController.onSubmit(srn, journey)
    )
}
