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

package utils

import cats.implicits.toFunctorOps
import play.api.libs.json.*

import java.net.URL
import scala.util.Try

object HttpUrl {

  implicit val format: Format[URL] = new Format[URL] {
    override def reads(json: JsValue): JsResult[URL] =
      json match {
        case JsString(s) => parseUrl(s).map(JsSuccess(_)).getOrElse(invalidUrlError)
        case _ => invalidUrlError
      }

    private def parseUrl(s: String): Option[URL] = Try(new URL(s)).toOption

    private def invalidUrlError: JsError = JsError(Seq(JsPath() -> Seq(JsonValidationError("error.expected.url"))))

    override def writes(o: URL): JsValue = JsString(o.toString)
  }

  def makeUrl(baseUrl: String, queryParams: Seq[(String, String)], isFirstQueryParam: Boolean = true): URL = {
    val init = if (isFirstQueryParam) "?" else "&"
    val queryParamsString = queryParams.map { case (key, value) => s"$key=$value" }.mkString(init, "&", "")
    val url = baseUrl + queryParams.headOption.as(queryParamsString).mkString
    new URL(url)
  }
}
