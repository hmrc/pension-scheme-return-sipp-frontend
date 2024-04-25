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

package models

object HeaderKeys {
  val headersForMemberDetails =
    """The questions in this template relate to member details. Mandatory questions are marked in Row 2 guidance text. What you need to do Each row represents a member. Complete the questions per member marked horizontally across the columns. Notes and hint text is underneath each question to help make sure that there are no errors in the template file upload.;
    |First name of scheme member;
    |Last name of scheme member;
    |Member date of birth;
    |Member National Insurance number;
    |If no National Insurance number for member, give reason;
    |Is the members address in the UK?;
    |Enter the members UK address line 1;
    |Enter members UK address line 2;
    |Enter members UK address line 3;
    |Enter name of members UK town or city;
    |Enter members post code;
    |Enter the members non-UK address line 1;
    |Enter members non-UK address line 2;
    |Enter members non-UK address line 3;
    |Enter members non-UK address line 4;
    |Enter members non-UK country;
    |ERRORS
    |""".stripMargin

  val questionHelpersMemberDetails =
    """Question help information. This will give you hints or tips to help you to complete the required cells.;
      |Enter the first name of the scheme member. Hyphens accepted. Mandatory question.;
      |Enter the last name of the scheme member. Hyphens accepted. Mandatory question.;
      |Use the format DD-MM-YYYY. Mandatory question.;
      |Enter the individuals National Insurance Number. If you do not know this, add the reason why you do not have this in the next column. 9 characters in alphanumeric in the format for example: AA999999A. Mandatory question.;
      |Enter reason for not having the members National Insurance number. Maximum of 160 characters.;
      |Enter YES or NO. Mandatory question.;
      |Enter UK Address Line 1. Maximum number of characters is 35. Should be letters A to Z, numbers 0 to 9. Hyphens and speech marks are accepted. Mandatory question for UK address.;
      |Enter UK address Line 2. This is an optional field. Maximum number of characters is 35. Should be letters A to Z, numbers 0 to 9. Hyphens and speech marks are accepted;
      |Enter UK address Line 3. This is an optional field. Maximum number of characters is 35. Should be letters A to Z, numbers 0 to 9. Hyphens and speech marks are accepted;
      |Enter the UK town or city. Should be letters A to Z, numbers 0 to 9. Hyphens and speech marks are accepted. Maximum number of characters is 35. Mandatory question for UK address.;
      |Enter UK post code. Mandatory question for UK address.;
      |Enter non-UK address Line 1. Should be letters A to Z, numbers 0 to 9. Hyphens and speech marks are accepted. Maximum number of characters is 35. Mandatory question for non-UK address.;
      |Enter non-UK address Line 2. This is an optional field. Should be letters A to Z, numbers 0 to 9. Hyphens and speech marks are accepted. Maximum number of characters is 35.;
      |Enter the non-UK address Line 3. This is an optional field. Should be letters A to Z, numbers 0 to 9. Hyphens and speech marks are accepted Maximum number of characters is 35.;
      |Enter the non-UK address line 4 This is an optional field. Should be letters A to Z, numbers 0 to 9. Hyphens and speech marks are accepted. Maximum number of characters is 35.;
      |Enter the name of the non-UK country Should be letters A to Z, numbers 0 to 9. Hyphens and speech marks are accepted.
      |""".stripMargin

