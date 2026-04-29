package com.jpb.reconciliation.reconciliation.entity;

import java.time.LocalDateTime;

import javax.persistence.CascadeType;
import javax.persistence.Column;
import javax.persistence.Entity;
import javax.persistence.GeneratedValue;
import javax.persistence.GenerationType;
import javax.persistence.Id;
import javax.persistence.JoinColumn;
import javax.persistence.ManyToOne;
import javax.persistence.OneToOne;
import javax.persistence.SequenceGenerator;
import javax.persistence.Table;

import org.springframework.data.annotation.CreatedBy;
import org.springframework.data.annotation.CreatedDate;
import org.springframework.data.annotation.LastModifiedBy;
import org.springframework.data.annotation.LastModifiedDate;

import com.fasterxml.jackson.annotation.JsonManagedReference;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Entity
@Table(name = "TEST_RCN_RECON_USER")
@Data
@AllArgsConstructor
@NoArgsConstructor
public class TestUser {

    @Id
    @GeneratedValue(strategy = GenerationType.SEQUENCE, generator = "SEQ_TEST_USER")
    @SequenceGenerator(name = "SEQ_TEST_USER", sequenceName = "SEQ_TEST_USER", allocationSize = 1)
    @Column(name = "user_id")
    private Long userId;

    private String institution;
    private String designation;

    @Column(name = "email_id")
    private String emailId;

    private String type;

    @Column(name = "user_status")
    private String userStatus;

    @OneToOne(mappedBy = "testUser", cascade = CascadeType.ALL)
    @JsonManagedReference
    private TestPasswordManager passwordManager;

    // ManyToOne → TestRole (TEST_RCN_ROLE_MASTER)
    @ManyToOne
    @JoinColumn(name = "role_id", nullable = false)
    @JsonManagedReference
    private TestRole role;

    @Column(name = "user_name")
    private String userName;

    @Column(name = "mobile_number")
    private Long mobileNumber;

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

    @Column(name = "LAST_LOGIN")
    private LocalDateTime lastLoginDateTime;
}