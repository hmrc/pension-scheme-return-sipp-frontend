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

package models.requests

import controllers.routes
import models.SchemeId.Srn
import models.{MinimalDetails, PensionSchemeId, SchemeDetails, UserAnswers}
import play.api.libs.json.Reads
import play.api.mvc.Results.Redirect
import play.api.mvc.{Result, WrappedRequest}
import queries.Gettable

import scala.concurrent.Future

case class OptionalDataRequest[A](request: AllowedAccessRequest[A], userAnswers: Option[UserAnswers])
    extends WrappedRequest[A](request) {

  val getUserId: String = request.getUserId

  val pensionSchemeId: PensionSchemeId = request.pensionSchemeId

  val schemeDetails: SchemeDetails = request.schemeDetails

  val srn: Srn = request.srn
}

case class DataRequest[A](underlying: AllowedAccessRequest[A], userAnswers: UserAnswers)
    extends WrappedRequest[A](underlying) {

  val pensionSchemeId: PensionSchemeId = underlying.pensionSchemeId

  val getUserId: String = underlying.getUserId

  val schemeDetails: SchemeDetails = underlying.schemeDetails

  val minimalDetails: MinimalDetails = underlying.minimalDetails

  val srn: Srn = underlying.srn

  def usingAnswer[B: Reads](page: Gettable[B]): UsingAnswer[B] = UsingAnswer(page, userAnswers)
}

class UsingAnswer[A: Reads](page: Gettable[A], userAnswers: UserAnswers) {

  def sync(block: A => Result): Result =
    userAnswers.get(page).fold(Redirect(routes.JourneyRecoveryController.onPageLoad()))(block)

  def async(block: A => Future[Result]): Future[Result] =
    userAnswers
      .get(page)
      .fold(
        Future.successful(Redirect(routes.JourneyRecoveryController.onPageLoad()))
      )(block)
}
