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

import controllers.actions.{AllowAccessActionProvider, DataRequiredAction, DataRetrievalAction, IdentifierAction}
import models.NormalMode
import models.SchemeId.Srn
import models.enumerations.TemplateFileType.MemberDetailsTemplateFile
import navigation.Navigator
import pages.DownloadTemplateFilePage
import play.api.i18n.{I18nSupport, MessagesApi}
import play.api.mvc.{Action, AnyContent, MessagesControllerComponents}
import uk.gov.hmrc.play.bootstrap.frontend.controller.FrontendBaseController
import viewmodels.DisplayMessage.{DownloadLinkMessage, Heading2, InsetTextMessage, ListMessage, ListType, Message, ParagraphMessage}
import viewmodels.implicits._
import viewmodels.models.{ContentPageViewModel, FormPageViewModel}
import views.html.ContentPageView

import javax.inject.{Inject, Named}

class DownloadTemplateFilePageController @Inject()(
  override val messagesApi: MessagesApi,
  @Named("sipp") navigator: Navigator,
  identify: IdentifierAction,
  allowAccess: AllowAccessActionProvider,
  getData: DataRetrievalAction,
  requireData: DataRequiredAction,
  val controllerComponents: MessagesControllerComponents,
  view: ContentPageView
) extends FrontendBaseController
    with I18nSupport {

  def onPageLoad(srn: Srn): Action[AnyContent] =
    identify.andThen(allowAccess(srn)).andThen(getData).andThen(requireData) { implicit request =>
      Ok(view(DownloadTemplateFilePageController.viewModel(srn)))
    }

  def onSubmit(srn: Srn): Action[AnyContent] =
    identify.andThen(allowAccess(srn)).andThen(getData).andThen(requireData) { implicit request =>
      Redirect(navigator.nextPage(DownloadTemplateFilePage(srn), NormalMode, request.userAnswers))
    }
}

object DownloadTemplateFilePageController {

  def viewModel(srn: Srn): FormPageViewModel[ContentPageViewModel] =
    FormPageViewModel(
      Message("downloadTemplateFile.title"),
      Message("downloadTemplateFile.heading"),
      ContentPageViewModel(isLargeHeading = true),
      routes.DownloadTemplateFilePageController.onSubmit(srn)
    ).withButtonText(Message("site.continue"))
      .withDescription(
        ParagraphMessage("downloadTemplateFile.paragraph") ++
          Heading2.medium("downloadTemplateFile.supportingInformation.heading") ++
          ParagraphMessage("downloadTemplateFile.supportingInformation.paragraph") ++
          Heading2("downloadTemplateFile.requiredFormat.heading") ++
          ParagraphMessage("downloadTemplateFile.requiredFormat.paragraph") ++
          ListMessage(
            ListType.Bullet,
            "downloadTemplateFile.requiredFormat.entity1",
            "downloadTemplateFile.requiredFormat.entity2",
            "downloadTemplateFile.requiredFormat.entity3",
            "downloadTemplateFile.requiredFormat.entity4"
          ) ++
          Heading2("downloadTemplateFile.downloadTheFile.heading") ++
          ParagraphMessage(
            DownloadLinkMessage(
              "downloadTemplateFile.downloadTheFile.linkMessage",
              routes.DownloadTemplateFileController.downloadFile(MemberDetailsTemplateFile).url
            ),
            "downloadTemplateFile.downloadTheFile.paragraph"
          ) ++
          InsetTextMessage(
            "downloadTemplateFile.hintMessage.paragraph1",
            "downloadTemplateFile.hintMessage.paragraph2"
          )
      )
}
