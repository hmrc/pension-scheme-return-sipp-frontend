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

import cats.implicits.{catsSyntaxFlatMapOps, toFlatMapOps}
import com.google.inject.ImplementedBy
import models.UserAnswers
import play.api.libs.json.{Reads, Writes}
import queries.{Gettable, Removable, Settable}
import repositories.SessionRepository
import uk.gov.hmrc.http.HeaderCarrier

import javax.inject.Inject
import scala.concurrent.{ExecutionContext, Future}

class SaveServiceImpl @Inject() (sessionRepository: SessionRepository) extends SaveService {
  override def save(userAnswers: UserAnswers)(implicit hc: HeaderCarrier, ec: ExecutionContext): Future[Unit] =
    sessionRepository.set(userAnswers)

  override def removeAndSave(
    userAnswers: UserAnswers,
    removable: Removable[?]
  )(implicit hc: HeaderCarrier, ec: ExecutionContext): Future[Unit] =
    Future.fromTry(userAnswers.remove(removable)) >>= save

  override def setAndSave[A: Writes](userAnswers: UserAnswers, settable: Settable[A], value: A)(implicit
    hc: HeaderCarrier,
    ec: ExecutionContext
  ): Future[UserAnswers] =
    Future.fromTry(userAnswers.set(settable, value)).flatTap(save)

  override def updateAndSave[A: Writes: Reads](
    userAnswers: UserAnswers,
    page: Settable[A] & Gettable[A]
  )(
    update: A => A,
    errorMessageIfEmpty: Option[String] = None
  )(implicit hc: HeaderCarrier, ec: ExecutionContext): Future[UserAnswers] =
    userAnswers.get(page) match {
      case None =>
        Future.failed(
          new Exception(s"${errorMessageIfEmpty.map(_ + ". ").mkString}Page not found in userAnswers: $page")
        )
      case Some(value) =>
        setAndSave(userAnswers, page, update(value))
    }
}

@ImplementedBy(classOf[SaveServiceImpl])
trait SaveService {
  def save(userAnswers: UserAnswers)(implicit hc: HeaderCarrier, ec: ExecutionContext): Future[Unit]

  def removeAndSave(userAnswers: UserAnswers, removable: Removable[?])(implicit
    hc: HeaderCarrier,
    ec: ExecutionContext
  ): Future[Unit]

  def setAndSave[A: Writes](userAnswers: UserAnswers, settable: Settable[A], value: A)(implicit
    hc: HeaderCarrier,
    ec: ExecutionContext
  ): Future[UserAnswers]

  def updateAndSave[A: Writes: Reads](userAnswers: UserAnswers, page: Settable[A] & Gettable[A])(
    update: A => A,
    errorMessageIfEmpty: Option[String] = None
  )(implicit
    hc: HeaderCarrier,
    ec: ExecutionContext
  ): Future[UserAnswers]
}
