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
@Table(name = "C_PRODUCT_MENU_MAP")
public class CProductMenuMap {

    @EmbeddedId
    private ProductMenuMapId id;

    @ManyToOne(fetch = FetchType.LAZY)
    @MapsId("menuId")
    @JoinColumn(name = "MENU_ID")
    private ReconMenuMaster menu;

    @Embeddable
    @Data
    @NoArgsConstructor
    @AllArgsConstructor
    public static class ProductMenuMapId implements Serializable {
        @Column(name = "PRODUCT_ID")
        private Long productId;

        @Column(name = "MENU_ID")
        private Long menuId;
    }
}
