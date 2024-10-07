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

import cats.data.NonEmptyList
import cats.implicits.toTraverseOps
import com.google.inject.ImplementedBy
import config.RefinedTypes.{Max3, OneToThree}
import connectors.PSRConnector
import eu.timepit.refined.refineV
import models.{DateRange, FormBundleNumber}
import models.SchemeId.{Pstr, Srn}
import models.requests.DataRequest
import pages.accountingperiod.AccountingPeriods
import uk.gov.hmrc.http.HeaderCarrier

import java.time.{LocalDateTime, ZoneId}
import javax.inject.Inject
import scala.concurrent.{ExecutionContext, Future}

class SchemeDateServiceImpl @Inject() (connector: PSRConnector) extends SchemeDateService {

  def now(): LocalDateTime = LocalDateTime.now(ZoneId.of("Europe/London"))

  def returnAccountingPeriods(srn: Srn)(implicit request: DataRequest[_]): Option[NonEmptyList[(DateRange, Max3)]] =
    NonEmptyList
      .fromList(request.userAnswers.list(AccountingPeriods(srn)))
      .traverseWithIndexM { case (date, index) =>
        date.traverse(d => refineV[OneToThree](index + 1).toOption.map(refined => d -> refined))
      }
      .flatten

  override def returnAccountingPeriodsFromEtmp(
    pstr: Pstr,
    fbNumber: FormBundleNumber
  )(implicit request: HeaderCarrier, ec: ExecutionContext): Future[Option[NonEmptyList[DateRange]]] =
    connector
      .getPSRSubmission(pstr.value, Some(fbNumber.value), None, None)
      .map { response =>
        response.accountingPeriodDetails.flatMap { details =>
          details.accountingPeriods.flatMap { periods =>
            NonEmptyList.fromList(periods.map(p => DateRange(p.accPeriodStart, p.accPeriodEnd)))
          }
        }
      }
}

@ImplementedBy(classOf[SchemeDateServiceImpl])
trait SchemeDateService {

  def now(): LocalDateTime

  def returnAccountingPeriods(srn: Srn)(implicit request: DataRequest[_]): Option[NonEmptyList[(DateRange, Max3)]]
  def returnAccountingPeriodsFromEtmp(pstr: Pstr, fbNumber: FormBundleNumber)(implicit
    request: HeaderCarrier,
    ec: ExecutionContext
  ): Future[Option[NonEmptyList[DateRange]]]

}
