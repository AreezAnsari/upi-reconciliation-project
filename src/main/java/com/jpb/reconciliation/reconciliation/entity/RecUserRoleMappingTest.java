//package com.jpb.reconciliation.reconciliation.entity;
//
//import lombok.*;
//import org.hibernate.annotations.CreationTimestamp;
//import org.hibernate.annotations.UpdateTimestamp;
//
//import javax.persistence.*;
//import java.time.LocalDateTime;
//
//@Entity
//@Table(
//    name = "REC_USER_ROLE_MAPPING_TEST",
//    uniqueConstraints = @UniqueConstraint(
//        name        = "UQ_REC_USER_ROLE",
//        columnNames = {"USER_ID", "ROLE_ID"}
//    )
//)
//@Getter
//@Setter
//@NoArgsConstructor
//@AllArgsConstructor
//@Builder
//@ToString(exclude = {"user", "role"})
//public class RecUserRoleMappingTest {
//
//    @Id
//    @GeneratedValue(strategy = GenerationType.SEQUENCE, generator = "rec_user_role_seq")
//    @SequenceGenerator(
//        name           = "rec_user_role_seq",
//        sequenceName   = "REC_USER_ROLE_SEQ",
//        allocationSize = 1
//    )
//    @Column(name = "MAPPING_ID")
//    private Long mappingId;
//
//    // REC_USER_TEST link
//    @ManyToOne(fetch = FetchType.LAZY)
//    @JoinColumn(name = "USER_ID", nullable = false)
//    private RecUserTest user;
//
//    // REC_ROLE_TEST to link (existing table)
//    @ManyToOne(fetch = FetchType.LAZY)
//    @JoinColumn(name = "ROLE_ID", nullable = false)
//    private RecRole role;
//
//    @Column(name = "MODULE_ASSIGNMENT", length = 50)
//    @Builder.Default
//    private String moduleAssignment = "ROLE_DEFAULTS";
//
//    @Column(name = "STATUS", length = 20)
//    @Builder.Default
//    private String status = "ACTIVE";
//
//    @Column(name = "ASSIGNED_BY", length = 100)
//    private String assignedBy;
//
//    @Column(name = "ASSIGNED_AT")
//    private LocalDateTime assignedAt;
//
//    @Column(name = "REMARKS", length = 500)
//    private String remarks;
//
//    @CreationTimestamp
//    @Column(name = "CREATED_AT", updatable = false)
//    private LocalDateTime createdAt;
//
//    @UpdateTimestamp
//    @Column(name = "UPDATED_AT")
//    private LocalDateTime updatedAt;
//}
