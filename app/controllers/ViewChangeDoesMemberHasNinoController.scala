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
import models.SchemeId.Srn
import models.backend.responses.MemberDetails
import navigation.Navigator
import pages.{UpdatePersonalDetailsMemberHasNinoQuestionPage, UpdatePersonalDetailsQuestionPage}
import play.api.data.Form
import play.api.i18n.{I18nSupport, MessagesApi}
import play.api.mvc.{Action, AnyContent, MessagesControllerComponents}
import services.SaveService
import uk.gov.hmrc.play.bootstrap.frontend.controller.FrontendBaseController
import viewmodels.DisplayMessage.Message
import viewmodels.models.{FormPageViewModel, YesNoPageViewModel}
import views.html.YesNoPageView

import viewmodels.implicits.*
import javax.inject.{Inject, Named}
import scala.concurrent.{ExecutionContext, Future}

class ViewChangeDoesMemberHasNinoController @Inject() (
  override val messagesApi: MessagesApi,
  saveService: SaveService,
  @Named("sipp") navigator: Navigator,
  identifyAndRequireData: IdentifyAndRequireData,
  formProvider: YesNoPageFormProvider,
  val controllerComponents: MessagesControllerComponents,
  view: YesNoPageView
)(implicit ec: ExecutionContext)
    extends FrontendBaseController
    with I18nSupport {

  private val form = ViewChangeDoesMemberHasNinoController.form(formProvider)

  def onPageLoad(srn: Srn, mode: Mode): Action[AnyContent] = identifyAndRequireData(srn) { implicit request =>
    request.userAnswers.get(UpdatePersonalDetailsQuestionPage(srn)) match {
      case None =>
        Redirect(controllers.routes.JourneyRecoveryController.onPageLoad())
      case Some(member) =>
        val viewModel = ViewChangeDoesMemberHasNinoController.viewModel(srn, member.updated)
        Ok(view(form.fill(member.updated.nino.isDefined), viewModel))
    }

  }

  def onSubmit(srn: Srn, mode: Mode): Action[AnyContent] = identifyAndRequireData(srn).async { implicit request =>
    request.userAnswers.get(UpdatePersonalDetailsQuestionPage(srn)) match {
      case None =>
        Future.successful(Redirect(controllers.routes.JourneyRecoveryController.onPageLoad()))
      case Some(member) =>
        val viewModel = ViewChangeDoesMemberHasNinoController.viewModel(srn, member.updated)

        form
          .bindFromRequest()
          .fold(
            formWithErrors => Future.successful(BadRequest(view(formWithErrors, viewModel))),
            value =>
              saveService
                .setAndSave(
                  request.userAnswers,
                  UpdatePersonalDetailsMemberHasNinoQuestionPage(srn),
                  value
                )
                .map(answers =>
                  Redirect(navigator.nextPage(UpdatePersonalDetailsMemberHasNinoQuestionPage(srn), mode, answers))
                )
          )
    }
  }
}

object ViewChangeDoesMemberHasNinoController {

  def form(formProvider: YesNoPageFormProvider): Form[Boolean] =
    formProvider("viewChange.doesHaveNino.required")

  def viewModel(
    srn: Srn,
    member: MemberDetails
  ): FormPageViewModel[YesNoPageViewModel] =
    FormPageViewModel(
      Message("viewChange.doesHaveNino.title", member.fullName),
      Message("viewChange.doesHaveNino.heading", member.fullName),
      YesNoPageViewModel(),
      onSubmit = routes.ViewChangeDoesMemberHasNinoController.onSubmit(srn),
      buttonText = Message("site.continue")
    )
}
