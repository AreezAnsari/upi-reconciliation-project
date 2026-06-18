package com.jpb.reconciliation.reconciliation.dto;

public class AdminReplacementRequest {

    // MAIN_ADMIN / BRANCH_ADMIN / USER
    private String entityType;

    private Long originalEntityId;

    // common fields
    private String newEmail;
    private String newUsername;
    private String reason;

    // USER-only fields
    private String fullName;
    private String role;        // MAKER / CHECKER / WORKER / AUDITOR / IT_OPS / SUPERVISOR / RCC_CXO
    private String userType;    // INTERNAL / EXTERNAL
    private String department;
    private String designation;
    private String mobileNumber;
    private String externalDepartmentName;
    private String externalSupervisorName;
    private String externalSupervisorEmail;
    private String externalSupervisorPhone;

    public String getEntityType() { return entityType; }
    public void setEntityType(String entityType) { this.entityType = entityType; }

    public Long getOriginalEntityId() { return originalEntityId; }
    public void setOriginalEntityId(Long originalEntityId) { this.originalEntityId = originalEntityId; }

    public String getNewEmail() { return newEmail; }
    public void setNewEmail(String newEmail) { this.newEmail = newEmail; }

    public String getNewUsername() { return newUsername; }
    public void setNewUsername(String newUsername) { this.newUsername = newUsername; }

    public String getReason() { return reason; }
    public void setReason(String reason) { this.reason = reason; }

    public String getFullName() { return fullName; }
    public void setFullName(String fullName) { this.fullName = fullName; }

    public String getRole() { return role; }
    public void setRole(String role) { this.role = role; }

    public String getUserType() { return userType; }
    public void setUserType(String userType) { this.userType = userType; }

    public String getDepartment() { return department; }
    public void setDepartment(String department) { this.department = department; }

    public String getDesignation() { return designation; }
    public void setDesignation(String designation) { this.designation = designation; }

    public String getMobileNumber() { return mobileNumber; }
    public void setMobileNumber(String mobileNumber) { this.mobileNumber = mobileNumber; }

    public String getExternalDepartmentName() { return externalDepartmentName; }
    public void setExternalDepartmentName(String externalDepartmentName) { this.externalDepartmentName = externalDepartmentName; }

    public String getExternalSupervisorName() { return externalSupervisorName; }
    public void setExternalSupervisorName(String externalSupervisorName) { this.externalSupervisorName = externalSupervisorName; }

    public String getExternalSupervisorEmail() { return externalSupervisorEmail; }
    public void setExternalSupervisorEmail(String externalSupervisorEmail) { this.externalSupervisorEmail = externalSupervisorEmail; }

    public String getExternalSupervisorPhone() { return externalSupervisorPhone; }
    public void setExternalSupervisorPhone(String externalSupervisorPhone) { this.externalSupervisorPhone = externalSupervisorPhone; }
}
