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
import com.google.inject.ImplementedBy
import config.Constants
import models.DateRange
import play.api.mvc.Request
import uk.gov.hmrc.time.{CurrentTaxYear, TaxYear}

import java.time.LocalDate
import javax.inject.Inject

class TaxYearServiceImpl @Inject() extends TaxYearService with CurrentTaxYear {
  override def now: () => LocalDate = () => LocalDate.now()

  override def latestFromAccountingPeriods(periods: NonEmptyList[DateRange]): TaxYear =
    TaxYear(periods.toList.maxBy(_.from).from.getYear)

  override def fromRequest()(implicit request: Request[?]): DateRange =
    request.session
      .get(Constants.taxYear)
      .map(LocalDate.parse(_))
      .map(_.getYear)
      .map(TaxYear(_))
      .map(DateRange.from)
      .getOrElse(DateRange.from(current))
}

@ImplementedBy(classOf[TaxYearServiceImpl])
trait TaxYearService {

  def fromRequest()(implicit request: Request[?]): DateRange

  def latestFromAccountingPeriods(periods: NonEmptyList[DateRange]): TaxYear

  def current: TaxYear
}
