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
//@Table(name = "REC_USER_TEST")
//@Getter
//@Setter
//@NoArgsConstructor
//@AllArgsConstructor
//@Builder
//@ToString
//public class RecUserTest {
//
//    @Id
//    @GeneratedValue(strategy = GenerationType.SEQUENCE, generator = "rec_user_seq")
//    @SequenceGenerator(
//        name           = "rec_user_seq",
//        sequenceName   = "REC_USER_SEQ",
//        allocationSize = 1
//    )
//    @Column(name = "USER_ID")
//    private Long userId;
//
//    @Column(name = "EMPLOYEE_CODE", nullable = false, unique = true, length = 20)
//    private String employeeCode;
//
//    @Column(name = "FULL_NAME", nullable = false, length = 100)
//    private String fullName;
//
//    @Column(name = "EMAIL", nullable = false, unique = true, length = 100)
//    private String email;
//
//    @Column(name = "MOBILE", length = 15)
//    private String mobile;
//
//    @Column(name = "DEPARTMENT", length = 50)
//    private String department;
//
//    @Column(name = "DESIGNATION", length = 50)
//    private String designation;
//
//    @Column(name = "IS_ACTIVE")
//    @Builder.Default
//    private Integer isActive = 1;
//
//    @CreationTimestamp
//    @Column(name = "CREATED_AT", updatable = false)
//    private LocalDateTime createdAt;
//
//    @UpdateTimestamp
//    @Column(name = "UPDATED_AT")
//    private LocalDateTime updatedAt;
//}
