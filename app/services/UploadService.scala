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

import org.apache.pekko.stream.scaladsl.Source
import org.apache.pekko.util.ByteString
import connectors.UpscanConnector
import models.UploadStatus.UploadStatus
import models._
import repositories.{UploadMetadataRepository, UploadRepository}
import uk.gov.hmrc.http.HeaderCarrier

import java.time.{Clock, Instant}
import javax.inject.Inject
import scala.concurrent.{ExecutionContext, Future}

class UploadService @Inject()(
  upscanConnector: UpscanConnector,
  metadataRepository: UploadMetadataRepository,
  uploadRepository: UploadRepository,
  clock: Clock
)(implicit ec: ExecutionContext) {

  def initiateUpscan(callBackUrl: String, successRedirectUrl: String, failureRedirectUrl: String)(
    implicit hc: HeaderCarrier
  ): Future[UpscanInitiateResponse] =
    upscanConnector.initiate(callBackUrl, successRedirectUrl, failureRedirectUrl)

  def registerUploadRequest(key: UploadKey, fileReference: Reference): Future[Unit] =
    metadataRepository.upsert(UploadDetails(key, fileReference, UploadStatus.InProgress, Instant.now(clock)))

  def registerUploadResult(reference: Reference, uploadStatus: UploadStatus): Future[Unit] =
    metadataRepository.updateStatus(reference, uploadStatus)

  def getUploadStatus(key: UploadKey): Future[Option[UploadStatus]] =
    metadataRepository.getUploadDetails(key).map(_.map(_.status))

  def downloadFromUpscan(downloadUrl: String)(implicit hc: HeaderCarrier): Future[(Int, Source[ByteString, _])] =
    upscanConnector.download(downloadUrl).map(result => (result.status, result.bodyAsSource))

  def getUploadValidationState(key: UploadKey): Future[Option[UploadState]] = metadataRepository.getValidationState(key)

  def setUploadValidationState(key: UploadKey, state: UploadState): Future[Unit] =
    metadataRepository.setValidationState(key, state)
}
