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
    
    public enum RoleType {
        RECON_USER, BANK_USER, BRANCH_USER
    }
 
    public enum UserType {
        INTERNAL, EXTERNAL
    }
 
    public enum UserStatus {
        REQUEST, VERIFIED , ACTIVE, INACTIVE , BLOCK ,
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
    
    // ── NEW: Password fields ──────────────────────────────────────────────────
    
    /**
     * BCrypt-encoded password — set when user sets their own password via
     * the verify link. NULL until user completes first-time setup.
     */
    @Column(name = "PASSWORD", length = 255)
    private String password;
 
    /**
     * Plain-text default password sent in welcome email.
     * Cleared to NULL after user successfully sets their own password.
     */
    @Column(name = "DEFAULT_PASSWORD", length = 100)
    private String defaultPassword;
 
    /**
     * 0 = user has NOT set a personal password yet (new user)
     * 1 = user HAS set their own password (returning user)
     */
    @Column(name = "PASSWORD_SET", nullable = false)
    @Builder.Default
    private Integer passwordSet = 0;
 
    /**
     * 6-digit OTP stored temporarily for forgot-password flow.
     * Cleared after successful use.
     */
    @Column(name = "FORGOT_OTP", length = 10)
    private String forgotOtp;
 
    /**
     * Expiry timestamp for FORGOT_OTP (10 minutes from generation).
     */
    @Column(name = "FORGOT_OTP_EXPIRY")
    private LocalDateTime forgotOtpExpiry;
    
    // ── Profile ───────────────────────────────────────────────────────────────
    
 
    @Column(name = "DEPARTMENT", length = 150)
    private String department;
 
    @Column(name = "DESIGNATION", length = 100)
    private String designation;
 
    @Column(name = "MOBILE_NUMBER", length = 20)
    private String mobileNumber;
    
    	// ── Role ──────────────────────────────────────────────────────────────────
    
 
    @Enumerated(EnumType.STRING)
    @Column(name = "USER_TYPE", nullable = false, length = 20)
    private UserType userType;              // INTERNAL | EXTERNAL
    
    @Enumerated(EnumType.STRING)
    @Column(name = "ROLE", nullable = false, length = 20)
    private Role role;                      // MAKER | CHECKER
    
    @Enumerated(EnumType.STRING)
    @Column(name = "ROLE_TYPE", nullable = false , length = 20)
    private RoleType roleType;
 
    // ── Status ────────────────────────────────────────────────────────────────
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
