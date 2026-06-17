package com.jpb.reconciliation.reconciliation.dto;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;

public class BranchBankDTO {

    // ─── System Generated ────────────────────────────────────────────────────
    private Long branchId;
    private String branchCode; // 8 chars: first 4 letters + 4 digits
    private String status;          // ACTIVE / INACTIVE / INACTIVE_PENDING / ACTIVE_PENDING / PENDING / BLOCKED / BLOCK_PENDING
    private LocalDateTime createdAt;
    private LocalDateTime inactivatedAt;         // when last set INACTIVE (for 30s cooldown display)
    private LocalDateTime updatedAt;             // last updated timestamp
    private LocalDateTime blockScheduledAt;      // when block was scheduled (for countdown display)
    private LocalDateTime inactivateScheduledAt; // when inactivation was scheduled (INACTIVE_PENDING countdown)
    private LocalDateTime reactivateScheduledAt; // when reactivation was scheduled (ACTIVE_PENDING countdown)
    private Long adminId;                    // numeric PK of BRANCH_ADMIN record
    private String branchAdminId;           // branch admin username (e.g. areez.ansari)
    private String blockScheduledBy;        // who scheduled the block
    private String preBlockStatus;          // status before block was scheduled
    private String preInactivateStatus;     // status before INACTIVE_PENDING was set
    private String preReactivateStatus;     // status before ACTIVE_PENDING was set
    private String createdBy;              // who created
    private String updatedBy;              // who last modified

    // ─── Step 1: Bank Details ────────────────────────────────────────
    private String branchNameFull;   // required
    private String branchNameShort;  // optional
    private List<String> bankType;        // ["Issuer","Acquirer","Settlement Bank"]
    private String logoPath;              // set after logo upload

    // ─── Step 2: Registered Address ─────────────────────────────────────────
    private String regAddressLine1;   // required
    private String regAddressLine2;
    private String regAddressLine3;
    private String regCity;           // required
    private String regState;          // auto-populated
    private String regCountry;        // auto-populated
    private String regPhoneCode;      // e.g. +91
    private String regCityCode;       // e.g. 022
    private String regPhone;          // required

    // ─── Step 2: Communication Address ──────────────────────────────────────
    private Boolean sameAsRegistered; // if true, comm = reg
    private String commAddressLine1;
    private String commAddressLine2;
    private String commAddressLine3;
    private String commCity;
    private String commState;
    private String commCountry;
    private String commPhoneCode;
    private String commCityCode;
    private String commPhone;

    // ─── Step 3: Primary Contact ─────────────────────────────────────────────
    private String primaryFullName;       // required
    private String primaryEmail;          // required
    private String primaryMobileCode;     // e.g. +91
    private String primaryMobile;         // required, 10 digits
    private String primaryAltMobileCode;
    private String primaryAltMobile;

    // ─── Step 3: Secondary Contact ───────────────────────────────────────────
    private String secondaryFullName;
    private String secondaryEmail;
    private String secondaryMobileCode;
    private String secondaryMobile;
    private String secondaryAltMobileCode;
    private String secondaryAltMobile;

    // ─── Step 4: Products ────────────────────────────────────────────────────
    // ["UPI","NEFT","RTGS","Credit Cards"]
    private List<String> selectedProducts;

    // Product validity dates — same pattern as MainBankDTO
    // { "UPI": { "validFrom": "2024-01-01", "validTo": "2024-12-31" }, ... }
    private Map<String, ProductDateEntry> productDates;

    // ─── Step 5: Security ────────────────────────────────────────────────────
    private Boolean enableMfa;
    private Boolean enableHrms;
    private Boolean enableOtp;

    // ─── Getters & Setters ───────────────────────────────────────────────────

    public Long getBranchId() { return branchId; }
    public void setBranchId(long branchId) { this.branchId = branchId; }

    public String getBranchCode() { return branchCode; }
    public void setBranchCode(String branchCode) { this.branchCode = branchCode; }

    public String getStatus() { return status; }
    public void setStatus(String status) { this.status = status; }

    public LocalDateTime getCreatedAt() { return createdAt; }
    public void setCreatedAt(LocalDateTime createdAt) { this.createdAt = createdAt; }

    public LocalDateTime getInactivatedAt() { return inactivatedAt; }
    public void setInactivatedAt(LocalDateTime inactivatedAt) { this.inactivatedAt = inactivatedAt; }

    public LocalDateTime getUpdatedAt() { return updatedAt; }
    public void setUpdatedAt(LocalDateTime updatedAt) { this.updatedAt = updatedAt; }

