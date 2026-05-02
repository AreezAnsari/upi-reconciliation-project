package com.jpb.reconciliation.reconciliation.entity;

import java.time.LocalDateTime;

import javax.persistence.CascadeType;
import javax.persistence.Column;
import javax.persistence.Entity;
import javax.persistence.GeneratedValue;
import javax.persistence.GenerationType;
import javax.persistence.Id;
import javax.persistence.OneToOne;
import javax.persistence.SequenceGenerator;
import javax.persistence.Table;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Entity
@Table(name = "KAL_EMPLOYEE_ADMIN")
@Data
@AllArgsConstructor
@NoArgsConstructor
public class KalEmployeeAdmin {

    @Id
    @GeneratedValue(strategy = GenerationType.SEQUENCE, generator = "SEQ_KAL_ADMIN")
    @SequenceGenerator(name = "SEQ_KAL_ADMIN", sequenceName = "SEQ_KAL_ADMIN", allocationSize = 1)
    @Column(name = "employee_id")
    private Long employeeId;

    // firstname.lastname format — enforced in service
    @Column(name = "username", unique = true, nullable = false)
    private String username;

    // Full name — derived or entered
    @Column(name = "full_name")
    private String fullName;

    // @kalinfotech.com only — enforced in service
    @Column(name = "email", unique = true, nullable = false)
    private String email;

    // 10-digit mobile
    @Column(name = "mobile")
    private String mobile;

    // Designation — e.g. "Admin", "Manager"
    @Column(name = "designation")
    private String designation;

    // ACTIVE / INACTIVE / PENDING
    @Column(name = "status")
    private String status;

    // Password stored via KalEmployeePassword (OneToOne)
    @OneToOne(mappedBy = "kalEmployee", cascade = CascadeType.ALL)
    private KalEmployeePassword kalEmployeePassword;

    @Column(name = "created_at", updatable = false)
    private LocalDateTime createdAt;

    @Column(name = "updated_at", insertable = false)
    private LocalDateTime updatedAt;

    @Column(name = "last_login")
    private LocalDateTime lastLogin;
}