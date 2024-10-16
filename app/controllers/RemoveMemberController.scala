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

import controllers.actions.*
import forms.YesNoPageFormProvider
import models.Mode
import models.SchemeId.{Pstr, Srn}
import models.backend.responses.MemberDetails
import models.requests.DataRequest
import navigation.Navigator
import pages.{RemoveMemberPage, RemoveMemberQuestionPage}
import play.api.data.Form
import play.api.i18n.{I18nSupport, MessagesApi}
import play.api.mvc.{Action, AnyContent, MessagesControllerComponents}
import services.{ReportDetailsService, SaveService}
import uk.gov.hmrc.play.bootstrap.frontend.controller.FrontendBaseController
import viewmodels.DisplayMessage.{Message, ParagraphMessage}
import viewmodels.implicits.*
import viewmodels.models.{FormPageViewModel, YesNoPageViewModel}
import views.html.YesNoPageView

import javax.inject.{Inject, Named}
import scala.concurrent.{ExecutionContext, Future}

class RemoveMemberController @Inject() (
  override val messagesApi: MessagesApi,
  saveService: SaveService,
  @Named("sipp") navigator: Navigator,
  identifyAndRequireData: IdentifyAndRequireData,
  formProvider: YesNoPageFormProvider,
  val controllerComponents: MessagesControllerComponents,
  reportDetailsService: ReportDetailsService,
  view: YesNoPageView
)(implicit ec: ExecutionContext)
    extends FrontendBaseController
    with I18nSupport {

  private val form = RemoveMemberController.form(formProvider)

  def onPageLoad(srn: Srn, mode: Mode): Action[AnyContent] = identifyAndRequireData(srn) { implicit request =>
    request.userAnswers.get(RemoveMemberPage(srn)) match {
      case None =>
        Redirect(controllers.routes.JourneyRecoveryController.onPageLoad())
      case Some(member) =>
        val preparedForm = request.userAnswers.fillForm(RemoveMemberQuestionPage(srn), form)
        val viewModel = RemoveMemberController.viewModel(srn, member)
        Ok(view(preparedForm, viewModel))
    }
  }

  def onSubmit(srn: Srn, mode: Mode): Action[AnyContent] = identifyAndRequireData.withFormBundle(srn).async { request =>
    implicit val dataRequest: DataRequest[AnyContent] = request.underlying

    dataRequest.userAnswers.get(RemoveMemberPage(srn)) match {
      case None =>
        Future.successful(Redirect(controllers.routes.JourneyRecoveryController.onPageLoad()))
      case Some(member) =>
        val viewModel = RemoveMemberController.viewModel(srn, member)

        form
          .bindFromRequest()
          .fold(
            formWithErrors => Future.successful(BadRequest(view(formWithErrors, viewModel))),
            value =>
              for {
                updatedAnswers <- Future.fromTry(dataRequest.userAnswers.set(RemoveMemberQuestionPage(srn), value))
                _ <- saveService.save(updatedAnswers)
                _ <-
                  if (value)
                    reportDetailsService
                      .deleteMemberDetail(request.formBundleNumber, Pstr(dataRequest.schemeDetails.pstr), member)
                  else Future.successful(())
              } yield Redirect(navigator.nextPage(RemoveMemberQuestionPage(srn), mode, updatedAnswers))
          )
    }
  }

}

object RemoveMemberController {
  def form(formProvider: YesNoPageFormProvider): Form[Boolean] =
    formProvider("deleteMember.required")

  def viewModel(
    srn: Srn,
    member: MemberDetails
  ): FormPageViewModel[YesNoPageViewModel] = {
    val memberFullName: String = s"${member.firstName} ${member.lastName}"
    FormPageViewModel(
      Message("deleteMember.title"),
      Message("deleteMember.heading"),
      YesNoPageViewModel(
        legend = Some(Message("deleteMember.question", memberFullName))
      ),
      onSubmit = routes.RemoveMemberController.onSubmit(srn)
    ).withDescription(
      ParagraphMessage(Message("deleteMember.paragraph"))
    )
  }
}
