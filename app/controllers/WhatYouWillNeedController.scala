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

package controllers

import controllers.actions._
import play.api.i18n._
import play.api.mvc._
import navigation.Navigator
import models.NormalMode
import uk.gov.hmrc.play.bootstrap.frontend.controller.FrontendBaseController
import viewmodels.models.{ContentPageViewModel, FormPageViewModel}
import viewmodels.implicits._
import viewmodels.DisplayMessage._
import views.html.ContentPageView
import WhatYouWillNeedController._
import pages.WhatYouWillNeedPage
import models.SchemeId.Srn

import javax.inject.{Inject, Named}

class WhatYouWillNeedController @Inject()(
  override val messagesApi: MessagesApi,
  @Named("root") navigator: Navigator,
  identify: IdentifierAction,
  allowAccess: AllowAccessActionProvider,
  getData: DataRetrievalAction,
  createData: DataCreationAction,
  val controllerComponents: MessagesControllerComponents,
  view: ContentPageView
) extends FrontendBaseController
    with I18nSupport {

  def onPageLoad(srn: Srn): Action[AnyContent] = identify.andThen(allowAccess(srn)) { implicit request =>
    Ok(view(viewModel(srn)))
  }

  def onSubmit(srn: Srn): Action[AnyContent] = identify.andThen(allowAccess(srn)).andThen(getData).andThen(createData) {
    implicit request =>
      Redirect(navigator.nextPage(WhatYouWillNeedPage(srn), NormalMode, request.userAnswers))
  }
}

object WhatYouWillNeedController {
  def viewModel(srn: Srn): FormPageViewModel[ContentPageViewModel] =
    FormPageViewModel(
      Message("whatYouWillNeed.title"),
      Message("whatYouWillNeed.heading"),
      ContentPageViewModel(isStartButton = true, isLargeHeading = false),
      routes.WhatYouWillNeedController.onSubmit(srn)
    ).withButtonText(Message("site.continue"))
      .withDescription(
        ParagraphMessage("whatYouWillNeed.paragraph1") ++
          ListMessage(
            ListType.Bullet,
            "whatYouWillNeed.listItem1",
            "whatYouWillNeed.listItem2",
            "whatYouWillNeed.listItem3"
          ) ++
          ParagraphMessage("whatYouWillNeed.paragraph2") ++
          Heading2("whatYouWillNeed.heading2") ++
          ParagraphMessage("whatYouWillNeed.paragraph3")
      )
}
