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

$! Generic !$
$if(directory.empty)$
package controllers
$else$
package controllers.$directory$
$endif$

$if(directory.empty)$
import pages.$className$Page
import controllers.$className;format="cap"$Controller.*
$else$
import pages.$directory$.$className$CompletedPage
import controllers.$directory$.$className;format="cap"$Controller.*
$endif$

$if(!index.empty)$
import config.RefinedTypes.*
$endif$

import models.NormalMode
import controllers.ControllerBaseSpec
$! Generic end !$

import views.html.CheckYourAnswersView

class $className;format="cap"$ControllerSpec extends ControllerBaseSpec {

  $! Generic !$
  $if(index.empty)$
  private lazy val onPageLoad = routes.$className;format="cap"$Controller.onPageLoad(srn, NormalMode)
  private lazy val onSubmit = routes.$className;format="cap"$Controller.onSubmit(srn, NormalMode)
  $else$
  private val index = refineUnsafe[Int, $index$.Refined](1)
  $if(secondaryIndex.empty)$
  private lazy val onPageLoad = routes.$className;format="cap"$Controller.onPageLoad(srn, index, NormalMode)
  private lazy val onSubmit = routes.$className;format="cap"$Controller.onSubmit(srn, index, NormalMode)
  $else$
  private val secondaryIndex = refineUnsafe[Int, $secondaryIndex$.Refined](1)
  private lazy val onPageLoad = routes.$className;format="cap"$Controller.onPageLoad(srn, index, secondaryIndex, NormalMode)
  private lazy val onSubmit = routes.$className;format="cap"$Controller.onSubmit(srn, index, secondaryIndex, NormalMode)
  $endif$
  $endif$

  private val userAnswers = defaultUserAnswers.unsafeSet(requiredPage(srn, index), ???)
  $! Generic end !$

  "$className;format="cap"$Controller" - {

    act like renderView(onPageLoad) { implicit app => implicit request =>
      injected[CheckYourAnswersView].apply(viewModel(srn$if(!index.empty)$, index: $index$$endif$$if(!secondaryIndex.empty)$, secondaryIndex: $secondaryIndex$$endif$, NormalMode))
    }

    act like redirectNextPage(onSubmit)

    act.like(journeyRecoveryPage(onPageLoad).updateName("onPageLoad" + _))

    act.like(journeyRecoveryPage(onSubmit).updateName("onSubmit" + _))
  }
}
