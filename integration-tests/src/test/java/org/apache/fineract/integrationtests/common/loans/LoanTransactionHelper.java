/**
 * Licensed to the Apache Software Foundation (ASF) under one
 * or more contributor license agreements. See the NOTICE file
 * distributed with this work for additional information
 * regarding copyright ownership. The ASF licenses this file
 * to you under the Apache License, Version 2.0 (the
 * "License"); you may not use this file except in compliance
 * with the License. You may obtain a copy of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing,
 * software distributed under the License is distributed on an
 * "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY
 * KIND, either express or implied. See the License for the
 * specific language governing permissions and limitations
 * under the License.
 */
package org.apache.fineract.integrationtests.common.loans;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import com.google.gson.Gson;
import io.restassured.specification.RequestSpecification;
import io.restassured.specification.ResponseSpecification;
import java.io.ByteArrayInputStream;
import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import javax.ws.rs.core.HttpHeaders;
import javax.ws.rs.core.MediaType;
import org.apache.fineract.integrationtests.common.CommonConstants;
import org.apache.fineract.integrationtests.common.Utils;
import org.apache.poi.hssf.usermodel.HSSFWorkbook;
import org.apache.poi.ss.usermodel.Workbook;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

@SuppressWarnings({ "rawtypes", "unchecked" })
public class LoanTransactionHelper {

    private final RequestSpecification requestSpec;
    private final ResponseSpecification responseSpec;
    private static final Logger LOG = LoggerFactory.getLogger(LoanTransactionHelper.class);

    private static final String CREATE_LOAN_PRODUCT_URL = "/fineract-provider/api/v1/loanproducts?" + Utils.TENANT_IDENTIFIER;
    private static final String APPLY_LOAN_URL = "/fineract-provider/api/v1/loans?" + Utils.TENANT_IDENTIFIER;
    private static final String LOAN_ACCOUNT_URL = "/fineract-provider/api/v1/loans";
    private static final String APPROVE_LOAN_COMMAND = "approve";
    private static final String UNDO_APPROVAL_LOAN_COMMAND = "undoApproval";
    private static final String DISBURSE_LOAN_COMMAND = "disburse";
    private static final String DISBURSE_LOAN_TO_SAVINGS_COMMAND = "disburseToSavings";
    private static final String UNDO_DISBURSE_LOAN_COMMAND = "undoDisbursal";
    private static final String REJECT_LOAN_COMMAND = "reject";
    private static final String UNDO_LAST_DISBURSE_LOAN_COMMAND = "undolastdisbursal";
    private static final String WRITE_OFF_LOAN_COMMAND = "writeoff";
    private static final String WAIVE_INTEREST_COMMAND = "waiveinterest";
    private static final String MAKE_REPAYMENT_COMMAND = "repayment";
    private static final String WITHDRAW_LOAN_APPLICATION_COMMAND = "withdrawnByApplicant";
    private static final String RECOVER_FROM_GUARANTORS_COMMAND = "recoverGuarantees";
    private static final String MAKE_REFUND_BY_CASH_COMMAND = "refundByCash";
    private static final String FORECLOSURE_COMMAND = "foreclosure";

    public static final String DATE_TIME_FORMAT = "dd MMMM yyyy HH:mm";

    public LoanTransactionHelper(final RequestSpecification requestSpec, final ResponseSpecification responseSpec) {
        this.requestSpec = requestSpec;
        this.responseSpec = responseSpec;
    }

    public Integer getLoanProductId(final String loanProductJSON) {
        return Utils.performServerPost(this.requestSpec, this.responseSpec, CREATE_LOAN_PRODUCT_URL, loanProductJSON, "resourceId");
    }

    public Integer getLoanId(final String loanApplicationJSON) {
        return Utils.performServerPost(this.requestSpec, this.responseSpec, APPLY_LOAN_URL, loanApplicationJSON, "loanId");
    }

    public HashMap<String, Integer> getGlimId(final String loanApplicationJSON) {
        return Utils.performServerPost(this.requestSpec, this.responseSpec, APPLY_LOAN_URL, loanApplicationJSON, "");
    }

    public Object getGlimLoanId(final String glimId) {
        final String GET_LOAN_URL = "/fineract-provider/api/v1/loans/glimAccount/" + glimId + "?" + Utils.TENANT_IDENTIFIER;
        return Utils.performServerGet(this.requestSpec, this.responseSpec, GET_LOAN_URL, "childLoanId");

    }

    public Object getLoanError(final String loanApplicationJSON, final String responseAttribute) {
        return Utils.performServerPost(this.requestSpec, this.responseSpec, APPLY_LOAN_URL, loanApplicationJSON, responseAttribute);
    }

    public Integer getLoanOfficerId(final String loanId) {
        final String GET_LOAN_URL = "/fineract-provider/api/v1/loans/" + loanId + "?" + Utils.TENANT_IDENTIFIER;
        return Utils.performServerGet(this.requestSpec, this.responseSpec, GET_LOAN_URL, "loanOfficerId");
    }

    public Object createLoanAccount(final String loanApplicationJSON, final String responseAttribute) {
        return Utils.performServerPost(this.requestSpec, this.responseSpec, APPLY_LOAN_URL, loanApplicationJSON, responseAttribute);
    }

    public Integer updateLoan(final Integer id, final String loanApplicationJSON) {
        return Utils.performServerPut(this.requestSpec, this.responseSpec,
                "/fineract-provider/api/v1/loans/" + id + "?" + Utils.TENANT_IDENTIFIER, loanApplicationJSON, "loanId");
    }

    public ArrayList getLoanRepaymentSchedule(final RequestSpecification requestSpec, final ResponseSpecification responseSpec,
            final Integer loanID) {
        final String URL = "/fineract-provider/api/v1/loans/" + loanID + "?associations=repaymentSchedule&" + Utils.TENANT_IDENTIFIER;
        final HashMap response = Utils.performServerGet(requestSpec, responseSpec, URL, "repaymentSchedule");
        return (ArrayList) response.get("periods");
    }