    public Long getAdminId() { return adminId; }
    public void setAdminId(Long adminId) { this.adminId = adminId; }

    public String getBranchAdminId() { return branchAdminId; }
    public void setBranchAdminId(String branchAdminId) { this.branchAdminId = branchAdminId; }

    public String getBlockScheduledBy() { return blockScheduledBy; }
    public void setBlockScheduledBy(String blockScheduledBy) { this.blockScheduledBy = blockScheduledBy; }

    public String getPreBlockStatus() { return preBlockStatus; }
    public void setPreBlockStatus(String preBlockStatus) { this.preBlockStatus = preBlockStatus; }

    public String getCreatedBy() { return createdBy; }
    public void setCreatedBy(String createdBy) { this.createdBy = createdBy; }

    public String getUpdatedBy() { return updatedBy; }
    public void setUpdatedBy(String updatedBy) { this.updatedBy = updatedBy; }

    public LocalDateTime getBlockScheduledAt() { return blockScheduledAt; }
    public void setBlockScheduledAt(LocalDateTime blockScheduledAt) { this.blockScheduledAt = blockScheduledAt; }

    public LocalDateTime getInactivateScheduledAt() { return inactivateScheduledAt; }
    public void setInactivateScheduledAt(LocalDateTime inactivateScheduledAt) { this.inactivateScheduledAt = inactivateScheduledAt; }

    public LocalDateTime getReactivateScheduledAt() { return reactivateScheduledAt; }
    public void setReactivateScheduledAt(LocalDateTime reactivateScheduledAt) { this.reactivateScheduledAt = reactivateScheduledAt; }

    public String getPreInactivateStatus() { return preInactivateStatus; }
    public void setPreInactivateStatus(String preInactivateStatus) { this.preInactivateStatus = preInactivateStatus; }

    public String getPreReactivateStatus() { return preReactivateStatus; }
    public void setPreReactivateStatus(String preReactivateStatus) { this.preReactivateStatus = preReactivateStatus; }

    public String getBranchNameFull() { return branchNameFull; }
    public void setBranchNameFull(String branchNameFull) { this.branchNameFull = branchNameFull; }

    public String getBranchNameShort() { return branchNameShort; }
    public void setBranchNameShort(String branchNameShort) { this.branchNameShort = branchNameShort; }

    public List<String> getBankType() { return bankType; }
    public void setBankType(List<String> bankType) { this.bankType = bankType; }

    public String getLogoPath() { return logoPath; }
    public void setLogoPath(String logoPath) { this.logoPath = logoPath; }

    public String getRegAddressLine1() { return regAddressLine1; }
    public void setRegAddressLine1(String regAddressLine1) { this.regAddressLine1 = regAddressLine1; }

    public String getRegAddressLine2() { return regAddressLine2; }
    public void setRegAddressLine2(String regAddressLine2) { this.regAddressLine2 = regAddressLine2; }

    public String getRegAddressLine3() { return regAddressLine3; }
    public void setRegAddressLine3(String regAddressLine3) { this.regAddressLine3 = regAddressLine3; }

    public String getRegCity() { return regCity; }
    public void setRegCity(String regCity) { this.regCity = regCity; }

    public String getRegState() { return regState; }
    public void setRegState(String regState) { this.regState = regState; }

    public String getRegCountry() { return regCountry; }
    public void setRegCountry(String regCountry) { this.regCountry = regCountry; }

    public String getRegPhoneCode() { return regPhoneCode; }
    public void setRegPhoneCode(String regPhoneCode) { this.regPhoneCode = regPhoneCode; }

    public String getRegCityCode() { return regCityCode; }
    public void setRegCityCode(String regCityCode) { this.regCityCode = regCityCode; }

    public String getRegPhone() { return regPhone; }
    public void setRegPhone(String regPhone) { this.regPhone = regPhone; }

    public Boolean getSameAsRegistered() { return sameAsRegistered; }
    public void setSameAsRegistered(Boolean sameAsRegistered) { this.sameAsRegistered = sameAsRegistered; }

    public String getCommAddressLine1() { return commAddressLine1; }
    public void setCommAddressLine1(String commAddressLine1) { this.commAddressLine1 = commAddressLine1; }

    public String getCommAddressLine2() { return commAddressLine2; }
    public void setCommAddressLine2(String commAddressLine2) { this.commAddressLine2 = commAddressLine2; }

    public String getCommAddressLine3() { return commAddressLine3; }
    public void setCommAddressLine3(String commAddressLine3) { this.commAddressLine3 = commAddressLine3; }

    public String getCommCity() { return commCity; }
    public void setCommCity(String commCity) { this.commCity = commCity; }

