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
import connectors.PSRConnector
import models.SchemeId.Pstr
import models.requests.common.YesNo
import models.requests.{DataRequest, FormBundleOrTaxYearRequest, FormBundleOrVersionTaxYearRequest}
import models.{BasicDetails, DateRange, FormBundleNumber, VersionTaxYear}
import play.api.mvc.AnyContent
import uk.gov.hmrc.http.{HeaderCarrier, NotFoundException}

import java.time.format.DateTimeFormatter
import java.time.{LocalDateTime, ZoneId}
import javax.inject.Inject
import scala.concurrent.{ExecutionContext, Future}

class SchemeDateServiceImpl @Inject() (connector: PSRConnector) extends SchemeDateService {

  def now(): LocalDateTime = LocalDateTime.now(ZoneId.of("Europe/London"))

  def returnAccountingPeriods[A](request: FormBundleOrVersionTaxYearRequest[A])(implicit
    executionContext: ExecutionContext,
    headerCarrier: HeaderCarrier,
    dataRequest: DataRequest[AnyContent]
  ): Future[Option[NonEmptyList[DateRange]]] =
    returnBasicDetails(request).map(_.flatMap(_.accountingPeriods))

  def returnAccountingPeriods[A](request: FormBundleOrTaxYearRequest[A])(implicit
    executionContext: ExecutionContext,
    headerCarrier: HeaderCarrier,
    dataRequest: DataRequest[AnyContent]
  ): Future[Option[NonEmptyList[DateRange]]] =
    returnBasicDetails(request).map(_.flatMap(_.accountingPeriods))

  override def returnBasicDetails(
    pstr: Pstr,
    fbNumber: FormBundleNumber
  )(implicit
    hc: HeaderCarrier,
    ec: ExecutionContext,
    dataRequest: DataRequest[AnyContent]
  ): Future[Option[BasicDetails]] =
    fetchBasicDetails(pstr, Some(fbNumber.value), None)

  override def returnBasicDetails(
    pstr: Pstr,
    versionTaxYear: VersionTaxYear
  )(implicit
    hc: HeaderCarrier,
    ec: ExecutionContext,
    dataRequest: DataRequest[AnyContent]
  ): Future[Option[BasicDetails]] =
    fetchBasicDetails(pstr, None, Some(versionTaxYear))

  private def fetchBasicDetails(
    pstr: Pstr,
    fbNumber: Option[String],
    versionTaxYear: Option[VersionTaxYear]
  )(implicit
    hc: HeaderCarrier,
    ec: ExecutionContext,
    dataRequest: DataRequest[AnyContent]
  ): Future[Option[BasicDetails]] =
    connector
      .getPSRSubmission(
        pstr.value,
        fbNumber,
        versionTaxYear.map(_.taxYearDateRange.from.format(DateTimeFormatter.ISO_DATE)),
        versionTaxYear.map(_.version)
      )
      .map { response =>
        Some(
          BasicDetails(
            accountingPeriods = response.accountingPeriodDetails.flatMap { details =>
              details.accountingPeriods.map { periods =>
                periods.map(p => DateRange(p.accPeriodStart, p.accPeriodEnd))
              }
            },
            taxYearDateRange = response.details.taxYearDateRange,
            memberDetails = response.details.memberTransactions,
            status = response.details.status,
            oneOrMoreTransactionFilesUploaded = YesNo(
              response.landArmsLength.isDefined || response.landConnectedParty.isDefined
                || response.loanOutstanding.isDefined || response.tangibleProperty.isDefined
                || response.otherAssetsConnectedParty.isDefined || response.unquotedShares.isDefined
            )
          )
        )
      }
      .recover { case _: NotFoundException =>
        None
      }

}

@ImplementedBy(classOf[SchemeDateServiceImpl])
trait SchemeDateService {

  def now(): LocalDateTime

  def returnAccountingPeriods[A](request: FormBundleOrVersionTaxYearRequest[A])(implicit
    executionContext: ExecutionContext,
    headerCarrier: HeaderCarrier,
    dataRequest: DataRequest[AnyContent]
  ): Future[Option[NonEmptyList[DateRange]]]

  def returnAccountingPeriods[A](request: FormBundleOrTaxYearRequest[A])(implicit
    executionContext: ExecutionContext,
    headerCarrier: HeaderCarrier,
    dataRequest: DataRequest[AnyContent]
  ): Future[Option[NonEmptyList[DateRange]]]

  def returnBasicDetails(pstr: Pstr, fbNumber: FormBundleNumber)(implicit
    request: HeaderCarrier,
    ec: ExecutionContext,
    dataRequest: DataRequest[AnyContent]
  ): Future[Option[BasicDetails]]

  def returnBasicDetails(pstr: Pstr, versionTaxYear: VersionTaxYear)(implicit
    hc: HeaderCarrier,
    ec: ExecutionContext,
    dataRequest: DataRequest[AnyContent]
  ): Future[Option[BasicDetails]]

  final def returnBasicDetails[A](request: FormBundleOrVersionTaxYearRequest[A])(implicit
    executionContext: ExecutionContext,
    headerCarrier: HeaderCarrier,
    dataRequest: DataRequest[AnyContent]
  ): Future[Option[BasicDetails]] = {
    implicit val underlying: DataRequest[A] = request.underlying

    for {
      pstr <- Future.successful(Pstr(underlying.schemeDetails.pstr))
      mDetailsFBundle <- request.formBundleNumber.flatTraverse(returnBasicDetails(pstr, _))
      mDetailsVersion <- request.versionTaxYear.flatTraverse(returnBasicDetails(pstr, _))
    } yield mDetailsFBundle.orElse(mDetailsVersion)
  }

  final def returnBasicDetails[A](request: FormBundleOrTaxYearRequest[A])(implicit
    executionContext: ExecutionContext,
    headerCarrier: HeaderCarrier,
    dataRequest: DataRequest[AnyContent]
  ): Future[Option[BasicDetails]] = {
    implicit val underlying: DataRequest[A] = request.underlying

    for {
      pstr <- Future.successful(Pstr(underlying.schemeDetails.pstr))
      mDetailsFBundle <- request.formBundleNumber.flatTraverse(returnBasicDetails(pstr, _))
    } yield mDetailsFBundle
  }
}
