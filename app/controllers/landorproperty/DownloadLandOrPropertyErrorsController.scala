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

package controllers.landorproperty

import akka.stream.Materializer
import akka.stream.alpakka.csv.scaladsl.CsvFormatting
import akka.stream.scaladsl.{FileIO, Keep, Source}
import cats.data.NonEmptyList
import controllers.actions.IdentifyAndRequireData
import models.SchemeId.Srn
import models.{Journey, UploadErrorsLandConnectedProperty, UploadFormatError, UploadKey}
import play.api.i18n.{I18nSupport, Messages}
import play.api.libs.Files.TemporaryFileCreator
import play.api.mvc.{AbstractController, Action, AnyContent, ControllerComponents}
import services.UploadService

import java.nio.file.Paths
import javax.inject.Inject
import scala.concurrent.{ExecutionContext, Future}

class DownloadLandOrPropertyErrorsController @Inject()(
  uploadService: UploadService,
  identifyAndRequireData: IdentifyAndRequireData
)(cc: ControllerComponents)(
  implicit ec: ExecutionContext,
  temporaryFileCreator: TemporaryFileCreator,
  mat: Materializer
) extends AbstractController(cc)
    with I18nSupport {

  def downloadFile(srn: Srn): Action[AnyContent] = identifyAndRequireData(srn).async { implicit request =>
    uploadService.getUploadResult(UploadKey.fromRequest(srn, Journey.LandOrProperty.uploadRedirectTag)).flatMap {
      case Some(UploadErrorsLandConnectedProperty(unvalidated, errors)) =>
        val tempFile = temporaryFileCreator.create(suffix = "output-interest-land-or-property.csv")
        val fileOutput = FileIO.toPath(tempFile.path)
        val groupedErr = errors.groupBy(_.row)

        val csvLines: NonEmptyList[List[String]] = unvalidated
          .map(
            raw =>
              List(
                "",
                raw.firstNameOfSchemeMember.value,
                raw.lastNameOfSchemeMember.value,
                raw.memberDateOfBirth.value,
                raw.countOfLandOrPropertyTransactions.value,
                raw.acquisitionDate.value,
                raw.rawAddressDetail.isLandOrPropertyInUK.value,
                raw.rawAddressDetail.landOrPropertyUkAddressLine1.value.getOrElse(""),
                raw.rawAddressDetail.landOrPropertyUkAddressLine2.value.getOrElse(""),
                raw.rawAddressDetail.landOrPropertyUkAddressLine3.value.getOrElse(""),
                raw.rawAddressDetail.landOrPropertyUkTownOrCity.value.getOrElse(""),
                raw.rawAddressDetail.landOrPropertyUkPostCode.value.getOrElse(""),
                raw.rawAddressDetail.landOrPropertyAddressLine1.value.getOrElse(""),
                raw.rawAddressDetail.landOrPropertyAddressLine2.value.getOrElse(""),
                raw.rawAddressDetail.landOrPropertyAddressLine3.value.getOrElse(""),
                raw.rawAddressDetail.landOrPropertyAddressLine4.value.getOrElse(""),
                raw.rawAddressDetail.landOrPropertyCountry.value.getOrElse(""),
                raw.isThereLandRegistryReference.value,
                raw.noLandRegistryReference.value.getOrElse(""),
                raw.rawAcquiredFrom.acquiredFromType.value,
                raw.rawAcquiredFrom.acquirerNinoForIndividual.value.getOrElse(""),
                raw.rawAcquiredFrom.acquirerCrnForCompany.value.getOrElse(""),
                raw.rawAcquiredFrom.acquirerUtrForPartnership.value.getOrElse(""),
                raw.rawAcquiredFrom.noIdOrAcquiredFromAnotherSource.value.getOrElse(""),
                raw.totalCostOfLandOrPropertyAcquired.value,
                raw.isSupportedByAnIndependentValuation.value,
                raw.rawJointlyHeld.isPropertyHeldJointly.value,
                raw.rawJointlyHeld.howManyPersonsJointlyOwnProperty.value.getOrElse(""),
                raw.rawJointlyHeld.firstPersonNameJointlyOwning.value.getOrElse(""),
                raw.rawJointlyHeld.firstPersonNinoJointlyOwning.value.getOrElse(""),
                raw.rawJointlyHeld.firstPersonNoNinoJointlyOwning.value.getOrElse(""),
                raw.rawJointlyHeld.secondPersonNameJointlyOwning.value.getOrElse(""),
                raw.rawJointlyHeld.secondPersonNinoJointlyOwning.value.getOrElse(""),
                raw.rawJointlyHeld.secondPersonNoNinoJointlyOwning.value.getOrElse(""),
                raw.rawJointlyHeld.thirdPersonNameJointlyOwning.value.getOrElse(""),
                raw.rawJointlyHeld.thirdPersonNinoJointlyOwning.value.getOrElse(""),
                raw.rawJointlyHeld.thirdPersonNoNinoJointlyOwning.value.getOrElse(""),
                raw.rawJointlyHeld.fourthPersonNameJointlyOwning.value.getOrElse(""),
                raw.rawJointlyHeld.fourthPersonNinoJointlyOwning.value.getOrElse(""),
                raw.rawJointlyHeld.fourthPersonNoNinoJointlyOwning.value.getOrElse(""),
                raw.rawJointlyHeld.fifthPersonNameJointlyOwning.value.getOrElse(""),
                raw.rawJointlyHeld.fifthPersonNinoJointlyOwning.value.getOrElse(""),
                raw.rawJointlyHeld.fifthPersonNoNinoJointlyOwning.value.getOrElse(""),
                raw.isPropertyDefinedAsSchedule29a.value,
                raw.rawLeased.isLeased.value,
                raw.rawLeased.first.name.value.getOrElse(""),
                raw.rawLeased.first.connection.value.getOrElse(""),
                raw.rawLeased.first.grantedDate.value.getOrElse(""),
                raw.rawLeased.first.annualAmount.value.getOrElse(""),
                raw.rawLeased.second.name.value.getOrElse(""),
                raw.rawLeased.second.connection.value.getOrElse(""),
                raw.rawLeased.second.grantedDate.value.getOrElse(""),
                raw.rawLeased.second.annualAmount.value.getOrElse(""),
                raw.rawLeased.third.name.value.getOrElse(""),
                raw.rawLeased.third.connection.value.getOrElse(""),
                raw.rawLeased.third.grantedDate.value.getOrElse(""),
                raw.rawLeased.third.annualAmount.value.getOrElse(""),
                raw.rawLeased.fourth.name.value.getOrElse(""),
                raw.rawLeased.fourth.connection.value.getOrElse(""),
                raw.rawLeased.fourth.grantedDate.value.getOrElse(""),
                raw.rawLeased.fourth.annualAmount.value.getOrElse(""),
                raw.rawLeased.fifth.name.value.getOrElse(""),
                raw.rawLeased.fifth.connection.value.getOrElse(""),
                raw.rawLeased.fifth.grantedDate.value.getOrElse(""),
                raw.rawLeased.fifth.annualAmount.value.getOrElse(""),
                raw.rawLeased.sixth.name.value.getOrElse(""),
                raw.rawLeased.sixth.connection.value.getOrElse(""),
                raw.rawLeased.sixth.grantedDate.value.getOrElse(""),
                raw.rawLeased.sixth.annualAmount.value.getOrElse(""),
                raw.rawLeased.seventh.name.value.getOrElse(""),
                raw.rawLeased.seventh.connection.value.getOrElse(""),
                raw.rawLeased.seventh.grantedDate.value.getOrElse(""),
                raw.rawLeased.seventh.annualAmount.value.getOrElse(""),
                raw.rawLeased.eighth.name.value.getOrElse(""),
                raw.rawLeased.eighth.connection.value.getOrElse(""),
                raw.rawLeased.eighth.grantedDate.value.getOrElse(""),
                raw.rawLeased.eighth.annualAmount.value.getOrElse(""),
                raw.rawLeased.ninth.name.value.getOrElse(""),
                raw.rawLeased.ninth.connection.value.getOrElse(""),
                raw.rawLeased.ninth.grantedDate.value.getOrElse(""),
                raw.rawLeased.ninth.annualAmount.value.getOrElse(""),
                raw.rawLeased.tenth.name.value.getOrElse(""),
                raw.rawLeased.tenth.connection.value.getOrElse(""),
                raw.rawLeased.tenth.grantedDate.value.getOrElse(""),
                raw.rawLeased.tenth.annualAmount.value.getOrElse(""),
                raw.totalAmountOfIncomeAndReceipts.value,
                raw.rawDisposal.wereAnyDisposalOnThisDuringTheYear.value,
                raw.rawDisposal.totalSaleProceedIfAnyDisposal.value.getOrElse(""),
                raw.rawDisposal.first.name.value.getOrElse(""),
                raw.rawDisposal.first.connection.value.getOrElse(""),
                raw.rawDisposal.second.name.value.getOrElse(""),
                raw.rawDisposal.second.connection.value.getOrElse(""),
                raw.rawDisposal.third.name.value.getOrElse(""),
                raw.rawDisposal.third.connection.value.getOrElse(""),
                raw.rawDisposal.fourth.name.value.getOrElse(""),
                raw.rawDisposal.fourth.connection.value.getOrElse(""),
                raw.rawDisposal.fifth.name.value.getOrElse(""),
                raw.rawDisposal.fifth.connection.value.getOrElse(""),
                raw.rawDisposal.sixth.name.value.getOrElse(""),
                raw.rawDisposal.sixth.connection.value.getOrElse(""),
                raw.rawDisposal.seventh.name.value.getOrElse(""),
                raw.rawDisposal.seventh.connection.value.getOrElse(""),
                raw.rawDisposal.eighth.name.value.getOrElse(""),
                raw.rawDisposal.eighth.connection.value.getOrElse(""),
                raw.rawDisposal.ninth.name.value.getOrElse(""),
                raw.rawDisposal.ninth.connection.value.getOrElse(""),
                raw.rawDisposal.tenth.name.value.getOrElse(""),
                raw.rawDisposal.tenth.connection.value.getOrElse(""),
                raw.rawDisposal.isTransactionSupportedByIndependentValuation.value.getOrElse(""),
                raw.rawDisposal.hasLandOrPropertyFullyDisposedOf.value.getOrElse(""),
                groupedErr.get(raw.row).map(_.map(m => Messages(m.message)).toList.mkString(", ")).getOrElse("")
              )
          )

        val headers =
          """
            |"The questions in this section relate to interest in land or property. Questions that are mandatory are stated in row 2. \n\nYou must tell us about all land or property the scheme held at any point during the period of this return. If no land or property transactions have taken place within the tax year, you do not need to complete the questions in the Interest in this land or property section.\n\nWhat you need to do \n\nComplete the questions per member marked horizontally across the columns. \n\nFor members that have multiple property transactions, complete one row per property and repeat the members first name, last name and date of birth for the required number of rows.\n\nNotes and hint text is underneath each question to help make sure that there are no errors in the template file upload.\n\n";
            |"First name of scheme member";
            |"Last name of scheme member";
            |"Member date of birth";
            |"How many land or property transactions did the member make during the tax year and not reported in a previous return for this member?";
            |"What is the date of acquisition?";
            |"Is the land or property in the UK?";
            |"Enter the UK address line 1 of the land or property";
            |"Enter UK address line 2 of the land or property";
            |"Enter UK address line 3 of the land or property";
            |"Enter name UK town or city of the land or property";
            |"Enter post code of the land or property";
            |"Enter the non-UK address line 1 of the land or property";
            |"Enter non-UK address line 2 of the land or property";
            |"Enter non-UK address line 3 of the land or property";
            |"Enter non-UK address line 4 of the land or property";
            |"Enter non-UK country name of the land or property";
            |"Is there a land Registry reference in respect of the land or property?";
            |"If NO land Registry reference, enter reason";
            |"Was the land or property acquired from an individual, a company, a partnership, or another source?";
            |"If the land or property was aquired from an individual, enter their National Insurance Number";
            |"If the land or property acquired from a company, enter the Company Registration Number (CRN)";
            |"If the land or property acquired from a partnership, enter the UTR";
            |"Add the reason you do not have the National Insurance number, CRN or UTR, or if the land or property was aquired from another source, enter the details";
            |"What is the total cost of the land or property acquired?";
            |"Is the transaction supported by an Independent Valuation?";
            |"Is the property held jointly?";
            |"If the property is held jointly, how many persons jointly own it?";
            |"Enter the name of the first joint owner";
            |"National Insurance Number of person or entity jointly owning the property";
            |"If no National Insurance number for the first joint owner, enter the reason";
            |"Enter the name of the second joint owner";
            |"National Insurance Number of second person or entity jointly owning the property";
            |"If no National Insurance number for the second joint owner, enter the reason";
            |"Enter the name of the third joint owner";
            |"National Insurance Number of third person or entity jointly owning the property";
            |"If no National Insurance number for the third joint owner, enter the reason";
            |"Enter the name of the fourth joint owner";
            |"National Insurance Number of fourth person or entity jointly owning the property";
            |"If no National Insurance number for the fourth joint owner, enter the reason";
            |"Enter the name of the fifth joint owner";
            |"National Insurance Number of fifth person or entity jointly owning the property";
            |"If no National Insurance number for the fifth joint owner, enter the reason";
            |"Is any part of the land or property residential property as defined by schedule 29a Finance Act 2004?\n";
            |"Is the land or property leased?";
            |"If the land or property is leased, enter the name of the first lessee";
            |"Is the first lessee a connected or unconnected party?";
            |"If the land or property linked to the first lessee is leased, what was the date that the lease was granted?";
            |"If the land or property linked to the first lessee is leased, what is the annual lease amount for the first lessee?";
            |"If the land or property leased, enter the name of the second lessee";
            |"Is the second lessee a connected or unconnected party?";
            |"If the land or property linked to the second lessee is leased, what was the date that the lease was granted?";
            |"If the land or property linked to the second lessee is leased, what is the annual lease amount?";
            |"If the land or property is leased, enter the name of the third lessee";
            |"Is the third lessee a connected or unconnected party?";
            |"If the land or property linked to the third lessee is leased, what was the date that the lease was granted?";
            |"If the land or property linked to the third lessee is leased, what is the annual lease amount?";
            |"If the land or property is leased, enter the name of the fourth lessee";
            |"Is the fourth lessee a connected or unconnected party?";
            |"If the land or property linked to the fourth lessee is leased, what was the date that the lease was granted?";
            |"If the land or property linked to the fourth lessee is leased, what is the annual lease amount?";
            |"If the land or property is leased, enter the name of the fifth lessee";
            |"Is the fifth lessee a connected or unconnected party?";
            |"If the land or property linked to the fifth lessee is leased, what was the date that the lease was granted?";
            |"If the land or property linked to the fifth lessee is leased, what is the annual lease amount?";
            |"If the land or property is leased, enter the name of the sixth lessee";
            |"Is the sixth lessee a connected or unconnected party?";
            |"If the land or property linked to the sixth lessee is leased, what was the date that the lease was granted?";
            |"If the land or property linked to the sixth lessee is leased, what is the annual lease amount?";
            |"If the land or property is leased, enter the name of the seventh lessee";
            |"Is the seventh lessee a connected or unconnected party?";
            |"If the land or property linked to the seventh lessee is leased, what was the date that the lease was granted?";
            |"If the land or property linked to the seventh lessee is leased, what is the annual lease amount?";
            |"If the land or property is leased, enter the name of the eighth lessee";
            |"Is the eighth lessee a connected or unconnected party?";
            |"If the land or property linked to the eighth lessee is leased, what was the date that the lease was granted?";
            |"If the land or property linked to the eighth lessee is leased, what is the annual lease amount?";
            |"If the land or property is leased, enter the name of the ninth lessee";
            |"Is the ninth lessee a connected or unconnected party?";
            |"If the land or property linked to the ninth lessee is leased, what was the date that the lease was granted?";
            |"If the land or property linked to the ninth lessee is leased, what is the annual lease amount?";
            |"If the land or property is leased, enter the name of the tenth lessee";
            |"Is the tenth lessee a connected or unconnected party?";
            |"If the land or property linked to the tenth lessee is leased, what was the date that the lease was granted?";
            |"If the land or property linked to the tenth lessee is leased, what is the annual lease amount?";
            |"What is the total amount of income and receipts in respect of the land or property during tax year";
            |"Were any disposals made on this?";
            |"What was the total sale proceed of any land sold, or interest in land sold, or premiums received, on the disposal of a leasehold interest in land";
            |"If disposals were made on this, what is the name of the purchaser?";
            |"Is the purchaser a connected or unconnected party?";
            |"If there are other purchasers, enter the name of the second purchaser";
            |"Is this second purchaser a connected or unconnected party?";
            |"If there are other purchasers, enter the name of the third purchaser";
            |"Is the third purchaser a connected or unconnected party?";
            |"If there are other purchasers, enter the name of the fourth purchaser";
            |"Is the fourth purchaser a connected or unconnected party?";
            |"If there are other purchasers, enter the name of the fifth purchaser";
            |"Is the fifth purchaser a connected or unconnected party?";
            |"If there are other purchasers, enter the name of the sixth purchaser";
            |"Is the sixth purchaser a connected or unconnected party?";
            |"If there are other purchasers, enter the name of the seventh purchaser";
            |"Is the seventh purchaser a connected or unconnected party?";
            |"If there are other purchasers, enter the name of the eighth purchaser";
            |"Is the eighth purchaser a connected or unconnected party?";
            |"If there are other purchasers, enter the name of the ninth purchaser";
            |"Is the ninth purchaser a connected or unconnected party?";
            |"If there are other purchasers, enter the name of the tenth purchaser";
            |"Is the tenth purchaser a connected or unconnected party?";
            |"Is the transaction supported by an independent valuation";
            |"Has the land or property been fully disposed of?"
            |"ERRORS WITH DETAILS"
            |""".stripMargin

        val questionHelpers =
          """
          |"Question help information. This will give you hints or tips to help you to complete the required cells.";
          |"Enter the first name of the scheme member. \nHyphens are accepted.\n\nMandatory question.";
          |"Enter the last name of the scheme member. \nHyphens are accepted.\n\nMandatory question.";
          |"Use the format DD-MM-YYYY.\nMandatory question.";
          |"Enter the number of transactions. Max of 50.\n\nIf no land or property transactions have taken place within the tax year, you do not need to complete any further questions in the Interest in land or property section.\n\nFor members that have multiple property transactions, complete one row per property and repeat the members first name, last name and date of birth for the required number of rows.";
          |"Use the format DD-MM-YYYY.\n";
          |"Enter YES or NO.";
          |"Enter UK Address Line 1. \n\nShould be letters A to Z, numbers 0 to 9. Hyphens and speech marks are accepted.\n\nMaximum number of characters is 35.\n\nMandatory question for UK address.";
          |"Enter UK address Line 2. \n\nThis is an optional field. \n\nShould be letters A to Z, numbers 0 to 9. Hyphens and speech marks are accepted.\n\nMaximum number of characters is 35.";
          |"Enter UK address Line 3. \n\nThis is an optional field. \n\nShould be letters A to Z, numbers 0 to 9. Hyphens and speech marks are accepted.\n\nMaximum number of characters is 35.";
          |"Enter the UK town or city. \n\nThis is an optional question. \n\nShould be letters A to Z, numbers 0 to 9. Hyphens and speech marks are accepted.\n\nMaximum number of characters is 35.\n\nMandatory question if UK address..";
          |"Enter UK post code.\n\nMandatory question for UK address.";
          |"Enter non-UK address Line 1. \n\nShould be letters A to Z, numbers 0 to 9. Hyphens and speech marks are accepted.\n\nMaximum number of characters is 35.\n\nMandatory question if non-UK address.";
          |"Enter non-UK address Line 2. \n\nThis is an optional field. \nShould be letters A to Z, numbers 0 to 9. Hyphens and speech marks are accepted.\n\nMaximum number of characters is 35.";
          |"Enter the non-UK address Line 3. \n\nThis is an optional field. \nShould be letters A to Z, numbers 0 to 9. Hyphens and speech marks are accepted.\n\nMaximum number of characters is 35.";
          |"Enter the non-UK address line 4\n\nThis is an optional field. \nShould be letters A to Z, numbers 0 to 9. Hyphens and speech marks are accepted.\n\nMaximum number of characters is 35.";
          |"Enter the name of the non-UK country.\n\nMandatory question if non-UK address.";
          |"Enter YES or N0.\nIf No - provide reason";
          |"Max of 160 characters";
          |"Enter INDIVIDUAL, COMPANY, PARTNERSHIP, or OTHER";
          |"If the land or property was aquired from an individual, enter the individuals National Insurance Number (NINO). \n\nFor example: AA999999A\n\nIf you do not know this, add the reason why you do not have this. \n";
          |"Enter the Company Registration Number (CRN). \n\nIf you do not know this, add the reason why you do not have this. \n";
          |"Enter the UTR. \n\nIf you do not know this, add the reason why you do not have this. \n";
          |"If aquired from an individual: Enter reason for not having the individuals National Insurance Number.\n\nIf aquired from a company: Enter reason for not having the CRN.\n\nIf aquired from a partnership: Enter reason for not having the UTR.\n\nIf aquired from another source: Enter the details.\n\nMaximum 160 characters.";
          |"Enter the total amount in GBP (in pounds and pence). \n\nInclude stamp duty and other costs related to the transaction. If the land or property was not an acquisition, provide the total value.";
          |"Enter YES or NO.\n\nMandatory question.";
          |"Enter YES or NO.\n\nMandatory question.\n";
          |"Enter number of persons that jointly own the property.\n\nYou can add up to 5 additional joint owners in the following columns.";
          |"Enter the full name of the first joint owner.\nHyphens are accepted.\n\nMandatory question if answered that property is held jointly.";
          |"Enter number National Insurance Number of persons that jointly own the property. \n\nIf no National Insurance Number, explain why in the next question.";
          |"Maximum 160 characters";
          |"Enter the full name of the second joint owner.\nHyphens are accepted.\n";
          |"Enter number National Insurance Number of persons that jointly own the property. \n\nIf no National Insurance Number, explain why in the next question.";
          |"Maximum 160 characters";
          |"Enter the full name of the third joint owner.\nHyphens are accepted.\n";
          |"Enter number National Insurance Number of persons that jointly own the property. \n\nIf no National Insurance Number, explain why in the next question.";
          |"Maximum 160 characters";
          |"Enter the full name of the fourth joint owner.\nHyphens are accepted.\n";
          |"Enter number National Insurance Number of fourth person that jointly own the property.\n\nIf no National Insurance Number, explain why in the next question.";
          |"Maximum 160 characters";
          |"Enter the full name of the fifth joint owner.\nHyphens are accepted.\n";
          |"Enter number National Insurance Number of fifth person that jointly own the property. \n\nIf no National Insurance Number, explain why in the next question.";
          |"Maximum 160 characters";
          |"Enter YES or NO. \nMandatory question.";
          |"Enter YES or NO.\n\nYou can enter up to x10 lessees (tenants) details for the land or property in the following columns. \n\nMandatory question.";
          |"Enter name of the lessee\n\nMin 1 character - Max 160 characters. \n";
          |"Add CONNECTED or UNCONNECTED for the first lessee";
          |"Enter DD-MM-YYYY";
          |"Enter the total amount in GBP";
          |"Enter name of the second lessee. \nMin 1 character - Max 160 characters.";
          |"Add CONNECTED or UNCONNECTED for the second lessee";
          |"Enter DD-MM-YYYY";
          |"Enter the total amount in GBP";
          |"Enter name of the third lessee. \nMin 1 character - Max 160 characters.";
          |"Add CONNECTED or UNCONNECTED for the third lessee";
          |"Enter DD-MM-YYYY. \n";
          |"Enter the total amount in GBP. \n";
          |"Enter name of the fourth lessee. \nMin 1 character - Max 160 characters.";
          |"Add CONNECTED or UNCONNECTED for the fourth lessee";
          |"Enter DD-MM-YYYY";
          |"Enter the total amount in GBP";
          |"Enter name of the fifth lessee. \n\nMin 1 character - Max 160 characters.";
          |"Add CONNECTED or UNCONNECTED for the fifth lessee";
          |"Enter DD-MM-YYYY";
          |"Enter the total amount in GBP";
          |"Enter name of the sixth lessee. \n\nMin 1 character - Max 160 characters.";
          |"Add CONNECTED or UNCONNECTED for the sixth lessee";
          |"Enter DD-MM-YYYY. \n";
          |"Enter the total amount in GBP. \n";
          |"Enter name of the seventh lessee. \nMin 1 character - Max 160 characters.\n\nHyphens are accepted.";
          |"Add CONNECTED or UNCONNECTED for the seventh lessee";
          |"Enter DD-MM-YYYY";
          |"Enter the total amount in GBP";
          |"Enter name of the eighth lessee. \n\nMin 1 character - Max 160 characters.\n\nHyphens are accepted.";
          |"Add CONNECTED or UNCONNECTED for the eighth lessee";
          |"Enter DD-MM-YYYY";
          |"Enter the total amount in GBP";
          |"Enter name of the ninth lessee. \n\nMin 1 character - Max 160 characters.\n\nHyphens are accepted.";
          |"Add CONNECTED or UNCONNECTED for the ninth lessee";
          |"Enter DD-MM-YYYY. \n";
          |"Enter the total amount in GBP. \n";
          |"Enter name of the tenth lessee. \n\nMin 1 character - Max 160 characters.";
          |"Add CONNECTED or UNCONNECTED for the tenth lessee";
          |"Enter DD-MM-YYYY. \n";
          |"Enter the total amount in GBP. \n";
          |"Enter the total amount in GBP (in pounds and pence)\nfor all properties.\n\nThis includes VAT.";
          |"Enter YES or NO if any disposal was made";
          |"Enter the total amount in GBP (pounds and pence)";
          |"Enter name of purchaser.\n\nMax 160 characters.\n\nHyphens are accepted.";
          |"Enter CONNECTED or UNCONNECTED for the purchaser";
          |"Enter name of second purchaser";
          |"Enter CONNECTED or UNCONNECTED for the second purchaser";
          |"Enter name of third purchaser";
          |"Enter CONNECTED or UNCONNECTED for the third purchaser";
          |"Enter name of fourth purchaser";
          |"Enter CONNECTED or UNCONNECTED for fourth purchaser";
          |"Enter name of fifth purchaser";
          |"Enter CONNECTED or UNCONNECTED for fifth purchaser";
          |"Enter name of sixth purchaser";
          |"Enter CONNECTED or UNCONNECTED for sixth purchase";
          |"Enter name of seventh purchaser";
          |"Enter CONNECTED or UNCONNECTED for seventh purchaser";
          |"Enter name of eighth purchaser";
          |"Enter CONNECTED or UNCONNECTED for eighth purchase";
          |"Enter name of ninth purchaser";
          |"Enter CONNECTED or UNCONNECTED for ninth purchaser";
          |"Enter name of tenth purchaser";
          |"Enter CONNECTED or UNCONNECTED for tenth purchaser";
          |"Enter YES or NO";
          |"Enter YES or NO";
          |"ERRORS WITH DETAILS"
          |""".stripMargin

        val write = Source(
          List(headers.split(";").map(_.replace("\n", "")).toList) ++
            List(questionHelpers.split(";").map(_.replace("\n", "")).toList) ++
          csvLines.toList
        )
          .via(CsvFormatting.format())
          .toMat(fileOutput)(Keep.right)

        write.run().map { _ =>
          Ok.sendFile(
            content = tempFile.toFile,
            fileName = _ => Option("output-interest-land-or-property.csv")
          )
        }

      case Some(UploadFormatError(_)) =>
        Future.successful(Redirect(controllers.routes.JourneyRecoveryController.onPageLoad()))
      case _ => Future.successful(Redirect(controllers.routes.JourneyRecoveryController.onPageLoad()))
    }
  }
}
