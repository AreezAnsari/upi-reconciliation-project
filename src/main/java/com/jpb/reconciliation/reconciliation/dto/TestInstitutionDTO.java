package com.jpb.reconciliation.reconciliation.dto;

import java.time.LocalDateTime;
import java.util.List;

public class TestInstitutionDTO {

    // ─── System Generated ────────────────────────────────────────────────────
    private Long institutionId;
    private String institutionCode; // 8 chars: first 4 letters + 4 digits (e.g. STAT4821)
    private String status;          // ACTIVE / INACTIVE / PENDING / BLOCKED
    private LocalDateTime createdAt;

    // ─── Step 1: Institution Details ────────────────────────────────────────
    private String institutionNameFull;   // required
    private String institutionNameShort;  // optional
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

    // ─── Step 5: Security ────────────────────────────────────────────────────
    private Boolean enableMfa;
    private Boolean enableHrms;
    private Boolean enableOtp;

    // ─── Getters & Setters ───────────────────────────────────────────────────

    public Long getInstitutionId() { return institutionId; }
    public void setInstitutionId(Long institutionId) { this.institutionId = institutionId; }

    public String getInstitutionCode() { return institutionCode; }
    public void setInstitutionCode(String institutionCode) { this.institutionCode = institutionCode; }

    public String getStatus() { return status; }
    public void setStatus(String status) { this.status = status; }

    public LocalDateTime getCreatedAt() { return createdAt; }
    public void setCreatedAt(LocalDateTime createdAt) { this.createdAt = createdAt; }

    public String getInstitutionNameFull() { return institutionNameFull; }
    public void setInstitutionNameFull(String institutionNameFull) { this.institutionNameFull = institutionNameFull; }

    public String getInstitutionNameShort() { return institutionNameShort; }
    public void setInstitutionNameShort(String institutionNameShort) { this.institutionNameShort = institutionNameShort; }

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
}