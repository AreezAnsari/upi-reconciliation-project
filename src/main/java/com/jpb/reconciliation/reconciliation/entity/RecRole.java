package com.jpb.reconciliation.reconciliation.entity;

import lombok.*;
import org.hibernate.annotations.CreationTimestamp;
import org.hibernate.annotations.UpdateTimestamp;
import javax.persistence.*;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.Random;

@Entity
@Table(name = "REC_ROLES_TEST")
@Getter @Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
@ToString(exclude = "permissions")
public class RecRole {

    @Id
    @GeneratedValue(strategy = GenerationType.SEQUENCE, generator = "role_seq")
    @SequenceGenerator(name = "role_seq", sequenceName = "ROLE_SEQ", allocationSize = 1)
    private Long id;

    @Column(name = "ROLE_NAME", nullable = false, length = 100)
    private String roleName;

    @Column(name = "ROLE_CODE", unique = true, length = 20, insertable = false, updatable = false)
    private String roleCode;  // DB trigger generates this

    @Column(name = "ROLE_TYPE", nullable = false, length = 20)
    private String roleType;  // INTERNAL / EXTERNAL

    @Column(name = "STATUS", nullable = false, length = 20)
    @Builder.Default
    private String status = "DRAFT";

    @Column(name = "DESCRIPTION", length = 500)
    private String description;

    @Column(name = "VALID_FROM")
    private LocalDate validFrom;

    @Column(name = "VALID_TO")
    private LocalDate validTo;

    @Column(name = "CREATED_BY", length = 100)
    private String createdBy;

    @CreationTimestamp
    @Column(name = "CREATED_AT", updatable = false)
    private LocalDateTime createdAt;

    @UpdateTimestamp
    @Column(name = "UPDATED_AT")
    private LocalDateTime updatedAt;
    
//    @PrePersist
//    public void prePersist() {
//        this.createdAt = LocalDateTime.now();
//
//        Random random = new Random();
//        int number = random.nextInt(10000);
//        this.roleCode = "ROLE-" + String.format("%04d", number);
//    }
}

