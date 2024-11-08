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

package controllers

import cats.syntax.option.*
import config.Constants
import connectors.PSRConnector
import controllers.actions.*
import forms.YesNoPageFormProvider
import models.JourneyType.Amend
import models.SchemeId.Srn
import models.backend.responses.MemberDetails
import models.requests.DataRequest
import models.{JourneyType, Mode}
import navigation.Navigator
import pages.{RemoveMemberPage, RemoveMemberQuestionPage}
import play.api.data.Form
import play.api.i18n.{I18nSupport, Messages, MessagesApi}
import play.api.mvc.{Action, AnyContent, MessagesControllerComponents}
import services.{SaveService, SchemeDetailsService}
import uk.gov.hmrc.govukfrontend.views.viewmodels.summarylist.{SummaryList, SummaryListRow}
import uk.gov.hmrc.http.HeaderCarrier
import uk.gov.hmrc.play.bootstrap.frontend.controller.FrontendBaseController
import viewmodels.Caption
import viewmodels.DisplayMessage.{CaptionHeading2, Message, ParagraphMessage}
import viewmodels.implicits.*
import viewmodels.govuk.summarylist.*
import viewmodels.models.{FormPageViewModel, FurtherDetailsViewModel, YesNoPageViewModel}
import views.html.YesNoPageView

import java.time.format.DateTimeFormatter
import javax.inject.{Inject, Named}
import scala.concurrent.{ExecutionContext, Future}

class RemoveMemberController @Inject() (
  override val messagesApi: MessagesApi,
  saveService: SaveService,
  schemeDetailsService: SchemeDetailsService,
  @Named("sipp") navigator: Navigator,
  identifyAndRequireData: IdentifyAndRequireData,
  formProvider: YesNoPageFormProvider,
  val controllerComponents: MessagesControllerComponents,
  psrConnector: PSRConnector,
  view: YesNoPageView
)(implicit ec: ExecutionContext)
    extends FrontendBaseController
    with I18nSupport {

  private val form = RemoveMemberController.form(formProvider)

  def onPageLoad(srn: Srn, mode: Mode): Action[AnyContent] = identifyAndRequireData(srn).async { implicit request =>
    request.userAnswers.get(RemoveMemberPage(srn)) match {
      case None =>
        Future.successful(Redirect(routes.JourneyRecoveryController.onPageLoad()))
      case Some(member) =>
        val preparedForm = request.userAnswers.fillForm(RemoveMemberQuestionPage(srn), form)
        schemeDetailsService.getMinimalSchemeDetails(request.pensionSchemeId, srn).map {
          case Some(schemeDetails) =>
            val viewModel = RemoveMemberController.viewModel(srn, member, schemeDetails.name)
            Ok(view(preparedForm, viewModel))
          case None =>
            Redirect(routes.UnauthorisedController.onPageLoad)
        }
    }
  }

  def onSubmit(srn: Srn, mode: Mode): Action[AnyContent] = identifyAndRequireData.withFormBundle(srn).async { request =>
    implicit val dataRequest: DataRequest[AnyContent] = request.underlying

    dataRequest.userAnswers.get(RemoveMemberPage(srn)) match {
      case None =>
        Future.successful(Redirect(routes.JourneyRecoveryController.onPageLoad()))
      case Some(member) =>
        schemeDetailsService.getMinimalSchemeDetails(request.underlying.pensionSchemeId, srn).flatMap {
          case Some(schemeDetails) =>
            val viewModel = RemoveMemberController.viewModel(srn, member, schemeDetails.name)

            val pstr = dataRequest.underlying.schemeDetails.pstr
            val fbNumber = request.formBundleNumber.value

            form
              .bindFromRequest()
              .fold(
                formWithErrors => Future.successful(BadRequest(view(formWithErrors, viewModel))),
                value =>
                  for {
                    updatedAnswers <- Future.fromTry(dataRequest.userAnswers.set(RemoveMemberQuestionPage(srn), value))
                    _ <- saveService.save(updatedAnswers)
                    formBundleNumber <- removeMember(pstr, fbNumber, member, value)
                  } yield Redirect(navigator.nextPage(RemoveMemberQuestionPage(srn), mode, updatedAnswers))
                    .addingToSession(Constants.formBundleNumber -> formBundleNumber)
              )

          case None =>
            Future.successful(Redirect(routes.UnauthorisedController.onPageLoad))
        }
    }
  }

  private def removeMember(pstr: String, fbNumber: String, member: MemberDetails, value: Boolean)(implicit
    hc: HeaderCarrier
  ): Future[String] =
    if (value) {
      psrConnector.deleteMember(pstr, Amend, fbNumber.some, None, None, member).map(_.formBundleNumber)
    } else {
      Future(fbNumber)
    }

}

object RemoveMemberController {
  def form(formProvider: YesNoPageFormProvider): Form[Boolean] =
    formProvider("deleteMember.required")

  def viewModel(
    srn: Srn,
    member: MemberDetails,
    schemeName: String
  )(implicit messages: Messages): FormPageViewModel[YesNoPageViewModel] =
    FormPageViewModel(
      title = Message("deleteMember.title"),
      heading = Message("deleteMember.heading"),
      page = YesNoPageViewModel(
        legend = Some(Message("deleteMember.question", member.fullName))
      ),
      onSubmit = routes.RemoveMemberController.onSubmit(srn)
    )
      .withDescription(ParagraphMessage(Message("deleteMember.paragraph")))
      .withAdditionalHeadingText(CaptionHeading2(Message(schemeName), Caption.Large))
      .withSummaryList(
        SummaryList(
          Seq(
            SummaryListRow("deleteMember.personalDetails.firstName", ValueViewModel(member.firstName)),
            SummaryListRow("deleteMember.personalDetails.lastName", ValueViewModel(member.lastName)),
            member.nino match {
              case Some(nino) =>
                SummaryListRow("deleteMember.personalDetails.nino", ValueViewModel(nino))
              case None =>
                SummaryListRow(
                  Message("deleteMember.personalDetails.reasonNoNINO", member.fullName).toMessage,
                  ValueViewModel(member.reasonNoNINO.mkString)
                )
            },
            SummaryListRow(
              "deleteMember.personalDetails.dob",
              ValueViewModel(member.dateOfBirth.format(DateTimeFormatter.ofPattern("dd MM yyyy")))
            )
          )
        )
      )
}
