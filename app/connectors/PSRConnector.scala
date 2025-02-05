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

package connectors

import cats.implicits.toTraverseOps
import cats.syntax.option.*
import config.FrontendAppConfig
import models.SchemeId.Srn
import models.audit.FileUploadAuditEvent
import models.backend.responses.*
import models.error.{EtmpRequestDataSizeExceedError, EtmpServerError}
import models.requests.*
import models.requests.AssetsFromConnectedPartyApi.*
import models.requests.LandOrConnectedPropertyApi.*
import models.requests.OutstandingLoanApi.*
import models.requests.PsrSubmissionRequest.PsrSubmittedResponse
import models.requests.TangibleMoveablePropertyApi.*
import models.requests.UnquotedShareApi.*
import models.requests.common.YesNo
import models.requests.psr.ReportDetails
import models.{
  DateRange,
  FormBundleNumber,
  Journey,
  JourneyType,
  PsrVersionsResponse,
  UploadKey,
  UploadStatus,
  VersionTaxYear
}
import play.api.Logging
import play.api.http.Status
import play.api.http.Status.{NOT_FOUND, REQUEST_ENTITY_TOO_LARGE}
import play.api.libs.json.*
import play.api.libs.ws.writeableOf_JsValue
import play.api.mvc.Session
import services.{AuditService, TaxYearService, UploadService}
import uk.gov.hmrc.http.HttpReads.Implicits.*
import uk.gov.hmrc.http.client.HttpClientV2
import uk.gov.hmrc.http.{
  HeaderCarrier,
  HttpResponse,
  InternalServerException,
  NotFoundException,
  StringContextOps,
  UpstreamErrorResponse
}
import utils.Country
import utils.HttpUrl.makeUrl

import java.net.URL
import java.time.LocalDate
import java.time.format.DateTimeFormatter
import java.util.UUID
import javax.inject.Inject
import scala.concurrent.{ExecutionContext, Future}

