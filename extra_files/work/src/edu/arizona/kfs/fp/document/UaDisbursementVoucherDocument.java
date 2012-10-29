/*
 * Copyright 2009 The Kuali Foundation.
 * 
 * Licensed under the Educational Community License, Version 1.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 * 
 * http://www.opensource.org/licenses/ecl1.php
 * 
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package edu.arizona.kfs.fp.document;

import org.apache.commons.lang.StringUtils;
import org.apache.log4j.Logger;
import org.kuali.kfs.fp.document.DisbursementVoucherConstants;
import org.kuali.kfs.fp.document.DisbursementVoucherDocument;
import org.kuali.kfs.sys.KFSConstants;
import org.kuali.kfs.sys.KFSPropertyConstants;
import org.kuali.kfs.sys.businessobject.AccountingLine;
import org.kuali.kfs.sys.businessobject.Bank;
import org.kuali.kfs.sys.businessobject.GeneralLedgerPendingEntry;
import org.kuali.kfs.sys.businessobject.GeneralLedgerPendingEntrySequenceHelper;
import org.kuali.kfs.sys.businessobject.GeneralLedgerPendingEntrySourceDetail;
import org.kuali.kfs.sys.businessobject.SourceAccountingLine;
import org.kuali.kfs.sys.context.SpringContext;
import org.kuali.kfs.vnd.businessobject.VendorAddress;
import org.kuali.kfs.vnd.businessobject.VendorDetail;
import org.kuali.rice.kew.dto.DocumentRouteStatusChangeDTO;
import org.kuali.rice.kew.util.KEWConstants;
import org.kuali.rice.kns.document.authorization.DocumentAuthorizer;
import org.kuali.rice.kns.document.authorization.TransactionalDocumentPresentationController;
import org.kuali.rice.kns.service.BusinessObjectService;
import org.kuali.rice.kns.service.DocumentHelperService;
import org.kuali.rice.kns.service.ParameterConstants.COMPONENT;
import org.kuali.rice.kns.util.GlobalVariables;
import org.kuali.rice.kns.util.KNSConstants;
import org.kuali.rice.kns.util.ObjectUtils;

import edu.arizona.kfs.fp.businessobject.DisbursementVoucherSourceAccountingLineExtension;
import edu.arizona.kfs.fp.businessobject.PaymentMethod;
import edu.arizona.kfs.fp.document.validation.impl.DisbursementVoucherHasNonAscii;
import edu.arizona.kfs.fp.service.UaPaymentMethodGeneralLedgerPendingEntryService;
import edu.arizona.kfs.sys.AZKFSConstants;
import edu.arizona.kfs.vnd.businessobject.VendorDetailExtension;

/**
 * Document class override to ensure that the bank code is synchronized with the
 * payment method code.
 * 
 * @author jonathan
 */
// This annotation is needed to make parameter lookups work properly
@COMPONENT( component = "DisbursementVoucher" )
public class UaDisbursementVoucherDocument extends DisbursementVoucherDocument {
    private static Logger LOG = Logger.getLogger(UaDisbursementVoucherDocument.class);

    public static final String DOCUMENT_TYPE_DV_NON_CHECK = "DVNC";
    
    private static UaPaymentMethodGeneralLedgerPendingEntryService paymentMethodGeneralLedgerPendingEntryService;

    @Override
    public void prepareForSave() {
        super.prepareForSave();
        
        DocumentHelperService documentHelperService = SpringContext.getBean(DocumentHelperService.class);
        DocumentAuthorizer docAuth = documentHelperService.getDocumentAuthorizer(this);

        // First, only do this if the document is in initiated status - after that, we don't want to 
        // accidentally reset the bank code
        if ( KEWConstants.ROUTE_HEADER_INITIATED_CD.equals( getDocumentHeader().getWorkflowDocument().getRouteHeader().getDocRouteStatus() )
                || KEWConstants.ROUTE_HEADER_SAVED_CD.equals( getDocumentHeader().getWorkflowDocument().getRouteHeader().getDocRouteStatus() ) ) {
            // need to check whether the user has the permission to edit the bank code
            // if so, don't synchronize since we can't tell whether the value coming in
            // was entered by the user or not.        
            if ( !docAuth.isAuthorizedByTemplate(this, 
                    KFSConstants.ParameterNamespaces.KFS, 
                    KFSConstants.PermissionTemplate.EDIT_BANK_CODE.name, 
                    GlobalVariables.getUserSession().getPrincipalId()  ) ) {
                synchronizeBankCodeWithPaymentMethod();        
            } else {
                refreshReferenceObject( "bank" );
            }
        } 
        else{           
            TransactionalDocumentPresentationController presentationController = (TransactionalDocumentPresentationController) documentHelperService.getDocumentPresentationController(this);
            if(presentationController.getEditModes(this).contains(AZKFSConstants.Authorization.PAYMENT_METHOD_EDIT_MODE)){
                synchronizeBankCodeWithPaymentMethod();
            }
        }
        
        // KFSI-653 KITT-3043 need to initialize extension primary key values because OJB doesn't do a good job
        for (Object o : getSourceAccountingLines()) {
            SourceAccountingLine accountingLine = (SourceAccountingLine) o;
            DisbursementVoucherSourceAccountingLineExtension accountingLineExtension = (DisbursementVoucherSourceAccountingLineExtension) accountingLine.getExtension();
            accountingLineExtension.setDocumentNumber(accountingLine.getDocumentNumber());
            accountingLineExtension.setSequenceNumber(accountingLine.getSequenceNumber());
        }
    }

