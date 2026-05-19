package com.jpb.reconciliation.reconciliation.entity;

import lombok.*;

import javax.persistence.*;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;

@Entity
@Table(name = "REC_ROLE_MASTER")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
@JsonIgnoreProperties({"hibernateLazyInitializer", "handler"})
public class RecRoleMaster {

    @Id
    @GeneratedValue(strategy = GenerationType.SEQUENCE,
            generator = "role_master_seq")
    @SequenceGenerator(
            name = "role_master_seq",
            sequenceName = "ROLE_MASTER_SEQ",
            allocationSize = 1
    )
    private Long id;

    @Column(name = "ROLE_NAME", nullable = false, unique = true)
    private String roleName;

    @Column(name = "ROLE_CODE", nullable = false, unique = true)
    private Integer roleCode;

    @Column(name = "IS_SYSTEM_ROLE")
    private Boolean isSystemRole;

    @Column(name = "STATUS")
    private String status;
}