  val headersForInterestLandOrProperty =
    """The questions in this section relate to interest in land or property from a connected party. Questions that are mandatory are stated in row 2. You must tell us about all land or property the scheme held at any point during the period of this return. If no land or property transactions have taken place within the tax year, you do not need to complete the questions in the Interest in this land or property section.What you need to do  Complete the questions per member marked horizontally across the columns. For members that have multiple property transactions, complete one row per property and repeat the members first name, last name and date of birth for the required number of rows.Notes and hint text is underneath each question to help make sure that there are no errors in the template file upload.;
      |First name of scheme member;
      |Last name of scheme member;
      |Member date of birth;
      |Member National Insurance number;
      |If no National Insurance number for member, give reason;
      |How many land or property transactions did the member make during the tax year and not reported in a previous return for this member?;
      |What is the date of acquisition?;
      |Is the land or property in the UK?;
      |Enter the UK address line 1 of the land or property;
      |Enter UK address line 2 of the land or property;
      |Enter UK address line 3 of the land or property;
      |Enter name UK town or city of the land or property;
      |Enter post code of the land or property;
      |Enter the non-UK address line 1 of the land or property;
      |Enter non-UK address line 2 of the land or property;
      |Enter non-UK address line 3 of the land or property;
      |Enter non-UK address line 4 of the land or property;
      |Enter non-UK country name of the land or property;
      |Is there a Land Registry reference in respect of the land or property?;
      |If no Land Registry reference, enter reason;
      |Who was the land or property acquired from?;
      |What is the total cost of the land or property acquired?;
      |Is the transaction supported by an Independent Valuation?;
      |Is the property held jointly?;
      |How many persons jointly own the property?;
      |Is any part of the land or property residential property as defined by schedule 29a Finance Act 2004?;
      |Is the land or property leased?;
      |How many lessees are there for the land or property?;
      |Are any of the lessees a connected party?;
      |What date was the lease granted?;
      |What is the annual lease amount?;
      |What is the total amount of income and receipts in respect of the land or property during tax year;
      |Were any disposals made on this?;
      |What was the total sale proceed of any land sold, or interest in land sold, or premiums received, on the disposal of a leasehold interest in land;
      |If disposals were made on this, what are the names of the purchasers?;
      |Are any of the purchasers connected parties?;
      |Is the transaction supported by an independent valuation;
      |Has the land or property been fully disposed of?;
      |ERRORS WITH DETAIL
      |""".stripMargin

  val questionHelpersMoveableProperty =
    """Question help information. The information in this row will give you hints or tips to help you to complete the required questions in the cells above.;
      |Enter the first name of the scheme member. Mandatory question. Should be letters A to Z. Hyphens are accepted.;
      |Enter the last name of the scheme member. Mandatory question. Should be letters A to Z. Hyphens are accepted.;
      |Use the format DD-MM-YYYY. Mandatory question.;
      |Enter number of transactions made using characters of 0-9. If no land or property transactions have taken place within the tax year, you do not need to complete any further questions in the Tangible moveable property section. For members that have multiple property transactions, complete one row per property and repeat the members first name, last name and date of birth for the required number of rows. Mandatory question;
      |Min 1 character - Max 160 characters. Mandatory question.;
      |Use format DD-MM-YYYY Mandatory question;
      |Enter the total amount in GBP (in pounds and pence). Include stamp duty and other costs related to the transaction. If the land or property was not an acquisition, provide the total value.;
      |Enter name. Min 1 character - Max 160 characters. Mandatory question;
      |Enter individual or company or partnership or other. Mandatory question;
      |Enter the individuals National Insurance Number using the format AB123456A. If you do not know this, add the reason why you do not have this in the relevant column.;
      |Enter the Company Registration Number (CRN). If you do not know this, add the reason why you do not have this in the relevant column.;
      |Enter unique tax reference (UTR). If you do not know this, add the reason why you do not have this in the relevant column.;
      |If acquired from another source: Enter the details. If acquired from an individual but you don't have this: Enter reason for not having the individuals National Insurance Number. If acquired from a company but don't have the CRN: Enter reason for not having the CRN. If acquired from a partnership but don't have the UTR: Enter reason for not having the UTR. Maximum 160 characters.;
      |Enter YES or NO. Mandatory question.;
      |Enter the total amount in GBP (pounds and pence). Enter amount as whole number and to two decimal places. Mandatory question.;
      |Enter if cost value or market value. As at 6 April [of the tax year that you are submitting]. Mandatory question;
      |Enter cost value or market value. Then enter amount in GBP (pounds and pence). Mandatory question;
      |Enter YES or NO. Mandatory question.;
      |Enter the total amount in GBP (pounds and pence).;
      |Enter YES or NO;
      |Enter name. Max. 160 characters. Hyphens are accepted.;
      |Enter CONNECTED or UNCONNECTED;
      |Enter name of second purchaser. Hyphens are accepted. 160 maximum character limit.;
      |Enter CONNECTED or UNCONNECTED for the second purchaser;
      |Enter name of third purchaser. Hyphens are accepted. 160 maximum character limit;
      |Enter CONNECTED or UNCONNECTED for third purchaser;
      |Enter name of fourth purchaser. Hyphens are accepted. 160 maximum character limit;
      |Enter CONNECTED or UNCONNECTED for fourth purchaser;
      |Enter name of fifth purchaser. Hyphens are accepted. 160 maximum character limit;
      |Enter CONNECTED or UNCONNECTED for fifth purchaser;
      |Enter name of sixth purchaser. Hyphens are accepted. 160 maximum character limit;
      |Enter CONNECTED or UNCONNECTED for sixth purchaser;
      |Enter name of seventh purchaser. Hyphens are accepted. 160 maximum character limit;
      |Enter CONNECTED or UNCONNECTED for seventh purchaser;
      |Enter name of eighth purchaser. Hyphens are accepted. 160 maximum character limit;
      |Enter CONNECTED or UNCONNECTED for eighth purchaser;
      |Enter name of ninth purchaser. Hyphens are accepted. 160 maximum character limit;
      |Enter CONNECTED or UNCONNECTED for ninth purchaser;
      |Enter name of tenth purchaser. Hyphens are accepted. 160 maximum character limit;
      |Enter CONNECTED or UNCONNECTED for tenth purchaser;
      |Enter yes or no. Mandatory question;
      |Enter yes or no. Mandatory question if disposals made.;
      |ERRORS WITH DETAIL
      |""".stripMargin