    public String getCommState() { return commState; }
    public void setCommState(String commState) { this.commState = commState; }

    public String getCommCountry() { return commCountry; }
    public void setCommCountry(String commCountry) { this.commCountry = commCountry; }

    public String getCommPhoneCode() { return commPhoneCode; }
    public void setCommPhoneCode(String commPhoneCode) { this.commPhoneCode = commPhoneCode; }

    public String getCommCityCode() { return commCityCode; }
    public void setCommCityCode(String commCityCode) { this.commCityCode = commCityCode; }

    public String getCommPhone() { return commPhone; }
    public void setCommPhone(String commPhone) { this.commPhone = commPhone; }

    public String getPrimaryFullName() { return primaryFullName; }
    public void setPrimaryFullName(String primaryFullName) { this.primaryFullName = primaryFullName; }

    public String getPrimaryEmail() { return primaryEmail; }
    public void setPrimaryEmail(String primaryEmail) { this.primaryEmail = primaryEmail; }

    public String getPrimaryMobileCode() { return primaryMobileCode; }
    public void setPrimaryMobileCode(String primaryMobileCode) { this.primaryMobileCode = primaryMobileCode; }

    public String getPrimaryMobile() { return primaryMobile; }
    public void setPrimaryMobile(String primaryMobile) { this.primaryMobile = primaryMobile; }

    public String getPrimaryAltMobileCode() { return primaryAltMobileCode; }
    public void setPrimaryAltMobileCode(String primaryAltMobileCode) { this.primaryAltMobileCode = primaryAltMobileCode; }

    public String getPrimaryAltMobile() { return primaryAltMobile; }
    public void setPrimaryAltMobile(String primaryAltMobile) { this.primaryAltMobile = primaryAltMobile; }

    public String getSecondaryFullName() { return secondaryFullName; }
    public void setSecondaryFullName(String secondaryFullName) { this.secondaryFullName = secondaryFullName; }

    public String getSecondaryEmail() { return secondaryEmail; }
    public void setSecondaryEmail(String secondaryEmail) { this.secondaryEmail = secondaryEmail; }

    public String getSecondaryMobileCode() { return secondaryMobileCode; }
    public void setSecondaryMobileCode(String secondaryMobileCode) { this.secondaryMobileCode = secondaryMobileCode; }

    public String getSecondaryMobile() { return secondaryMobile; }
    public void setSecondaryMobile(String secondaryMobile) { this.secondaryMobile = secondaryMobile; }

    public String getSecondaryAltMobileCode() { return secondaryAltMobileCode; }
    public void setSecondaryAltMobileCode(String secondaryAltMobileCode) { this.secondaryAltMobileCode = secondaryAltMobileCode; }

    public String getSecondaryAltMobile() { return secondaryAltMobile; }
    public void setSecondaryAltMobile(String secondaryAltMobile) { this.secondaryAltMobile = secondaryAltMobile; }

    public List<String> getSelectedProducts() { return selectedProducts; }
    public void setSelectedProducts(List<String> selectedProducts) { this.selectedProducts = selectedProducts; }

    public Boolean getEnableMfa() { return enableMfa; }
    public void setEnableMfa(Boolean enableMfa) { this.enableMfa = enableMfa; }

    public Boolean getEnableHrms() { return enableHrms; }
    public void setEnableHrms(Boolean enableHrms) { this.enableHrms = enableHrms; }

    public Boolean getEnableOtp() { return enableOtp; }
    public void setEnableOtp(Boolean enableOtp) { this.enableOtp = enableOtp; }

    // ─── Super-User Credentials (returned on create, shown on success page) ──
    private String superUserId;
    private String defaultPassword;

    public String getSuperUserId() { return superUserId; }
    public void setSuperUserId(String superUserId) { this.superUserId = superUserId; }

    public String getDefaultPassword() { return defaultPassword; }
    public void setDefaultPassword(String defaultPassword) { this.defaultPassword = defaultPassword; }

    public Map<String, ProductDateEntry> getProductDates() { return productDates; }
    public void setProductDates(Map<String, ProductDateEntry> productDates) { this.productDates = productDates; }

    // ─── Nested: product validity dates — same as MainBankDTO.ProductDateEntry ──
    public static class ProductDateEntry {
        private LocalDate validFrom;
        private LocalDate validTo;
        public LocalDate getValidFrom() { return validFrom; }
        public void setValidFrom(LocalDate validFrom) { this.validFrom = validFrom; }
        public LocalDate getValidTo() { return validTo; }
        public void setValidTo(LocalDate validTo) { this.validTo = validTo; }
    }
}
