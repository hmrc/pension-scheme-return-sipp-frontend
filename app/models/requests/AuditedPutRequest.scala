/*
 * Copyright 2025 HM Revenue & Customs
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

package models.requests

import models.audit.FileUploadAuditEvent.FileUploadAuditContext
import play.api.libs.json.{JsObject, Json, Writes}

case class AuditedPutRequest[T](request: T, auditContext: Option[FileUploadAuditContext])

object AuditedPutRequest {

  implicit def writesRequestWithAuditContext[T](implicit tWrites: Writes[T]): Writes[AuditedPutRequest[T]] =
    (req: AuditedPutRequest[T]) => {
      val jO = Json.toJson(req.request)

      req.auditContext match {
        case Some(audit) => jO.as[JsObject] + ("auditContext" -> Json.toJson(audit))
        case None => jO
      }
    }
}
