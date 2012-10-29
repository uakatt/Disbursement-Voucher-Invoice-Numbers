/*
 * Copyright 2010 The Kuali Foundation.
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
package edu.arizona.kfs.fp.document.validation.impl;

import java.text.MessageFormat;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.TreeSet;

import javax.mail.Session;

import org.apache.commons.lang.StringUtils;
import org.kuali.kfs.fp.businessobject.DisbursementVoucherPayeeDetail;
import org.kuali.kfs.fp.businessobject.DisbursementVoucherWireTransfer;
import org.kuali.kfs.fp.document.DisbursementVoucherConstants;
import org.kuali.kfs.fp.document.DisbursementVoucherDocument;
import org.kuali.kfs.fp.document.validation.impl.DisbursementVoucherDocumentPreRules;
import org.kuali.kfs.module.purap.PurapConstants;
import org.kuali.kfs.module.purap.document.PaymentRequestDocument;
import org.kuali.kfs.sys.KFSConstants;
import org.kuali.kfs.sys.KFSKeyConstants;
import org.kuali.kfs.sys.KFSPropertyConstants;
import org.kuali.kfs.sys.businessobject.SourceAccountingLine;
import org.kuali.kfs.sys.context.SpringContext;
import org.kuali.rice.kns.document.Document;
import org.kuali.rice.kns.service.BusinessObjectService;
import org.kuali.rice.kns.service.DataDictionaryService;
import org.kuali.rice.kns.service.KualiConfigurationService;
import org.kuali.rice.kns.util.ObjectUtils;
import org.kuali.rice.kns.workflow.service.KualiWorkflowDocument;

import edu.arizona.kfs.fp.FPKeyConstants;
import edu.arizona.kfs.fp.businessobject.DisbursementVoucherSourceAccountingLine;
import edu.arizona.kfs.fp.businessobject.DisbursementVoucherSourceAccountingLineExtension;
import edu.arizona.kfs.fp.document.UaDisbursementVoucherDocument;
import edu.arizona.kfs.fp.service.UaDisbursementVoucherInvoiceService;

public class UaDisbursementVoucherDocumentPreRules extends DisbursementVoucherDocumentPreRules {
    protected static final String DUPLICATE_INVOICE_QUESTION_ID = "DVDuplicateInvoice";
    
    private UaDisbursementVoucherInvoiceService disbursementVoucherInvoiceService = null;
    
    @Override
    public boolean doPrompts(Document document) {
        boolean result = super.doPrompts(document);
        
        // KFSI-2726
        // check the payment method - if not processed by PDP, we need to ensure
        // that the fields which would cause the payment to be pulled out specially
        // are not changed
        if ( document instanceof UaDisbursementVoucherDocument ) {
            if ( ObjectUtils.isNotNull( ((UaDisbursementVoucherDocument)document).getPaymentMethod() ) ) {
                if ( !((UaDisbursementVoucherDocument)document).getPaymentMethod().isProcessedUsingPdp() ) {
                    ((UaDisbursementVoucherDocument)document).setDisbVchrAttachmentCode( false );
                    ((UaDisbursementVoucherDocument)document).setDisbVchrSpecialHandlingCode( false );
                }
            }
        }
        
        result &= checkInvoiceNumberDuplicate((DisbursementVoucherDocument) document);
        
        return result;
    }
    
    protected boolean checkInvoiceNumberDuplicate(DisbursementVoucherDocument document) {
        try {
            // pre-analyze the question to prevent unncessary DB retrievals
            boolean okToProceed = super.askOrAnalyzeYesNoQuestion(DUPLICATE_INVOICE_QUESTION_ID, "");
            // if the question was not answered yet, an exception will be thrown by the above line
            if (!okToProceed) {
                abortRulesCheck();
            }
            return true;
        }
        catch (RuntimeException e) {
            // catch the IsAskingException to prevent the system from asking the question now... instead we'll continue processing so that it'll ask the question
        }

        KualiWorkflowDocument wfDoc = document.getDocumentHeader().getWorkflowDocument();
        if (!wfDoc.stateIsInitiated() && !wfDoc.stateIsSaved() && 
                !(wfDoc.stateIsEnroute() && wfDoc.isApprovalRequested() && wfDoc.getCurrentRouteNodeNames().contains(DisbursementVoucherConstants.RouteLevelNames.CAMPUS))) {
            return true;
        }
        
        boolean result = true;
        Set<String> matchingPreqs = new HashSet<String>();
        Set<String> matchingDvs = new HashSet<String>();
        for (DisbursementVoucherSourceAccountingLine sourceAccountingLine : (List<DisbursementVoucherSourceAccountingLine>) document.getSourceAccountingLines()) {
            DisbursementVoucherSourceAccountingLineExtension extension = sourceAccountingLine.getExtension();
            String invoiceNumber = extension.getInvoiceNumber();
            if (StringUtils.isNotBlank(invoiceNumber)) {
                matchingPreqs.addAll(findDuplicatePaymentRequests(document, invoiceNumber));
                matchingDvs.addAll(findDuplicateDisbursementVouchers(document, invoiceNumber));
            }
        }

        if (!matchingDvs.isEmpty() || !matchingPreqs.isEmpty()) {
            String questionText = SpringContext.getBean(KualiConfigurationService.class).getPropertyString(FPKeyConstants.MESSAGE_DV_DUPLICATE_INVOICE);

            Object[] args = { toCommaDelimitedString(matchingDvs), toCommaDelimitedString(matchingPreqs) };
            questionText = MessageFormat.format(questionText, args);
            
            boolean okToProceed = super.askOrAnalyzeYesNoQuestion(DUPLICATE_INVOICE_QUESTION_ID, questionText);
            
            if (!okToProceed) {
                abortRulesCheck();
            }
        }
        return true;
    }
    
    protected String toCommaDelimitedString(Set<String> documentIds) {
        if (documentIds == null || documentIds.isEmpty()) {
            return "N/A";
        }
        return StringUtils.join(documentIds, ",");
    }

    protected Set<String> findDuplicateDisbursementVouchers(DisbursementVoucherDocument document, String invoiceNumber) {
        DisbursementVoucherPayeeDetail payeeDetail = document.getDvPayeeDetail();
        return getDisbursementVoucherInvoiceService().findDisbursementVouchersWithDuplicateInvoices(document.getDocumentNumber(),
                payeeDetail.getDisbVchrPayeeIdNumber(), payeeDetail.getDisbursementVoucherPayeeTypeCode(), invoiceNumber);
    }
    
    protected Set<String> findDuplicatePaymentRequests(DisbursementVoucherDocument document, String invoiceNumber) {
        DisbursementVoucherPayeeDetail payeeDetail = document.getDvPayeeDetail();
        if (payeeDetail.isVendor()) {
            String vendorHeaderGeneratedIdentifier = payeeDetail.getDisbVchrVendorHeaderIdNumber();
            String vendorDetailAssignedIdentifier = payeeDetail.getDisbVchrVendorDetailAssignedIdNumber();
            return getDisbursementVoucherInvoiceService().findPaymentRequestsWithDuplicateInvoices(vendorHeaderGeneratedIdentifier, vendorDetailAssignedIdentifier, invoiceNumber);
        }
        return Collections.emptySet();
    }

    /**
     * Returns true if the state of all the tabs is valid, false otherwise.
     * 
     * @param dvDocument submitted disbursemtn voucher document
     * @return true if the state of all the tabs is valid, false otherwise.
     */
    @Override
    protected boolean checkForeignDraftTabState(DisbursementVoucherDocument dvDocument) {
        boolean tabStatesOK = true;

        DisbursementVoucherWireTransfer dvForeignDraft = dvDocument.getDvWireTransfer();

        // if payment method is CHECK and wire tab contains data, ask user to clear tab
        if ( !StringUtils.equals(DisbursementVoucherConstants.PAYMENT_METHOD_DRAFT, dvDocument.getDisbVchrPaymentMethodCode())
                && hasForeignDraftValues(dvForeignDraft)) {
            String questionText = SpringContext.getBean(KualiConfigurationService.class).getPropertyString(KFSKeyConstants.QUESTION_CLEAR_UNNEEDED_TAB);

            Object[] args = { "payment method", dvDocument.getDisbVchrPaymentMethodCode(), "Foreign Draft", DisbursementVoucherConstants.PAYMENT_METHOD_DRAFT };
            questionText = MessageFormat.format(questionText, args);

            boolean clearTab = super.askOrAnalyzeYesNoQuestion(KFSConstants.DisbursementVoucherDocumentConstants.CLEAR_FOREIGN_DRAFT_TAB_QUESTION_ID, questionText);
            if (clearTab) {
                // NOTE: Can't replace with new instance because Wire Transfer uses same object
                clearForeignDraftValues(dvForeignDraft);
            }
            else {
                // return to document if the user doesn't want to clear the Wire Transfer tab
                super.event.setActionForwardName(KFSConstants.MAPPING_BASIC);
                tabStatesOK = false;
            }
        }

        return tabStatesOK;
    }
    
    /**
     * This method returns true if the state of all the tabs is valid, false otherwise.
     * 
     * @param dvDocument submitted disbursement voucher document
     * @return Returns true if the state of all the tabs is valid, false otherwise.
     */
    @Override
    protected boolean checkWireTransferTabState(DisbursementVoucherDocument dvDocument) {
        boolean tabStatesOK = true;

        DisbursementVoucherWireTransfer dvWireTransfer = dvDocument.getDvWireTransfer();

        // if payment method is not W (Wire Transfer) and wire tab contains data, ask user to clear tab
        
        // NOTE: This is lousy - but there is no distinction in the payment method table 
        // between wire transfers and foreign drafts.  So, we still need the hard-coded
        // values of those payment methods here for business rules.
        if (!StringUtils.equals(DisbursementVoucherConstants.PAYMENT_METHOD_WIRE, dvDocument.getDisbVchrPaymentMethodCode()) 
                && hasWireTransferValues(dvWireTransfer)) { 
            String questionText = SpringContext.getBean(KualiConfigurationService.class).getPropertyString(KFSKeyConstants.QUESTION_CLEAR_UNNEEDED_TAB);

            Object[] args = { "payment method", dvDocument.getDisbVchrPaymentMethodCode(), "Wire Transfer", DisbursementVoucherConstants.PAYMENT_METHOD_WIRE };
            questionText = MessageFormat.format(questionText, args);

            boolean clearTab = super.askOrAnalyzeYesNoQuestion(KFSConstants.DisbursementVoucherDocumentConstants.CLEAR_WIRE_TRANSFER_TAB_QUESTION_ID, questionText);
            if (clearTab) {
                // NOTE: Can't replace with new instance because Foreign Draft uses same object
                clearWireTransferValues(dvWireTransfer);
            }
            else {
                // return to document if the user doesn't want to clear the Wire Transfer tab
                super.event.setActionForwardName(KFSConstants.MAPPING_BASIC);
                tabStatesOK = false;
            }
        }

        return tabStatesOK;
    }

    /**
     * Gets the disbursementVoucherInvoiceService attribute. 
     * @return Returns the disbursementVoucherInvoiceService.
     */
    public UaDisbursementVoucherInvoiceService getDisbursementVoucherInvoiceService() {
        if (disbursementVoucherInvoiceService == null) {
            disbursementVoucherInvoiceService = SpringContext.getBean(UaDisbursementVoucherInvoiceService.class);
        }
        return disbursementVoucherInvoiceService;
    }

    
}