    public ArrayList getLoanCharges(final RequestSpecification requestSpec, final ResponseSpecification responseSpec,
            final Integer loanID) {
        final String URL = "/fineract-provider/api/v1/loans/" + loanID + "?associations=charges&" + Utils.TENANT_IDENTIFIER;
        return (ArrayList) Utils.performServerGet(requestSpec, responseSpec, URL, "charges");
    }

    public ArrayList getLoanTransactions(final RequestSpecification requestSpec, final ResponseSpecification responseSpec,
            final Integer loanID) {
        final String URL = "/fineract-provider/api/v1/loans/" + loanID + "?associations=transactions&" + Utils.TENANT_IDENTIFIER;
        return (ArrayList) Utils.performServerGet(requestSpec, responseSpec, URL, "transactions");
    }

    public ArrayList getLoanFutureRepaymentSchedule(final RequestSpecification requestSpec, final ResponseSpecification responseSpec,
            final Integer loanID) {
        final String URL = "/fineract-provider/api/v1/loans/" + loanID + "?associations=repaymentSchedule,futureSchedule&"
                + Utils.TENANT_IDENTIFIER;
        final HashMap response = Utils.performServerGet(requestSpec, responseSpec, URL, "repaymentSchedule");
        return (ArrayList) response.get("futurePeriods");
    }

    public HashMap getLoanSummary(final RequestSpecification requestSpec, final ResponseSpecification responseSpec, final Integer loanID) {
        final String URL = "/fineract-provider/api/v1/loans/" + loanID + "?" + Utils.TENANT_IDENTIFIER;
        final HashMap response = Utils.performServerGet(requestSpec, responseSpec, URL, "summary");
        return response;
    }

    public Object getLoanDetail(final RequestSpecification requestSpec, final ResponseSpecification responseSpec, final Integer loanID,
            final String param) {
        final String URL = "/fineract-provider/api/v1/loans/" + loanID + "?associations=all&" + Utils.TENANT_IDENTIFIER;
        return Utils.performServerGet(requestSpec, responseSpec, URL, param);
    }

    public Object getLoanDetailExcludeFutureSchedule(final RequestSpecification requestSpec, final ResponseSpecification responseSpec,
            final Integer loanID, final String param) {
        final String URL = "/fineract-provider/api/v1/loans/" + loanID + "?associations=all&exclude=guarantors,futureSchedule&"
                + Utils.TENANT_IDENTIFIER;
        return Utils.performServerGet(requestSpec, responseSpec, URL, param);
    }

    public String getLoanDetails(final RequestSpecification requestSpec, final ResponseSpecification responseSpec, final Integer loanID) {
        final String URL = "/fineract-provider/api/v1/loans/" + loanID + "?associations=all&" + Utils.TENANT_IDENTIFIER;
        return Utils.performServerGet(requestSpec, responseSpec, URL, null);
    }

    public Object getLoanProductDetail(final RequestSpecification requestSpec, final ResponseSpecification responseSpec,
            final Integer loanProductId, final String jsonAttributeToGetBack) {
        final String URL = "/fineract-provider/api/v1/loanproducts/" + loanProductId + "?associations=all&" + Utils.TENANT_IDENTIFIER;
        return Utils.performServerGet(requestSpec, responseSpec, URL, jsonAttributeToGetBack);
    }

    public String getLoanProductDetails(final RequestSpecification requestSpec, final ResponseSpecification responseSpec,
            final Integer loanProductId) {
        final String URL = "/fineract-provider/api/v1/loanproducts/" + loanProductId + "?associations=all&" + Utils.TENANT_IDENTIFIER;
        return Utils.performServerGet(requestSpec, responseSpec, URL, null);
    }

    public ArrayList getLoanCharges(final Integer loanId) {
        final String GET_LOAN_CHARGES_URL = "/fineract-provider/api/v1/loans/" + loanId + "/charges?" + Utils.TENANT_IDENTIFIER;
        return Utils.performServerGet(requestSpec, responseSpec, GET_LOAN_CHARGES_URL, "");
    }

    public ArrayList<HashMap> getRepayments(final Integer loanId) {
        final String GET_REPAYMENTS_URL = "/fineract-provider/api/v1/loans/" + loanId + "/transactions/template?command=disburse&"
                + Utils.TENANT_IDENTIFIER;
        return Utils.performServerGet(requestSpec, responseSpec, GET_REPAYMENTS_URL, "loanRepaymentScheduleInstallments");
    }

    public HashMap approveLoan(final String approvalDate, final Integer loanID) {
        String loanApprovalCommand = createLoanOperationURL(APPROVE_LOAN_COMMAND, loanID);
        String loanApprovalRequest = getApproveLoanAsJSON(approvalDate);
        LOG.info("Loan approval command: {} ", loanApprovalCommand);
        LOG.info("Loan approval request: {} ", loanApprovalRequest);
        return performLoanTransaction(loanApprovalCommand, loanApprovalRequest);
    }

    public HashMap approveLoanWithApproveAmount(final String approvalDate, final String expectedDisbursementDate,
            final String approvalAmount, final Integer loanID, List<HashMap> tranches) {
        return performLoanTransaction(createLoanOperationURL(APPROVE_LOAN_COMMAND, loanID),
                getApproveLoanAsJSON(approvalDate, expectedDisbursementDate, approvalAmount, tranches));
    }

    public List<HashMap<String, Object>> approveLoanForTranches(final String approvalDate, final String expectedDisbursementDate,
            final String approvalAmount, final Integer loanID, List<HashMap> tranches, final String responseAttribute) {
        return (List<HashMap<String, Object>>) performLoanTransaction(createLoanOperationURL(APPROVE_LOAN_COMMAND, loanID),
                getApproveLoanAsJSON(approvalDate, expectedDisbursementDate, approvalAmount, tranches), responseAttribute);
    }

    public Object approveLoan(final String approvalDate, final String approvalAmount, final Integer loanID,
            final String responseAttribute) {

        final String approvalURL = createLoanOperationURL(APPROVE_LOAN_COMMAND, loanID);
        final String approvalJSONData = getApproveLoanAsJSON(approvalDate, null, approvalAmount, null);

        return performLoanTransaction(approvalURL, approvalJSONData, responseAttribute);
    }

