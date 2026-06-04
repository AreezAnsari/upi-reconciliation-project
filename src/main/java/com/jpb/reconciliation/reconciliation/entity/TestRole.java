package com.jpb.reconciliation.reconciliation.entity;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.Set;

import javax.persistence.Column;
import javax.persistence.Entity;
import javax.persistence.GeneratedValue;
import javax.persistence.GenerationType;
import javax.persistence.Id;
import javax.persistence.OneToMany;
import javax.persistence.SequenceGenerator;
import javax.persistence.Table;

import org.springframework.data.annotation.CreatedBy;
import org.springframework.data.annotation.CreatedDate;
import org.springframework.data.annotation.LastModifiedBy;
import org.springframework.data.annotation.LastModifiedDate;

import com.fasterxml.jackson.annotation.JsonBackReference;

import lombok.AllArgsConstructor;
import lombok.NoArgsConstructor;

@Entity
@AllArgsConstructor
@NoArgsConstructor
@Table(name = "TEST_RCN_ROLE_MASTER")
public class TestRole {

    @Id
    @GeneratedValue(strategy = GenerationType.SEQUENCE, generator = "SEQ_TEST_ROLE_MAST")
    @SequenceGenerator(name = "SEQ_TEST_ROLE_MAST", sequenceName = "SEQ_TEST_ROLE_MAST", allocationSize = 1)
    @Column(name = "ROLE_ID")
    private Long roleId;

    @Column(name = "ROLE_NAME")
    private String roleName;

    @Column(name = "ROLE_CODE")
    private String roleCode;

    @Column(name = "ROLE_TYPE")
    private String roleType;

    @Column(name = "ROLE_STATUS")
    private String roleStatus;

    @Column(name = "ROLE_DESC")
    private String roleDesc;

    @Column(name = "VALID_FROM")
    private LocalDate validFrom;

    @Column(name = "VALID_TO")
    private LocalDate validTo;

    @OneToMany(mappedBy = "role")
    @JsonBackReference
    private Set<ReconUser> reconUser;

    @CreatedDate
    @Column(updatable = false, name = "crated_at")
    private LocalDateTime createdAt;

    @CreatedBy
    @Column(updatable = false, name = "created_by")
    private String createdBy;

    @LastModifiedDate
    @Column(insertable = false, name = "updated_at")
    private LocalDateTime updatedAt;

    @LastModifiedBy
    @Column(insertable = false, name = "updated_by")
    private String updatedBy;

    @Column(name = "APPROVED_YN")
    private String approvedYn = "N";

    @Column(name = "APPROVED_BY")
    private String approvedBy;

    public Long getRoleId() {
        return roleId;
    }

    public void setRoleId(Long roleId) {
        this.roleId = roleId;
    }

    public String getRoleName() {
        return roleName;
    }

    public void setRoleName(String roleName) {
        this.roleName = roleName;
    }

    public String getRoleCode() {
        return roleCode;
    }

    public void setRoleCode(String roleCode) {
        this.roleCode = roleCode;
    }

    public String getRoleType() {
        return roleType;
    }

    public void setRoleType(String roleType) {
        this.roleType = roleType;
    }

    public String getRoleStatus() {
        return roleStatus;
    }

    public void setRoleStatus(String roleStatus) {
        this.roleStatus = roleStatus;
    }

    public String getRoleDesc() {
        return roleDesc;
    }

    public void setRoleDesc(String roleDesc) {
        this.roleDesc = roleDesc;
    }

    public LocalDate getValidFrom() {
        return validFrom;
    }

    public void setValidFrom(LocalDate validFrom) {
        this.validFrom = validFrom;
    }

    public LocalDate getValidTo() {
        return validTo;
    }

    public void setValidTo(LocalDate validTo) {
        this.validTo = validTo;
    }

    public Set<ReconUser> getReconUser() {
        return reconUser;
    }

    public void setReconUser(Set<ReconUser> reconUser) {
        this.reconUser = reconUser;
    }

    public LocalDateTime getCreatedAt() {
        return createdAt;
    }

    public void setCreatedAt(LocalDateTime createdAt) {
        this.createdAt = createdAt;
    }

    public String getCreatedBy() {
        return createdBy;
    }

    public void setCreatedBy(String createdBy) {
        this.createdBy = createdBy;
    }

    public LocalDateTime getUpdatedAt() {
        return updatedAt;
    }

    public void setUpdatedAt(LocalDateTime updatedAt) {
        this.updatedAt = updatedAt;
    }

    public String getUpdatedBy() {
        return updatedBy;
    }

    public void setUpdatedBy(String updatedBy) {
        this.updatedBy = updatedBy;
    }

    public String getApprovedYn() {
        return approvedYn;
    }

    public void setApprovedYn(String approvedYn) {
        this.approvedYn = approvedYn;
    }

    public String getApprovedBy() {
        return approvedBy;
    }

    public void setApprovedBy(String approvedBy) {
        this.approvedBy = approvedBy;
    }
}