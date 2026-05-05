package com.jpb.reconciliation.reconciliation.entity;

import lombok.Data;

import javax.persistence.*;

@Data
@Entity
@Table(name = "user_assignment")
public class UserAssignment {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    private Long userId;
    private Long moduleId;
    private String role;
}


