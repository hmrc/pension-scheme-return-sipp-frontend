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

package connectors

import com.github.tomakehurst.wiremock.client.ResponseDefinitionBuilder
import com.github.tomakehurst.wiremock.client.WireMock.{delete, get, post, put, urlEqualTo}
import com.github.tomakehurst.wiremock.stubbing.StubMapping
import uk.gov.hmrc.http.test.WireMockSupport
import utils.BaseSpec

abstract class BaseConnectorSpec extends BaseSpec with WireMockSupport {

  def stubGet(url: String, response: ResponseDefinitionBuilder): StubMapping =
    wireMockServer.stubFor(
      get(urlEqualTo(url))
        .willReturn(response)
    )

  def stubDelete(url: String, response: ResponseDefinitionBuilder): StubMapping =
    wireMockServer.stubFor(
      delete(urlEqualTo(url))
        .willReturn(response)
    )

  def stubPut(url: String, response: ResponseDefinitionBuilder): StubMapping =
    wireMockServer.stubFor(
      put(urlEqualTo(url))
        .willReturn(response)
    )

  def stubPost(url: String, response: ResponseDefinitionBuilder): StubMapping =
    wireMockServer.stubFor(
      post(urlEqualTo(url))
        .willReturn(response)
    )
}
