package com.jpb.reconciliation.reconciliation.dto;

import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import lombok.ToString;

@Getter
@Setter
@AllArgsConstructor
@NoArgsConstructor
@ToString
public class ReconMenuMasterDto {

    private Long menuId;
    /** Permanent catalog identity of the menu being mapped. Authoritative — the backend validates
     *  and resolves against this, not against the (renameable) display name. */
    private String systemMenuCode;
    private String menuType;
    private String menuName;
    private String menuDescription;
    private String parentMenuCode;
    private String masterMenuParent;
    private String subMenuReq;
    private String menuUrl;
    private String operations;
    private Long userId;
    private Long roleId;
    private Long productId;
    private Long menuProcessId;
    private String processType;
    private String reconFilePath;
}
