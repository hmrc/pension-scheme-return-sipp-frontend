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

package generators

import cats.syntax.option.*
import models.requests.common.YesNo.Yes
import models.requests.common.{
  AddressDetails,
  CostOrMarketType,
  DisposalDetails,
  LesseeDetails,
  RegistryDetails,
  SharesCompanyDetails,
  UnquotedShareDisposalDetail,
  YesNo
}
import models.requests.{
  AssetsFromConnectedPartyApi,
  LandOrConnectedPropertyApi,
  OutstandingLoanApi,
  TangibleMoveablePropertyApi,
  UnquotedShareApi
}
import org.scalacheck.Arbitrary.arbitrary
import org.scalacheck.Gen
import org.scalacheck.Gen.option
import org.scalacheck.Gen.stringOfN
import org.scalacheck.Gen.chooseNum

import java.time.LocalDate

trait TransactionDetailsGenerators { this: ModelGenerators =>
  val addressDetailsGen: Gen[AddressDetails] = for {
    line1 <- arbitrary[String]
    line2 <- arbitrary[Option[String]]
    line3 <- arbitrary[Option[String]]
    line4 <- arbitrary[Option[String]]
    line5 <- arbitrary[Option[String]]
    ukPostCode <- option(stringOfN(6, Gen.alphaNumChar))
    countryCode <- stringOfN(2, Gen.alphaUpperChar)
  } yield AddressDetails(line1, line2, line3, line4, line5, ukPostCode, countryCode)

  val registryDetailsGen: Gen[RegistryDetails] = for {
    registryRefExists <- yesNoGen
    registryReference <- arbitrary[Option[String]]
    noRegistryRefReason <- condGen(registryReference.isEmpty, arbitrary[String])
  } yield RegistryDetails(registryRefExists, registryReference, noRegistryRefReason)

  val lesseeDetailsGen: Gen[LesseeDetails] = for {
    numberOfLessees <- Gen.choose(1, 100)
    anyLesseeConnectedParty <- yesNoGen
    leaseGrantedDate <- arbitrary[LocalDate]
    annualLeaseAmount <- arbitrary[Double]
  } yield LesseeDetails(numberOfLessees, anyLesseeConnectedParty, leaseGrantedDate, annualLeaseAmount)

  val disposalDetailsGen: Gen[DisposalDetails] = for {
    disposedPropertyProceedsAmt <- arbitrary[Double]
    purchasersNames <- arbitrary[String]
    anyPurchaserDisconnectedParty <- yesNoGen
    independentValuationDisposal <- yesNoGen
    propertyFullyDisposed <- yesNoGen
  } yield DisposalDetails(
    disposedPropertyProceedsAmt,
    purchasersNames,
    anyPurchaserDisconnectedParty,
    independentValuationDisposal,
    propertyFullyDisposed
  )

  val sharesCompanyDetailsGen: Gen[SharesCompanyDetails] = for {
    companySharesName <- arbitrary[String]
    companySharesCrn <- option(crnGen)
    reasonNoCrn <- condGen(companySharesCrn.isEmpty, arbitrary[String])
    sharesClass <- arbitrary[String]
    noOfShares <- Gen.choose(1, 1000)
  } yield SharesCompanyDetails(companySharesName, companySharesCrn, reasonNoCrn, sharesClass, noOfShares)

  val sharesDisposalDetailsGen: Gen[UnquotedShareDisposalDetail] = for {
    disposedShareAmount <- arbitrary[Double]
    purchasers <- arbitrary[String]
    connected <- yesNoGen
    independent <- yesNoGen
    sharesSold <- Gen.chooseNum(1, 100)
    sharesHeld <- Gen.chooseNum(1, 100)
  } yield UnquotedShareDisposalDetail(disposedShareAmount, purchasers, connected, independent, sharesSold, sharesHeld)

  implicit val landOrPropertyGen: Gen[LandOrConnectedPropertyApi.TransactionDetail] =
    for {
      row <- chooseNum(1, 1000).map(_.some)
      nameDob <- nameDobGen
      nino <- ninoTypeGen
      acquisitionDate <- arbitrary[LocalDate]
      landOrPropertyInUK <- yesNoGen
      addressDetails <- addressDetailsGen
      registryDetails <- registryDetailsGen
      acquiredFromName <- arbitrary[String]
      totalCost <- arbitrary[Double]
      independentValuation <- yesNoGen
      jointlyHeld <- yesNoGen
      noOfPersons <- option(chooseNum(1, 10))
      residentialSchedule29A <- yesNoGen
      isLeased <- yesNoGen
      lesseeDetails <- option(lesseeDetailsGen)
      totalIncomeOrReceipts <- arbitrary[Double]
      isPropertyDisposed <- yesNoGen
      disposalDetails <- condGen(isPropertyDisposed == Yes, disposalDetailsGen)
    } yield LandOrConnectedPropertyApi.TransactionDetail(
      row = row,
      nameDOB = nameDob,
      nino = nino,
      acquisitionDate = acquisitionDate,
      landOrPropertyInUK = landOrPropertyInUK,
      addressDetails = addressDetails,
      registryDetails = registryDetails,
      acquiredFromName = acquiredFromName,
      totalCost = totalCost,
      independentValuation = independentValuation,
      jointlyHeld = jointlyHeld,
      noOfPersons = noOfPersons,
      residentialSchedule29A = residentialSchedule29A,
      isLeased = isLeased,
      lesseeDetails = lesseeDetails,
      totalIncomeOrReceipts = totalIncomeOrReceipts,
      isPropertyDisposed = isPropertyDisposed,
      disposalDetails = disposalDetails
    )