    public HashMap undoApproval(final Integer loanID) {
        final String undoBodyJson = "{'note':'UNDO APPROVAL'}";
        return performLoanTransaction(createLoanOperationURL(UNDO_APPROVAL_LOAN_COMMAND, loanID), undoBodyJson);
    }

    public HashMap disburseLoan(final String date, final Integer loanID, final String netDisbursalAmount) {
        return performLoanTransaction(createLoanOperationURL(DISBURSE_LOAN_COMMAND, loanID),
                getDisburseLoanAsJSON(date, null, netDisbursalAmount));
    }

    public HashMap disburseLoanWithPostDatedChecks(final String date, final Integer loanId, final BigDecimal transactionAmount,
            final List<HashMap> postDatedChecks) {
        return performLoanTransaction(createLoanOperationURL(DISBURSE_LOAN_COMMAND, loanId),
                getDisburseLoanWithPostDatedChecksAsJSON(date, transactionAmount.toString(), postDatedChecks));
    }

    private String getDisburseLoanWithPostDatedChecksAsJSON(final String actualDisbursementDate, final String transactionAmount,
            final List<HashMap> postDatedChecks) {
        final HashMap<String, Object> map = new HashMap<String, Object>();
        map.put("locale", "en");
        map.put("dateFormat", "dd MMMM yyyy");
        map.put("actualDisbursementDate", actualDisbursementDate);
        map.put("note", "DISBURSE NOTE");
        if (transactionAmount != null) {
            map.put("transactionAmount", transactionAmount);
        }

        map.put("postDatedChecks", postDatedChecks);
        LOG.info("Loan Application disburse request : {} ", map);
        return new Gson().toJson(map);
    }

    public HashMap disburseLoanWithRepaymentReschedule(final String date, final Integer loanID, String adjustRepaymentDate) {
        return performLoanTransaction(createLoanOperationURL(DISBURSE_LOAN_COMMAND, loanID),
                getDisburseLoanWithRepaymentRescheduleAsJSON(date, null, adjustRepaymentDate));
    }

    public HashMap disburseLoan(final String date, final Integer loanID, final String disburseAmt, final String netDisbursalAmount) {
        return performLoanTransaction(createLoanOperationURL(DISBURSE_LOAN_COMMAND, loanID),
                getDisburseLoanAsJSON(date, disburseAmt, netDisbursalAmount));
    }

    public Object disburseLoan(final String date, final Integer loanID, ResponseSpecification responseValidationError,
            final String netDisbursalAmount) {
        return performLoanTransaction(createLoanOperationURL(DISBURSE_LOAN_COMMAND, loanID),
                getDisburseLoanAsJSON(date, null, netDisbursalAmount), responseValidationError);
    }

    public HashMap disburseLoanToSavings(final String date, final Integer loanID, final String netDisbursalAmount) {
        return performLoanTransaction(createLoanOperationURL(DISBURSE_LOAN_TO_SAVINGS_COMMAND, loanID),
                getDisburseLoanAsJSON(date, null, netDisbursalAmount));
    }

    public HashMap undoDisbursal(final Integer loanID) {
        final String undoDisburseJson = "{'note' : 'UNDO DISBURSAL'}";
        LOG.info("IN DISBURSE LOAN");
        final String url = createLoanOperationURL(UNDO_DISBURSE_LOAN_COMMAND, loanID);
        LOG.info("IN DISBURSE LOAN URL : {} ", url);
        return performLoanTransaction(createLoanOperationURL(UNDO_DISBURSE_LOAN_COMMAND, loanID), undoDisburseJson);
    }

    public Float undoLastDisbursal(final Integer loanID) {
        final String undoLastDisburseJson = "{'note' : 'UNDO LAST DISBURSAL'}";
        final String url = createLoanOperationURL(UNDO_LAST_DISBURSE_LOAN_COMMAND, loanID);
        LOG.info("IN UNDO LAST DISBURSE LOAN URL : {} ", url);
        return performUndoLastLoanDisbursementTransaction(createLoanOperationURL(UNDO_LAST_DISBURSE_LOAN_COMMAND, loanID),
                undoLastDisburseJson);
    }

    public HashMap approveGlimAccount(final RequestSpecification requestSpec, final ResponseSpecification responseSpec,
            final List<Map<String, Object>> approvalFormData, final Integer glimID) {
        String approvalForm = new LoanApplicationTestBuilder() //
                .withApprovalFormData(approvalFormData).build();

        final String approvalURL = createGlimAccountURL(APPROVE_LOAN_COMMAND, glimID);
        return performLoanTransaction(approvalURL, approvalForm);
    }

    public HashMap disburseGlimAccount(final String date, final Integer glimID) {
        LOG.info("--------------------------------- GLIM DISBURSEMENT APPLICATION -------------------------------");
        return performLoanTransaction(createGlimAccountURL(DISBURSE_LOAN_COMMAND, glimID), getDisbursementAsJSON(date));
    }

    public HashMap undoDisburseGlimAccount(final Integer glimID) {
        LOG.info("--------------------------------- UNDO DISBURSAL GLIM APPLICATION -------------------------------");
        final String undoBodyJson = "{'note':'UNDO DISBURSAL'}";
        return performLoanTransaction(createGlimAccountURL(UNDO_DISBURSE_LOAN_COMMAND, glimID), undoBodyJson);
    }

    public HashMap undoApprovalGlimAccount(final Integer glimID) {
        LOG.info("--------------------------------- UNDO APPROVAL GLIM APPLICATION -------------------------------");
        final String undoBodyJson = "{'note':'UNDO APPROVAL'}";
        return performLoanTransaction(createGlimAccountURL(UNDO_APPROVAL_LOAN_COMMAND, glimID), undoBodyJson);
    }

    public HashMap rejectGlimAccount(final String date, final Integer glimID) {
        LOG.info("--------------------------------- REJECT GLIM APPLICATION -------------------------------");
        return performLoanTransaction(createGlimAccountURL(REJECT_LOAN_COMMAND, glimID), getRejectAsJSON(date));
    }

