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

import cats.implicits.toShow
import controllers.ViewChangeQuestionController._
import controllers.actions._
import forms.RadioListFormProvider
import models.SchemeId.{Pstr, Srn}
import models.TypeOfViewChangeQuestion.{ChangeReturn, ViewReturn}
import models.{FormBundleNumber, Mode, TypeOfViewChangeQuestion}
import navigation.Navigator
import pages.ViewChangeQuestionPage
import play.api.Logger
import play.api.data.Form
import play.api.i18n.{I18nSupport, MessagesApi}
import play.api.mvc.{Action, AnyContent, MessagesControllerComponents}
import services.{SaveService, SchemeDateService, TaxYearService}
import uk.gov.hmrc.play.bootstrap.frontend.controller.FrontendBaseController
import uk.gov.hmrc.time.TaxYear
import utils.DateTimeUtils.localDateShow
import viewmodels.DisplayMessage.Message
import viewmodels.implicits._
import viewmodels.models.{FormPageViewModel, RadioListRowViewModel, RadioListViewModel}
import views.html.RadioListView

import javax.inject.{Inject, Named}
import scala.concurrent.{ExecutionContext, Future}

class ViewChangeQuestionController @Inject()(
  override val messagesApi: MessagesApi,
  @Named("sipp") navigator: Navigator,
  formProvider: RadioListFormProvider,
  saveService: SaveService,
  val controllerComponents: MessagesControllerComponents,
  identify: IdentifierAction,
  allowAccess: AllowAccessActionProvider,
  getData: DataRetrievalAction,
  createData: DataCreationAction,
  view: RadioListView,
  schemeDateService: SchemeDateService,
  taxYearService: TaxYearService
)(implicit ec: ExecutionContext)
    extends FrontendBaseController
    with I18nSupport {

  private val logger: Logger = Logger(classOf[ViewChangeQuestionController])
  private val form = ViewChangeQuestionController.form(formProvider)

  def onPageLoad(srn: Srn, mode: Mode): Action[AnyContent] =
    identify.andThen(allowAccess(srn)).async { implicit request =>
      FormBundleNumber
        .optFromSession(request.session)
        .fold {
          logger.error("onPageLoad: could not find 'fbNumber' in the request")
          Future.successful(Redirect(routes.JourneyRecoveryController.onPageLoad()))
        } { fbNumber =>
          val maybePeriods =
            schemeDateService.returnAccountingPeriodsFromEtmp(Pstr(request.schemeDetails.pstr), fbNumber)

          maybePeriods.map { mPeriods =>
            val taxYear = mPeriods.map(taxYearService.latestFromAccountingPeriods).getOrElse(taxYearService.current)

            Ok(
              view(
                form,
                viewModel(srn, fbNumber.value, taxYear, mode)
              )
            )
          }
        }
    }

  def onSubmit(srn: Srn, fbNumber: String, taxYear: Int, mode: Mode): Action[AnyContent] =
    identify.andThen(allowAccess(srn)).andThen(getData).andThen(createData).async { implicit request =>
      form
        .bindFromRequest()
        .fold(
          formWithErrors =>
            Future
              .successful(
                BadRequest(
                  view(
                    formWithErrors,
                    viewModel(srn, fbNumber, TaxYear(taxYear), mode)
                  )
                )
              ),
          answer => {
            for {
              updatedAnswers <- Future.fromTry(
                request.userAnswers.set(ViewChangeQuestionPage(srn), answer)
              )
              _ <- saveService.save(updatedAnswers)
            } yield Redirect(
              navigator.nextPage(
                ViewChangeQuestionPage(srn),
                mode,
                updatedAnswers
              )
            )
          }
        )
    }
}

object ViewChangeQuestionController {

  def form(formProvider: RadioListFormProvider): Form[TypeOfViewChangeQuestion] =
    formProvider[TypeOfViewChangeQuestion](
      "viewChangeQuestion.error.required"
    )

  val radioListItems: List[RadioListRowViewModel] =
    List(
      RadioListRowViewModel(Message("viewChangeQuestion.radioList1"), ViewReturn.name),
      RadioListRowViewModel(Message("viewChangeQuestion.radioList2"), ChangeReturn.name)
    )

  def viewModel(
    srn: Srn,
    fbNumber: String,
    taxYear: TaxYear,
    mode: Mode
  ): FormPageViewModel[RadioListViewModel] =
    FormPageViewModel(
      Message("viewChangeQuestion.title"),
      Message("viewChangeQuestion.heading"),
      RadioListViewModel(
        None,
        radioListItems,
        hint = Some(
          Message("viewChangeQuestion.hint", taxYear.starts.show, taxYear.finishes.show)
        )
      ),
      routes.ViewChangeQuestionController.onSubmit(srn, fbNumber, taxYear.startYear, mode)
    )
}
