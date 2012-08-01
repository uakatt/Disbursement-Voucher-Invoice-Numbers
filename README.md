Disbursement Voucher Invoice Numbers
======================

This git repository represents the University of Arizona (the UA)'s _Disbursement Voucher Invoice Numbers_ modification to **KFS 3.0**, in the form of patch files (generated by svn diff), liquibase scripts, and documentation.
This "patch package" is designed to be informative to technical developers in the position to
apply patch files to the java source code of KFS. In order to better serve such an endeavor,
this README contains several informative sections:

* <a href="#jiras">List of Jiras</a> - This list contains every Jira ticket at the University of Arizona
  that relates to this modification. It provides reverse documentation back to the developers at
  the UA in case of questions regarding how this patch package was created.
* <a href="#liquibase-changesets">List of Liquibase Changesets</a> - This list contains any
  liquibase changeset files that were associated with this modification.
* <a href="#patch-files">List of Patch Files</a> - This is a list of each patch file that needs
  to be applied to the KFS source code in order to realize the modification. This list does _not_
  include patch files for revisions that didn't touch the `trunk/` at the UA.
  Before a modification was merged with `trunk/`, it may have been tweaked, reworked, refactored,
  code reviewed, etc, in handfuls of revisions in a feature branch.
* <a href="#revisions">List of Revisions</a> - This list contains every revision associated with
  this modification. Many of which, as you will see, only touch files in a feature branch. The
  revisions that actually made it into the actual modification touch files in `trunk/`. The list
  of patch files is a better reference of which are these revisions.
* <a href="#files">Lists of Files</a> - These lists contain every file that was created,
  modified, or deleted for this enhancement.
* <a href="#extra-local-files">List of Local Files</a> - This list contains any extra files that were not
  explicitly included in the enhancement. The enhancement may rely on these files, and they may
  not be a part of the original release. They may also include functional and technical specifications.
* <a href="#known-issues">List of Known Issues</a> - This list contains known issues related to
  this enhancement.

Disclaimer
==========

> The permissions SQL that allow FSO AP Travel Specialists to edit the invoice number on DVs is included in the <a href="#extra-local-files">Extra Local Files</a> list.

<h2>Jiras</h2>

This is a list of Jira tickets at the University of Arizona that relate to this modification. The subversion revisions tagged against each such jira are also listed:

* **KFSI-653**: (The main jira for this modification)<br />
  revisions: #24826, #24840

<h2>Liquibase Changesets</h2>

* `update/KITT-3083.xml` saw the following activity:
  * created in #24826 (resultant file: [`whole_files/r24826/KITT-3083.xml`](Disbursement-Voucher-Invoice-Numbers/blob/master/whole_files/r24826/KITT-3083.xml)).

(1 changes among 1 files)

<h2>Patch Files</h2>

This is a list of all of the patches for revisions that affected files in `trunk/`. The filenames in each has been modified, for easy digestion. UA's subversion server manages many Kuali projects in one Subversion project, so a file path like:

```
financial-system/kfs/trunk/src/org/kuali/kfs...
```

has been modified to:

```
src/org/kuali/kfs...
```

* [`patches/24840_KFSI-653_cleaned.diff`](Disbursement-Voucher-Invoice-Numbers/blob/master/patches/24840_KFSI-653_cleaned.diff) is the patch file for #24840.

<h2>Revisions</h2>

This is an ordered list of revisions that relate to this modification. There may not be a patch
file for every revision listed below for the following reasons:

* A revision might only affect a branch, perhaps one where development primarily took place. Any
  changes that finally made it into `trunk/` will be seen in revisions that specifically touch
  files in `trunk/`. Therefore, we do not create patch files for revisions that only affect a
  branch.
* A revision might only include a liquibase changeset that is executed by some automated process.
  Since each institution maintains different revision controls on their database, liquibase
  changesets are not provided as patches. They are instead presented as intact files under the
  `liquibase-changesets/` directory.

