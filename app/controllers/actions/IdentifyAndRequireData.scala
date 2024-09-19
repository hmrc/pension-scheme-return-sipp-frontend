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

package controllers.actions

import com.google.inject.Inject
import models.SchemeId.Srn
import models.requests.{DataRequest, FormBundleOrVersionTaxYearRequest, FormBundleRequest, VersionTaxYearRequest}
import play.api.mvc.{ActionBuilder, AnyContent}

class IdentifyAndRequireData @Inject()(
  identify: IdentifierAction,
  allowAccess: AllowAccessActionProvider,
  getData: DataRetrievalAction,
  requireData: DataRequiredAction,
  requireFormBundle: FormBundleRequiredAction,
  requireFormBundleOrVersionTaxYear: FormBundleOrVersionTaxYearRequiredAction,
  requireVersionTaxYear: VersionTaxYearRequiredAction
) {
  def apply(srn: Srn): ActionBuilder[DataRequest, AnyContent] =
    identify.andThen(allowAccess(srn)).andThen(getData).andThen(requireData)

  def withFormBundle(srn: Srn): ActionBuilder[FormBundleRequest, AnyContent] =
    identify.andThen(allowAccess(srn)).andThen(getData).andThen(requireData).andThen(requireFormBundle)

  def withFormBundleOrVersionAndTaxYear(srn: Srn): ActionBuilder[FormBundleOrVersionTaxYearRequest, AnyContent] =
    identify.andThen(allowAccess(srn)).andThen(getData).andThen(requireData).andThen(requireFormBundleOrVersionTaxYear)

  def withVersionAndTaxYear(srn: Srn): ActionBuilder[VersionTaxYearRequest, AnyContent] =
    identify.andThen(allowAccess(srn)).andThen(getData).andThen(requireData).andThen(requireVersionTaxYear)
}
