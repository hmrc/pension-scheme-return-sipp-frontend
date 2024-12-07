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
import models.{Journey, JourneyType, NormalMode}
import navigation.Navigator
import pages.DownloadTemplateFilePage
import play.api.i18n.{I18nSupport, MessagesApi}
import play.api.mvc.{Action, AnyContent, MessagesControllerComponents}
import uk.gov.hmrc.play.bootstrap.frontend.controller.FrontendBaseController
import viewmodels.DisplayMessage
import viewmodels.DisplayMessage.{
  DownloadLinkMessage,
  Heading2,
  Heading3,
  InsetTextMessage,
  ListMessage,
  ListType,
  Message,
  ParagraphMessage
}
import viewmodels.implicits.*
import viewmodels.models.{ContentPageViewModel, FormPageViewModel}
import views.html.ContentPageView

import javax.inject.{Inject, Named}

class DownloadTemplateFilePageController @Inject() (
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

  def onPageLoad(srn: Srn, journey: Journey, journeyType: JourneyType): Action[AnyContent] =
    identify.andThen(allowAccess(srn)).andThen(getData).andThen(requireData) { implicit request =>
      Ok(view(DownloadTemplateFilePageController.viewModel(srn, journey, journeyType)))
    }

  def onSubmit(srn: Srn, journey: Journey, journeyType: JourneyType): Action[AnyContent] =
    identify.andThen(allowAccess(srn)).andThen(getData).andThen(requireData) { implicit request =>
      Redirect(navigator.nextPage(DownloadTemplateFilePage(srn, journey, journeyType), NormalMode, request.userAnswers))
    }
}

object DownloadTemplateFilePageController {

  def viewModel(srn: Srn, journey: Journey, journeyType: JourneyType): FormPageViewModel[ContentPageViewModel] =
    FormPageViewModel(
      Message(s"${journey.messagePrefix}.download.template.file.title"),
      Message(s"${journey.messagePrefix}.download.template.file.heading"),
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
          (if (journeyType == JourneyType.Amend) {
             InsetTextMessage(
               "download.template.file.hintMessage.paragraph1",
               "download.template.file.hintMessage.paragraph2"
             )
           } else {
             InsetTextMessage("download.template.file.hintMessage.paragraph1")
           })
      )

  private def journeyDetails(journey: Journey): DisplayMessage.CompoundMessage = journey match {
    case Journey.InterestInLandOrProperty =>
      prologue(journey) ++
        formats ++
        whatWeNeedFromYouHeading(journey) ++
        ListMessage(
          ListType.Bullet,
          "download.template.file.weNeedFromYou.address",
          "download.template.file.weNeedFromYou.totalCost",
          "download.template.file.weNeedFromYou.totalIncome",
          "download.template.file.weNeedFromYou.detailsLand"
        )

    case Journey.ArmsLengthLandOrProperty =>
      prologue(journey) ++
        formats ++
        whatWeNeedFromYouHeading(journey) ++
        ListMessage(
          ListType.Bullet,
          "download.template.file.weNeedFromYou.addressIfApplicable",
          "download.template.file.weNeedFromYou.acquiredDate",
          "download.template.file.weNeedFromYou.totalCost",
          "download.template.file.weNeedFromYou.totalIncome",
          "download.template.file.weNeedFromYou.detailsLand"
        )

    case Journey.TangibleMoveableProperty =>
      prologue(journey) ++
        formats ++
        whatWeNeedFromYouHeading(journey) ++
        ListMessage(
          ListType.Bullet,
          "download.template.file.weNeedFromYou.reference",
          "download.template.file.weNeedFromYou.acquiredDate",
          "download.template.file.weNeedFromYou.totalCost",
          "download.template.file.weNeedFromYou.totalIncome",
          "download.template.file.weNeedFromYou.detailsLease"
        )

    case Journey.OutstandingLoans =>
      prologue(journey) ++
        formats ++
        whatWeNeedFromYouHeading(journey) ++
        ListMessage(
          ListType.Bullet,
          "download.template.file.weNeedFromYou.repaymentDate",
          "download.template.file.weNeedFromYou.interestRate"
        )

    case Journey.UnquotedShares =>
      prologue(journey) ++
        formats ++
        whatWeNeedFromYouHeading(journey) ++
        ListMessage(
          ListType.Bullet,
          "download.template.file.weNeedFromYou.repaymentDate",
          "download.template.file.weNeedFromYou.interestRate"
        )

    case Journey.AssetFromConnectedParty =>
      prologue(journey) ++
        formats ++
        whatWeNeedFromYouHeading(journey) ++
        ListMessage(
          ListType.Bullet,
          "download.template.file.weNeedFromYou.reference",
          "download.template.file.weNeedFromYou.acquiredDateAndValuation",
          "download.template.file.weNeedFromYou.totalCost",
          "download.template.file.weNeedFromYou.totalIncomeFromAsset",
          "download.template.file.weNeedFromYou.detailsDisposal"
        )
  }

  private def prologue(journey: Journey): DisplayMessage.CompoundMessage =
    ParagraphMessage(s"${journey.messagePrefix}.download.template.file.paragraph") ++
      Heading2.medium("download.template.file.supportingInformation.heading") ++
      ParagraphMessage("download.template.file.supportingInformation.paragraph") ++
      Heading3("download.template.file.requiredFormat.heading") ++
      ParagraphMessage("download.template.file.requiredFormat.paragraph")

  private val formats =
    ListMessage(
      ListType.Bullet,
      "download.template.file.requiredFormat.nino",
      "download.template.file.requiredFormat.date",
      "download.template.file.requiredFormat.money",
      "download.template.file.requiredFormat.all"
    )

  private def whatWeNeedFromYouHeading(journey: Journey): DisplayMessage.CompoundMessage =
    Heading3(s"${journey.messagePrefix}.download.template.file.weNeedFromYou.heading") ++
      ParagraphMessage(s"${journey.messagePrefix}.download.template.file.weNeedFromYou.paragraph")
}
