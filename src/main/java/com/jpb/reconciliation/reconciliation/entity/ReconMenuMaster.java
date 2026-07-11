package com.jpb.reconciliation.reconciliation.entity;

import java.sql.Timestamp;
import java.util.Date;

import javax.persistence.Column;
import javax.persistence.Entity;
import javax.persistence.GeneratedValue;
import javax.persistence.GenerationType;
import javax.persistence.Id;
import javax.persistence.SequenceGenerator;
import javax.persistence.Table;

import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import lombok.ToString;

@Entity
@Getter
@Setter
@AllArgsConstructor
@NoArgsConstructor
@ToString
@Table(name = "RECON_MENU_MASTER")
public class ReconMenuMaster {

    @Id
    @GeneratedValue(strategy = GenerationType.SEQUENCE, generator = "SEQ_MENU")
    @SequenceGenerator(name = "SEQ_MENU", sequenceName = "SEQ_MENU", allocationSize = 1)
    @Column(name = "MENU_ID")
    private Long menuId;

    // Permanent business identity of a menu. MENU_NAME is a display label and may be renamed at
    // any time, so no logic may key on it — validation, hierarchy resolution and duplicate
    // detection all key on this code instead. A catalog row defines the code; every bank-owned
    // row appended from that catalog row inherits it (so the same code repeats across banks and
    // products, and is unique only among catalog rows — see index UX_MENU_CATALOG_CODE).
    @Column(name = "SYSTEM_MENU_CODE")
    private String systemMenuCode;

    @Column(name = "MENU_TYPE")
    private String menuType;

    @Column(name = "MENU_NAME")
    private String menuName;

    @Column(name = "DESCRIPTION")
    private String menuDescription;

    @Column(name = "MENU_PARENT")
    private String parentMenuCode;

    @Column(name = "SUB_MENU_FLAG")
    private String subMenu;

    @Column(name = "MASTER_MENU_PARENT")
    private String masterMenuParent;

    @Column(name = "MENU_URL")
    private String menuUrl;

    @Column(name = "OPERATIONS")
    private String operations;

    @Column(name = "STATUS")
    private String status;

    @Column(name = "CREATED_BY")
    private String createdBy;

    @Column(name = "CREATED_DATE")
    private Date createdDate;

    @Column(name = "MODIFIED_BY")
    private String modifiedBy;

    @Column(name = "MODIFIED_DATE")
    private Timestamp modifiedDate;

    @Column(name = "USER_ID")
    private Long insertUserId;

    @Column(name = "INSERT_DATE")
    private Date insertDate;

    @Column(name = "INSERT_CODE")
    private Long insertCode;

    @Column(name = "LAST_UPDATED_USER")
    private Long lastUpdatedUser;

    @Column(name = "LAST_UPDATED_DATE")
    private Date lastUpdatedDate;

    @Column(name = "MENU_PROCESS_ID")
    private Long menuProcessId;

    @Column(name = "PROCESS_TYPE")
    private String processType;

    // ── Ownership and scope ──────────────────────────────────────────────────────
    // These answer two different questions, and C_ROLE_MENU_MAP answers a third:
    //     BANK_ID    : which institution this row belongs to
    //     PRODUCT_ID : which product it belongs to
    //     C_ROLE_MENU_MAP : which roles may see it
    //
    // A branch is its own RECON_BANK_MASTER row, so one BANK_ID covers banks and branches.
    // Replaces ROLE_ID, whose only real job was reaching the bank (bank -> users -> roles ->
    // menus). PRODUCT_ID cannot do this: RECON_PRODUCT_MASTER.PRODUCT_NAME is unique, so one
    // product row is shared by every bank that bought it — it identifies the product, never
    // the owner.
    //
    // NULL on both = a catalog (reference) row: registered for validation, owned by nobody,
    // never granted, so it never reaches a sidebar. Kal Admin's bootstrap menus also carry a
    // NULL BANK_ID (KAL_ADMIN has no bank) but are granted, which is what tells them apart.
    @Column(name = "BANK_ID")
    private Long bankId;

    // NULL means "no product restriction" — platform menus (Dashboard / My Organization /
    // Administration) that every product sees. Same "empty scope = unrestricted" convention
    // resolveProductScope()/isVisibleToChecker() already apply to C_ROLE_PRODUCT_MAP.
    @Column(name = "PRODUCT_ID")
    private Long productId;

    @Column(name = "SUBMITTED_BY")
    private String submittedBy;

    @Column(name = "APPROVED_BY")
    private String approvedBy;

    // "Y" for a /user-portal twin auto-created by ReconRoleMasterServiceImpl.savePrivileges()
    // (see getOrCreateUserTwinMenu) — an internal implementation detail, never something an
    // Admin selects directly, so it's excluded from the Menu Access Tree / Menu List.
    @Column(name = "IS_PORTAL_TWIN")
    private String isPortalTwin;

    // For a twin row only — the MENU_ID of the original Admin-portal menu it was cloned from.
    // A twin's own MENU_ID reflects when it happened to be created (privilege-assignment time),
    // not the original's canonical bootstrap position — this lets the Sidebar sort a twin back
    // into that same canonical position instead of wherever it landed chronologically.
    @Column(name = "TWIN_OF_MENU_ID")
    private Long twinOfMenuId;
}
