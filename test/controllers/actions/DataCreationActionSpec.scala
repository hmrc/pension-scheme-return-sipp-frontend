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

import models.requests.{DataRequest, OptionalDataRequest}
import play.api.mvc.AnyContentAsEmpty
import play.api.test.FakeRequest
import repositories.SessionRepository
import utils.BaseSpec

import scala.concurrent.ExecutionContext.Implicits.global
import scala.concurrent.{ExecutionContext, Future}
import models.requests.AllowedAccessRequest

class DataCreationActionSpec extends BaseSpec {

  class Harness(request: OptionalDataRequest[AnyContentAsEmpty.type], sessionRepository: SessionRepository)(implicit
    ec: ExecutionContext
  ) extends DataCreationActionImpl(sessionRepository)(ec) {
    def callTransform(): Future[DataRequest[AnyContentAsEmpty.type]] =
      transform(request)
  }

  val request: AllowedAccessRequest[AnyContentAsEmpty.type] = allowedAccessRequestGen(FakeRequest()).sample.value
  val userAnswers = arbitraryUserData.arbitrary.sample.value

  "Data Creation Action" - {

    "add user answers to repository" in {

      val sessionRepository = mock[SessionRepository]
      when(sessionRepository.set(any)).thenReturn(Future.successful(()))

      val optionalDataRequest = OptionalDataRequest(request, None)
      val action = Harness(optionalDataRequest, sessionRepository)

      val result = action.callTransform().futureValue

      result.underlying mustBe request
      result.userAnswers.id mustBe request.getUserId + request.srn
      verify(sessionRepository, times(1)).set(any)
    }
  }
}