  val headersForTangibleMoveableProperty =
    """The questions in this section relate to Tangible moveable property. What you need to do Complete the questions per member marked horizontally across the columns. For members that have multiple property assets, complete one row per asset and repeat the members first name, last name and date of birth for the required number of rows. Notes and hint text is underneath each question to help make sure that there are no errors in the template file upload. Mandatory questions are marked in the hint text.;
      |First name of scheme member;
      |Last name of scheme member;
      |Member date of birth;
      |How many transactions of tangible moveable property were made during the tax year and not reported in a previous return for this member?;
      |Description of asset;
      |What was the date of acquisiiton of the asset?;
      |What was the total cost of the asset acquired?;
      |Who was the asset acquired from?;
      |Was the asset acquired from an individual or company or partnership or other?;
      |If the asset was acquired from an individual enter the individuals National Insurance Number;
      |If the asset was acquired from a company enter the CRN details;
      |If the asset was acquired from a partnership enter the UTR details;
      |Add the reason you do not have the individuals National Insurance number, the CRN or the UTR. Or if the land or property was acquired from another source, enter the details;
      |Is this transaction supported by an Independent Valuation?;
      |What is the total amount of income and receipts received in respect of the asset during tax year?;
      |Is the total cost value or market value of the asset?;
      |What is the total cost value or market value of the asset, as at 6 April [of the tax year that you are submitting];
      |During the year was there any disposal of the tangible moveable property made?;
      |If yes, there was disposal of tangible moveable property - what is the total amount of consideration received from the sale or disposal of the asset?;
      |Were any disposals made?;
      |If disposals were made on this, what is the name of the purchaser?;
      |Is this purchaser a connected or unconnected party?;
      |If there are other purchasers enter the name of the second purchaser;
      |Is this second purchaser a connected or unconnected party?;
      |If there are other purchasers enter the name of the third purchaser;
      |Is the third purchaser a connected or unconnected party?;
      |If there are other purchasers enter the name of the fourth purchaser;
      |Is the fourth purchaser a connected or unconnected party?;
      |If there are other purchasers enter the name of the fifth purchaser;
      |Is the fifth purchaser a connected or unconnected party?;
      |If there are other purchasers, enter the name of the sixth purchaser;
      |Is the sixth purchaser a connected or unconnected party?;
      |If there are other purchasers, enter the name of the seventh purchaser;
      |Is the seventh purchaser a connected or unconnected party?;
      |If there are other purchasers, enter the name of the eighth purchaser;
      |Is the eighth purchaser a connected or unconnected party?;
      |If there are other purchasers, enter the name of the ninth purchaser;
      |Is the ninth purchaser a connected or unconnected party?;
      |If there are other purchasers, enter the name of the tenth purchaser;
      |Is the tenth purchaser a connected or unconnected party?;
      |Was the transaction supported by an independent valuation?;
      |Is any part of the asset still held?;
      |ERRORS WITH DETAIL
      |""".stripMargin

