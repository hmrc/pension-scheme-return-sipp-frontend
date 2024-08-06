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

package util

import models.requests.common.CostOrMarketType.CostValue
import models.requests.common.YesNo.Yes
import models.{NameDOB, NinoType}
import models.requests.{
  AssetsFromConnectedPartyApi,
  LandOrConnectedPropertyApi,
  OutstandingLoanApi,
  OutstandingLoanRequest,
  TangibleMoveablePropertyApi,
  UnquotedShareApi,
  UnquotedShareRequest
}
import models.requests.common.{
  AddressDetails,
  DisposalDetails,
  LesseeDetails,
  RegistryDetails,
  SharesCompanyDetails,
  UnquotedShareDisposalDetail,
  YesNo
}

import java.time.LocalDate

trait TestTransactions {
  private val date: LocalDate = LocalDate.now()
  private val nameDob: NameDOB = NameDOB("TestName", "LastName", date)
  private val nino: NinoType = NinoType(nino = Some("AB123456C"), reasonNoNino = None)
  private val testNames: String = "Test Name 1, Test Name 2"
  private val disposalDetails: Some[DisposalDetails] = Some(
    DisposalDetails(
      disposedPropertyProceedsAmt = 1234.12,
      purchasersNames = testNames,
      anyPurchaserConnectedParty = Yes,
      independentValuationDisposal = Yes,
      propertyFullyDisposed = Yes
    )
  )

  private val shareCompanyDetails: SharesCompanyDetails = SharesCompanyDetails(
    companySharesName = "Test",
    companySharesCRN = None,
    reasonNoCRN = Some("Not applicable"),
    sharesClass = "Class",
    noOfShares = 1
  )

  val landConnectedPartyTransaction: LandOrConnectedPropertyApi.TransactionDetail =
    LandOrConnectedPropertyApi.TransactionDetail(
      row = Some(1),
      nameDOB = nameDob,
      nino = nino,
      acquisitionDate = date,
      landOrPropertyInUK = Yes,
      addressDetails = AddressDetails(
        addressLine1 = "Line 1",
        addressLine2 = "Line 2",
        addressLine3 = Some("Line 3"),
        addressLine4 = Some("Line 4"),
        addressLine5 = Some("Line 5"),
        ukPostCode = Some("BR2 3ER"),
        countryCode = "GB"
      ),
      registryDetails = RegistryDetails(
        registryRefExist = Yes,
        registryReference = Some("Reference"),
        noRegistryRefReason = None
      ),
      acquiredFromName = testNames,
      totalCost = 1234.12,
      independentValuation = Yes,
      jointlyHeld = Yes,
      noOfPersons = Some(3),
      residentialSchedule29A = Yes,
      isLeased = Yes,
      lesseeDetails = Some(
        LesseeDetails(
          numberOfLessees = 2,
          anyLesseeConnectedParty = Yes,
          leaseGrantedDate = date,
          annualLeaseAmount = 1234.12
        )
      ),
      totalIncomeOrReceipts = 1234.12,
      isPropertyDisposed = Yes,
      disposalDetails = disposalDetails,
      transactionCount = Some(1)
    )

  val outstandingLoanTransaction: OutstandingLoanApi.TransactionDetail =
    OutstandingLoanApi.TransactionDetail(
      row = Some(1),
      nameDOB = nameDob,
      nino = nino,
      loanRecipientName = testNames,
      dateOfLoan = date,
      amountOfLoan = 1234.54,
      loanConnectedParty = Yes,
      repayDate = date,
      interestRate = 12.22,
      loanSecurity = Yes,
      capitalRepayments = 123.32,
      arrearsOutstandingPrYears = Yes,
      outstandingYearEndAmount = 123.12,
      arrearsOutstandingPrYearsAmt = Some(123.44),
      transactionCount = Some(2)
    )

  val assetsFromConnectedPartyTransaction: AssetsFromConnectedPartyApi.TransactionDetail =
    AssetsFromConnectedPartyApi.TransactionDetail(
      row = Some(1),
      nameDOB = nameDob,
      nino = nino,
      acquisitionDate = date,
      assetDescription = "Dummy Description",
      acquisitionOfShares = Yes,
      sharesCompanyDetails = Some(shareCompanyDetails),
      acquiredFromName = testNames,
      totalCost = 1234.12,
      independentValuation = Yes,
      tangibleSchedule29A = Yes,
      totalIncomeOrReceipts = 123.12,
      isPropertyDisposed = Yes,
      disposalDetails = disposalDetails,
      disposalOfShares = Some(Yes),
      noOfSharesHeld = Some(1),
      transactionCount = Some(1)
    )

  val tangibleMoveablePropertyTransaction: TangibleMoveablePropertyApi.TransactionDetail =
    TangibleMoveablePropertyApi.TransactionDetail(
      row = Some(1),
      nameDOB = nameDob,
      nino = nino,
      assetDescription = "Dummy Description",
      acquisitionDate = date,
      totalCost = 123.12,
      acquiredFromName = testNames,
      independentValuation = Yes,
      totalIncomeOrReceipts = 123.33,
      costOrMarket = CostValue,
      costMarketValue = 1234.1,
      isPropertyDisposed = Yes,
      disposalDetails = disposalDetails,
      transactionCount = Some(1)
    )

  val unquotedShareTransaction: UnquotedShareApi.TransactionDetail =
    UnquotedShareApi.TransactionDetail(
      row = Some(1),
      nameDOB = nameDob,
      nino = nino,
      sharesCompanyDetails = shareCompanyDetails,
      acquiredFromName = testNames,
      totalCost = 1234.11,
      independentValuation = Yes,
      totalDividendsIncome = 123.11,
      sharesDisposed = Yes,
      sharesDisposalDetails = Some(
        UnquotedShareDisposalDetail(
          disposedShareAmount = 123.11,
          purchasersNames = testNames,
          disposalConnectedParty = Yes,
          independentValuationDisposal = Yes,
          noOfSharesSold = 1,
          noOfSharesHeld = 1
        )
      ),
      transactionCount = Some(1)
    )
}
