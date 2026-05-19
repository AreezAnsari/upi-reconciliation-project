package com.jpb.reconciliation.reconciliation.entity;

import lombok.*;
import javax.persistence.*;

import com.fasterxml.jackson.annotation.JsonIgnore;
import com.fasterxml.jackson.annotation.JsonIgnoreProperties;

@Entity
@Table(
    name = "REC_ROLE_MODULE_PERMISSIONS_TEST",
    uniqueConstraints = @UniqueConstraint(columnNames = {"ROLE_ID", "MODULE_ID"})
)
@Getter
@Setter
@EqualsAndHashCode(exclude = "role")   // prevent circular reference
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class RecRoleModulePermission {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "ROLE_ID", nullable = false)
    @JsonIgnore
    private RecRole role;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "MODULE_ID", nullable = false)
    @JsonIgnoreProperties({"hibernateLazyInitializer", "handler"}) 
    private RecModule module;

    @Column(name = "HAS_ACCESS")  @Builder.Default private boolean hasAccess  = false;
    @Column(name = "CAN_VIEW")    @Builder.Default private boolean canView     = false;
    @Column(name = "CAN_CREATE")  @Builder.Default private boolean canCreate   = false;
    @Column(name = "CAN_EDIT")    @Builder.Default private boolean canEdit     = false;
    @Column(name = "CAN_APPROVE") @Builder.Default private boolean canApprove  = false;
    @Column(name = "CAN_DOWNLOAD")@Builder.Default private boolean canDownload = false;
}
