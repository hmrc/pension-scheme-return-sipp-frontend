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
import models.SchemeId.Srn
import models.{Journey, NormalMode}
import navigation.Navigator
import pages.DownloadTemplateFilePage
import play.api.i18n.{I18nSupport, MessagesApi}
import play.api.mvc.{Action, AnyContent, MessagesControllerComponents}
import uk.gov.hmrc.play.bootstrap.frontend.controller.FrontendBaseController
import viewmodels.DisplayMessage
import viewmodels.DisplayMessage.{
  DownloadLinkMessage,
  Heading2,
  InsetTextMessage,
  ListMessage,
  ListType,
  Message,
  ParagraphMessage
}
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

  def onPageLoad(srn: Srn, journey: Journey): Action[AnyContent] =
    identify.andThen(allowAccess(srn)).andThen(getData).andThen(requireData) { implicit request =>
      Ok(view(DownloadTemplateFilePageController.viewModel(srn, journey)))
    }

  def onSubmit(srn: Srn, journey: Journey): Action[AnyContent] =
    identify.andThen(allowAccess(srn)).andThen(getData).andThen(requireData) { implicit request =>
      Redirect(navigator.nextPage(DownloadTemplateFilePage(srn, journey), NormalMode, request.userAnswers))
    }
}

object DownloadTemplateFilePageController {

  def viewModel(srn: Srn, journey: Journey): FormPageViewModel[ContentPageViewModel] =
    FormPageViewModel(
      Message(s"${journey.name}.download.template.title"),
      Message(s"${journey.name}.download.template.heading"),
      ContentPageViewModel(isLargeHeading = true),
      routes.DownloadTemplateFilePageController.onSubmit(srn, journey)
    ).withButtonText(Message("site.continue"))
      .withDescription(
        journeyDetails(journey) ++
          Heading2("download.template.file.downloadTheFile.heading") ++
          ParagraphMessage(
            DownloadLinkMessage(
              "download.template.file.downloadTheFile.linkMessage",
              routes.DownloadTemplateFileController.downloadFile(journey.templateFileType).url
            ),
            "download.template.file.downloadTheFile.paragraph"
          ) ++
          InsetTextMessage(
            "download.template.file.hintMessage.paragraph1",
            "download.template.file.hintMessage.paragraph2"
          )
      )

  private def journeyDetails(journey: Journey): DisplayMessage.CompoundMessage = journey match {
    case Journey.MemberDetails =>
      prologue(journey) ++ formats

    case Journey.InterestInLandOrProperty | Journey.ArmsLengthLandOrProperty =>
      prologue(journey) ++
        formats ++
        whatWeNeedFromYouHeading(journey) ++
        ListMessage(
          ListType.Bullet,
          "download.template.file.weNeedFromYou.address",
          "download.template.file.weNeedFromYou.vendorType",
          "download.template.file.weNeedFromYou.acquisitionDetails",
          "download.template.file.weNeedFromYou.totalCost",
          "download.template.file.weNeedFromYou.jointOwners",
          "download.template.file.weNeedFromYou.totalIncome",
          "download.template.file.weNeedFromYou.leaseDetails"
        )
  }

  private def prologue(journey: Journey): DisplayMessage.CompoundMessage =
    ParagraphMessage(s"${journey.messagePrefix}.download.template.paragraph") ++
      Heading2.medium("download.template.supportingInformation.heading") ++
      ParagraphMessage("download.template.supportingInformation.paragraph") ++
      Heading2("download.template.requiredFormat.heading") ++
      ParagraphMessage("download.template.requiredFormat.paragraph")

  private val formats =
    ListMessage(
      ListType.Bullet,
      "download.template.requiredFormat.nino",
      "download.template.requiredFormat.date",
      "download.template.requiredFormat.money",
      "download.template.requiredFormat.all"
    )

  private def whatWeNeedFromYouHeading(journey: Journey): DisplayMessage.CompoundMessage =
    Heading2(s"${journey.messagePrefix}.download.template.weNeedFromYou.heading") ++
      ParagraphMessage(s"${journey.messagePrefix}.download.template.weNeedFromYou.paragraph")
}