[Here](Disbursement-Voucher-Invoice-Numbers/blob/master/patch_log.txt) is a printout of `svn log -v` for each revision.

*   \#24840 was committed against KFSI-653 on 2012-03-20 22:25:32 UTC by <strong>wliang@CATNET.ARIZONA.EDU</strong>.

    > KFSI-653 KITT-3043 add duplicate invoice number checking to DV and PREQ will check for duplicate DV numbers

<h2>Files</h2>

Files **created** for this modification (8 files)

    /work/src/edu/arizona/kfs/fp/FPKeyConstants.java
    /work/src/edu/arizona/kfs/fp/businessobject/DisbursementVoucherSourceAccountingLine.java
    /work/src/edu/arizona/kfs/fp/businessobject/DisbursementVoucherSourceAccountingLineExtension.java
    /work/src/edu/arizona/kfs/fp/businessobject/datadictionary/DisbursementVoucherSourceAccountingLine.xml
    /work/src/edu/arizona/kfs/fp/businessobject/datadictionary/DisbursementVoucherSourceAccountingLineExtension.xml
    /work/src/edu/arizona/kfs/fp/document/validation/impl/DisbursementVoucherInvoiceNumberEnteredValidation.java
    /work/src/edu/arizona/kfs/fp/service/UaDisbursementVoucherInvoiceService.java
    /work/src/edu/arizona/kfs/fp/service/impl/UaDisbursementVoucherInvoiceServiceImpl.java

Files **modified** for this modification (11 files)

    /.classpath
    /work/src/arizona-ApplicationResources.properties
    /work/src/edu/arizona/kfs/fp/document/UaDisbursementVoucherDocument.java
    /work/src/edu/arizona/kfs/fp/document/datadictionary/DisbursementVoucherDocument.xml
    /work/src/edu/arizona/kfs/fp/document/validation/configuration/DisbursementVoucherValidation.xml
    /work/src/edu/arizona/kfs/fp/document/validation/impl/UaDisbursementVoucherDocumentPreRules.java
    /work/src/edu/arizona/kfs/fp/ojb-fp.xml
    /work/src/edu/arizona/kfs/fp/spring-fp.xml
    /work/src/edu/arizona/kfs/module/purap/AzPurapConstants.java
    /work/src/edu/arizona/kfs/module/purap/UaPurapKeyConstants.java
    /work/src/edu/arizona/kfs/module/purap/document/web/struts/PaymentRequestAction.java

<h2>Extra Local Files</h2>

Extra local files for this modification that have been included for some reason or another. Most likely, these files are not code, but instead might be specifications, or other documentation. (1 files)

* [`extra_files/permissions.sql`](Disbursement-Voucher-Invoice-Numbers/blob/master/extra_files/permissions.sql)

<h2>Known Issues</h2>

These are issues, related to this enhancement, that are currently documented and open at the UA. the UA may be contacted for more information regarding each issue (Has it been prioritized? Has work begun? How to recreate? ...). Any institution receiving this enhancement is also welcome to fix any known issue and contribute back to the UA. :) (3 known issues)

* **KATTS-179:** "ACH and check payments made via Disbursement Vouchers do not have invoice numbers on remittance"
  <br />
  Resolution as of Aug  1, 2012: Unresolved
  <br />
  Priority as of Aug  1, 2012: Critical
* **KATTS-507:** "Search for Disbursement Vouchers in KFS and PDP should include the Invoice Number field"
  <br />
  Resolution as of Aug  1, 2012: Unresolved
  <br />
  Priority as of Aug  1, 2012: Critical
* **KATTS-424:** "Need to update our DV Invoice Mod to use the Purchasing Module service to check for duplicate invoices. "
  <br />
  Resolution as of Aug  1, 2012: Unresolved
  <br />
  Priority as of Aug  1, 2012: Major

