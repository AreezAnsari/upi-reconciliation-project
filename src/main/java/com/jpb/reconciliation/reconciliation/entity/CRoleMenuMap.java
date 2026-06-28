package com.jpb.reconciliation.reconciliation.entity;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;
import javax.persistence.*;
import java.io.Serializable;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Entity
@Table(name = "C_ROLE_MENU_MAP")
public class CRoleMenuMap {

    @EmbeddedId
    private RoleMenuMapId id;

    @ManyToOne(fetch = FetchType.LAZY)
    @MapsId("roleId")
    @JoinColumn(name = "ROLE_ID")
    private ReconRoleMaster role;

    @ManyToOne(fetch = FetchType.LAZY)
    @MapsId("menuId")
    @JoinColumn(name = "MENU_ID")
    private ReconMenuMaster menu;

    @Embeddable
    @Data
    @NoArgsConstructor
    @AllArgsConstructor
    public static class RoleMenuMapId implements Serializable {
        @Column(name = "ROLE_ID")
        private Long roleId;

        @Column(name = "MENU_ID")
        private Long menuId;
    }
}