    public void recoverFromGuarantor(final Integer loanID) {
        performLoanTransaction(createLoanOperationURL(RECOVER_FROM_GUARANTORS_COMMAND, loanID), "", "");
    }

    public HashMap writeOffLoan(final String date, final Integer loanID) {
        return performLoanTransaction(createLoanTransactionURL(WRITE_OFF_LOAN_COMMAND, loanID), getWriteOffBodyAsJSON(date));
    }

    public HashMap waiveInterest(final String date, final String amountToBeWaived, final Integer loanID) {
        return performLoanTransaction(createLoanTransactionURL(WAIVE_INTEREST_COMMAND, loanID), getWaiveBodyAsJSON(date, amountToBeWaived));
    }

    public Integer waiveInterestAndReturnTransactionId(final String date, final String amountToBeWaived, final Integer loanID) {
        Integer resourceId = Utils.performServerPost(this.requestSpec, this.responseSpec,
                createLoanTransactionURL(WAIVE_INTEREST_COMMAND, loanID), getWaiveBodyAsJSON(date, amountToBeWaived), "resourceId");
        return resourceId;
    }

    public HashMap makeRepayment(final String date, final Float amountToBePaid, final Integer loanID) {
        return (HashMap) performLoanTransaction(createLoanTransactionURL(MAKE_REPAYMENT_COMMAND, loanID),
                getRepaymentBodyAsJSON(date, amountToBePaid), "");
    }

    public HashMap makeRepaymentWithPDC(final String date, final Float amountToBePaid, final Integer loanID, final Integer paymentType) {
        return (HashMap) performLoanTransaction(createLoanTransactionURL(MAKE_REPAYMENT_COMMAND, loanID),
                getRepaymentWithPDCBodyAsJSON(date, amountToBePaid, paymentType), "");
    }

    public HashMap forecloseLoan(final String transactionDate, final Integer loanID) {
        return (HashMap) performLoanTransaction(createLoanTransactionURL(FORECLOSURE_COMMAND, loanID),
                getForeclosureBodyAsJSON(transactionDate, loanID), "");
    }

    public HashMap withdrawLoanApplicationByClient(final String date, final Integer loanID) {
        return performLoanTransaction(createLoanOperationURL(WITHDRAW_LOAN_APPLICATION_COMMAND, loanID),
                getWithdrawLoanApplicationBodyAsJSON(date));
    }

    public Integer addChargesForLoan(final Integer loanId, final String request) {
        LOG.info("--------------------------------- ADD CHARGES FOR LOAN --------------------------------");
        final String ADD_CHARGES_URL = LOAN_ACCOUNT_URL + "/" + loanId + "/charges?" + Utils.TENANT_IDENTIFIER;
        final HashMap response = Utils.performServerPost(requestSpec, responseSpec, ADD_CHARGES_URL, request, "");
        return (Integer) response.get("resourceId");
    }

    public Integer addChargesForLoan(final Integer loanId, final String request, final ResponseSpecification responseSpecParam) {
        LOG.info("--------------------------------- ADD CHARGES FOR LOAN --------------------------------");
        final String ADD_CHARGES_URL = LOAN_ACCOUNT_URL + "/" + loanId + "/charges?" + Utils.TENANT_IDENTIFIER;
        final HashMap response = Utils.performServerPost(requestSpec, responseSpecParam, ADD_CHARGES_URL, request, "");
        return (Integer) response.get("resourceId");
    }

    public Object addChargesForAllreadyDisursedLoan(final Integer loanId, final String request,
            final ResponseSpecification responseSpecification) {
        final String ADD_CHARGES_URL = "/fineract-provider/api/v1/loans/" + loanId + "/charges?" + Utils.TENANT_IDENTIFIER;
        return Utils.performServerPost(this.requestSpec, responseSpecification, ADD_CHARGES_URL, request, "");
    }

    public Integer updateChargesForLoan(final Integer loanId, final Integer loanchargeId, final String request) {
        LOG.info("--------------------------------- ADD CHARGES FOR LOAN --------------------------------");
        final String UPDATE_CHARGES_URL = "/fineract-provider/api/v1/loans/" + loanId + "/charges/" + loanchargeId + "?"
                + Utils.TENANT_IDENTIFIER;
        final HashMap response = Utils.performServerPut(requestSpec, responseSpec, UPDATE_CHARGES_URL, request, "");
        return (Integer) response.get("resourceId");
    }

    public Integer deleteChargesForLoan(final Integer loanId, final Integer loanchargeId) {
        LOG.info("--------------------------------- DELETE CHARGES FOR LOAN --------------------------------");
        final String DELETE_CHARGES_URL = "/fineract-provider/api/v1/loans/" + loanId + "/charges/" + loanchargeId + "?"
                + Utils.TENANT_IDENTIFIER;
        final HashMap response = Utils.performServerDelete(requestSpec, responseSpec, DELETE_CHARGES_URL, "");
        return (Integer) response.get("resourceId");
    }

    public Integer waiveChargesForLoan(final Integer loanId, final Integer loanchargeId, final String json) {
        LOG.info("--------------------------------- WAIVE CHARGES FOR LOAN --------------------------------");
        final String CHARGES_URL = "/fineract-provider/api/v1/loans/" + loanId + "/charges/" + loanchargeId + "?command=waive&"
                + Utils.TENANT_IDENTIFIER;
        final HashMap response = Utils.performServerPost(requestSpec, responseSpec, CHARGES_URL, json, "");
        return (Integer) response.get("resourceId");
    }

    public Integer undoWaiveChargesForLoan(final Integer loanId, final Integer transactionId, final String body) {
        LOG.info("--------------------------------- UNDO WAIVE CHARGES FOR LOAN --------------------------------");
        final String TRANSAC_URL = "/fineract-provider/api/v1/loans/" + loanId + "/transactions/" + transactionId + "?"
                + Utils.TENANT_IDENTIFIER;
        final HashMap response = Utils.performServerPut(requestSpec, responseSpec, TRANSAC_URL, body, "");
        return (Integer) response.get("resourceId");
    }

