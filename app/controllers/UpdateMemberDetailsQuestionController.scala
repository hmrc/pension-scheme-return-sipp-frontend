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

import controllers.actions._
import forms.YesNoPageFormProvider
import models.Mode
import models.SchemeId.Srn
import navigation.Navigator
import pages.UpdateMemberDetailsQuestionPage
import play.api.data.Form
import play.api.i18n.{I18nSupport, MessagesApi}
import play.api.mvc.{Action, AnyContent, MessagesControllerComponents}
import services.SaveService
import uk.gov.hmrc.play.bootstrap.frontend.controller.FrontendBaseController
import viewmodels.DisplayMessage.{InsetTextMessage, Message, ParagraphMessage}
import viewmodels.models.{FormPageViewModel, YesNoPageViewModel}
import views.html.YesNoPageView

import javax.inject.{Inject, Named}
import scala.concurrent.{ExecutionContext, Future}

class UpdateMemberDetailsQuestionController @Inject() (
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

  private val form = UpdateMemberDetailsQuestionController.form(formProvider)

  def onPageLoad(srn: Srn, mode: Mode): Action[AnyContent] = identifyAndRequireData(srn) { implicit request =>
    val preparedForm = request.userAnswers.fillForm(UpdateMemberDetailsQuestionPage(srn), form)
    val viewModel = UpdateMemberDetailsQuestionController.viewModel(srn)
    Ok(view(preparedForm, viewModel))
  }

  def onSubmit(srn: Srn, mode: Mode): Action[AnyContent] = identifyAndRequireData(srn).async { implicit request =>
    val viewModel = UpdateMemberDetailsQuestionController.viewModel(srn)

    form
      .bindFromRequest()
      .fold(
        formWithErrors => Future.successful(BadRequest(view(formWithErrors, viewModel))),
        value =>
          saveService
            .setAndSave(request.userAnswers, UpdateMemberDetailsQuestionPage(srn), value)
            .map(updated => Redirect(navigator.nextPage(UpdateMemberDetailsQuestionPage(srn), mode, updated)))
      )
  }
}

object UpdateMemberDetailsQuestionController {

  def form(formProvider: YesNoPageFormProvider): Form[Boolean] =
    formProvider("updateMemberDetailsQuestion.required")

  def viewModel(
    srn: Srn
  ): FormPageViewModel[YesNoPageViewModel] =
    FormPageViewModel(
      Message("updateMemberDetailsQuestion.title"),
      Message("updateMemberDetailsQuestion.heading"),
      YesNoPageViewModel(
        legend = Some(Message("updateMemberDetailsQuestion.question"))
      ),
      onSubmit = routes.UpdateMemberDetailsQuestionController.onSubmit(srn)
    ).withDescription(
      ParagraphMessage(Message("updateMemberDetailsQuestion.paragraph1")) ++
        ParagraphMessage(Message("updateMemberDetailsQuestion.paragraph2")) ++
        InsetTextMessage(Message("updateMemberDetailsQuestion.paragraph3"))
    )
}
