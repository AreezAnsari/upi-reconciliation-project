//package com.jpb.reconciliation.reconciliation.entity;
//
//
//import lombok.*;
//import javax.persistence.*;
//import java.time.LocalDateTime;
//
//@Entity
//@Table(name = "JPB_ROLES_TEST")
//@Data
//@NoArgsConstructor
//@AllArgsConstructor
//@Builder
//public class JpbRole {
//    
//    @Id
//    @GeneratedValue(strategy = GenerationType.IDENTITY)
//    private Long id;
//    
//    @Column(name = "ROLE_NAME", nullable = false, unique = true, length = 100)
//    private String roleName;
//    
//    @Column(name = "ROLE_TYPE", nullable = false, length = 20)
//    private String roleType;
//    
//    @Column(name = "STATUS", nullable = false, length = 20)
//    @Builder.Default
//    private String status = "ACTIVE";
//    
//    @Column(name = "DESCRIPTION", length = 500)
//    private String description;
//    
//    @Column(name = "CREATED_BY", length = 100)
//    private String createdBy;
//    
//    @Column(name = "CREATED_AT", updatable = false)
//    private LocalDateTime createdAt;
//    
//    @Column(name = "UPDATED_BY", length = 100)
//    private String updatedBy;
//    
//    @Column(name = "UPDATED_AT")
//    private LocalDateTime updatedAt;
//    
//    @PrePersist
//    protected void onCreate() {
//        createdAt = LocalDateTime.now();
//        updatedAt = LocalDateTime.now();
//    }
//    
//    @PreUpdate
//    protected void onUpdate() {
//        updatedAt = LocalDateTime.now();
//    }
//}