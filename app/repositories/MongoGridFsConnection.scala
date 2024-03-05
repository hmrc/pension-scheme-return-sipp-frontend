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

package repositories

import config.FrontendAppConfig
import org.mongodb.scala.gridfs.GridFSBucket
import org.mongodb.scala.model.{IndexModel, IndexOptions, Indexes}
import uk.gov.hmrc.mongo.MongoUtils
import uk.gov.hmrc.mongo.play.PlayMongoComponent

import java.util.concurrent.TimeUnit
import javax.inject.{Inject, Singleton}
import scala.concurrent.ExecutionContext

@Singleton
class MongoGridFsConnection @Inject()(mongo: PlayMongoComponent, appConfig: FrontendAppConfig)(
  implicit ec: ExecutionContext
) {
  private val db = mongo.database
  private val collection = db.getCollection("upload")
  val gridFSBucket: GridFSBucket = GridFSBucket(db, "upload")

  private val filesIndex = Seq(
    IndexModel(
      Indexes.ascending("id"),
      IndexOptions()
        .name("_filesIndex_")
        .unique(true)
        .background(true)
        .sparse(false)
    ),
//    IndexModel(
//      Indexes.ascending("lastUpdated"),
//      IndexOptions()
//        .name("lastUpdatedIdx")
//        .expireAfter(appConfig.uploadTtl, TimeUnit.SECONDS)
//    )
  )

  private val chunksIndex = IndexModel(
    Indexes.ascending("files_id"),
    IndexOptions()
      .name("_chunksIndex_")
      .unique(true)
      .background(true)
      .sparse(false)
  )
//
//  private val indexes = filesIndex :+ chunksIndex
//  MongoUtils.ensureIndexes(collection, indexes, replaceIndexes = false)
}
