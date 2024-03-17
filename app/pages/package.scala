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

import models.Journey
import play.api.libs.json.{__, JsPath}

package object pages {

  private val assets: JsPath = __ \ "assets"
  def journeyAssetsPath(journey: Journey): JsPath = journey match {
    case Journey.MemberDetails => assets \ "memberDetails"
    case Journey.InterestInLandOrProperty | Journey.ArmsLengthLandOrProperty | Journey.TangibleMoveableProperty | Journey.OutstandingLoans =>
      assets \ "landOrProperty" \ "landOrPropertyTransactions" \ journeyPath(journey)
  }

  def journeyPath(journey: Journey): String = journey match {
    case Journey.MemberDetails => "memberDetails"
    case Journey.InterestInLandOrProperty => "interestInLandOrProperty"
    case Journey.ArmsLengthLandOrProperty => "armsLengthLandOrProperty"
    case Journey.TangibleMoveableProperty => "tangibleMoveableProperty"
    case Journey.OutstandingLoans => "outstandingLoans"
  }
}