  val headersForArmsLength =
    """The questions in this section relate to Arm's length land or property from a connected party. You must tell us about all arms length land or property held at any point during the period of this return. What you need to doEach row of the file corresponds to one asset of the scheme. Complete all of the relevant columns for each member. Notes and hint text is underneath each question to help make sure that there are no errors in the template file upload.Mandatory questions are marked in the hint text. ;
      |First name of scheme member;
      |Last name of scheme member;
      |Member date of birth;
      |Member National Insurance number;
      |If no National Insurance number for member, give reason;
      |How many land or property transactions were made during the tax year and not reported in a previous return for this member?;
      |What is the date of acquisition?;
      |Is the land or property in the UK?;
      |Enter the UK address line 1 of the land or property;
      |Enter UK address line 2 of the land or property;
      |Enter UK address line 3 of the land or property;
      |Enter name UK town or city of the land or property;
      |Enter post code of the land or property;
      |Enter the non-UK address line 1 of the land or property;
      |Enter non-UK address line 2 of the land or property;
      |Enter non-UK address line 3 of the land or property;
      |Enter non-UK address line 4 of the land or property;
      |Enter non-UK country name of the land or property;
      |Is there a Land Registry reference in respect of the land or property?;
      |If no Land Registry reference, enter reason;
      |Who was the land or property acquired from?;
      |What is the total cost of the land or property acquired?;
      |Is the transaction supported by an Independent Valuation?;
      |Is the property held jointly?;
      |How many persons jointly own the property?;
      |Is any part of the land or property residential property as defined by schedule 29a Finance Act 2004?;
      |Is the land or property leased?;
      |What are the names of the lessees?;
      |Are any of the lessees a connected party?;
      |What date was the lease granted?;
      |What is the annual lease amount?;
      |What is the total amount of income and receipts in respect of the land or property during tax year;
      |Was any disposal of the land or property made during the year?;
      |What was the total sale proceed of any land sold, or interest in land sold, or premiums received, on the disposal of a leasehold interest in land;
      |If disposals were made on this, what are the names of the purchasers?;
      |Are any of the purchasers connected parties?;
      |Is the transaction supported by an independent valuation;
      |Has the land or property been fully disposed of?;
      |ERRORS WITH DETAIL
      |""".stripMargin

  val questionHelpers =
    """Question help information. The information in this row will give you hints or tips to help you to complete the required questions in the cells above. ;
      |Enter the first name of the scheme member. Hyphens are accepted. Should be letters A to Z. Names have a maximum of 35 characters. Mandatory question.;
      |Enter the last name of the scheme member. Hyphens are accepted. Should be letters A to Z. Names have a maximum of 35 characters. Mandatory question.;
      |Use the format DD-MM-YYYY. Mandatory question.;
      |Enter the individuals National Insurance Number.  If you do not know this, add the reason why you do not have this in the next column. 9 characters in alphanumeric in the format for example: AA999999A.  Mandatory question.;
      |Enter reason for not having the members National Insurance number. Maximum of 160 characters.;
      |Enter number of transactions made using characters of 0-9. Mandatory question.;
      |Use the format DD-MM-YYY. This is a mandatory question.;
      |Enter YES or NO. Mandatory question;
      |Enter UK Address Line 1. Should be letters A to Z, numbers 0 to 9. Hyphens and speech marks are accepted. Maximum number of characters is 35.  Mandatory question for UK address.;
      |Enter UK address Line 2. This is an optional field. Should be letters A to Z, numbers 0 to 9. Hyphens and speech marks are accepted. Maximum number of characters is 35.;
      |Enter UK address Line 3. This is an optional field. Should be letters A to Z, numbers 0 to 9. Hyphens and speech marks are accepted. Maximum number of characters is 35.;
      |Enter the UK town or city. Should be letters A to Z, numbers 0 to 9. Hyphens and speech marks are accepted. Maximum number of characters is 35.  Mandatory question for UK address.;
      |Enter UK post code. Mandatory question for UK address.;
      |Enter non-UK address Line 1. Should be letters A to Z, numbers 0 to 9. Hyphens and speech marks are accepted. Maximum number of characters is 35. Mandatory question if non-UK address.;
      |Enter non-UK address Line 2. This is an optional field. Should be letters A to Z, numbers 0 to 9. Hyphens and speech marks are accepted. Maximum number of characters is 35.;
      |Enter the non-UK address Line 3. This is an optional field. Should be letters A to Z, numbers 0 to 9. Hyphens and speech marks are accepted. Maximum number of characters is 35.;
      |Enter the non-UK address line 4. This is an optional field. Should be letters A to Z, numbers 0 to 9. Hyphens and speech marks are accepted. Maximum number of characters is 35.;
      |Enter the name of the non-UK country  Should be letters A to Z, numbers 0 to 9. Hyphens and speech marks are accepted. Mandatory question if non-UK address.;
      |Enter YES or NO. Mandatory question.;
      |Max of 160 characters. Mandatory field if there is no Land Registry Reference.;
      |Enter name. Max 160 characters  Mandatory Question.;
      |Enter the total amount in GBP (in pounds and pence). Include stamp duty and other costs related to the transaction. If the land or property was not an acquisition, provide the total value. Mandatory question.;
      |Enter YES or NO  Mandatory question.;
      |Enter YES or NO. Mandatory question.;
      |Enter number of joint owners of property.;
      |Enter YES or NO. Mandatory question;
      |Enter YES or NO. Mandatory question.;
      |Enter names of lessees.;
      |Enter YES or NO.;
      |Use the format DD-MM-YYYY.;
      |Enter the annual lease amount in GBP (pounds and pence).;
      |Enter the total amount in GBP (pounds and pence). Enter the total of all land or property for that member. If multiple properties - enter the total on one the last row that relates to that individual member, do not repeat the amount on all rows relating to the member.;
      |Enter YES or NO  Mandatory field.;
      |Enter the total amount in GBP (pounds and pence). Mandatory if disposals were made.;
      |Enter names of purchasers.;
      |Enter YES or NO.;
      |Enter YES or NO. Mandatory if any disposal of the land or property was made during the year;
      |Enter YES or NO  Mandatory if any disposal of the land or property was made during the year;
      |ERRORS WITH DETAIL
      |""".stripMargin

