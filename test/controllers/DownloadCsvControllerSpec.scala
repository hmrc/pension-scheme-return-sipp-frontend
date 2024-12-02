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

import cats.data.NonEmptyList
import config.Crypto
import repositories.{CsvRowStateSerialization, UploadRepository}
import connectors.PSRConnector
import models.Journey
import models.Journey.*
import models.csv.CsvRowState.CsvRowValid
import models.requests.*
import org.apache.pekko.stream.scaladsl.Source
import org.apache.pekko.util.ByteString
import org.scalacheck.Gen
import play.api.inject.bind
import play.api.inject.guice.GuiceableModule
import play.api.libs.json.Json
import uk.gov.hmrc.crypto.{Crypted, Decrypter, Encrypter, PlainBytes, PlainContent, PlainText}

import java.nio.ByteOrder
import scala.concurrent.Future

class DownloadCsvControllerSpec extends ControllerBaseSpec {
  private val uploadRepo = mock[UploadRepository]
  private val psrConnector = mock[PSRConnector]
  private val crypto = new Crypto {
    override val getCrypto: Encrypter & Decrypter = new Encrypter with Decrypter {
      override def encrypt(plain: PlainContent): Crypted = Crypted("Hello")
      override def decrypt(reversiblyEncrypted: Crypted): PlainText = PlainText("Hello")
      override def decryptAsBytes(reversiblyEncrypted: Crypted): PlainBytes = PlainBytes("Hello".getBytes)
    }
  }
  private val csvRowStateSerialization = mock[CsvRowStateSerialization]

  override val additionalBindings: List[GuiceableModule] =
    List(
      bind[UploadRepository].toInstance(uploadRepo),
      bind[PSRConnector].toInstance(psrConnector),
      bind[Crypto].toInstance(crypto),
      bind[CsvRowStateSerialization].toInstance(csvRowStateSerialization)
    )

  "DownloadCsvController" - {

    def downloadFilePage(journey: Journey = InterestInLandOrProperty) =
      routes.DownloadCsvController.downloadFile(srn, journey)
    def downloadEtmpFilePage(journey: Journey) =
      routes.DownloadCsvController.downloadEtmpFile(srn, journey, None, None, None)

    act.like(
      notFound(downloadFilePage(), defaultUserAnswers)
        .withName("must return 404 NOT_FOUND if the file is not found in the repo")
        .before(
          when(uploadRepo.retrieve(any)(any)).thenReturn(Future.successful(None))
        )
    )

    "download csv" - {
      Journey.values.foreach { journey =>
        act.like(
          streamContent(downloadFilePage(journey), defaultUserAnswers)
            .updateName(_ + " - " + journey)
            .before {
              when(uploadRepo.retrieve(any)(any))
                .thenReturn(Future.successful(Some(Source.single(framedByteString("Hello")))))
              when(csvRowStateSerialization.read(any)(using any, any))
                .thenReturn(CsvRowValid(1, Json.obj(), NonEmptyList.one("dummy")))
            }
        )
      }
    }

    "download etmp csv" - {
      Journey.values.foreach { journey =>
        act.like {
          streamContent(downloadEtmpFilePage(journey), defaultUserAnswers)
            .updateName(_ + s" - Download etmp csv - $journey")
            .before {
              when(psrConnector.getLandOrConnectedProperty(any, any, any, any)(any))
                .thenReturn {
                  val list = Gen.listOfN(5, landOrPropertyGen).sample.value
                  Future.successful(LandOrConnectedPropertyResponse(list))
                }
              when(psrConnector.getLandArmsLength(any, any, any, any)(any))
                .thenReturn {
                  val list = Gen.listOfN(5, landOrPropertyGen).sample.value
                  Future.successful(LandOrConnectedPropertyResponse(list))
                }
              when(psrConnector.getTangibleMoveableProperty(any, any, any, any)(any))
                .thenReturn {
                  val list = Gen.listOfN(5, tangibleMoveablePropertyGen).sample.value
                  Future.successful(TangibleMoveablePropertyResponse(list))
                }
              when(psrConnector.getOutstandingLoans(any, any, any, any)(any))
                .thenReturn {
                  val list = Gen.listOfN(5, outstandingLoansGen).sample.value
                  Future.successful(OutstandingLoanResponse(list))
                }
              when(psrConnector.getUnquotedShares(any, any, any, any)(any))
                .thenReturn {
                  val list = Gen.listOfN(5, unquotedSharesGen).sample.value
                  Future.successful(UnquotedShareResponse(list))
                }
              when(psrConnector.getAssetsFromConnectedParty(any, any, any, any)(any))
                .thenReturn {
                  val list = Gen.listOfN(5, assetsFromConnectedGen).sample.value
                  Future.successful(AssetsFromConnectedPartyResponse(list))
                }
            }
        }
      }
    }

  }

  def framedByteString(str: String): ByteString = {
    val bs = ByteString(str)
    ByteString.newBuilder.putInt(bs.length)(ByteOrder.BIG_ENDIAN).result ++ bs
  }

}
