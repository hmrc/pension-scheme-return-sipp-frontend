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
import com.softwaremill.quicklens.*
import controllers.actions.IdentifyAndRequireData
import forms.TextFormProvider
import models.Mode
import models.SchemeId.Srn
import models.backend.responses.MemberDetails
import navigation.Navigator
import pages.{UpdateMembersNoNinoReasonPage, UpdatePersonalDetailsQuestionPage}
import play.api.Logging
import play.api.data.Form
import play.api.i18n.*
import play.api.mvc.{Action, AnyContent, MessagesControllerComponents}
import services.SaveService
import uk.gov.hmrc.play.bootstrap.frontend.controller.FrontendBaseController
import utils.FormUtils.*
import viewmodels.DisplayMessage.Message
import viewmodels.implicits.*
import viewmodels.models.{FormPageViewModel, TextAreaViewModel}
import views.html.CountedTextAreaView

import javax.inject.{Inject, Named}
import scala.concurrent.{ExecutionContext, Future}

class ChangeMembersNoNinoReasonController @Inject() (
  override val messagesApi: MessagesApi,
  @Named("sipp") navigator: Navigator,
  identifyAndRequireData: IdentifyAndRequireData,
  saveService: SaveService,
  formProvider: TextFormProvider,
  view: CountedTextAreaView,
  val controllerComponents: MessagesControllerComponents
)(implicit ec: ExecutionContext)
    extends FrontendBaseController
    with I18nSupport
    with Logging {

  def onPageLoad(srn: Srn, mode: Mode): Action[AnyContent] = identifyAndRequireData(srn) { implicit request =>
    request.userAnswers.get(UpdatePersonalDetailsQuestionPage(srn)) match {
      case None =>
        logger.error(s"Unable to retrieve member details for srn: $srn")
        Redirect(controllers.routes.JourneyRecoveryController.onPageLoad())
      case Some(member) =>
        val form = ChangeMembersNoNinoReasonController
          .form(formProvider)
          .fromUserAnswersMapOpt(UpdatePersonalDetailsQuestionPage(srn))(_.updated.reasonNoNINO)

        Ok(view(form, ChangeMembersNoNinoReasonController.viewModel(srn, member.updated)))
    }
  }

  def onSubmit(srn: Srn, mode: Mode): Action[AnyContent] = identifyAndRequireData(srn).async { implicit request =>
    val form = ChangeMembersNoNinoReasonController.form(formProvider)

    request.userAnswers.get(UpdatePersonalDetailsQuestionPage(srn)) match {
      case None =>
        logger.error(s"Unable to retrieve member details for srn: $srn")
        Future.successful(Redirect(controllers.routes.JourneyRecoveryController.onPageLoad()))
      case Some(member) =>
        form
          .bindFromRequest()
          .fold(
            formWithErrors =>
              Future.successful(
                BadRequest(view(formWithErrors, ChangeMembersNoNinoReasonController.viewModel(srn, member.updated)))
              ),
            answer =>
              saveService
                .updateAndSave(request.userAnswers, UpdatePersonalDetailsQuestionPage(srn))(
                  _.modify(_.updated.reasonNoNINO)
                    .setTo(answer.some)
                    .modify(_.updated.nino)
                    .setTo(None)
                )
                .map(answers => Redirect(navigator.nextPage(UpdateMembersNoNinoReasonPage(srn), mode, answers)))
                .recover { t =>
                  logger.error(s"Failed to update No Nino Reason of the member: ${t.getMessage}", t)
                  Redirect(routes.JourneyRecoveryController.onPageLoad())
                }
          )

    }
  }
}

object ChangeMembersNoNinoReasonController {
  private val characterLimit = 160

  def viewModel(srn: Srn, member: MemberDetails): FormPageViewModel[TextAreaViewModel] =
    FormPageViewModel.applyWithContinue(
      "viewChange.personalDetails.updateNoNinoReason.title",
      Message("viewChange.personalDetails.updateNoNinoReason.heading", member.fullName),
      TextAreaViewModel(limit = Some(characterLimit)),
      routes.ChangeMembersNoNinoReasonController.onSubmit(srn)
    )

  def form(formProvider: TextFormProvider): Form[String] = formProvider.textArea(
    "viewChange.personalDetails.updateNoNinoReason.upload.error.required",
    "noNINO.upload.error.length",
    "viewChange.personalDetails.updateNoNinoReason.upload.error.invalid"
  )
}
