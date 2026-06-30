package com.jpb.reconciliation.reconciliation.entity;

import lombok.*;
import org.hibernate.annotations.CreationTimestamp;
import org.hibernate.annotations.UpdateTimestamp;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;

import javax.persistence.*;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

@Entity
@Table(name = "REC_ROLES_TEST")
@Getter @Setter
@NoArgsConstructor
@Builder
@AllArgsConstructor
@ToString(exclude = {"roleMasters","permissions"})
public class RecRole {

    @Id
    @GeneratedValue(strategy = GenerationType.SEQUENCE, generator = "role_seq")
    @SequenceGenerator(name = "role_seq", sequenceName = "ROLE_SEQ", allocationSize = 1)
    private Long id;
    
 // ── CHANGED: was @ManyToOne RecRoleMaster roleMaster ─────────────────────
    // Now @ManyToMany — one RecRole can hold MAKER + SUPERVISOR together etc.
    // Backed by join table REC_ROLE_MASTER_MAP (run join_table.sql once).
    @ManyToMany(fetch = FetchType.LAZY, cascade = {CascadeType.PERSIST, CascadeType.MERGE})
    @JoinTable(
        name               = "REC_ROLE_MASTER_MAP",
        joinColumns        = @JoinColumn(name = "ROLE_ID"),
        inverseJoinColumns = @JoinColumn(name = "ROLE_MASTER_ID")
    )
    @Builder.Default
    @JsonIgnoreProperties({"hibernateLazyInitializer", "handler"})
    private Set<RecRoleMaster> roleMasters = new HashSet<>();

    @Column(name = "ROLE_NAME", nullable = false, length = 25)
    private String roleName;

    @Column(name = "ROLE_CODE", nullable = false, unique = true, length = 20)
    private String roleCode;  // DB trigger generates this
    
    
    /**
     * NEW FIELD — stores the StandardRole category name.
     * Values: "MAKER", "CHECKER", "WORKER", "AUDITOR",
     *         "IT_OPS", "SUPERVISOR", "RCC_CXO", "OTHER"
     *
     * Allows querying "all MAKER-family roles" even though each has a
     * different unique roleCode.**/
    
    @Column(name = "ROLE_MASTER_NAME", nullable = false, length = 50)
    private String roleMasterName;   // e.g. "MAKER"

    @Column(name = "ROLE_TYPE", nullable = false, length = 20)
    private String roleType;  // INTERNAL / EXTERNAL

    @Column(name = "STATUS", nullable = false, length = 20)
    @Builder.Default
    private String status = "ACTIVE"; // ACTIVE | DELETED
    
    @Column(name = "SESSION_TIMEOUT", nullable = false)
    @Builder.Default
    private Integer sessionTimeout = 15; // default 30 minutes

    @Column(name = "DESCRIPTION", length = 500)
    private String description;
    
    @Column(name = "DEPARTMENT", length = 500)
    private String department;
    
 // New for assigning user entity
    @Column(name = "ASSIGNED_USER_ID")
    private Long assignedUserId;

    @Column(name = "ASSIGNED_USER_NAME", length = 200)
    private String assignedUserName;

    @Column(name = "ASSIGNED_USER_EMAIL", length = 150)
    private String assignedUserEmail;

    @Column(name = "VALID_FROM")
    private LocalDate validFrom;

    @Column(name = "VALID_TO")
    private LocalDate validTo;
    
   // ── External-org fields (only required when roleType = EXTERNAL) ──────────

//    @Column(name = "EXTERNAL_DEPARTMENT_NAME")
//    private String externalDepartmentName;
//
//    @Column(name = "EXTERNAL_SUPERVISOR_NAME")
//    private String externalSupervisorName;
//
//    @Column(name = "EXTERNAL_SUPERVISOR_EMAIL")
//    private String externalSupervisorEmail;
//    
//    @Column(name = "EXTERNAL_SUPERVISOR_PHONE")
//    private String externalSupervisorPhone;

    // ── Bank / Branch scope ───────────────────────────────────────────────────
    // Bank Admin creates role → bankCode set, branchCode null
    // Branch Admin creates role → bankCode (parent) + branchCode both set

    @Column(name = "BANK_CODE", length = 50)
    private String bankCode;

    @Column(name = "BRANCH_CODE", length = 50)
    private String branchCode;

    // ── Audit ─────────────────────────────────────────────────────────────────

    @Column(name = "CREATED_BY", length = 100)
    private String createdBy;

    @CreationTimestamp
    @Column(name = "CREATED_AT", updatable = false)
    private LocalDateTime createdAt;

    @UpdateTimestamp
    @Column(name = "UPDATED_AT")
    private LocalDateTime updatedAt;
    
    // ── NEW: use your existing RecRoleModulePermission ────────────────────────
    // mappedBy = "role"  →  RecRoleModulePermission.role field
    @OneToMany(mappedBy = "role", cascade = CascadeType.ALL,
               orphanRemoval = true, fetch = FetchType.LAZY)
    @Builder.Default
    @JsonIgnoreProperties({"hibernateLazyInitializer", "handler"})
    private List<RecRoleModulePermission> permissions = new ArrayList<>();


 // ── Helpers ───────────────────────────────────────────────────────────────
    
    // NEW: wire a master into the join table
    public void addRoleMaster(RecRoleMaster master) {
        this.roleMasters.add(master);
    }
    
    public void addPermission(RecRoleModulePermission permission) {
        permission.setRole(this);
        this.permissions.add(permission);
    }

    
//    @PrePersist
//    public void prePersist() {
//        this.createdAt = LocalDateTime.now();
//
//        Random random = new Random();
//        int number = random.nextInt(10000);
//        this.roleCode = "ROLE-" + String.format("%04d", number);
//    }
}