    public Integer payChargesForLoan(final Integer loanId, final Integer loanchargeId, final String json) {
        LOG.info("--------------------------------- WAIVE CHARGES FOR LOAN --------------------------------");
        final String CHARGES_URL = "/fineract-provider/api/v1/loans/" + loanId + "/charges/" + loanchargeId + "?command=pay&"
                + Utils.TENANT_IDENTIFIER;
        final HashMap response = Utils.performServerPost(requestSpec, responseSpec, CHARGES_URL, json, "");
        return (Integer) response.get("resourceId");
    }

    public ArrayList<HashMap> getLoanTransactionDetails(final RequestSpecification requestSpec, final ResponseSpecification responseSpec,
            final Integer loanID) {
        final String URL = "/fineract-provider/api/v1/loans/" + loanID + "?associations=all&exclude=guarantors,futureSchedule&"
                + Utils.TENANT_IDENTIFIER;
        return Utils.performServerGet(requestSpec, responseSpec, URL, "transactions");
    }

    public HashMap getLoanCharge(final Integer loanId, final Integer chargeId) {
        final String GET_LOAN_CHARGES_URL = "/fineract-provider/api/v1/loans/" + loanId + "/charges/" + chargeId + "?"
                + Utils.TENANT_IDENTIFIER;
        return Utils.performServerGet(requestSpec, responseSpec, GET_LOAN_CHARGES_URL, "");
    }

    public HashMap getPostDatedCheck(final Integer loanId, final Integer installmentId) {
        final String GET_POST_DATED_TRANS_URL = "/fineract-provider/api/v1/loans/" + loanId + "/postdatedchecks/" + installmentId + "?"
                + Utils.TENANT_IDENTIFIER;
        return Utils.performServerGet(requestSpec, responseSpec, GET_POST_DATED_TRANS_URL, "");
    }

    private String getDisburseLoanAsJSON(final String actualDisbursementDate, final String transactionAmount,
            final String netDisbursalAmount) {
        final HashMap<String, String> map = new HashMap<>();
        map.put("locale", "en");
        map.put("dateFormat", "dd MMMM yyyy");
        map.put("actualDisbursementDate", actualDisbursementDate);
        map.put("netDisbursalAmount", netDisbursalAmount);
        map.put("note", "DISBURSE NOTE");
        if (transactionAmount != null) {
            map.put("transactionAmount", transactionAmount);
        }
        LOG.info("Loan Application disburse request : {} ", map);
        return new Gson().toJson(map);
    }

    private String getDisburseLoanWithRepaymentRescheduleAsJSON(final String actualDisbursementDate, final String transactionAmount,
            final String adjustRepaymentDate) {
        final HashMap<String, String> map = new HashMap<>();
        map.put("locale", "en");
        map.put("dateFormat", "dd MMMM yyyy");
        map.put("actualDisbursementDate", actualDisbursementDate);
        map.put("adjustRepaymentDate", adjustRepaymentDate);
        map.put("note", "DISBURSE NOTE");
        if (transactionAmount != null) {
            map.put("transactionAmount", transactionAmount);
        }
        LOG.info("Loan Application disburse request : {} ", map);
        return new Gson().toJson(map);
    }

    private String getApproveLoanAsJSON(final String approvalDate) {
        return getApproveLoanAsJSON(approvalDate, null, null, null);
    }

    private String getApproveLoanAsJSON(final String approvalDate, final String expectedDisbursementDate, final String approvalAmount,
            List<HashMap> tranches) {
        final HashMap<String, Object> map = new HashMap<>();
        map.put("locale", "en");
        map.put("dateFormat", "dd MMMM yyyy");
        if (approvalAmount != null) {
            map.put("approvedLoanAmount", approvalAmount);
        }
        map.put("approvedOnDate", approvalDate);
        if (expectedDisbursementDate != null) {
            map.put("expectedDisbursementDate", expectedDisbursementDate);
        }
        if (tranches != null && tranches.size() > 0) {
            map.put("disbursementData", tranches);
        }
        map.put("note", "Approval NOTE");
        return new Gson().toJson(map);
    }

    private String getDisbursementAsJSON(final String date) {
        final HashMap<String, String> map = new HashMap<>();
        map.put("actualDisbursementDate", date);
        map.put("locale", "en");
        map.put("dateFormat", "dd MMMM yyyy");
        return new Gson().toJson(map);
    }

    private String getRejectAsJSON(final String date) {
        final HashMap<String, String> map = new HashMap<>();
        map.put("rejectedOnDate", date);
        map.put("locale", "en");
        map.put("dateFormat", "dd MMMM yyyy");
        return new Gson().toJson(map);
    }

    private String getRepaymentBodyAsJSON(final String transactionDate, final Float transactionAmount) {
        final HashMap<String, String> map = new HashMap<>();
        map.put("locale", "en");
        map.put("dateFormat", "dd MMMM yyyy");
        map.put("transactionDate", transactionDate);
        map.put("transactionAmount", transactionAmount.toString());
        map.put("note", "Repayment Made!!!");
        return new Gson().toJson(map);
    }

    private String getRepaymentWithPDCBodyAsJSON(final String transactionDate, final Float transactionAmount, final Integer paymentTypeId) {
        final HashMap<String, String> map = new HashMap<>();
        map.put("locale", "en");
        map.put("paymentTypeId", paymentTypeId.toString());
        map.put("dateFormat", "dd MMMM yyyy");
        map.put("transactionDate", transactionDate);
        map.put("transactionAmount", transactionAmount.toString());
        map.put("note", "Repayment Made!!!");
        return new Gson().toJson(map);
    }

    private String getForeclosureBodyAsJSON(final String transactionDate, final Integer loanId) {
        final HashMap<String, Object> map = new HashMap<>();
        map.put("locale", "en");
        map.put("dateFormat", "dd MMMM yyyy");
        map.put("transactionDate", transactionDate);
        map.put("note", "Foreclosure Made!!!");
        String json = new Gson().toJson(map);
        LOG.info("{}", json);
        return json;
    }

