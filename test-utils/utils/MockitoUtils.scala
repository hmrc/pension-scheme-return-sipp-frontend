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

package utils

import org.mockito.{ArgumentMatchers, Mockito}
import org.mockito.stubbing.OngoingStubbing
import org.mockito.verification.VerificationMode
import org.scalatestplus.mockito.MockitoSugar

// To be dropped when scala-mockito supports Scala 3 (by mixing in its MockitoSugar trait to BaseSpec)
trait MockitoUtils extends MockitoSugar {
  inline def eqTo[T](value: T): T = ArgumentMatchers.eq(value)
  inline def any[T]: T = ArgumentMatchers.any
  inline def when[T](methodCall: T): OngoingStubbing[T] = Mockito.when(methodCall)
  inline def reset(mocks: AnyRef*): Unit = Mockito.reset(mocks*)
  inline def verify[T](mock: T): T = Mockito.verify(mock)
  inline def verify[T](mock: T, mode: VerificationMode): T = Mockito.verify(mock, mode)
  inline def never: VerificationMode = Mockito.never()
  inline def times(wantedNumberOfInvocations: Int): VerificationMode = Mockito.times(wantedNumberOfInvocations)
}
