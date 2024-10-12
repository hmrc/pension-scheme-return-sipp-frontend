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

package controllers.accountingperiod

import cats.implicits.toShow
import com.google.inject.Inject
import config.RefinedTypes.{refineUnsafe, Max3, OneToThree}
import eu.timepit.refined.auto.autoUnwrap
import controllers.accountingperiod.AccountingPeriodCheckYourAnswersController.viewModel
import controllers.actions.{AllowAccessActionProvider, DataRequiredAction, DataRetrievalAction, IdentifierAction}
import models.SchemeId.Srn
import models.{DateRange, Mode}
import navigation.Navigator
import pages.accountingperiod.{AccountingPeriodCheckYourAnswersPage, AccountingPeriodPage}
import play.api.i18n.{I18nSupport, MessagesApi}
import play.api.mvc.{Action, AnyContent, MessagesControllerComponents}
import uk.gov.hmrc.play.bootstrap.frontend.controller.FrontendBaseController
import utils.DateTimeUtils.localDateShow
import viewmodels.models.{CheckYourAnswersRowViewModel, CheckYourAnswersViewModel, FormPageViewModel, SummaryAction}
import views.html.CheckYourAnswersView

import javax.inject.Named

class AccountingPeriodCheckYourAnswersController @Inject() (
  override val messagesApi: MessagesApi,
  @Named("sipp") navigator: Navigator,
  identify: IdentifierAction,
  allowAccess: AllowAccessActionProvider,
  getData: DataRetrievalAction,
  requireData: DataRequiredAction,
  val controllerComponents: MessagesControllerComponents,
  view: CheckYourAnswersView
) extends FrontendBaseController
    with I18nSupport {

  def onPageLoad(srn: Srn, index: Int, mode: Mode): Action[AnyContent] = {
    val indexRefined = refineUnsafe[Int, OneToThree](index)
    identify.andThen(allowAccess(srn)).andThen(getData).andThen(requireData) { implicit request =>
      request.userAnswers.get(AccountingPeriodPage(srn, indexRefined, mode)) match {
        case None =>
          Redirect(controllers.accountingperiod.routes.AccountingPeriodController.onPageLoad(srn, index, mode).url)
        case Some(accountingPeriod) =>
          Ok(view(viewModel(srn, indexRefined, accountingPeriod, mode)))
      }
    }
  }

  def onSubmit(srn: Srn, mode: Mode): Action[AnyContent] =
    identify.andThen(allowAccess(srn)).andThen(getData).andThen(requireData) { implicit request =>
      Redirect(navigator.nextPage(AccountingPeriodCheckYourAnswersPage(srn, mode), mode, request.userAnswers))
    }
}

object AccountingPeriodCheckYourAnswersController {

  private def rows(srn: Srn, index: Max3, dateRange: DateRange, mode: Mode): List[CheckYourAnswersRowViewModel] = List(
    CheckYourAnswersRowViewModel("site.startDate", dateRange.from.show)
      .withAction(
        SummaryAction(
          "site.change",
          controllers.accountingperiod.routes.AccountingPeriodController.onPageLoad(srn, index, mode).url
        ).withVisuallyHiddenContent("site.startDate")
      ),
    CheckYourAnswersRowViewModel("site.endDate", dateRange.to.show)
      .withAction(
        SummaryAction(
          "site.change",
          controllers.accountingperiod.routes.AccountingPeriodController.onPageLoad(srn, index, mode).url
        ).withVisuallyHiddenContent("site.endDate")
      )
  )

  def viewModel(srn: Srn, index: Max3, dateRange: DateRange, mode: Mode): FormPageViewModel[CheckYourAnswersViewModel] =
    CheckYourAnswersViewModel(
      rows(srn, index, dateRange, mode),
      controllers.accountingperiod.routes.AccountingPeriodCheckYourAnswersController.onSubmit(srn, mode)
    )
}