    private String getWriteOffBodyAsJSON(final String transactionDate) {
        final HashMap<String, String> map = new HashMap<>();
        map.put("dateFormat", "dd MMMM yyyy");
        map.put("locale", "en");
        map.put("note", " LOAN WRITE OFF!!!");
        map.put("transactionDate", transactionDate);
        return new Gson().toJson(map);
    }

    private String getWaiveBodyAsJSON(final String transactionDate, final String amountToBeWaived) {
        final HashMap<String, String> map = new HashMap<>();
        map.put("locale", "en");
        map.put("dateFormat", "dd MMMM yyyy");
        map.put("transactionDate", transactionDate);
        map.put("transactionAmount", amountToBeWaived);
        map.put("note", " Interest Waived!!!");
        return new Gson().toJson(map);
    }

    private String getWithdrawLoanApplicationBodyAsJSON(final String withdrawDate) {
        final HashMap<String, String> map = new HashMap<>();
        map.put("locale", "en");
        map.put("dateFormat", "dd MMMM yyyy");
        map.put("withdrawnOnDate", withdrawDate);
        map.put("note", " Loan Withdrawn By Client!!!");
        return new Gson().toJson(map);

    }

    public static String getSpecifiedDueDateChargesForLoanAsJSON(final String chargeId) {
        return getSpecifiedDueDateChargesForLoanAsJSON(chargeId, "12 January 2013", "100");
    }

    public static String getSpecifiedDueDateChargesForLoanAsJSON(final String chargeId, final String dueDate, final String amount) {
        final HashMap<String, String> map = new HashMap<>();
        map.put("locale", "en_GB");
        map.put("dateFormat", "dd MMMM yyyy");
        map.put("amount", amount);
        map.put("dueDate", dueDate);
        map.put("chargeId", chargeId);
        String json = new Gson().toJson(map);
        LOG.info("{}", json);
        return json;
    }

    public static String getDisbursementChargesForLoanAsJSON(final String chargeId) {
        return getDisbursementChargesForLoanAsJSON(chargeId, "100");
    }

    public static String getDisbursementChargesForLoanAsJSON(final String chargeId, String amount) {
        final HashMap<String, String> map = new HashMap<>();
        map.put("locale", "en_GB");
        map.put("dateFormat", "dd MMMM yyyy");
        map.put("amount", amount);
        map.put("chargeId", chargeId);
        String json = new Gson().toJson(map);
        LOG.info("{}", json);
        return json;
    }

    public static String getInstallmentChargesForLoanAsJSON(final String chargeId, final String amount) {
        final HashMap<String, String> map = new HashMap<>();
        map.put("locale", "en_GB");
        map.put("dateFormat", "dd MMMM yyyy");
        map.put("amount", amount);
        map.put("chargeId", chargeId);
        String json = new Gson().toJson(map);
        LOG.info("{}", json);
        return json;
    }

    public static String getUpdateChargesForLoanAsJSON(String amount) {
        final HashMap<String, String> map = new HashMap<>();
        map.put("locale", "en_GB");
        map.put("dateFormat", "dd MMMM yyyy");
        map.put("amount", amount);
        String json = new Gson().toJson(map);
        LOG.info("{}", json);
        return json;
    }

    public static String getPayChargeJSON(final String date, final String installmentNumber) {
        final HashMap<String, String> map = new HashMap<>();
        map.put("locale", "en_GB");
        map.put("dateFormat", "dd MMMM yyyy");
        map.put("transactionDate", date);
        if (installmentNumber != null) {
            map.put("installmentNumber", installmentNumber);
        }
        String json = new Gson().toJson(map);
        LOG.info("{}", json);
        return json;
    }

    public static String getWaiveChargeJSON(final String installmentNumber) {
        final HashMap<String, String> map = new HashMap<>();
        map.put("locale", "en_GB");
        map.put("installmentNumber", installmentNumber);
        String json = new Gson().toJson(map);
        LOG.info("{}", json);
        return json;
    }

    public String getLoanCalculationBodyAsJSON(final String productID) {
        final HashMap<String, String> map = new HashMap<>();
        map.put("dateFormat", "dd MMMM yyyy");
        map.put("locale", "en_GB");
        map.put("productId", productID);
        map.put("principal", "4,500.00");
        map.put("loanTermFrequency", "4");
        map.put("loanTermFrequencyType", "2");
        map.put("numberOfRepayments", "4");
        map.put("repaymentEvery", "1");
        map.put("repaymentFrequencyType", "2");
        map.put("interestRateFrequencyType", "2");
        map.put("interestRatePerPeriod", "2");
        map.put("amortizationType", "1");
        map.put("interestType", "0");
        map.put("interestCalculationPeriodType", "1");
        map.put("expectedDisbursementDate", "20 September 2011");
        map.put("transactionProcessingStrategyId", "1");
        return new Gson().toJson(map);
    }

    private String createLoanOperationURL(final String command, final Integer loanID) {
        return "/fineract-provider/api/v1/loans/" + loanID + "?command=" + command + "&" + Utils.TENANT_IDENTIFIER;
    }

    private String createLoanTransactionURL(final String command, final Integer loanID) {
        return "/fineract-provider/api/v1/loans/" + loanID + "/transactions?command=" + command + "&" + Utils.TENANT_IDENTIFIER;
    }

    private String createGlimAccountURL(final String command, final Integer glimID) {
        return "/fineract-provider/api/v1/loans/glimAccount/" + glimID + "?command=" + command + "&" + Utils.TENANT_IDENTIFIER;
    }

    private HashMap performLoanTransaction(final String postURLForLoanTransaction, final String jsonToBeSent) {

        final HashMap response = Utils.performServerPost(this.requestSpec, this.responseSpec, postURLForLoanTransaction, jsonToBeSent,
                "changes");
        return (HashMap) response.get("status");
    }

    private Float performUndoLastLoanDisbursementTransaction(final String postURLForLoanTransaction, final String jsonToBeSent) {

        final HashMap response = Utils.performServerPost(this.requestSpec, this.responseSpec, postURLForLoanTransaction, jsonToBeSent,
                "changes");
        return (Float) response.get("disbursedAmount");
    }

