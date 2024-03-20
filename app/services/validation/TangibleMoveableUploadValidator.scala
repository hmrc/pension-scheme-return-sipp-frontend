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

package services.validation

import akka.stream.scaladsl.Source
import akka.util.ByteString
import models._
import play.api.i18n.Messages

import java.time.LocalDate
import javax.inject.Inject
import scala.concurrent.Future

class TangibleMoveableUploadValidator @Inject()() extends Validator with UploadValidator {

  def validateUpload(
    source: Source[ByteString, _],
    validDateThreshold: Option[LocalDate]
  )(implicit messages: Messages): Future[(Upload, Int, Long)] =
    Future.successful((UploadSuccessTangibleMoveableProperty(List.empty[String]), 0, 0)) //TODO: Implement validation logc

}
