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
import models.ReportStatus.SubmittedAndSuccessfullyProcessed
import models.SchemeId.Srn
import models.requests.DataRequest
import models.{DateRange, PsrVersionsResponse, ReportSubmitterDetails}
import navigation.Navigator
import pages.WhichTaxYearPage
import play.api.i18n.{I18nSupport, MessagesApi}
import play.api.mvc.{Action, AnyContent, MessagesControllerComponents, Result}
import services.{PsrVersionsService, TaxYearService}
import uk.gov.hmrc.http.HeaderCarrier
import uk.gov.hmrc.play.bootstrap.frontend.controller.FrontendBaseController
import utils.DateTimeUtils.localDateShow
import views.html.PsrReturnsView

import java.time.{LocalDate, ZonedDateTime}
import javax.inject.{Inject, Named}
import scala.concurrent.{ExecutionContext, Future}

class PsrVersionsController @Inject()(
     override val messagesApi: MessagesApi,
     @Named("sipp") navigator: Navigator,
     identify: IdentifierAction,
     allowAccess: AllowAccessActionProvider,
     getData: DataRetrievalAction,
     requireData: DataRequiredAction,
     view: PsrReturnsView,
     taxYearService: TaxYearService,
     psrVersionsService: PsrVersionsService,
     val controllerComponents: MessagesControllerComponents
 )(implicit ec: ExecutionContext) extends FrontendBaseController with I18nSupport {



  val psrVersionResponse1 = PsrVersionsResponse(
    reportFormBundleNumber = "123456",
    reportVersion = 1,
    reportStatus = SubmittedAndSuccessfullyProcessed,
    compilationOrSubmissionDate = ZonedDateTime.now,
    reportSubmitterDetails = Some(ReportSubmitterDetails("Omiros", None, None)),
    psaDetails = None
  )

  // Second instance
  val psrVersionResponse2 = PsrVersionsResponse(
    reportFormBundleNumber = "654321",
    reportVersion = 2,
    reportStatus = SubmittedAndSuccessfullyProcessed,
    compilationOrSubmissionDate = ZonedDateTime.now,
    reportSubmitterDetails = Some(ReportSubmitterDetails("Tom", None, None)),
    psaDetails = None
  )

  // third instance
  val psrVersionResponse3 = PsrVersionsResponse(
    reportFormBundleNumber = "654321",
    reportVersion = 3,
    reportStatus = SubmittedAndSuccessfullyProcessed,
    compilationOrSubmissionDate = ZonedDateTime.now,
    reportSubmitterDetails = Some(ReportSubmitterDetails("Gios tis", None, None)),
    psaDetails = None
  )

  // Sequence of instances
  val psrVersionsResponses = Seq(psrVersionResponse1, psrVersionResponse2, psrVersionResponse3)

  def onPageLoad(srn: Srn): Action[AnyContent] =
    identify.andThen(allowAccess(srn)).andThen(getData).andThen(requireData).async { implicit request =>
      getWhichTaxYear(srn) { taxYear =>
        getPsrVersions(request.schemeDetails.pstr, taxYear.from).map { versions =>
          Ok(view(taxYear.from.show, taxYear.to.show, loggedInUserNameOrRedirect.getOrElse(""),versions, psrVersionsResponses))
        }
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
      case Some(individual) => Right(individual.firstName + " " + individual.lastName)
      case None =>
        request.minimalDetails.organisationName match {
          case Some(orgName) => Right(orgName)
          case None => Left(Redirect(controllers.routes.JourneyRecoveryController.onPageLoad()))
        }
    }

  def getPsrVersions(psr: String, startDate: LocalDate)(implicit headerCarrier: HeaderCarrier): Future[Seq[PsrVersionsResponse]] = {
    psrVersionsService.getPsrVersions(psr, startDate).map {
      case result: Seq[PsrVersionsResponse] =>
        result
      case _ => throw new Exception("PSR backend call failed with code exception")
    }
  }
}