class PSRConnector @Inject() (
  appConfig: FrontendAppConfig,
  http: HttpClientV2,
  auditService: AuditService,
  uploadService: UploadService,
  taxYearService: TaxYearService
)(implicit
  ec: ExecutionContext
) extends Logging {

  private val baseUrl = s"${appConfig.pensionSchemeReturn.baseUrl}/pension-scheme-return-sipp/psr"

  def createEmptyPsr(
    reportDetails: ReportDetails
  )(implicit hc: HeaderCarrier): Future[SippPsrJourneySubmissionEtmpResponse] =
    http
      .post(url"$baseUrl/empty/sipp")
      .setHeader(headers*)
      .withBody(Json.toJson(reportDetails))
      .execute[HttpResponse]
      .flatMap {
        case response if response.status == Status.CREATED =>
          Future.successful(response.json.as[SippPsrJourneySubmissionEtmpResponse])
        case response =>
          Future.failed(Exception(s"Create empty PSR failed with status ${response.status}"))
      }
      .recoverWith(handleError)

  def updateMemberTransactions(
    memberTransactionsHeld: Boolean
  )(implicit hc: HeaderCarrier, req: DataRequest[?]): Future[SippPsrJourneySubmissionEtmpResponse] = {
    val pstr = req.schemeDetails.pstr
    val queryParams = createQueryParamsFromSession(req.session)
    val url = makeUrl(
      s"$baseUrl/report-details/member-transactions/$pstr?journeyType=${JourneyType.Standard}",
      queryParams,
      isFirstQueryParam = false
    )

    val request = MemberTransactions(YesNo(memberTransactionsHeld))
    submitRequest(request, url, auditParameters = None)
  }

  def updateAccountingPeriodsDetails(
    accountingPeriodDetails: AccountingPeriodDetails
  )(implicit hc: HeaderCarrier, req: DataRequest[?]): Future[SippPsrJourneySubmissionEtmpResponse] = {
    val pstr = req.schemeDetails.pstr
    val queryParams = createQueryParamsFromSession(req.session)
    val url = makeUrl(
      s"$baseUrl/accounting-periods/$pstr?journeyType=${JourneyType.Standard}",
      queryParams,
      isFirstQueryParam = false
    )

    submitRequest(accountingPeriodDetails, url, auditParameters = None)
  }

  def submitLandArmsLength(
    request: LandOrConnectedPropertyRequest,
    journeyType: JourneyType,
    journey: Journey,
    srn: Srn
  )(implicit hc: HeaderCarrier, req: DataRequest[?]): Future[SippPsrJourneySubmissionEtmpResponse] = {
    val queryParams = createQueryParamsFromSession(req.session)
    val url = makeUrl(s"$baseUrl/land-arms-length?journeyType=$journeyType", queryParams, isFirstQueryParam = false)
    submitRequest(request, url, Some(AuditParameters(journey, srn, req)))
  }

  def getLandArmsLength(
    pstr: String,
    optFbNumber: Option[String],
    optPeriodStartDate: Option[String],
    optPsrVersion: Option[String]
  )(implicit hc: HeaderCarrier): Future[LandOrConnectedPropertyResponse] = {
    val queryParams = createQueryParams(optFbNumber, optPeriodStartDate, optPsrVersion)
    val url = makeUrl(s"$baseUrl/land-arms-length/$pstr", queryParams)
    http
      .get(url)
      .setHeader(headers*)
      .execute[LandOrConnectedPropertyResponse]
      .recoverWith(handleError)
  }

  def submitLandOrConnectedProperty(
    request: LandOrConnectedPropertyRequest,
    journeyType: JourneyType,
    journey: Journey,
    srn: Srn
  )(implicit hc: HeaderCarrier, req: DataRequest[?]): Future[SippPsrJourneySubmissionEtmpResponse] = {
    val queryParams = createQueryParamsFromSession(req.session)
    val url = makeUrl(
      s"$baseUrl/land-or-connected-property?journeyType=$journeyType",
      queryParams,
      isFirstQueryParam = false
    )
    submitRequest(request, url, Some(AuditParameters(journey, srn, req)))
  }

  def getLandOrConnectedProperty(
    pstr: String,
    optFbNumber: Option[String],
    optPeriodStartDate: Option[String],
    optPsrVersion: Option[String]
  )(implicit hc: HeaderCarrier): Future[LandOrConnectedPropertyResponse] = {
    val queryParams = createQueryParams(optFbNumber, optPeriodStartDate, optPsrVersion)
    val url = makeUrl(s"$baseUrl/land-or-connected-property/$pstr", queryParams)
    http
      .get(url)
      .setHeader(headers*)
      .execute[LandOrConnectedPropertyResponse]
      .map(updateCountryFromCountryCode)
      .recoverWith(handleError)
  }

  private def updateCountryFromCountryCode(response: LandOrConnectedPropertyResponse) = {
    val updatedTransactions = response.transactions.map { transaction =>
      if (transaction.landOrPropertyInUK == YesNo.No) {
        transaction.copy(
          addressDetails = transaction.addressDetails.copy(
            countryCode = Country
              .getCountry(transaction.addressDetails.countryCode)
              .getOrElse(transaction.addressDetails.countryCode)
          )
        )
      } else {
        transaction
      }
    }

    response.copy(transactions = updatedTransactions)
  }

  def submitOutstandingLoans(
    request: OutstandingLoanRequest,
    journeyType: JourneyType,
    journey: Journey,
    srn: Srn
  )(implicit hc: HeaderCarrier, req: DataRequest[?]): Future[SippPsrJourneySubmissionEtmpResponse] = {
    val queryParams = createQueryParamsFromSession(req.session)
    val url = makeUrl(s"$baseUrl/outstanding-loans?journeyType=$journeyType", queryParams, isFirstQueryParam = false)
    submitRequest(request, url, Some(AuditParameters(journey, srn, req)))
  }

  def getOutstandingLoans(
    pstr: String,
    optFbNumber: Option[String],
    optPeriodStartDate: Option[String],
    optPsrVersion: Option[String]
  )(implicit hc: HeaderCarrier): Future[OutstandingLoanResponse] = {
    val queryParams = createQueryParams(optFbNumber, optPeriodStartDate, optPsrVersion)
    val url = makeUrl(s"$baseUrl/outstanding-loans/$pstr", queryParams)
    http
      .get(url)
      .setHeader(headers*)
      .execute[OutstandingLoanResponse]
      .recoverWith(handleError)
  }

  def submitAssetsFromConnectedParty(
    request: AssetsFromConnectedPartyRequest,
    journeyType: JourneyType,
    journey: Journey,
    srn: Srn
  )(implicit hc: HeaderCarrier, req: DataRequest[?]): Future[SippPsrJourneySubmissionEtmpResponse] = {
    val queryParams = createQueryParamsFromSession(req.session)
    val url = makeUrl(
      s"$baseUrl/assets-from-connected-party?journeyType=$journeyType",
      queryParams,
      isFirstQueryParam = false
    )
    submitRequest(request, url, Some(AuditParameters(journey, srn, req)))
  }

  def getAssetsFromConnectedParty(
    pstr: String,
    optFbNumber: Option[String],
    optPeriodStartDate: Option[String],
    optPsrVersion: Option[String]
  )(implicit hc: HeaderCarrier): Future[AssetsFromConnectedPartyResponse] = {
    val queryParams = createQueryParams(optFbNumber, optPeriodStartDate, optPsrVersion)
    val url = makeUrl(s"$baseUrl/assets-from-connected-party/$pstr", queryParams)
    http
      .get(url)
      .setHeader(headers*)
      .execute[AssetsFromConnectedPartyResponse]
      .recoverWith(handleError)
  }

  def submitTangibleMoveableProperty(
    request: TangibleMoveablePropertyRequest,
    journeyType: JourneyType,
    journey: Journey,
    srn: Srn
  )(implicit hc: HeaderCarrier, req: DataRequest[?]): Future[SippPsrJourneySubmissionEtmpResponse] = {
    val queryParams = createQueryParamsFromSession(req.session)
    val url = makeUrl(
      s"$baseUrl/tangible-moveable-property?journeyType=$journeyType",
      queryParams,
      isFirstQueryParam = false
    )
    submitRequest(request, url, Some(AuditParameters(journey, srn, req)))
  }

  def getTangibleMoveableProperty(
    pstr: String,
    optFbNumber: Option[String],
    optPeriodStartDate: Option[String],
    optPsrVersion: Option[String]
  )(implicit hc: HeaderCarrier): Future[TangibleMoveablePropertyResponse] = {
    val queryParams = createQueryParams(optFbNumber, optPeriodStartDate, optPsrVersion)
    val url = makeUrl(s"$baseUrl/tangible-moveable-property/$pstr", queryParams)
    http
      .get(url)
      .setHeader(headers*)
      .execute[TangibleMoveablePropertyResponse]
      .recoverWith(handleError)
  }

  def submitUnquotedShares(
    request: UnquotedShareRequest,
    journeyType: JourneyType,
    journey: Journey,
    srn: Srn
  )(implicit hc: HeaderCarrier, req: DataRequest[?]): Future[SippPsrJourneySubmissionEtmpResponse] = {
    val queryParams = createQueryParamsFromSession(req.session)
    val url = makeUrl(s"$baseUrl/unquoted-shares?journeyType=$journeyType", queryParams, isFirstQueryParam = false)
    submitRequest(request, url, Some(AuditParameters(journey, srn, req)))
  }

  def getUnquotedShares(
    pstr: String,
    optFbNumber: Option[String],
    optPeriodStartDate: Option[String],
    optPsrVersion: Option[String]
  )(implicit hc: HeaderCarrier): Future[UnquotedShareResponse] = {
    val queryParams = createQueryParams(optFbNumber, optPeriodStartDate, optPsrVersion)
    val url = makeUrl(s"$baseUrl/unquoted-shares/$pstr", queryParams)
    http
      .get(url)
      .setHeader(headers*)
      .execute[UnquotedShareResponse]
      .recoverWith(handleError)
  }

  def getPSRSubmission(
    pstr: String,
    optFbNumber: Option[String],
    optPeriodStartDate: Option[String],
    optPsrVersion: Option[String]
  )(implicit hc: HeaderCarrier): Future[PSRSubmissionResponse] = {
    val queryParams = createQueryParams(optFbNumber, optPeriodStartDate, optPsrVersion)
    val url = makeUrl(s"$baseUrl/sipp/$pstr", queryParams)
    http
      .get(url)
      .execute[PSRSubmissionResponse]
      .recoverWith(handleError)
  }

  def getMemberDetails(
    pstr: String,
    optFbNumber: Option[String],
    optPeriodStartDate: Option[String],
    optPsrVersion: Option[String]
  )(implicit headerCarrier: HeaderCarrier): Future[MemberDetailsResponse] = {
    val queryParams = createQueryParams(optFbNumber, optPeriodStartDate, optPsrVersion)
    val url = makeUrl(s"$baseUrl/member-details/$pstr", queryParams)
    http
      .get(url)
      .execute[MemberDetailsResponse]
      .recoverWith(handleError)
  }

  def deleteMember(
    pstr: String,
    journeyType: JourneyType,
    optFbNumber: Option[String],
    optPeriodStartDate: Option[String],
    optPsrVersion: Option[String],
    memberDetails: MemberDetails
  )(implicit headerCarrier: HeaderCarrier): Future[SippPsrJourneySubmissionEtmpResponse] = {
    val queryParams = createQueryParams(optFbNumber, optPeriodStartDate, optPsrVersion)
    val url =
      makeUrl(s"$baseUrl/delete-member/$pstr?journeyType=$journeyType", queryParams, isFirstQueryParam = false)
    submitRequest(memberDetails, url, None)
  }

  def deleteAssets(
    pstr: String,
    journey: Journey,
    journeyType: JourneyType,
    optFbNumber: Option[String],
    optPeriodStartDate: Option[String],
    optPsrVersion: Option[String]
  )(implicit headerCarrier: HeaderCarrier): Future[SippPsrJourneySubmissionEtmpResponse] = {
    val queryParams = createQueryParams(optFbNumber, optPeriodStartDate, optPsrVersion)
    val url = makeUrl(
      s"$baseUrl/delete-assets/$pstr?journey=$journey&journeyType=$journeyType",
      queryParams,
      isFirstQueryParam = false
    )
    submitRequest(None, url, None)
  }

  def submitPsr(
    pstr: String,
    journeyType: JourneyType,
    fbNumber: Option[String],
    periodStartDate: Option[String],
    psrVersion: Option[String],
    taxYear: DateRange,
    schemeName: Option[String],
    psaId: String
  )(implicit hc: HeaderCarrier): Future[PsrSubmittedResponse] = {

    val request = PsrSubmissionRequest(
      pstr,
      fbNumber,
      periodStartDate,
      psrVersion,
      psaId,
      taxYear,
      schemeName
    )

    http
      .post(url"$baseUrl/sipp?journeyType=$journeyType")
      .setHeader(headers*)
      .withBody(Json.toJson(request))
      .execute[PsrSubmittedResponse]
      .recoverWith(handleError)
  }

  def getPsrVersions(pstr: String, startDate: LocalDate)(implicit hc: HeaderCarrier): Future[Seq[PsrVersionsResponse]] =
    http
      .get(makeUrl(s"$baseUrl/versions/$pstr", Seq("startDate" -> startDate.format(DateTimeFormatter.ISO_DATE))))
      .execute[Seq[PsrVersionsResponse]]
      .recoverWith(handleError)

  private def headers: Seq[(String, String)] = Seq(
    "CorrelationId" -> UUID.randomUUID().toString
  )

  private def createQueryParams(
    optFbNumber: Option[String],
    optPeriodStartDate: Option[String],
    optPsrVersion: Option[String]
  ): Seq[(String, String)] =
    (optPeriodStartDate, optPsrVersion, optFbNumber) match {
      case (_, _, Some(fbNumber)) =>
        Seq("fbNumber" -> fbNumber)
      case (Some(startDate), Some(version), _) =>
        Seq("periodStartDate" -> startDate, "psrVersion" -> version)
      case _ =>
        throw EtmpServerError(
          "Invalid query parameters: must provide 'fbNumber' or both 'periodStartDate' and 'psrVersion'."
        )
    }

  private def createQueryParamsFromSession(session: Session): Seq[(String, String)] = {
    val optFbNumber = FormBundleNumber.optFromSession(session)
    optFbNumber match {
      case Some(fbNumber) =>
        Seq("fbNumber" -> fbNumber.value)
      case _ =>
        val optVersionTaxYear = VersionTaxYear.optFromSession(session)
        optVersionTaxYear match {
          case Some(versionTaxYear) =>
            Seq("periodStartDate" -> versionTaxYear.taxYear, "psrVersion" -> versionTaxYear.version)
          case _ =>
            throw EtmpServerError(
              "Invalid query parameters: must provide 'fbNumber' or both 'periodStartDate' and 'psrVersion'."
            )
        }
    }
  }

  private case class AuditParameters(journey: Journey, srn: Srn, dr: DataRequest[?])

  private def submitRequest[T](request: T, url: URL, auditParameters: Option[AuditParameters])(implicit
    hc: HeaderCarrier,
    w: Writes[T]
  ): Future[SippPsrJourneySubmissionEtmpResponse] = {
    def executeRequest(body: JsValue): Future[SippPsrJourneySubmissionEtmpResponse] =
      http
        .put(url)
        .setHeader(headers*)
        .withBody(body)
        .execute[HttpResponse]
        .flatMap {
          case response if response.status == Status.CREATED || response.status == Status.OK =>
            Future.successful(response.json.as[SippPsrJourneySubmissionEtmpResponse])
          case response =>
            Future.failed(UpstreamErrorResponse(response.body, response.status))
        }
        .recoverWith(handleError)

    val mAuditContext = auditParameters
      .flatTraverse(param => getFileUploadEvent(param.srn, param.journey)(param.dr))

    val jsonRequest = Json.toJson(request)
    val jsonSizeInBytes = jsonRequest.toString.getBytes("UTF-8").length

    if (jsonSizeInBytes > appConfig.maxRequestSize) {
      val errorMessage = s"Request body size exceeds maximum limit of ${appConfig.maxRequestSize} bytes"
      logger.error(errorMessage)

      auditParameters
        .flatTraverse(param =>
          implicit val dr: DataRequest[?] = param.dr
          mAuditContext.flatMap(_.traverse(auditService.sendEvent))
        )
        .flatMap(_ => Future.failed(EtmpRequestDataSizeExceedError(errorMessage)))
    } else {
      mAuditContext
        .map(_.map(FileUploadAuditEvent.getAuditContext))
        .flatMap(aC => executeRequest(Json.toJson(AuditedPutRequest[T](request, aC))))
    }
  }

  private def getFileUploadEvent(srn: Srn, journey: Journey)(implicit
    request: DataRequest[?]
  ): Future[Option[FileUploadAuditEvent]] =
    uploadService.getUploadStatus(UploadKey.fromRequest(srn, journey.uploadRedirectTag)).map {
      case Some(upload: UploadStatus.Success) =>
        Some(
          FileUploadAuditEvent.buildAuditEvent(
            fileUploadType = journey.entryName,
            fileUploadStatus = FileUploadAuditEvent.ERROR,
            typeOfError = FileUploadAuditEvent.ERROR_SIZE_LIMIT,
            fileName = upload.name,
            fileReference = upload.downloadUrl,
            fileSize = upload.size.getOrElse(0),
            validationCompleted = LocalDate.now(),
            taxYear = taxYearService.fromRequest()
          )
        )
      case _ =>
        logger.info("FileUploadAuditEvent not created: no valid file upload")
        None

    }

  def updateMemberDetails(
    pstr: String,
    journeyType: JourneyType,
    optFbNumber: String,
    optPeriodStartDate: Option[String],
    optPsrVersion: Option[String],
    request: UpdateMemberDetailsRequest
  )(implicit headerCarrier: HeaderCarrier): Future[SippPsrJourneySubmissionEtmpResponse] = {
    val queryParams = createQueryParams(optFbNumber.some, optPeriodStartDate, optPsrVersion)
    val fullUrl =
      makeUrl(s"$baseUrl/member-details/$pstr?journeyType=$journeyType", queryParams, isFirstQueryParam = false)
    submitRequest(request, fullUrl, None)
  }

  def getPsrAssetCounts(
    pstr: String,
    optFbNumber: Option[String],
    optPeriodStartDate: Option[String],
    optPsrVersion: Option[String]
  )(implicit headerCarrier: HeaderCarrier): Future[Option[PsrAssetCountsResponse]] = {
    val queryParams = createQueryParams(optFbNumber, optPeriodStartDate, optPsrVersion)
    implicit val formatter: OFormat[OptionalResponse[PsrAssetCountsResponse]] =
      OptionalResponse.formatter()(using PsrAssetCountsResponse.formatPSRSubmissionResponse)

    http
      .get(makeUrl(s"$baseUrl/asset-counts/$pstr", queryParams))
      .execute[OptionalResponse[PsrAssetCountsResponse]]
      .map(_.response)
      .recoverWith(handleError)
  }

  def getPsrAssetDeclarations(
    pstr: String,
    optFbNumber: Option[String],
    optPeriodStartDate: Option[String],
    optPsrVersion: Option[String]
  )(implicit headerCarrier: HeaderCarrier): Future[PsrAssetDeclarationsResponse] = {
    val queryParams = createQueryParams(optFbNumber, optPeriodStartDate, optPsrVersion)

    http
      .get(makeUrl(s"$baseUrl/asset-declarations/$pstr", queryParams))
      .execute[PsrAssetDeclarationsResponse]
      .map(_.response)
      .recoverWith(handleError)
  }

  private def handleError: PartialFunction[Throwable, Future[Nothing]] = {
    case UpstreamErrorResponse(message, NOT_FOUND, _, _) =>
      logger.error(s"PSR backend call failed with code 404 and message $message")
      Future.failed(NotFoundException(message))
    case UpstreamErrorResponse(message, REQUEST_ENTITY_TOO_LARGE, _, _) =>
      logger.error(s"PSR backend call failed with code 413 and message $message")
      Future.failed(EtmpRequestDataSizeExceedError(message))
    case UpstreamErrorResponse(message, statusCode, _, _) =>
      logger.error(s"PSR backend call failed with code $statusCode and message $message")
      Future.failed(EtmpServerError(message))
    case e: Exception =>
      logger.error(s"PSR backend call failed with exception $e")
      Future.failed(InternalServerException(e.getMessage))
  }
}
