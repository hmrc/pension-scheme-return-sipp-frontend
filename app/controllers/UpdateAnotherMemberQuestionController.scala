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

import cats.implicits.toFunctorOps
import controllers.actions.*
import forms.YesNoPageFormProvider
import models.Mode
import models.SchemeId.Srn
import navigation.Navigator
import pages.{UpdateAnotherMemberQuestionPage, UpdatePersonalDetailsQuestionPage}
import play.api.data.Form
import play.api.i18n.{I18nSupport, Messages, MessagesApi}
import play.api.mvc.{Action, AnyContent, MessagesControllerComponents}
import services.SaveService
import uk.gov.hmrc.play.bootstrap.frontend.controller.FrontendBaseController
import viewmodels.DisplayMessage.Message
import viewmodels.models.{FormPageViewModel, YesNoPageViewModel}
import views.html.YesNoPageView

import javax.inject.{Inject, Named}
import scala.concurrent.{ExecutionContext, Future}

class UpdateAnotherMemberQuestionController @Inject() (
  override val messagesApi: MessagesApi,
  saveService: SaveService,
  @Named("sipp") navigator: Navigator,
  identifyAndRequireData: IdentifyAndRequireData,
  formProvider: YesNoPageFormProvider,
  val controllerComponents: MessagesControllerComponents,
  view: YesNoPageView
)(implicit ec: ExecutionContext) extends FrontendBaseController with I18nSupport {

  private val form = UpdateAnotherMemberQuestionController.form(formProvider)

  def onPageLoad(srn: Srn, mode: Mode): Action[AnyContent] = identifyAndRequireData(srn).async { implicit request =>
    val model = UpdateAnotherMemberQuestionController.viewModel(srn)
    saveService
      .removeAndSave(request.userAnswers, UpdatePersonalDetailsQuestionPage(srn))
      .as(Ok(view(form, model)))
  }

  def onSubmit(srn: Srn, mode: Mode): Action[AnyContent] = identifyAndRequireData(srn).async { implicit request =>
    val viewModel = UpdateAnotherMemberQuestionController.viewModel(srn)

    form
      .bindFromRequest()
      .fold(
        formWithErrors => Future.successful(BadRequest(view(formWithErrors, viewModel))),
        value =>
          saveService
            .setAndSave(request.userAnswers, UpdateAnotherMemberQuestionPage(srn), value)
            .map(updated => Redirect(navigator.nextPage(UpdateAnotherMemberQuestionPage(srn), mode, updated)))
      )
  }
}

object UpdateAnotherMemberQuestionController {

  def form(formProvider: YesNoPageFormProvider): Form[Boolean] =
    formProvider("updateAnotherMember.error.required")

  def viewModel(srn: Srn)(implicit messages: Messages): FormPageViewModel[YesNoPageViewModel] =
    FormPageViewModel(
      Message("updateAnotherMember.title"),
      Message("updateAnotherMember.heading"),
      YesNoPageViewModel(
        yes = Some(Message("updateAnotherMember.selectionYes")),
        no = Some(Message("updateAnotherMember.selectionNo"))
      ),
      routes.UpdateAnotherMemberQuestionController.onSubmit(srn)
    )
}
