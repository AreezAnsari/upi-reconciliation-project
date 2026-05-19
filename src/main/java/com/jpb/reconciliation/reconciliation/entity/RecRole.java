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

    @Column(name = "ROLE_NAME", nullable = false, length = 100)
    private String roleName;

    @Column(name = "ROLE_CODE", unique = true, length = 20)
    private String roleCode;  // DB trigger generates this

    @Column(name = "ROLE_TYPE", nullable = false, length = 20)
    private String roleType;  // INTERNAL / EXTERNAL

    @Column(name = "STATUS", nullable = false, length = 20)
    @Builder.Default
    private String status = "DRAFT";

    @Column(name = "DESCRIPTION", length = 500)
    private String description;
    
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

    @Column(name = "EXTERNAL_DEPARTMENT_NAME")
    private String externalDepartmentName;

    @Column(name = "EXTERNAL_SUPERVISOR_NAME")
    private String externalSupervisorName;

    @Column(name = "EXTERNAL_SUPERVISOR_EMAIL")
    private String externalSupervisorEmail;
    
    @Column(name = "EXTERNAL_SUPERVISOR_PHONE")
    private String externalSupervisorPhone;

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

