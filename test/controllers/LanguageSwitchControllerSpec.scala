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

package controllers

import config.FrontendAppConfig
import org.scalatestplus.mockito.MockitoSugar
import play.api.i18n.Lang
import play.api.test.FakeRequest
import play.api.test.Helpers.*
import uk.gov.hmrc.play.language.LanguageUtils

class LanguageSwitchControllerSpec extends ControllerBaseSpec with MockitoSugar {

  private val mockAppConfig = mock[FrontendAppConfig]
  private val mockLanguageUtils = mock[LanguageUtils]
  private val mockControllerComponents = stubControllerComponents()

  private val controller = LanguageSwitchController(
    mockAppConfig,
    mockLanguageUtils,
    mockControllerComponents
  )

  "redirect to fallback URL when language is not supported" in {
    when(mockAppConfig.languageMap).thenReturn(Map("en" -> Lang("en"), "cy" -> Lang("cy")))
    when(mockLanguageUtils.getCurrentLang(any)).thenReturn(Lang("en"))

    val result = controller.switchToLanguage("fr")(FakeRequest())

    status(result) mustEqual SEE_OTHER
    redirectLocation(result).value mustEqual controller.fallbackURL
  }

  "redirect to the same URL with language parameter when language is supported" in {
    when(mockAppConfig.languageMap).thenReturn(Map("en" -> Lang("en"), "cy" -> Lang("cy")))
    when(mockLanguageUtils.getCurrentLang(any)).thenReturn(Lang("cy"))

    val result = controller.switchToLanguage("cy")(FakeRequest().withHeaders("Referer" -> "/foo"))

    status(result) mustEqual SEE_OTHER
    redirectLocation(result).value mustEqual "/foo"
  }

  "redirect to fallback URL when referer header is missing" in {
    when(mockAppConfig.languageMap).thenReturn(Map("en" -> Lang("en"), "cy" -> Lang("cy")))
    when(mockLanguageUtils.getCurrentLang(any)).thenReturn(Lang("en"))

    val result = controller.switchToLanguage("cy")(FakeRequest())

    status(result) mustEqual SEE_OTHER
    redirectLocation(result).value mustEqual controller.fallbackURL
  }
}