  val headersForOutstandingLoans =
    """The questions in this section relate to outstanding loans. You must tell us about all outstanding loans were made or were outstanding at any point during the period of this return. What you need to do Complete the questions per member marked horizontally across the columns. For members that have multiple loans, complete one row per loan and repeat the members first name, last name and date of birth for the required number of rows. Notes and hint text is underneath each question to help make sure that there are no errors in the template file upload. Mandatory questions are marked in the hint text.;
      |First name of scheme member;
      |Last name of scheme member;
      |Member date of birth;
      |How many separate loans were made or outstanding during the tax year and not reported in a previous return for this member?;
      |What is the name of the recipient of loan?;
      |Is the recipient of the loan an individual or company or partnership or other;
      |If the recipient of the loan was an individual, enter the individuals National Insurance Number;
      |If the recipient of the loan was a company, enter the CRN details;
      |If the recipient of the loan was a partnership, enter the UTR details;
      |Add the reason you do not have the individuals National Insurance number, the CRN or the UTR. Or if the loan was acquired from another source, enter the details;
      |What was the date of loan?;
      |What is the amount of the loan?;
      |Is the loan associated with a connected party?;
      |What is the repayment date of the loan?;
      |What is the Interest Rate for the loan?;
      |Has security been given for the loan?;
      |In respect of this loan, what Capital Repayments have been received by the scheme during the year?;
      |In respect of this loan, what interest payments have been received by the scheme during the year?;
      |In respect of this loan, are there any arrears outstanding from previous years;
      |In respect of this loan, what is the amount outstanding at the year end?;
      |ERRORS WITH DETAIL
      |""".stripMargin

  val headersForAssetFromConnectedParty =
    """The questions in this section relate to outstanding loans. You must tell us about all outstanding loans were made or were outstanding at any point during the period of this return. What you need to do Complete the questions per member marked horizontally across the columns. For members that have multiple loans, complete one row per loan and repeat the members first name, last name and date of birth for the required number of rows. Notes and hint text is underneath each question to help make sure that there are no errors in the template file upload. Mandatory questions are marked in the hint text.;
      |First name of scheme member;
      |Last name of scheme member;
      |Member date of birth;
      |Member National Insurance number;
      |If no National Insurance number for member, give reason;
      |How many asset transactions were made during the tax year and not reported in a previous return for this member?;
      |What was the date the asset was acquired?;
      |Description of asset;
      |Was this an acquisition of shares?;
      |If yes, What is the name of the company to which the shares relate?;
      |What is the CRN of the company that has the shares?;
      |If no CRN, enter reason you do not have this;
      |What are the class of shares?;
      |What are the total number of shares acquired or received/held in respect of this transaction?;
      |Who was the asset acquired from?;
      |What was the total cost of the asset acquired? ;
      |Is this transaction supported by an Independent Valuation;
      |Is any part of the asset Tangible Moveable Property as defined by Schedule 29a Finance Act 2004?;
      |What is the total amount of income and receipts received in respect of the asset during tax year;
      |During the year was any disposal of the asset made?;
      |If disposals were made what is the total amount of consideration received from the sale or disposal of the asset?;
      |Names of purchasers;
      |Are any of the purchasers connected parties?;
      |Was the transaction supported by an independent valuation?;
      |Was there disposal of shares?;
      |If there were disposals of shares what is the total number of shares now held;
      |If no disposals of shares were made has the asset been fully disposed of?;
      |ERRORS WITH DETAIL
      |""".stripMargin

