package com.jpb.reconciliation.reconciliation.entity;

import lombok.*;

import javax.persistence.*;
import java.time.LocalDateTime;
 
@Entity
@Table(name = "REC_USER")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
@ToString
public class AddUser {
 
    // ---- Inner enums ----
 
    public enum Role {
        MAKER, CHECKER, WORKER, AUDITOR, IT_OPS, SUPERVISOR, RCC_CXO
    }
 
    public enum UserType {
        INTERNAL, EXTERNAL
    }
 
    public enum UserStatus {
        REQUEST, ACTIVE, INACTIVE , BLOCK , RETIRED
    }
 
    // ---- Fields ----
 
    @Id
    @GeneratedValue(strategy = GenerationType.SEQUENCE, generator = "user_seq")
    @SequenceGenerator(name = "user_seq", sequenceName = "APP_USER_SEQ", allocationSize = 1)
    private Long id;
    
    // NEW: full name stored as typed (e.g. "Karan Joshi")
    @Column(name = "FULL_NAME", nullable = false, length = 200)
    private String fullName;
 
    @Column(name = "USERNAME", unique = true, nullable = false, length = 100)
    private String username;
 
    @Column(name = "EMAIL", unique = true, nullable = false, length = 150)
    private String email;
 
    @Column(name = "DEPARTMENT", length = 150)
    private String department;
 
    @Column(name = "DESIGNATION", length = 100)
    private String designation;
 
    @Column(name = "MOBILE_NUMBER", length = 20)
    private String mobileNumber;
 
    @Enumerated(EnumType.STRING)
    @Column(name = "USER_TYPE", nullable = false, length = 20)
    private UserType userType;              // INTERNAL | EXTERNAL
    
    @Enumerated(EnumType.STRING)
    @Column(name = "ROLE", nullable = false, length = 20)
    private Role role;                      // MAKER | CHECKER
 
    @Enumerated(EnumType.STRING)
    @Column(name = "STATUS", nullable = false, length = 20)
    @Builder.Default
    private UserStatus status = UserStatus.REQUEST;

    
    // ── NEW: external organisation fields (only when userType = EXTERNAL) ────
    @Column(name = "EXTERNAL_DEPARTMENT_NAME", length = 200)
    private String externalDepartmentName;
 
    @Column(name = "EXTERNAL_SUPERVISOR_NAME", length = 100)
    private String externalSupervisorName;
 
    @Column(name = "EXTERNAL_SUPERVISOR_EMAIL", length = 150)
    private String externalSupervisorEmail;
 
    @Column(name = "EXTERNAL_SUPERVISOR_PHONE", length = 20)
    private String externalSupervisorPhone;
    
 // ── Audit ─────────────────────────────────────────────────────────────────
    @Column(name = "INSTITUTION_CODE", length = 50)
    private String institutionCode;
 
    @Column(name = "CREATED_BY", length = 100)
    private String createdBy;
 
    @Column(name = "CREATED_AT", updatable = false)
    private LocalDateTime createdAt;
 
    @Column(name = "UPDATED_AT")
    private LocalDateTime updatedAt;
 
    @PrePersist
    protected void onCreate() {
        this.createdAt = LocalDateTime.now();
        this.updatedAt = LocalDateTime.now();
    }
 
    @PreUpdate
    protected void onUpdate() {
        this.updatedAt = LocalDateTime.now();
    }
}