  implicit val tangibleMoveablePropertyGen: Gen[TangibleMoveablePropertyApi.TransactionDetail] = for {
    row <- chooseNum(1, 1000).map(_.some)
    nameDob <- nameDobGen
    nino <- ninoTypeGen
    assetDescription <- arbitrary[String]
    acquisitionDate <- arbitrary[LocalDate]
    totalCost <- arbitrary[Double]
    acquiredFromName <- arbitrary[String]
    independentValuation <- yesNoGen
    totalIncomeOrReceipts <- arbitrary[Double]
    costOrMarket <- Gen.oneOf(CostOrMarketType.MarketValue, CostOrMarketType.CostValue)
    costMarketValue <- arbitrary[Double]
    isPropertyDisposed <- yesNoGen
    disposalDetails <- condGen(isPropertyDisposed == Yes, disposalDetailsGen)
  } yield TangibleMoveablePropertyApi.TransactionDetail(
    row = row,
    nameDOB = nameDob,
    nino = nino,
    assetDescription = assetDescription,
    acquisitionDate = acquisitionDate,
    totalCost = totalCost,
    acquiredFromName = acquiredFromName,
    independentValuation = independentValuation,
    totalIncomeOrReceipts = totalIncomeOrReceipts,
    costOrMarket = costOrMarket,
    costMarketValue = costMarketValue,
    isPropertyDisposed = isPropertyDisposed,
    disposalDetails = disposalDetails
  )

  implicit val outstandingLoansGen: Gen[OutstandingLoanApi.TransactionDetail] = for {
    row <- chooseNum(1, 1000).map(_.some)
    nameDob <- nameDobGen
    nino <- ninoTypeGen
    loanRecipientName <- arbitrary[String]
    dateOfLoan <- arbitrary[LocalDate]
    amountOfLoan <- arbitrary[Double]
    loanConnectedParty <- yesNoGen
    repayDate <- arbitrary[LocalDate]
    interestRate <- arbitrary[Double]
    loanSecurity <- yesNoGen
    capitalRepayments <- arbitrary[Double]
    arrearsOutstandingPrYears <- yesNoGen
    outstandingYearEndAmount <- arbitrary[Double]
    arrearsOutstandingPrYearsAmt <- option(arbitrary[Double])
  } yield OutstandingLoanApi.TransactionDetail(
    row = row,
    nameDOB = nameDob,
    nino = nino,
    loanRecipientName = loanRecipientName,
    dateOfLoan = dateOfLoan,
    amountOfLoan = amountOfLoan,
    loanConnectedParty = loanConnectedParty,
    repayDate = repayDate,
    interestRate = interestRate,
    loanSecurity = loanSecurity,
    capitalRepayments = capitalRepayments,
    arrearsOutstandingPrYears = arrearsOutstandingPrYears,
    outstandingYearEndAmount = outstandingYearEndAmount,
    arrearsOutstandingPrYearsAmt = arrearsOutstandingPrYearsAmt
  )

  implicit val unquotedSharesGen: Gen[UnquotedShareApi.TransactionDetail] = for {
    row <- chooseNum(1, 1000).map(_.some)
    nameDob <- nameDobGen
    nino <- ninoTypeGen
    sharesCompanyDetails <- sharesCompanyDetailsGen
    acquiredFromName <- arbitrary[String]
    totalCost <- arbitrary[Double]
    independentValuation <- yesNoGen
    totalDividendsIncome <- arbitrary[Double]
    sharesDisposed <- yesNoGen
    sharesDisposalDetails <- condGen(sharesDisposed == Yes, sharesDisposalDetailsGen)
  } yield UnquotedShareApi.TransactionDetail(
    row = row,
    nameDOB = nameDob,
    nino = nino,
    sharesCompanyDetails = sharesCompanyDetails,
    acquiredFromName = acquiredFromName,
    totalCost = totalCost,
    independentValuation = independentValuation,
    totalDividendsIncome = totalDividendsIncome,
    sharesDisposed = sharesDisposed,
    sharesDisposalDetails = sharesDisposalDetails
  )

  val assetsFromConnectedGen: Gen[AssetsFromConnectedPartyApi.TransactionDetail] = for {
    row <- chooseNum(1, 1000).map(_.some)
    nameDob <- nameDobGen
    nino <- ninoTypeGen
    acquisitionDate <- arbitrary[LocalDate]
    assetDescription <- arbitrary[String]
    acquisitionOfShares <- yesNoGen
    sharesCompanyDetails <- condGen(acquisitionOfShares == Yes, sharesCompanyDetailsGen)
    acquiredFromName <- arbitrary[String]
    totalCost <- arbitrary[Double]
    independentValuation <- yesNoGen
    tangibleSchedule29A <- yesNoGen
    totalIncomeOrReceipts <- arbitrary[Double]
    isPropertyDisposed <- yesNoGen
    disposalDetails <- condGen(isPropertyDisposed == Yes, disposalDetailsGen)
    disposalOfShares <- option(yesNoGen)
    noOfSharesHeld <- option(Gen.chooseNum(1, 100))
  } yield AssetsFromConnectedPartyApi.TransactionDetail(
    row = row,
    nameDOB = nameDob,
    nino = nino,
    acquisitionDate = acquisitionDate,
    assetDescription = assetDescription,
    acquisitionOfShares = acquisitionOfShares,
    sharesCompanyDetails = sharesCompanyDetails,
    acquiredFromName = acquiredFromName,
    totalCost = totalCost,
    independentValuation = independentValuation,
    tangibleSchedule29A = tangibleSchedule29A,
    totalIncomeOrReceipts = totalIncomeOrReceipts,
    isPropertyDisposed = isPropertyDisposed,
    disposalDetails = disposalDetails,
    disposalOfShares = disposalOfShares,
    noOfSharesHeld = noOfSharesHeld
  )
}
