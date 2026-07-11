-- =============================================================================
-- Backfill: Administration > Maker Dashboard for banks/branches already onboarded.
--
-- New onboardings get it from ReconBankMasterServiceImpl.createDefaultAdminMenus (and the
-- ensureAdminMenus backfill runs on the next onboarding attempt). This is the one-off for
-- institutions created before the menu existed.
--
-- Idempotent: re-running inserts nothing. Verify with the SELECT at the bottom.
-- =============================================================================

-- 1. The menu row, once per admin role that has an Administration master but no Maker Dashboard.
INSERT INTO RECON_MENU_MASTER (
    MENU_ID, MENU_TYPE, MENU_NAME, MENU_PARENT, SUB_MENU_FLAG, MENU_URL,
    STATUS, BANK_ID, CREATED_BY, CREATED_DATE, INSERT_DATE)
SELECT SEQ_MENU.NEXTVAL,
       'Main',
       'Maker Dashboard',
       'Administration',
       'N',
       CASE WHEN r.ROLE_NAME LIKE 'Branch Admin - %' THEN '/branch-admin/maker-queue'
            ELSE '/bank-admin/maker-queue' END,
       'Y',
       am.BANK_ID,
       'MIGRATION',
       SYSDATE,
       SYSDATE
  FROM RECON_ROLE_MASTER r
  JOIN RECON_MENU_MASTER am
    ON am.MENU_NAME = 'Administration' AND am.MENU_TYPE = 'Master' AND am.BANK_ID IS NOT NULL
   AND am.BANK_ID IN (SELECT u.BANK_ID FROM RCN_RECON_USER u WHERE u.ROLE_ID = r.ROLE_ID)
 WHERE (r.ROLE_NAME LIKE 'Bank Admin - %' OR r.ROLE_NAME LIKE 'Branch Admin - %')
   AND NOT EXISTS (SELECT 1 FROM RECON_MENU_MASTER m
                    WHERE m.BANK_ID = am.BANK_ID AND m.MENU_NAME = 'Maker Dashboard');

-- 2. Grant it to the admin role — the Sidebar reads C_ROLE_MENU_MAP, not RECON_MENU_MASTER.
INSERT INTO C_ROLE_MENU_MAP (ROLE_ID, MENU_ID, CREATED_AT, CREATED_BY)
SELECT r.ROLE_ID, m.MENU_ID, SYSTIMESTAMP, 'MIGRATION'
  FROM RECON_MENU_MASTER m
  JOIN RCN_RECON_USER u ON u.BANK_ID = m.BANK_ID
  JOIN RECON_ROLE_MASTER r ON r.ROLE_ID = u.ROLE_ID
                          AND (r.ROLE_NAME LIKE 'Bank Admin - %' OR r.ROLE_NAME LIKE 'Branch Admin - %')
 WHERE m.MENU_NAME = 'Maker Dashboard' AND m.CREATED_BY = 'MIGRATION'
   AND NOT EXISTS (SELECT 1 FROM C_ROLE_MENU_MAP x WHERE x.ROLE_ID = r.ROLE_ID AND x.MENU_ID = m.MENU_ID);

COMMIT;

-- 3. Verify: every Bank/Branch Admin role should show one Maker Dashboard row.
SELECT r.ROLE_NAME, m.MENU_ID, m.MENU_URL,
       CASE WHEN x.MENU_ID IS NULL THEN 'NOT GRANTED' ELSE 'OK' END AS GRANT_STATUS
  FROM RECON_ROLE_MASTER r
  JOIN RCN_RECON_USER u ON u.ROLE_ID = r.ROLE_ID
  JOIN RECON_MENU_MASTER m ON m.BANK_ID = u.BANK_ID AND m.MENU_NAME = 'Maker Dashboard'
  LEFT JOIN C_ROLE_MENU_MAP x ON x.ROLE_ID = r.ROLE_ID AND x.MENU_ID = m.MENU_ID
 WHERE r.ROLE_NAME LIKE 'Bank Admin - %' OR r.ROLE_NAME LIKE 'Branch Admin - %'
 ORDER BY r.ROLE_NAME;
