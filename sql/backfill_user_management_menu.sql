-- =============================================================================
-- Backfill: Administration > User Management for banks/branches already onboarded.
--
-- ReconBankMasterServiceImpl.ensureAdminMenus() adds this menu, but it only runs when
-- onboarding is attempted for that bank. Institutions onboarded before the menu existed
-- never re-run it, so they need this one-off insert.
--
-- Idempotent: re-running it inserts nothing. Verify with the SELECT at the bottom.
-- DO NOT EXECUTE blindly — run on a copy first.
-- =============================================================================

-- 1. The menu row itself, once per admin role that has an Administration master
--    but no User Management child yet.
INSERT INTO RECON_MENU_MASTER (
    MENU_ID, MENU_TYPE, MENU_NAME, MENU_PARENT, SUB_MENU_FLAG, MENU_URL,
    STATUS, ROLE_ID, CREATED_BY, CREATED_DATE, INSERT_DATE)
SELECT SEQ_MENU.NEXTVAL,
       'Main',
       'User Management',
       'Administration',
       'N',
       CASE WHEN r.ROLE_NAME LIKE 'Branch Admin - %'
            THEN '/branch-admin/user-management'
            ELSE '/bank-admin/user-management' END,
       'Y',
       r.ROLE_ID,
       'MIGRATION',
       SYSDATE,
       SYSDATE
  FROM RECON_ROLE_MASTER r
 WHERE (r.ROLE_NAME LIKE 'Bank Admin - %' OR r.ROLE_NAME LIKE 'Branch Admin - %')
   AND EXISTS (SELECT 1 FROM RECON_MENU_MASTER am
                WHERE am.ROLE_ID = r.ROLE_ID
                  AND am.MENU_NAME = 'Administration'
                  AND am.MENU_TYPE = 'Master')
   AND NOT EXISTS (SELECT 1 FROM RECON_MENU_MASTER um
                    WHERE um.ROLE_ID = r.ROLE_ID
                      AND um.MENU_NAME = 'User Management');

-- 2. Grant it. The Sidebar reads C_ROLE_MENU_MAP, not RECON_MENU_MASTER.ROLE_ID —
--    without this row the menu exists but never renders.
INSERT INTO C_ROLE_MENU_MAP (ROLE_ID, MENU_ID, CREATED_AT, CREATED_BY)
SELECT m.ROLE_ID, m.MENU_ID, SYSTIMESTAMP, 'MIGRATION'
  FROM RECON_MENU_MASTER m
 WHERE m.MENU_NAME = 'User Management'
   AND m.CREATED_BY = 'MIGRATION'
   AND NOT EXISTS (SELECT 1 FROM C_ROLE_MENU_MAP x
                    WHERE x.ROLE_ID = m.ROLE_ID AND x.MENU_ID = m.MENU_ID);

COMMIT;

-- 3. Verify: every Bank/Branch Admin role should return exactly one row.
SELECT r.ROLE_ID, r.ROLE_NAME, m.MENU_ID, m.MENU_URL,
       CASE WHEN x.MENU_ID IS NULL THEN 'NOT GRANTED' ELSE 'OK' END AS GRANT_STATUS
  FROM RECON_ROLE_MASTER r
  JOIN RECON_MENU_MASTER m ON m.ROLE_ID = r.ROLE_ID AND m.MENU_NAME = 'User Management'
  LEFT JOIN C_ROLE_MENU_MAP x ON x.ROLE_ID = m.ROLE_ID AND x.MENU_ID = m.MENU_ID
 WHERE r.ROLE_NAME LIKE 'Bank Admin - %' OR r.ROLE_NAME LIKE 'Branch Admin - %'
 ORDER BY r.ROLE_NAME;
