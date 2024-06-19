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
import controllers.actions.{AllowAccessActionProvider, DataRequiredAction, DataRetrievalAction, IdentifierAction}
import models.DateRange
import models.SchemeId.Srn
import models.requests.DataRequest
import navigation.Navigator
import pages.WhichTaxYearPage
import play.api.i18n.{I18nSupport, MessagesApi}
import play.api.mvc.{Action, AnyContent, MessagesControllerComponents, Result}
import services.TaxYearService
import uk.gov.hmrc.play.bootstrap.frontend.controller.FrontendBaseController
import utils.DateTimeUtils.localDateShow
import views.html.PreviousReturnsView

import javax.inject.{Inject, Named}
import scala.concurrent.Future

class PreviousReturnsController @Inject()(
     override val messagesApi: MessagesApi,
     @Named("sipp") navigator: Navigator,
     identify: IdentifierAction,
     allowAccess: AllowAccessActionProvider,
     getData: DataRetrievalAction,
     requireData: DataRequiredAction,
     view: PreviousReturnsView,
     taxYearService: TaxYearService,
     val controllerComponents: MessagesControllerComponents
 ) extends FrontendBaseController with I18nSupport {

  def onPageLoad(srn: Srn): Action[AnyContent] = identify.andThen(allowAccess(srn)).andThen(getData).andThen(requireData).async { implicit request =>
    getWhichTaxYear(srn) { taxYear =>
      Future.successful(Ok(view(taxYear.from.show, taxYear.to.show, loggedInUserNameOrRedirect.getOrElse(""))))
    }
  }

  private def getWhichTaxYear(
                               srn: Srn
                             )(f: DateRange => Future[Result])(implicit request: DataRequest[_]): Future[Result] =
    request.userAnswers.get(WhichTaxYearPage(srn)) match {
      case Some(taxYear) => f(taxYear)
      case None => f(DateRange.from(taxYearService.current))
    }

  private def loggedInUserNameOrRedirect(implicit request: DataRequest[_]): Either[Result, String] =
    request.minimalDetails.individualDetails match {
      case Some(individual) => Right(individual.fullName)
      case None =>
        request.minimalDetails.organisationName match {
          case Some(orgName) => Right(orgName)
          case None => Left(Redirect(controllers.routes.JourneyRecoveryController.onPageLoad()))
        }
    }
}

object PreviousReturnsController {

//  def viewModel(
//               srn: Srn,
//
//               )(implicit messages: Messages) = ???
//
//  private def rows(
//                    srn: Srn,
//                    date: LocalDate,
//                    name: String
//                  )(implicit messages: Messages): List[CheckYourAnswersRowViewModel] = List(
//    CheckYourAnswersRowViewModel("3", controllers.routes.PreviousReturnsController.onPageLoad(srn).url).withAction(SummaryAction(""))
//  )

}
