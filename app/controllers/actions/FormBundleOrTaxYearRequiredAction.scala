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

package controllers.actions

import controllers.routes
import models.requests.{DataRequest, FormBundleOrTaxYearRequest}
import models.{FormBundleNumber, TaxYear}
import play.api.mvc.Results.Redirect
import play.api.mvc.{ActionRefiner, Result}

import javax.inject.Inject
import scala.concurrent.{ExecutionContext, Future}
import scala.util.chaining.*

class FormBundleOrTaxYearRequiredActionImpl @Inject() (implicit val executionContext: ExecutionContext)
    extends FormBundleOrTaxYearRequiredAction {

  override protected def refine[A](
    request: DataRequest[A]
  ): Future[Either[Result, FormBundleOrTaxYearRequest[A]]] = {
    val session = request.session

    // First, try to get the FormBundleNumber from the session
    val formBundleNumberOpt = FormBundleNumber.optFromSession(session)
    val taxYearOpt = TaxYear.optFromSession(session)

    ((formBundleNumberOpt, taxYearOpt) match {
      case (None, None) =>
        Left(Redirect(routes.JourneyRecoveryController.onPageLoad()))
      case _ =>
        Right(FormBundleOrTaxYearRequest(formBundleNumberOpt, taxYearOpt, request))
    }).pipe(Future.successful)
  }
}

trait FormBundleOrTaxYearRequiredAction extends ActionRefiner[DataRequest, FormBundleOrTaxYearRequest]