    private Object performLoanTransaction(final String postURLForLoanTransaction, final String jsonToBeSent,
            final String responseAttribute) {
        return Utils.performServerPost(this.requestSpec, this.responseSpec, postURLForLoanTransaction, jsonToBeSent, responseAttribute);
    }

    private Object performLoanTransaction(final String postURLForLoanTransaction, final String jsonToBeSent,
            ResponseSpecification responseValidationError) {

        return Utils.performServerPost(this.requestSpec, responseValidationError, postURLForLoanTransaction, jsonToBeSent,
                CommonConstants.RESPONSE_ERROR);
    }

    public Object adjustLoanTransaction(final Integer loanId, final Integer transactionId, final String date,
            final String transactionAmount, final String responseAttribute) {
        return adjustLoanTransaction(loanId, transactionId, getAdjustTransactionJSON(date, transactionAmount), responseAttribute);
    }

    private Object adjustLoanTransaction(final Integer loanId, final Integer tansactionId, final String jsonToBeSent,
            final String responseAttribute) {
        final String URL = "/fineract-provider/api/v1/loans/" + loanId + "/transactions/" + tansactionId + "?" + Utils.TENANT_IDENTIFIER;
        return Utils.performServerPost(this.requestSpec, this.responseSpec, URL, jsonToBeSent, responseAttribute);
    }

    private String getAdjustTransactionJSON(final String date, final String transactionAmount) {
        final HashMap<String, String> map = new HashMap<>();
        map.put("locale", "en_GB");
        map.put("dateFormat", "dd MMMM yyyy");
        map.put("transactionDate", date);
        map.put("transactionAmount", transactionAmount);
        String json = new Gson().toJson(map);
        LOG.info("{}", json);
        return json;
    }

    public HashMap getPrepayAmount(final RequestSpecification requestSpec, final ResponseSpecification responseSpec, final Integer loanID) {
        final String URL = "/fineract-provider/api/v1/loans/" + loanID + "/transactions/template?command=prepayLoan&"
                + Utils.TENANT_IDENTIFIER;
        final HashMap response = Utils.performServerGet(requestSpec, responseSpec, URL, "");
        return response;
    }

    private String createLoanRefundTransferURL() {
        return "/fineract-provider/api/v1/accounttransfers/refundByTransfer?tenantIdentifier=default";
    }

    public void verifyRepaymentScheduleEntryFor(final int repaymentNumber, final float expectedPrincipalOutstanding, final Integer loanID) {
        LOG.info("---------------------------GETTING LOAN REPAYMENT SCHEDULE--------------------------------");
        final ArrayList<HashMap> repaymentPeriods = getLoanRepaymentSchedule(this.requestSpec, this.responseSpec, loanID);
        assertEquals(expectedPrincipalOutstanding, repaymentPeriods.get(repaymentNumber).get("principalLoanBalanceOutstanding"),
                "Mismatch in Principal Loan Balance Outstanding ");
    }

    public void checkAccrualTransactionForRepayment(final LocalDate transactionDate, final Float interestPortion, final Float feePortion,
            final Float penaltyPortion, final Integer loanID) {

        ArrayList<HashMap> transactions = (ArrayList<HashMap>) getLoanTransactions(this.requestSpec, this.responseSpec, loanID);
        boolean isTransactionFound = false;
        for (int i = 0; i < transactions.size(); i++) {
            HashMap transactionType = (HashMap) transactions.get(i).get("type");
            boolean isAccrualTransaction = (Boolean) transactionType.get("accrual");

            if (isAccrualTransaction) {
                ArrayList<Integer> accrualEntryDateAsArray = (ArrayList<Integer>) transactions.get(i).get("date");
                LocalDate accrualEntryDate = LocalDate.of(accrualEntryDateAsArray.get(0), accrualEntryDateAsArray.get(1),
                        accrualEntryDateAsArray.get(2));

                if (transactionDate.equals(accrualEntryDate)) {
                    isTransactionFound = true;
                    assertEquals(interestPortion, Float.valueOf(String.valueOf(transactions.get(i).get("interestPortion"))),
                            "Mismatch in transaction amounts");
                    assertEquals(feePortion, Float.valueOf(String.valueOf(transactions.get(i).get("feeChargesPortion"))),
                            "Mismatch in transaction amounts");
                    assertEquals(penaltyPortion, Float.valueOf(String.valueOf(transactions.get(i).get("penaltyChargesPortion"))),
                            "Mismatch in transaction amounts");
                    break;
                }
            }
        }
        assertTrue(isTransactionFound, "No Accrual entries are posted");

    }

    public HashMap makeRefundByCash(final String date, final Float amountToBeRefunded, final Integer loanID) {
        return performLoanTransaction(createLoanTransactionURL(MAKE_REFUND_BY_CASH_COMMAND, loanID),
                getRefundByCashBodyAsJSON(date, amountToBeRefunded));
    }

    public HashMap makeRefundByTransfer(final Integer fromAccountId, final Integer toClientId, final Integer toAccountId,
            final Integer fromClientId, final String date, final Float amountToBeRefunded) {
        return performLoanTransaction(createLoanRefundTransferURL(),
                getRefundByTransferBodyAsJSON(fromAccountId, toClientId, toAccountId, fromClientId, date, amountToBeRefunded));
    }

    private String getRefundByCashBodyAsJSON(final String transactionDate, final Float transactionAmount) {
        final HashMap<String, String> map = new HashMap<>();
        map.put("locale", "en");
        map.put("dateFormat", "dd MMMM yyyy");
        map.put("transactionDate", transactionDate);
        map.put("transactionAmount", transactionAmount.toString());
        map.put("note", "Refund Made!!!");
        return new Gson().toJson(map);
    }

