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

import controllers.actions._
import models.Journey.{ArmsLengthLandOrProperty, AssetFromConnectedParty, InterestInLandOrProperty, OutstandingLoans, TangibleMoveableProperty, UnquotedShares}
import models.SchemeId.Srn
import models.{Journey, UploadKey}
import navigation.Navigator
import play.api.i18n._
import play.api.mvc.{Action, AnyContent, MessagesControllerComponents}
import services.PendingFileActionService
import services.validation.ValidateUploadService
import uk.gov.hmrc.play.bootstrap.frontend.controller.FrontendBaseController
import views.html.ETMPErrorReceivedView

import javax.inject.{Inject, Named}
import scala.concurrent.ExecutionContext

class ETMPErrorReceivedController @Inject()(
                                             override val messagesApi: MessagesApi,
                                             val controllerComponents: MessagesControllerComponents,
                                             @Named("sipp") navigator: Navigator,
                                             view: ETMPErrorReceivedView,
                                             allowAccess: AllowAccessActionProvider,
                                             getData: DataRetrievalAction,
                                             requireData: DataRequiredAction,
                                             identify: IdentifierAction,
                                             validateUploadService: ValidateUploadService
                                           )(implicit ec: ExecutionContext) extends FrontendBaseController with I18nSupport {

//  def onPageLoad(srn: Srn): Action[AnyContent] = identify.andThen(allowAccess(srn)).andThen(getData).andThen(requireData).async { implicit request =>
//    val journey = ArmsLengthLandOrProperty
//    val key = UploadKey.fromRequest(srn, journey.uploadRedirectTag)
//    validateUploadService.validateUpload(key, request.pensionSchemeId, srn, journey).map {
//      case PendingFileActionService.Complete(_) => Ok("test")
//      case _ => Ok(view())
//    }
//  }

  def onPageLoad(srn: Srn): Action[AnyContent] = identify.andThen(allowAccess(srn)).andThen(getData).andThen(requireData).async { implicit request =>
    def key(srn: Srn, journey: Journey) = UploadKey.fromRequest(srn, journey.uploadRedirectTag)
    for {
      interestInLandOrProperty <- validateUploadService.validateUpload(key(srn, InterestInLandOrProperty), request.pensionSchemeId, srn, InterestInLandOrProperty)
      armsLengthLandOrProperty <- validateUploadService.validateUpload(key(srn, ArmsLengthLandOrProperty), request.pensionSchemeId, srn, ArmsLengthLandOrProperty)
      tangibleMoveableProperty <- validateUploadService.validateUpload(key(srn, TangibleMoveableProperty), request.pensionSchemeId, srn, TangibleMoveableProperty)
      outstandingLoans <- validateUploadService.validateUpload(key(srn, OutstandingLoans), request.pensionSchemeId, srn, OutstandingLoans)
      unquotedShares <- validateUploadService.validateUpload(key(srn, UnquotedShares), request.pensionSchemeId, srn, UnquotedShares)
      assetFromConnectedParty <- validateUploadService.validateUpload(key(srn, AssetFromConnectedParty), request.pensionSchemeId, srn, AssetFromConnectedParty)
    } yield (interestInLandOrProperty, armsLengthLandOrProperty, tangibleMoveableProperty, outstandingLoans, unquotedShares, assetFromConnectedParty) match {
      case (PendingFileActionService.Complete(_), PendingFileActionService.Complete(_), PendingFileActionService.Complete(_), PendingFileActionService.Complete(_), PendingFileActionService.Complete(_), PendingFileActionService.Complete(_)) => Ok("test")
      case _ => Ok(view())
    }
  }
  //  val complete = PendingFileActionService.Complete(_)
}
