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

    // ✅ DB requires this column
    @Column(name = "username", nullable = false)
    private String username;

    // ✅ only required data
    @Column(name = "super_user_id")
    private String superUserId;

    @Column(name = "email", nullable = false)
    private String email;

    @Column(name = "password")
    private String password;

    @Column(name = "status")
    private String status;

    @Column(name = "created_at")
    private LocalDateTime createdAt;
}