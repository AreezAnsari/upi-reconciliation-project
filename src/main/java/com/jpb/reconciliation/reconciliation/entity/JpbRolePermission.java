//package com.jpb.reconciliation.reconciliation.entity;
//
//
//import lombok.*;
//import javax.persistence.*;
//import java.time.LocalDateTime;
//
//@Entity
//@Table(
//    name = "JPB_ROLE_PERMISSIONS_TEST",
//    uniqueConstraints = @UniqueConstraint(columnNames = {"ROLE_ID", "MODULE_ID"})
//)
//@Data
//@NoArgsConstructor
//@AllArgsConstructor
//@Builder
//public class JpbRolePermission {
//    
//    @Id
//    @GeneratedValue(strategy = GenerationType.IDENTITY)
//    private Long id;
//    
//    @ManyToOne(fetch = FetchType.EAGER)
//    @JoinColumn(name = "ROLE_ID", nullable = false)
//    private JpbRole role;
//    
//    @ManyToOne(fetch = FetchType.EAGER)
//    @JoinColumn(name = "MODULE_ID", nullable = false)
//    private JpbModule module;
//    
//    @Column(name = "HAS_ACCESS")
//    @Builder.Default
//    private boolean hasAccess = false;
//    
//    @Column(name = "CAN_VIEW")
//    @Builder.Default
//    private boolean canView = false;
//    
//    @Column(name = "CAN_CREATE")
//    @Builder.Default
//    private boolean canCreate = false;
//    
//    @Column(name = "CAN_EDIT")
//    @Builder.Default
//    private boolean canEdit = false;
//    
//    @Column(name = "CAN_APPROVE")
//    @Builder.Default
//    private boolean canApprove = false;
//    
//    @Column(name = "CAN_DOWNLOAD")
//    @Builder.Default
//    private boolean canDownload = false;
//    
//    @Column(name = "CREATED_AT", updatable = false)
//    private LocalDateTime createdAt;
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