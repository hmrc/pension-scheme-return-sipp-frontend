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

package config

import models.{IdentitySubject, Journey, JourneyType}
import models.SchemeId.Srn
import models.enumerations.TemplateFileType
import play.api.mvc.{PathBindable, QueryStringBindable}

object Binders {

  implicit val srnBinder: PathBindable[Srn] = new PathBindable[Srn] {

    override def bind(key: String, value: String): Either[String, Srn] =
      Srn(value).toRight("Invalid scheme reference number")

    override def unbind(key: String, value: Srn): String = value.value
  }

  implicit val templateFileTypeBinder: PathBindable[TemplateFileType] = new PathBindable[TemplateFileType] {

    override def bind(key: String, value: String): Either[String, TemplateFileType] =
      Option(TemplateFileType.withNameWithDefault(value))
        .toRight(s" $key value $value unknown identity type")

    override def unbind(key: String, value: TemplateFileType): String = value.name
  }

  implicit val identitySubjectBinder: PathBindable[IdentitySubject] = new PathBindable[IdentitySubject] {

    override def bind(key: String, value: String): Either[String, IdentitySubject] =
      Option(IdentitySubject.withNameWithDefault(value))
        .toRight(s" $key value $value unknown identity type")

    override def unbind(key: String, value: IdentitySubject): String = value.name
  }

  implicit def journeyPathBinder: PathBindable[Journey] =
    new PathBindable[Journey] {
      override def bind(key: String, value: String): Either[String, Journey] =
        Journey.withNameOption(value).toRight(s"Unknown journey: $value")

      override def unbind(key: String, journey: Journey): String = journey.entryName
    }

  implicit def journeyTypeQueryStringBinder(implicit
    stringBinder: QueryStringBindable[String]
  ): QueryStringBindable[JourneyType] = stringBinder.transform(JourneyType.withName, _.entryName)
}