  val questionHelpersAssetFromConnectedParty =
    """Question help information. This will give you hints or tips to help you to complete the required cells.;
      |Enter the first name of the scheme member. Hyphens are accepted.Mandatory question.;
      |Enter the last name of the scheme member. Hyphens are accepted.Mandatory question.;
      |Use the format DD-MM-YYYY. Mandatory question.;
      |Enter the individuals National Insurance Number. If you do not know this, add the reason why you do not have this in the next column. 9 characters in alphanumeric in the format for example: AA999999A. Mandatory question.;
      |Numerical. Mandatory question.;
      |Use the format DD-MM-YYY Mandatory question.;
      |Mandatory question. Max of 160 characters.;
      |Enter YES or NO. Mandatory question.;
      |Max of 160 characters;
      |Enter the Company Registration Number (CRN). If you do not know this, add the reason why you do not have this in the next column.;
      |Add the reason why you do not have the CRN. Maximum of 160 characters.;
      |Max of 160 characters. Mandatory question.;
      |Enter number. Mandatory question.;
      |Enter name or details. Max 160 characters. Mandatory Question;
      |Enter the total amount in GBP (in pounds and pence). Enter amount as whole number and to two decimal places. Mandatory question.;
      |Enter YES or NO. Mandatory question;
      |Enter YES or NO. Mandatory question;
      |Enter the total amount in GBP (pounds and pence). Mandatory question.;
      |Enter YES or NO. Mandatory question.;
      |Enter the total amount in GBP (pounds and pence);
      |Enter name or details. Max 160 characters. ;
      |Enter YES or NO;
      |Enter YES or NO;
      |Enter YES or NO.;
      |Min 1 - Max 7. Characters of 0-9;
      |Enter YES or NO. Mandatory question.;
      |ERRORS WITH DETAIL
      |""".stripMargin

  val questionHelpersForOutstandingLoans =
    """Question help information. This will give you hints or tips to help you to complete the required cells.;
      |Enter the first name of the scheme member. Hyphens are accepted.Mandatory question.;
      |Enter the last name of the scheme member. Hyphens are accepted.Mandatory question.;
      |Use the format DD-MM-YYYY. Mandatory question.;
      |Enter number. Mandatory question.;
      |Enter name. Maximum 160 characters. Mandatory question;
      |Enter individual or company or partnership or other. Mandatory question.;
      |Enter the individuals National Insurance Number using the format AB123456A. If you do not know this, add the reason why you do not have this in the relevant column.;
      |Enter Company Registration Number (CRN). If you do not know this, add the reason why you do not have this in the relevant column.;
      |Enter unique tax reference (UTR). If you do not know this, add the reason why you do not have this in the relevant column.;
      |If aquired from another source: Enter the details. If aquired from an individual but you don't have this: Enter reason for not having the individuals National Insurance Number. If aquired from a company but don't have the CRN: Enter reason for not having the CRN. If aquired from a partnership but don't have the UTR: Enter reason for not having the UTR. Maximum 160 characters.;
      |Use the format DD-MM-YYYY. Mandatory question.;
      |Enter the total amount in GBP (pounds and pence). Enter amount as whole number and to two decimal places Mandatory question.;
      |Enter YES or NO. Mandatory question.;
      |Use the format DD-MM-YYYY. Mandatory question.;
      |Enter the % Enter percentage as whole number and to two decimal places Mandatory question.;
      |Enter YES or NO. Mandatory question.;
      |Enter the amount in GBP (pounds and pence) Enter amount as whole number and to two decimal places;
      |Enter the amount in GBP (pounds and pence) Enter amount as whole number and to two decimal places;
      |Enter YES or NO. Mandatory question.;
      |Enter the amount in GBP (pounds and pence) Enter amount as whole number and to two decimal places;
      |ERRORS WITH DETAIL
      |""".stripMargin
}
