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

package controllers.landorproperty

import controllers.actions.{AllowAccessActionProvider, DataRequiredAction, DataRetrievalAction, IdentifierAction}
import controllers.routes
import models.NormalMode
import models.SchemeId.Srn
import models.enumerations.TemplateFileType
import navigation.Navigator
import pages.interestlandorproperty.DownloadInterestLandOrPropertyTemplateFilePage
import play.api.i18n.{I18nSupport, MessagesApi}
import play.api.mvc.{Action, AnyContent, MessagesControllerComponents}
import uk.gov.hmrc.play.bootstrap.frontend.controller.FrontendBaseController
import viewmodels.DisplayMessage.{DownloadLinkMessage, Heading2, InsetTextMessage, ListMessage, ListType, Message, ParagraphMessage}
import viewmodels.implicits._
import viewmodels.models.{ContentPageViewModel, FormPageViewModel}
import views.html.ContentPageView

import javax.inject.{Inject, Named}

class DownloadInterestLandOrPropertyTemplateFilePageController @Inject()(
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
      Ok(view(DownloadInterestLandOrPropertyTemplateFilePageController.viewModel(srn)))
    }

  def onSubmit(srn: Srn): Action[AnyContent] =
    identify.andThen(allowAccess(srn)).andThen(getData).andThen(requireData) { implicit request =>
      Redirect(navigator.nextPage(DownloadInterestLandOrPropertyTemplateFilePage(srn), NormalMode, request.userAnswers))
    }
}

object DownloadInterestLandOrPropertyTemplateFilePageController {

  def viewModel(srn: Srn): FormPageViewModel[ContentPageViewModel] =
    FormPageViewModel(
      Message("downloadInterestLandOrPropertyTemplateFile.title"),
      Message("downloadInterestLandOrPropertyTemplateFile.heading"),
      ContentPageViewModel(isLargeHeading = true),
      controllers.landorproperty.routes.DownloadInterestLandOrPropertyTemplateFilePageController.onSubmit(srn)
    ).withButtonText(Message("site.continue"))
      .withDescription(
        ParagraphMessage("downloadInterestLandOrPropertyTemplateFile.paragraph") ++
          Heading2.medium("downloadInterestLandOrPropertyTemplateFile.supportingInformation.heading") ++
          ParagraphMessage("downloadInterestLandOrPropertyTemplateFile.supportingInformation.paragraph") ++
          Heading2("downloadInterestLandOrPropertyTemplateFile.requiredFormat.heading") ++
          ParagraphMessage("downloadInterestLandOrPropertyTemplateFile.requiredFormat.paragraph") ++
          ListMessage(
            ListType.Bullet,
            "downloadInterestLandOrPropertyTemplateFile.requiredFormat.entity1",
            "downloadInterestLandOrPropertyTemplateFile.requiredFormat.entity2",
            "downloadInterestLandOrPropertyTemplateFile.requiredFormat.entity3",
            "downloadInterestLandOrPropertyTemplateFile.requiredFormat.entity4"
          ) ++
          Heading2("downloadInterestLandOrPropertyTemplateFile.weNeedFromYou.heading") ++
          ParagraphMessage("downloadInterestLandOrPropertyTemplateFile.weNeedFromYou.paragraph") ++
          ListMessage(
            ListType.Bullet,
            "downloadInterestLandOrPropertyTemplateFile.weNeedFromYou.entity1",
            "downloadInterestLandOrPropertyTemplateFile.weNeedFromYou.entity2",
            "downloadInterestLandOrPropertyTemplateFile.weNeedFromYou.entity3",
            "downloadInterestLandOrPropertyTemplateFile.weNeedFromYou.entity4",
            "downloadInterestLandOrPropertyTemplateFile.weNeedFromYou.entity5",
            "downloadInterestLandOrPropertyTemplateFile.weNeedFromYou.entity6",
            "downloadInterestLandOrPropertyTemplateFile.weNeedFromYou.entity7"
          ) ++
          Heading2("downloadInterestLandOrPropertyTemplateFile.downloadTheFile.heading") ++
          ParagraphMessage(
            DownloadLinkMessage(
              "downloadInterestLandOrPropertyTemplateFile.downloadTheFile.linkMessage",
              routes.DownloadTemplateFileController.downloadFile(TemplateFileType.InterestLandOrPropertyTemplateFile).url
            ),
            "downloadInterestLandOrPropertyTemplateFile.downloadTheFile.paragraph"
          ) ++
          InsetTextMessage(
            "downloadInterestLandOrPropertyTemplateFile.hintMessage.paragraph1",
            "downloadInterestLandOrPropertyTemplateFile.hintMessage.paragraph2"
          )
      )
}