    private String getRefundByTransferBodyAsJSON(final Integer fromAccountId, final Integer toClientId, final Integer toAccountId,
            final Integer fromClientId, final String transactionDate, final Float transactionAmount) {
        final HashMap<String, String> map = new HashMap<>();
        map.put("fromAccountId", fromAccountId.toString());
        map.put("fromAccountType", "1");
        map.put("toOfficeId", "1");
        map.put("toClientId", toClientId.toString());
        map.put("toAccountType", "2");
        map.put("toAccountId", toAccountId.toString());
        map.put("dateFormat", "dd MMMM yyyy");
        map.put("transferDate", transactionDate);
        map.put("transferAmount", transactionAmount.toString());
        map.put("transferDescription", "Refund Made!!!");
        map.put("fromClientId", fromClientId.toString());
        map.put("fromOfficeId", "1");
        map.put("locale", "en");
        return new Gson().toJson(map);
    }

    public HashMap createTrancheDetail(final String id, final String date, final String amount) {
        HashMap<String, Object> detail = new HashMap<>();
        if (id != null) {
            detail.put("id", id);
        }
        detail.put("expectedDisbursementDate", date);
        detail.put("principal", amount);

        return detail;
    }

    public Object editDisbursementDetail(final Integer loanID, final Integer disbursementId, final String approvalAmount,
            final String expectedDisbursementDate, final String updatedExpectedDisbursementDate, final String updatedPrincipal,
            final String jsonAttributeToGetBack) {

        return Utils.performServerPut(this.requestSpec, this.responseSpec, createEditDisbursementURL(loanID, disbursementId),
                getEditDisbursementsAsJSON(approvalAmount, expectedDisbursementDate, updatedExpectedDisbursementDate, updatedPrincipal),
                jsonAttributeToGetBack);
    }

    public Object addAndDeleteDisbursementDetail(final Integer loanID, final String approvalAmount, final String expectedDisbursementDate,
            List<HashMap> disbursementData, final String jsonAttributeToGetBack) {

        return Utils.performServerPut(this.requestSpec, this.responseSpec, createAddAndDeleteDisbursementURL(loanID),
                getAddAndDeleteDisbursementsAsJSON(approvalAmount, expectedDisbursementDate, disbursementData), jsonAttributeToGetBack);
    }

    private String createEditDisbursementURL(Integer loanID, Integer disbursementId) {
        return "/fineract-provider/api/v1/loans/" + loanID + "/disbursements/" + disbursementId + "?" + Utils.TENANT_IDENTIFIER;
    }

    private String createAddAndDeleteDisbursementURL(Integer loanID) {
        return "/fineract-provider/api/v1/loans/" + loanID + "/disbursements/editDisbursements?" + Utils.TENANT_IDENTIFIER;
    }

    public static String getEditDisbursementsAsJSON(final String approvalAmount, final String expectedDisbursementDate,
            final String updatedExpectedDisbursementDate, final String updatedPrincipal) {
        final HashMap<String, String> map = new HashMap<>();
        map.put("locale", "en");
        map.put("dateFormat", "dd MMMM yyyy");
        map.put("approvedLoanAmount", approvalAmount);
        map.put("expectedDisbursementDate", expectedDisbursementDate);
        map.put("updatedExpectedDisbursementDate", updatedExpectedDisbursementDate);
        map.put("updatedPrincipal", updatedPrincipal);
        String json = new Gson().toJson(map);
        LOG.info("{}", json);
        return json;
    }

    public static String getAddAndDeleteDisbursementsAsJSON(final String approvalAmount, final String expectedDisbursementDate,
            final List<HashMap> disbursementData) {
        final HashMap map = new HashMap<>();
        map.put("locale", "en");
        map.put("dateFormat", "dd MMMM yyyy");
        map.put("approvedLoanAmount", approvalAmount);
        map.put("expectedDisbursementDate", expectedDisbursementDate);
        map.put("disbursementData", disbursementData);
        String json = new Gson().toJson(map);
        LOG.info("{}", json);
        return json;
    }

    public static List<HashMap<String, Object>> getTestDatatableAsJson(final String registeredTableName) {
        List<HashMap<String, Object>> datatablesListMap = new ArrayList<>();
        HashMap<String, Object> datatableMap = new HashMap<>();
        HashMap<String, Object> dataMap = new HashMap<>();
        dataMap.put("locale", "en");
        dataMap.put("Spouse Name", Utils.randomNameGenerator("Spouse_name", 4));
        dataMap.put("Number of Dependents", 5);
        dataMap.put("Time of Visit", "01 December 2016 04:03");
        dataMap.put("dateFormat", DATE_TIME_FORMAT);
        dataMap.put("Date of Approval", "02 December 2016 00:00");
        datatableMap.put("registeredTableName", registeredTableName);
        datatableMap.put("data", dataMap);
        datatablesListMap.add(datatableMap);
        return datatablesListMap;
    }

    public Workbook getLoanWorkbook(String dateFormat) throws IOException {
        requestSpec.header(HttpHeaders.CONTENT_TYPE, "application/vnd.ms-excel");
        byte[] byteArray = Utils.performGetBinaryResponse(requestSpec, responseSpec,
                LOAN_ACCOUNT_URL + "/downloadtemplate" + "?" + Utils.TENANT_IDENTIFIER + "&dateFormat=" + dateFormat);
        InputStream inputStream = new ByteArrayInputStream(byteArray);
        Workbook workbook = new HSSFWorkbook(inputStream);
        return workbook;
    }

    public String importLoanTemplate(File file) {

        String locale = "en";
        String dateFormat = "dd MMMM yyyy";
        String legalFormType = null;
        requestSpec.header(HttpHeaders.CONTENT_TYPE, MediaType.MULTIPART_FORM_DATA);
        return Utils.performServerTemplatePost(requestSpec, responseSpec,
                LOAN_ACCOUNT_URL + "/uploadtemplate" + "?" + Utils.TENANT_IDENTIFIER, legalFormType, file, locale, dateFormat);
    }

    public String getOutputTemplateLocation(final String importDocumentId) {
        requestSpec.header(HttpHeaders.CONTENT_TYPE, MediaType.TEXT_PLAIN);
        return Utils.performServerOutputTemplateLocationGet(requestSpec, responseSpec,
                "/fineract-provider/api/v1/imports/getOutputTemplateLocation" + "?" + Utils.TENANT_IDENTIFIER, importDocumentId);
    }
}
