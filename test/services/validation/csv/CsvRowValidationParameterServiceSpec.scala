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

package services.validation.csv

import models.PensionSchemeId
import models.PensionSchemeId.PsaId
import services.SchemeDetailsService
import uk.gov.hmrc.http.HeaderCarrier
import utils.BaseSpec

import concurrent.ExecutionContext.Implicits.global
import scala.concurrent.Future

class CsvRowValidationParameterServiceSpec extends BaseSpec {
  implicit val hc: HeaderCarrier = HeaderCarrier()

  "CsvRowValidationParameterService" - {
    "return validation parameters" in {
      val schemeDetailsService: SchemeDetailsService = mock[SchemeDetailsService]
      val service = CsvRowValidationParameterService(schemeDetailsService)
      val psId = PsaId("id")
      val srn = srnGen.sample.value
      val minimalSchemeDetails = minimalSchemeDetailsGen.sample.value
      when(schemeDetailsService.getMinimalSchemeDetails(psId, srn))
        .thenReturn(Future.successful(Some(minimalSchemeDetails)))
      whenReady(service.csvRowValidationParameters(psId, srn)) { result =>
        result mustBe CsvRowValidationParameters(minimalSchemeDetails.windUpDate)
      }
    }
  }
}
