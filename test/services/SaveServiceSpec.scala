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

package services

import models.UserAnswers
import models.UserAnswers.SensitiveJsObject
import org.scalatestplus.scalacheck.ScalaCheckPropertyChecks
import play.api.libs.json.{JsPath, Json}
import queries.{Gettable, Removable, Settable}
import repositories.SessionRepository
import uk.gov.hmrc.http.HeaderCarrier
import utils.BaseSpec

import scala.concurrent.ExecutionContext.Implicits.global
import scala.concurrent.Future

class SaveServiceSpec extends BaseSpec with ScalaCheckPropertyChecks {

  implicit val hc: HeaderCarrier = HeaderCarrier()

  val mockSessionRepository: SessionRepository = mock[SessionRepository]
  val service = new SaveServiceImpl(mockSessionRepository)

  override def beforeEach(): Unit = {
    super.beforeEach()
    reset(mockSessionRepository)
  }

  "save should save user answers successfully" in {
    val userAnswers = UserAnswers("id", SensitiveJsObject(Json.obj()))

    when(mockSessionRepository.set(userAnswers)).thenReturn(Future.successful(()))

    val result = service.save(userAnswers).futureValue

    result mustEqual (())
    verify(mockSessionRepository, times(1)).set(userAnswers)
  }

  "removeAndSave should remove and save user answers successfully" in {
    val userAnswers = UserAnswers("id", SensitiveJsObject(Json.obj("key" -> "value")))
    val removable = new Removable[String] {
      override def path = JsPath() \ "key"
    }

    when(mockSessionRepository.set(any)).thenReturn(Future.successful(()))

    val result = service.removeAndSave(userAnswers, removable).futureValue

    result mustEqual (())
    verify(mockSessionRepository, times(1)).set(any)
  }

  "setAndSave should set and save user answers successfully" in {
    val userAnswers = UserAnswers("id", SensitiveJsObject(Json.obj()))
    val settable = new Settable[String] {
      override def path = JsPath() \ "key"
    }
    val value = "newValue"

    when(mockSessionRepository.set(any)).thenReturn(Future.successful(()))

    val result = service.setAndSave(userAnswers, settable, value).futureValue

    result.data.decryptedValue mustBe Json.obj("key" -> "newValue")
    verify(mockSessionRepository, times(1)).set(any)
  }

  "updateAndSave should update and save user answers successfully" in {
    val userAnswers = UserAnswers("id", SensitiveJsObject(Json.obj("key" -> "oldValue")))
    val page = new Settable[String] with Gettable[String] {
      override def path = JsPath() \ "key"
    }
    val update: String => String = _ => "updatedValue"

    when(mockSessionRepository.set(any)).thenReturn(Future.successful(()))

    val result = service.updateAndSave(userAnswers, page)(update).futureValue

    result.data.decryptedValue mustBe Json.obj("key" -> "updatedValue")
    verify(mockSessionRepository, times(1)).set(any)
  }

  "updateAndSave should fail when page is not found in user answers" in {
    val userAnswers = UserAnswers("id", SensitiveJsObject(Json.obj()))
    val page = new Settable[String] with Gettable[String] {
      override def path = JsPath() \ "key"
    }
    val update: String => String = _ => "updatedValue"

    val result = service.updateAndSave(userAnswers, page)(update).failed.futureValue

    result mustBe a[Exception]
    result.getMessage must include("Page not found in userAnswers")
  }
}
