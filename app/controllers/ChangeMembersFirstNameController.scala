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

import cats.implicits.catsSyntaxOptionId
import controllers.ChangeMembersFirstNameController.viewModel
import controllers.actions.IdentifyAndRequireData
import forms.TextFormProvider
import models.Mode
import models.SchemeId.Srn
import navigation.Navigator
import pages.{UpdateMembersFirstNamePage, UpdatePersonalDetailsQuestionPage}
import play.api.Logging
import play.api.data.Form
import play.api.i18n._
import play.api.mvc.{Action, AnyContent, MessagesControllerComponents}
import services.SaveService
import uk.gov.hmrc.play.bootstrap.frontend.controller.FrontendBaseController
import utils.FormUtils._
import viewmodels.DisplayMessage.Message
import viewmodels.implicits._
import viewmodels.models.{FormPageViewModel, TextInputViewModel}
import views.html.TextInputView

import javax.inject.{Inject, Named}
import scala.concurrent.{ExecutionContext, Future}

class ChangeMembersFirstNameController @Inject()(
  override val messagesApi: MessagesApi,
  @Named("sipp") navigator: Navigator,
  identifyAndRequireData: IdentifyAndRequireData,
  saveService: SaveService,
  formProvider: TextFormProvider,
  view: TextInputView,
  val controllerComponents: MessagesControllerComponents
)(implicit ec: ExecutionContext)
    extends FrontendBaseController
    with I18nSupport
    with Logging {
  def onPageLoad(srn: Srn, mode: Mode): Action[AnyContent] = identifyAndRequireData(srn) { implicit request =>
    val form = ChangeMembersFirstNameController.form(formProvider).fromUserAnswers(UpdateMembersFirstNamePage(srn))
    Ok(view(form, viewModel(srn)))
  }

  def onSubmit(srn: Srn, mode: Mode): Action[AnyContent] = identifyAndRequireData(srn).async { implicit request =>
    val form = ChangeMembersFirstNameController.form(formProvider)
    form
      .bindFromRequest()
      .fold(
        formWithErrors => Future.successful(BadRequest(view(formWithErrors, viewModel(srn)))),
        answer => {
          val op = for {
            updatedAnswers <- Future.fromTry(request.userAnswers.set(UpdateMembersFirstNamePage(srn), answer))
            personalDetails <- request.userAnswers.get(UpdatePersonalDetailsQuestionPage(srn)) match {
              case Some(personalDetails) =>
                Future.successful(personalDetails.copy(updated = personalDetails.updated.copy(firstName = answer)))
              case None =>
                Future.failed(new Exception(s"Expected UpdatePersonalDetailsQuestionPage(${srn.value})"))
            }
            _ <- saveService.setAndSave(updatedAnswers, UpdatePersonalDetailsQuestionPage(srn), personalDetails)
          } yield updatedAnswers
          op.map(answers => Redirect(navigator.nextPage(UpdateMembersFirstNamePage(srn), mode, answers)))
            .recover { t =>
              logger.error("Failed to update first name of the member", t)
              Redirect(routes.JourneyRecoveryController.onPageLoad())
            }
        }
      )
  }
}
object ChangeMembersFirstNameController {
  def viewModel(srn: Srn): FormPageViewModel[TextInputViewModel] =
    FormPageViewModel(
      "viewChange.personalDetails.updateFirstName.title",
      "viewChange.personalDetails.updateFirstName.heading",
      TextInputViewModel(Message("viewChange.personalDetails.updateFirstName.firstName").some, isFixedLength = true),
      routes.ChangeMembersFirstNameController.onSubmit(srn)
    )

  def form(formProvider: TextFormProvider): Form[String] = formProvider.name(
    "memberDetails.firstName.upload.error.required",
    "viewChange.personalDetails.firstName.upload.error.length",
    "viewChange.personalDetails.firstName.upload.error.invalid"
  )
}
