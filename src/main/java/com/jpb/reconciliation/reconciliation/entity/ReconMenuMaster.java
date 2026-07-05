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

    @Column(name = "ROLE_ID")
    private Long roleId;

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
