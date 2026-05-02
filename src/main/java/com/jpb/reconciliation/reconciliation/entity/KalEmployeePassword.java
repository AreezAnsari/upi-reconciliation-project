package com.jpb.reconciliation.reconciliation.entity;

import java.time.LocalDateTime;

import javax.persistence.Column;
import javax.persistence.Entity;
import javax.persistence.GeneratedValue;
import javax.persistence.GenerationType;
import javax.persistence.Id;
import javax.persistence.JoinColumn;
import javax.persistence.OneToOne;
import javax.persistence.SequenceGenerator;
import javax.persistence.Table;

import lombok.Getter;
import lombok.Setter;

@Entity
@Getter
@Setter
@Table(name = "KAL_EMPLOYEE_PASSWORD")
public class KalEmployeePassword {

    @Id
    @GeneratedValue(strategy = GenerationType.SEQUENCE, generator = "SEQ_KAL_ADM_PWD")
    @SequenceGenerator(name = "SEQ_KAL_ADM_PWD", sequenceName = "SEQ_KAL_ADM_PWD", allocationSize = 1)
    @Column(name = "pwd_id")
    private Long pwdId;

    @Column(name = "user_password")
    private String userPassword;

    @Column(name = "expiration_date")
    private LocalDateTime expirationDate;

    // JWT token stored here — same as PasswordManager
    private String token;

    @OneToOne
    @JoinColumn(name = "EMPLOYEE_ID")
    private KalEmployeeAdmin kalEmployee;

    @Column(name = "created_at", updatable = false)
    private LocalDateTime createdAt;

    @Column(name = "updated_at", insertable = false)
    private LocalDateTime updatedAt;
}