    protected void synchronizeBankCodeWithPaymentMethod() {
        Bank bank = getPaymentMethodGeneralLedgerPendingEntryService().getBankForPaymentMethod( getDisbVchrPaymentMethodCode() );
        if ( bank != null ) {
            if ( !StringUtils.equals(bank.getBankCode(), getDisbVchrBankCode()) ) {
                setDisbVchrBankCode(bank.getBankCode());
                refreshReferenceObject( "bank" );
            }
        } else {
            // no bank code, no bank needed
            setDisbVchrBankCode(null);
            setBank(null);
        }
    }
    
    /**
     * Override to change the doc type based on payment method. This is needed to pick up different offset definitions.
     * 
     * MOD-PA2000-01
     * Replacing baseline method completely since has an else clause which needs to be replaced.
     * 
     * @param financialDocument submitted accounting document
     * @param accountingLine accounting line in submitted accounting document
     * @param explicitEntry explicit GLPE
     * @see org.kuali.module.financial.rules.FinancialDocumentRuleBase#customizeExplicitGeneralLedgerPendingEntry(org.kuali.rice.kns.document.FinancialDocument,
     *      org.kuali.rice.kns.bo.AccountingLine, org.kuali.module.gl.bo.GeneralLedgerPendingEntry)
     */
    @Override
    public void customizeExplicitGeneralLedgerPendingEntry(GeneralLedgerPendingEntrySourceDetail accountingLine, GeneralLedgerPendingEntry explicitEntry) {

        /* change document type based on payment method to pick up different offsets */
        if ( getPaymentMethodGeneralLedgerPendingEntryService().isPaymentMethodProcessedUsingPdp(getDisbVchrPaymentMethodCode())) {
            explicitEntry.setFinancialDocumentTypeCode(DisbursementVoucherConstants.DOCUMENT_TYPE_CHECKACH);
        } else { // wire transfer or foreign draft
            explicitEntry.setFinancialDocumentTypeCode(DOCUMENT_TYPE_DV_NON_CHECK);
        }
        if ( LOG.isDebugEnabled() ) {
            LOG.debug("changed doc type on pending entry " + explicitEntry.getTransactionLedgerEntrySequenceNumber() + " to " + explicitEntry.getFinancialDocumentTypeCode() );
        }
    }

    
    /**
     * Generates additional document-level GL entries for the DV, depending on the payment method code. 
     * 
     * Return true if GLPE's are generated successfully (i.e. there are either 0 GLPE's or 1 GLPE in disbursement voucher document)
     * 
     * @param financialDocument submitted financial document
     * @param sequenceHelper helper class to keep track of GLPE sequence
     * @return true if GLPE's are generated successfully
     * @see org.kuali.rice.kns.rule.GenerateGeneralLedgerDocumentPendingEntriesRule#processGenerateDocumentGeneralLedgerPendingEntries(org.kuali.rice.kns.document.FinancialDocument,org.kuali.kfs.sys.businessobject.GeneralLedgerPendingEntrySequenceHelper)
     */
    @Override
    public boolean generateDocumentGeneralLedgerPendingEntries(GeneralLedgerPendingEntrySequenceHelper sequenceHelper) {
        if (getGeneralLedgerPendingEntries() == null || getGeneralLedgerPendingEntries().size() < 2) {
            LOG.warn("No gl entries for accounting lines.");
            return true;
        }
        // waive any fees only for wire charges when the waiver flag is set
        boolean feesWaived = DisbursementVoucherConstants.PAYMENT_METHOD_WIRE.equals(getDisbVchrPaymentMethodCode()) 
                && getDvWireTransfer().isDisbursementVoucherWireTransferFeeWaiverIndicator();

        getPaymentMethodGeneralLedgerPendingEntryService().generatePaymentMethodSpecificDocumentGeneralLedgerPendingEntries(this,getDisbVchrPaymentMethodCode(), getDisbVchrBankCode(),KNSConstants.DOCUMENT_PROPERTY_NAME + "." + KFSPropertyConstants.DISB_VCHR_BANK_CODE, getGeneralLedgerPendingEntry(0), feesWaived, false, sequenceHelper);
        
        return true;
    }
    
