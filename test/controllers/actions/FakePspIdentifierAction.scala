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

import generators.Generators
import models.PensionSchemeId.PspId
import models.requests.IdentifierRequest
import org.scalatest.OptionValues
import play.api.mvc.*

import javax.inject.Inject
import scala.concurrent.{ExecutionContext, Future}

class FakePspIdentifierAction @Inject() (
  val bodyParsers: PlayBodyParsers
)(implicit
  override val executionContext: ExecutionContext
) extends IdentifierAction
    with Generators
    with OptionValues {

  override def invokeBlock[A](request: Request[A], block: IdentifierRequest[A] => Future[Result]): Future[Result] =
    block(practitionerRequestGen(request).map(_.copy(userId = "id", pspId = PspId("A7654321"))).sample.value)

  override def parser: BodyParser[AnyContent] = bodyParsers.default
}
