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

import controllers.RemoveFileController._
import controllers.actions._
import forms.YesNoPageFormProvider
import models.SchemeId.Srn
import models.{Journey, JourneyType, Mode}
import navigation.Navigator
import pages.RemoveFilePage
import play.api.data.Form
import play.api.i18n.{I18nSupport, MessagesApi}
import play.api.mvc.{Action, AnyContent, MessagesControllerComponents}
import services._
import uk.gov.hmrc.play.bootstrap.frontend.controller.FrontendBaseController
import viewmodels.DisplayMessage.Message
import viewmodels.models.{FormPageViewModel, YesNoPageViewModel}
import views.html.YesNoPageView

import javax.inject.{Inject, Named}
import scala.concurrent.{ExecutionContext, Future}

class RemoveFileController @Inject()(
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

  def onPageLoad(srn: Srn, journey: Journey, journeyType: JourneyType, mode: Mode): Action[AnyContent] =
    identifyAndRequireData(srn) { implicit request =>
      val preparedForm =
        request.userAnswers.fillForm(RemoveFilePage(srn, journey, journeyType), form(formProvider))
      Ok(view(preparedForm, viewModel(srn, journey, journeyType, mode)))
    }

  def onSubmit(srn: Srn, journey: Journey, journeyType: JourneyType, mode: Mode): Action[AnyContent] =
    identifyAndRequireData(srn).async { implicit request =>
      RemoveFileController
        .form(formProvider)
        .bindFromRequest()
        .fold(
          formWithErrors => Future(BadRequest(view(formWithErrors, viewModel(srn, journey, journeyType, mode)))),
          value =>
            for {
              updatedAnswers <- Future
                .fromTry(request.userAnswers.set(RemoveFilePage(srn, journey, journeyType), value))
              _ <- saveService.save(updatedAnswers)
            } yield Redirect(navigator.nextPage(RemoveFilePage(srn, journey, journeyType), mode, updatedAnswers))
        )
    }
}

object RemoveFileController {

  private val keyBase = "fileDelete"
  def form(formProvider: YesNoPageFormProvider): Form[Boolean] = formProvider(
    "fileDelete.error.required"
  )

  def viewModel(
    srn: Srn,
    journey: Journey,
    journeyType: JourneyType,
    mode: Mode
  ): FormPageViewModel[YesNoPageViewModel] = {
    val journeyKeyBase = s"$keyBase.${journey.entryName}"
    YesNoPageViewModel(
      Message(s"$keyBase.title"),
      Message(s"$journeyKeyBase.heading"),
      onSubmit = routes.RemoveFileController.onSubmit(srn, journey, journeyType)
    )
  }

}