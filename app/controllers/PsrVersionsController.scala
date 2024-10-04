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
import controllers.actions.IdentifyAndRequireData
import models.SchemeId.{Pstr, Srn}
import models.requests.DataRequest
import play.api.i18n.{I18nSupport, MessagesApi}
import play.api.mvc.{Action, AnyContent, MessagesControllerComponents, Result}
import services.{PsrVersionsService, SchemeDateService, TaxYearService}
import uk.gov.hmrc.play.bootstrap.frontend.controller.FrontendBaseController
import utils.DateTimeUtils.localDateShow
import views.html.PsrReturnsView

import javax.inject.Inject
import scala.concurrent.ExecutionContext

class PsrVersionsController @Inject() (
  override val messagesApi: MessagesApi,
  view: PsrReturnsView,
  psrVersionsService: PsrVersionsService,
  schemeDateService: SchemeDateService,
  taxYearService: TaxYearService,
  identifyAndRequireData: IdentifyAndRequireData,
  val controllerComponents: MessagesControllerComponents
)(implicit ec: ExecutionContext)
    extends FrontendBaseController
    with I18nSupport {

  def onPageLoad(srn: Srn): Action[AnyContent] =
    identifyAndRequireData.withFormBundle(srn).async { request =>
      implicit val dataRequest = request.underlying
      val pstr = request.underlying.schemeDetails.pstr

      for {
        accPeriods <- schemeDateService.returnAccountingPeriodsFromEtmp(Pstr(pstr), request.formBundleNumber)
        taxYear = accPeriods.map(taxYearService.latestFromAccountingPeriods).getOrElse(taxYearService.current)
        versions <- psrVersionsService.getPsrVersions(pstr, taxYear.starts)
      } yield Ok(
        view(srn, taxYear.starts.show, taxYear.finishes.show, loggedInUserNameOrRedirect.getOrElse(""), versions)
      )

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
}
