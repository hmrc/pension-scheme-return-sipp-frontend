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
import models.SchemeId.Srn
import models.requests.DataRequest
import navigation.Navigator
import play.api.i18n.{I18nSupport, MessagesApi}
import play.api.mvc.{Action, AnyContent, MessagesControllerComponents, Result}
import services.PsrVersionsService
import uk.gov.hmrc.play.bootstrap.frontend.controller.FrontendBaseController
import utils.DateTimeUtils.localDateShow
import views.html.PsrReturnsView

import java.time.LocalDate
import javax.inject.{Inject, Named}
import scala.concurrent.ExecutionContext

class PsrVersionsController @Inject()(
     override val messagesApi: MessagesApi,
     @Named("sipp") navigator: Navigator,
     identify: IdentifierAction,
     allowAccess: AllowAccessActionProvider,
     getData: DataRetrievalAction,
     requireData: DataRequiredAction,
     view: PsrReturnsView,
     psrVersionsService: PsrVersionsService,
     val controllerComponents: MessagesControllerComponents
 )(implicit ec: ExecutionContext) extends FrontendBaseController with I18nSupport {

  def onPageLoad(srn: Srn): Action[AnyContent] =
    identify.andThen(allowAccess(srn)).andThen(getData).andThen(requireData).async { implicit request =>
      val dateFrom: LocalDate = LocalDate.of(2022, 4, 6)
      val dateTo: LocalDate = LocalDate.of(2023, 4, 6)
      //getWhichTaxYear(srn) { taxYear =>
      psrVersionsService.getPsrVersions(request.schemeDetails.pstr, dateFrom).map { versions =>
        Ok(view(dateFrom.show, dateTo.show, loggedInUserNameOrRedirect.getOrElse(""), versions))
      }
    }

  //  private def getWhichTaxYear(
  //                               srn: Srn
  //                             )(f: DateRange => Future[Result])(implicit request: DataRequest[_]): Future[Result] =
  //    request.userAnswers.get(WhichTaxYearPage(srn)) match {
  //      case Some(taxYear) => f(taxYear)
  //      case None => f(DateRange.from(taxYearService.current))
  //    }

  private def loggedInUserNameOrRedirect(implicit request: DataRequest[_]): Either[Result, String] =
    request.minimalDetails.individualDetails match {
      case Some(individual) => Right(individual.firstName + " " + individual.lastName)
      case None =>
        request.minimalDetails.organisationName match {
          case Some(orgName) => Right(orgName)
          case None => Left(Redirect(controllers.routes.JourneyRecoveryController.onPageLoad()))
        }
    }
}
