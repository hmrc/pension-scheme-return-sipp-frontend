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

import controllers.NewFileUploadController._
import controllers.actions._
import forms.UploadNewFileQuestionPageFormProvider
import models.SchemeId.Srn
import models.{Journey, Mode}
import navigation.Navigator
import pages.{NewFileUploadPage, TaskListStatusPage}
import play.api.data.Form
import play.api.i18n.{I18nSupport, MessagesApi}
import play.api.mvc.{Action, AnyContent, MessagesControllerComponents}
import services.SaveService
import uk.gov.hmrc.play.bootstrap.frontend.controller.FrontendBaseController
import viewmodels.DisplayMessage.Message
import viewmodels.implicits._
import viewmodels.models.{FormPageViewModel, UploadNewFileQuestionPageViewModel}
import views.html.UploadNewFileQuestionView

import javax.inject.{Inject, Named}
import scala.concurrent.{ExecutionContext, Future}

class NewFileUploadController @Inject()(
  override val messagesApi: MessagesApi,
  @Named("sipp") navigator: Navigator,
  identifyAndRequireData: IdentifyAndRequireData,
  formProvider: UploadNewFileQuestionPageFormProvider,
  view: UploadNewFileQuestionView,
  saveService: SaveService,
  val controllerComponents: MessagesControllerComponents
)(implicit ec: ExecutionContext)
    extends FrontendBaseController
    with I18nSupport {

  def onPageLoad(srn: Srn, journey: Journey, mode: Mode): Action[AnyContent] = identifyAndRequireData(srn).async {
    implicit request =>
      request.userAnswers.get(TaskListStatusPage(srn, journey)) match {
        case Some(res) =>
          val preparedForm = request.userAnswers.fillForm(NewFileUploadPage(srn, journey), form(formProvider))
          Future.successful(Ok(view(preparedForm, viewModel(srn, journey, res.countOfTransactions))))
        case None => Future.successful(Redirect(controllers.routes.JourneyRecoveryController.onPageLoad()))
      }
  }

  def onSubmit(srn: Srn, journey: Journey, mode: Mode): Action[AnyContent] = identifyAndRequireData(srn).async {
    implicit request =>
      NewFileUploadController
        .form(formProvider)
        .bindFromRequest()
        .fold(
          formWithErrors =>
            Future.successful(
              BadRequest(
                view(formWithErrors, viewModel(srn, journey, formWithErrors.data.get("count").getOrElse("0").toInt))
              )
            ),
          value =>
            for {
              updatedAnswers <- Future.fromTry(request.userAnswers.set(NewFileUploadPage(srn, journey), value))
              _ <- saveService.save(updatedAnswers)
              redirectTo <- Future
                .successful(Redirect(navigator.nextPage(NewFileUploadPage(srn, journey), mode, updatedAnswers)))
            } yield redirectTo
        )
  }
}

object NewFileUploadController {

  def form(formProvider: UploadNewFileQuestionPageFormProvider): Form[Boolean] = formProvider(
    s"newFileUpload.error.required"
  )
  def viewModel(
    srn: Srn,
    journey: Journey,
    count: Int
  ): FormPageViewModel[UploadNewFileQuestionPageViewModel] =
    UploadNewFileQuestionPageViewModel(
      title = Message("newFileUpload.title"),
      heading = Message("newFileUpload.heading"),
      question = Message("newFileUpload.question"),
      hint = Message("newFileUpload.hint"),
      details = Message("newFileUpload.records", count),
      count = count,
      onSubmit = routes.NewFileUploadController.onSubmit(srn, journey)
    )
}
