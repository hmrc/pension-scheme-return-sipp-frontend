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

package views

import forms.mappings.Mappings
import play.api.test.FakeRequest
import viewmodels.models.UploadViewModel
import views.html.UploadView

class UploadViewSpec extends ViewSpec with Mappings {

  runningApplication { implicit app =>
    val view = injected[UploadView]

    implicit val request = FakeRequest()

    val viewModelGen = formPageViewModelGen[UploadViewModel]

    val successViewModelGen = viewModelGen.retryUntil(_.page.error.isEmpty)
    val errorViewModelGen = viewModelGen.retryUntil(_.page.error.nonEmpty)

    "UploadView" - {
      act.like(renderTitle(successViewModelGen)(view(_), _.title.key))
      act.like(renderHeading(successViewModelGen)(view(_), _.heading))
      act.like(renderForm(successViewModelGen)(view(_), _.onSubmit))
      act.like(renderUpload(successViewModelGen)(view(_)))
      act.like(renderErrors(errorViewModelGen)(view(_), _.page.error.value.message))
      act.like(renderButtonText(successViewModelGen)(view(_), _.buttonText))
    }
  }
}
