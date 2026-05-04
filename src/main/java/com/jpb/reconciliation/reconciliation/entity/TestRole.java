package com.jpb.reconciliation.reconciliation.entity;

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
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Entity
@Getter
@Setter
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

    // ✅ NEW: Approval fields (same pattern as ReconUser)
    @Column(name = "APPROVED_YN")
    private String approvedYn = "N";

    @Column(name = "APPROVED_BY")
    private String approvedBy;
}
