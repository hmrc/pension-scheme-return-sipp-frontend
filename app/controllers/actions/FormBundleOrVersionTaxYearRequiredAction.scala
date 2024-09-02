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
import models.requests.{DataRequest, FormBundleOrVersionTaxYearRequest}
import models.{FormBundleNumber, VersionTaxYear}
import play.api.mvc.Results.Redirect
import play.api.mvc.{ActionRefiner, Result}

import javax.inject.Inject
import scala.concurrent.{ExecutionContext, Future}

class FormBundleOrVersionTaxYearRequiredActionImpl @Inject()(implicit val executionContext: ExecutionContext)
    extends FormBundleOrVersionTaxYearRequiredAction {

  override protected def refine[A](
    request: DataRequest[A]
  ): Future[Either[Result, FormBundleOrVersionTaxYearRequest[A]]] = {
    val session = request.session

    // First, try to get the FormBundleNumber from the session
    val formBundleNumberOpt = FormBundleNumber.optFromSession(session)

    formBundleNumberOpt match {
      case Some(formBundleNumber) =>
        // If FormBundleNumber exists, then check version and tax year and add all of them
        VersionTaxYear.optFromSession(session) match {
          case Some(versionTaxYear) =>
            Future.successful(Right(FormBundleOrVersionTaxYearRequest(Some(formBundleNumber), Some(versionTaxYear), request)))
          case None =>
            Future.successful(Right(FormBundleOrVersionTaxYearRequest(Some(formBundleNumber), None, request)))
        }

      case None =>
        VersionTaxYear.optFromSession(session) match {
          case Some(versionTaxYear) =>
            Future.successful(Right(FormBundleOrVersionTaxYearRequest(None, Some(versionTaxYear), request)))
          case None =>
            Future.successful(Left(Redirect(routes.JourneyRecoveryController.onPageLoad())))
        }
    }
  }
}

trait FormBundleOrVersionTaxYearRequiredAction extends ActionRefiner[DataRequest, FormBundleOrVersionTaxYearRequest]