    /**
     * Returns the name associated with the payment method code
     * 
     * @return String
     */
    public String getDisbVchrPaymentMethodName() {
        if ( getPaymentMethod() != null ) {
            return getDisbVchrPaymentMethodCode() + " - " + getPaymentMethod().getPaymentMethodName();
        }
        return getDisbVchrPaymentMethodCode();
    }
    
    protected PaymentMethod paymentMethod;
    
    public PaymentMethod getPaymentMethod() {
        if ( paymentMethod == null || !StringUtils.equals( paymentMethod.getPaymentMethodCode(), getDisbVchrPaymentMethodCode() ) ) {
            paymentMethod = SpringContext.getBean(BusinessObjectService.class).findBySinglePrimaryKey(PaymentMethod.class, getDisbVchrPaymentMethodCode());
        }
        return paymentMethod;
    }
    
    protected UaPaymentMethodGeneralLedgerPendingEntryService getPaymentMethodGeneralLedgerPendingEntryService() {
        if ( paymentMethodGeneralLedgerPendingEntryService == null ) {
            paymentMethodGeneralLedgerPendingEntryService = SpringContext.getBean(UaPaymentMethodGeneralLedgerPendingEntryService.class);
        }
        return paymentMethodGeneralLedgerPendingEntryService;
    }
    
    /** KITT-592
     * 
     * Update to baseline method to additionally set the payment method when a vendor is selected.
     * 
     */
    @Override
    public void templateVendor(VendorDetail vendor, VendorAddress vendorAddress) {
        super.templateVendor(vendor, vendorAddress);
        if ( vendor != null ) {
            if ( ObjectUtils.isNotNull( vendor.getExtension() ) 
                    && vendor.getExtension() instanceof VendorDetailExtension ) {
                if ( StringUtils.isNotBlank(((VendorDetailExtension)vendor.getExtension()).getDefaultB2BPaymentMethodCode())) {
                    disbVchrPaymentMethodCode = ((VendorDetailExtension)vendor.getExtension()).getDefaultB2BPaymentMethodCode();
                    // Ensure the bank code now matches the new payment method code
                    synchronizeBankCodeWithPaymentMethod();
                }
            }
        }
    }

    /**
     * @see org.kuali.kfs.sys.document.GeneralLedgerPostingDocumentBase#doRouteStatusChange(org.kuali.rice.kew.dto.DocumentRouteStatusChangeDTO)
     */
    @Override
    public void doRouteStatusChange(DocumentRouteStatusChangeDTO statusChangeEvent) {
        super.doRouteStatusChange(statusChangeEvent);
        
        // KFSI-5154 KITT-2899 set the paid date when the DV will not go through PDP 
        if (getDocumentHeader().getWorkflowDocument().stateIsProcessed()) {
            if (!getPaymentMethodGeneralLedgerPendingEntryService().isPaymentMethodProcessedUsingPdp(getDisbVchrPaymentMethodCode())) {
                setPaidDate(getDateTimeService().getCurrentSqlDate());
            }
        }
    }

    @Override
    public boolean answerSplitNodeQuestion(String nodeName) throws UnsupportedOperationException {
        if (DisbursementVoucherHasNonAscii.SPLIT_ROUTE_NODE_TO_CHECK_FOR_NONASCII.equals(nodeName)) {
            return doesDvCheckStubTextContainNonAscii();
        } else {
            return super.answerSplitNodeQuestion(nodeName);
        }
    }
    
    protected boolean doesDvCheckStubTextContainNonAscii() {
        for (int i = 0; i < super.disbVchrCheckStubText.length() ; i++) {
            char testChar = super.disbVchrCheckStubText.charAt(i);
            int testInt = (int)testChar;
            if (testInt > 127) {
                return true;
            }
        }
        return false;
    }
    
}
