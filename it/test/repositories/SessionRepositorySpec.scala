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

package repositories

import config.{FakeCrypto, FrontendAppConfig}
import models.UserAnswers
import models.UserAnswers.SensitiveJsObject
import org.mongodb.scala.bson.BsonDocument
import org.mongodb.scala.model.Filters
import org.mockito.Mockito.when
import org.mongodb.scala.ObservableFuture

import java.time.Instant
import scala.concurrent.ExecutionContext.Implicits.global

class SessionRepositorySpec extends BaseRepositorySpec[UserAnswers] {

  private val savedAnswers = jsObjectGen(maxDepth = 5).sample.value
  private val userAnswers = UserAnswers("id", SensitiveJsObject(savedAnswers), Instant.ofEpochSecond(1))

  private val mockAppConfig = mock[FrontendAppConfig]
  when(mockAppConfig.cacheTtl).thenReturn(1L)

  override protected val repository: SessionRepository = SessionRepository(
    mongoComponent = mongoComponent,
    appConfig = mockAppConfig,
    clock = stubClock,
    crypto = FakeCrypto
  )

  ".set" - {
    "must set the last updated time on the supplied user answers to `now`, and save them" in {
      val expectedResult = userAnswers.copy(lastUpdated = instant)
      repository.set(userAnswers).futureValue
      val updatedRecord = find(Filters.equal("_id", userAnswers.id)).futureValue.headOption.value
      updatedRecord mustEqual expectedResult
    }
  }

  ".get" - {
    "when there is a record for this id" - {
      "must update the lastUpdated time and get the record" in {
        insert(userAnswers).futureValue
        val result = repository.get(userAnswers.id).futureValue
        val expectedResult = userAnswers.copy(lastUpdated = instant)
        result.value mustEqual expectedResult
      }
    }

    "when there is no record for this id" - {
      "must return None" in {
        repository.get("id that does not exist").futureValue must not be defined
      }
    }
  }

  ".clear" - {
    "must remove a record" in {
      insert(userAnswers).futureValue
      repository.clear(userAnswers.id).futureValue
      repository.get(userAnswers.id).futureValue must not be defined
    }

    "must return unit when there is no record to remove" in {
      val result = repository.clear("id that does not exist").futureValue
      result mustEqual ()
    }
  }

  ".keepAlive" - {
    "when there is a record for this id" - {
      "must update its lastUpdated to `now` and return unit" in {
        insert(userAnswers).futureValue
        repository.keepAlive(userAnswers.id).futureValue
        val expectedUpdatedAnswers = userAnswers.copy(lastUpdated = instant)
        val updatedAnswers = find(Filters.equal("_id", userAnswers.id)).futureValue.headOption.value
        updatedAnswers mustEqual expectedUpdatedAnswers
      }
    }

    "when there is no record for this id" - {
      "must return unit" in {
        repository.keepAlive("id that does not exist").futureValue mustEqual ()
      }
    }
  }

  "encrypt data at rest" in {
    insert(userAnswers).futureValue
    val rawData =
      repository.collection
        .find[BsonDocument](Filters.equal("_id", userAnswers.id))
        .toFuture()
        .futureValue
        .headOption

    assert(rawData.nonEmpty)
    rawData.map(_.get("data").asString().getValue must fullyMatch.regex("^[A-Za-z0-9+/=]+$"))
  }
}
