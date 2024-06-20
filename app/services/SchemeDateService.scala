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
import eu.timepit.refined.refineV
import models.DateRange
import models.SchemeId.Srn
import models.requests.DataRequest
import pages.WhichTaxYearPage
import pages.accountingperiod.AccountingPeriods

import java.time.{LocalDateTime, ZoneId}
import javax.inject.Inject

class SchemeDateServiceImpl @Inject()() extends SchemeDateService {

  def now(): LocalDateTime = LocalDateTime.now(ZoneId.of("Europe/London"))

  def returnAccountingPeriods(srn: Srn)(implicit request: DataRequest[_]): Option[NonEmptyList[(DateRange, Max3)]] =
    NonEmptyList
      .fromList(request.userAnswers.list(AccountingPeriods(srn)))
      .traverseWithIndexM {
        case (date, index) => date.traverse(d => refineV[OneToThree](index + 1).toOption.map(refined => d -> refined))
      }
      .flatten

  def returnPeriods(srn: Srn)(implicit request: DataRequest[_]): Option[NonEmptyList[DateRange]] = {
    val accountingPeriods = request.userAnswers.list(AccountingPeriods(srn))

    if (accountingPeriods.isEmpty) {
      request.userAnswers.get(WhichTaxYearPage(srn)).map(NonEmptyList.one)
    } else {
      NonEmptyList.fromList(accountingPeriods)
    }
  }

}

@ImplementedBy(classOf[SchemeDateServiceImpl])
trait SchemeDateService {

  def now(): LocalDateTime

  def returnPeriods(srn: Srn)(implicit request: DataRequest[_]): Option[NonEmptyList[DateRange]]
  def returnAccountingPeriods(srn: Srn)(implicit request: DataRequest[_]): Option[NonEmptyList[(DateRange, Max3)]]

}
