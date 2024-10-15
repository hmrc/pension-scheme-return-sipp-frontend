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

import org.apache.pekko.actor.ActorSystem
import cats.implicits.toTraverseOps
import config.FrontendAppConfig
import play.api.Logger

import java.time.{Clock, Instant}
import javax.inject.{Inject, Singleton}
import scala.concurrent.ExecutionContext
import scala.concurrent.duration.DurationInt

@Singleton
class MongoGridFSTTLScheduler @Inject() (
  actorSystem: ActorSystem,
  repository: UploadRepository,
  config: FrontendAppConfig,
  clock: Clock
)(implicit executionContext: ExecutionContext) {
  private val logger = Logger(classOf[MongoGridFSTTLScheduler])

  actorSystem.scheduler.scheduleWithFixedDelay(initialDelay = 20.seconds, delay = 1.hour) { () =>
    val expired = Instant.now(clock).minusSeconds(config.uploadTtl)

    for {
      expiredFiles <- repository.findAllOnOrBefore(expired)
      _ = logger.info(s"About to delete files from Upload Repositiry: [${expiredFiles.map(_.value).mkString(", ")}]")
      _ <- expiredFiles.traverse(repository.delete)
      _ = logger.info(s"Deleted files from Upload Repositiry: [${expiredFiles.map(_.value).mkString(", ")}]")
    } yield ()
  }
}
