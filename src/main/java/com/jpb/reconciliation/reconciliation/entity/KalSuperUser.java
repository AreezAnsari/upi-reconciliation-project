package com.jpb.reconciliation.reconciliation.entity;

import java.time.LocalDateTime;
import javax.persistence.*;
import lombok.Data;

@Data
@Entity
@Table(name = "KAL_SUPER_USER")
public class KalSuperUser {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "institution_code")
    private String institutionCode;

    // username aur superUserId same value rakhte hain
    @Column(name = "username", nullable = false)
    private String username;

    @Column(name = "super_user_id")
    private String superUserId;

    @Column(name = "email", nullable = false)
    private String email;

    @Column(name = "password")
    private String password;

    @Column(name = "status")
    private String status;

    // ✅ One-time setup guard — ek baar set hone ke baad dobara verify nahi hoga
    @Column(name = "password_set")
    private Boolean passwordSet = false;

    // ✅ Forgot password OTP — DB mein store, 10-min expiry
    @Column(name = "forgot_otp")
    private String forgotOtp;

    @Column(name = "forgot_otp_expiry")
    private LocalDateTime forgotOtpExpiry;

    @Column(name = "created_at")
    private LocalDateTime createdAt;

    @Column(name = "updated_at")
    private LocalDateTime updatedAt;
    
    
}