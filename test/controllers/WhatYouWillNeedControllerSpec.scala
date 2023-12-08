package controllers

import models.NormalMode
import views.html.ContentPageView
import WhatYouWillNeedController._

class WhatYouWillNeedControllerSpec extends ControllerBaseSpec {

  private lazy val onPageLoad = routes.WhatYouWillNeedController.onPageLoad(srn, NormalMode)
  private lazy val onSubmit = routes.WhatYouWillNeedController.onSubmit(srn, NormalMode)

  "WhatYouWillNeedController" - {

    act like renderView(onPageLoad) { implicit app => implicit request =>
      injected[ContentPageView].apply(viewModel(srn, NormalMode))
    }

    act like redirectNextPage(onSubmit)

    act.like(journeyRecoveryPage(onPageLoad).updateName("onPageLoad" + _))

    act.like(journeyRecoveryPage(onSubmit).updateName("onSubmit" + _))
  }
}
