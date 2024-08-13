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

import forms.DatePageFormProvider
import models.NormalMode
import play.api.data.FormError
import play.api.i18n.Messages
import play.api.test.FakeRequest
import play.api.test.Helpers.stubMessagesApi
import views.html.DOBView

import java.time.LocalDate

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

class ChangeMemberDOBControllerTest extends ControllerBaseSpec {

  private lazy val onPageLoad = routes.ChangeMemberDOBController.onPageLoad(srn)
  private lazy val onSubmit = routes.ChangeMemberDOBController.onSubmit(srn)

  private val validForm = List(
    "value.day" -> "12",
    "value.month" -> "12",
    "value.year" -> "2020"
  )

  private val dobInFutureForm = List(
    "value.day" -> "12",
    "value.month" -> "12",
    "value.year" -> (LocalDate.now().getYear + 1).toString
  )

  private val dobTooEarlyForm = List(
    "value.day" -> "31",
    "value.month" -> "12",
    "value.year" -> "1899"
  )

  "ChangeMemberDOBController" - {

    act.like(renderView(onPageLoad) { implicit app => implicit request =>
      injected[DOBView]
        .apply(
          ChangeMemberDOBController.form(injected[DatePageFormProvider], None),
          ChangeMemberDOBController.viewModel(srn, NormalMode)
        )
    })

    act.like(journeyRecoveryPage(onPageLoad).updateName("onPageLoad " + _))

    act.like(saveAndContinue(onSubmit, validForm: _*))
    act.like(invalidForm(onSubmit))
    act.like(invalidForm(onSubmit, dobInFutureForm: _*))
    act.like(invalidForm(onSubmit, dobTooEarlyForm: _*))
    act.like(journeyRecoveryPage(onSubmit).updateName("onSubmit" + _))
  }

  "must bind validation errors when earlier than earliest date is submitted" in {

    val application = applicationBuilder(userAnswers = Some(emptyUserAnswers)).build()
    implicit val stubMessages: Messages = stubMessagesApi().preferred(FakeRequest())

    running(application) {
      val formProvider = application.injector.instanceOf[DatePageFormProvider]
      val testForm = ChangeMemberDOBController.form(formProvider, Some(LocalDate.now()))
      val boundForm = testForm.bind(
        Map(
          "value.day" -> tooEarlyDate.getDayOfMonth.toString,
          "value.month" -> tooEarlyDate.getMonthValue.toString,
          "value.year" -> tooEarlyDate.getYear.toString
        )
      )
      boundForm.errors must contain(FormError("value", "memberDetails.dateOfBirth.upload.error.after"))
    }
  }
}
