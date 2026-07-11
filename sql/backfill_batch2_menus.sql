-- =============================================================================
-- Batch 2 menu changes for banks/branches already onboarded.
--   1. Rename "User Management" -> "Handover & Delegation History" (same URL).
--   2. Add "Approval Request History" under Administration (+ grant).
--
-- New onboardings get both from ReconBankMasterServiceImpl; this is the one-off for
-- institutions created earlier. Idempotent. SET DEFINE OFF because '&' is a substitution
-- character in SQL*Plus.
-- =============================================================================
SET DEFINE OFF;

-- 1. Rename the Handover & Delegation screen (was "User Management").
UPDATE RECON_MENU_MASTER
   SET MENU_NAME = 'Handover & Delegation History'
 WHERE MENU_NAME = 'User Management'
   AND MENU_TYPE = 'Main';
COMMIT;

-- 2a. Insert "Approval Request History" once per admin role that has an Administration master
--     but no Approval Request History yet.
INSERT INTO RECON_MENU_MASTER (
    MENU_ID, MENU_TYPE, MENU_NAME, MENU_PARENT, SUB_MENU_FLAG, MENU_URL,
    STATUS, BANK_ID, CREATED_BY, CREATED_DATE, INSERT_DATE)
SELECT SEQ_MENU.NEXTVAL,
       'Main',
       'Approval Request History',
       'Administration',
       'N',
       CASE WHEN r.ROLE_NAME LIKE 'Branch Admin - %' THEN '/branch-admin/approval-history'
            ELSE '/bank-admin/approval-history' END,
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
                    WHERE m.BANK_ID = am.BANK_ID AND m.MENU_NAME = 'Approval Request History');

-- 2b. Grant it to the admin role (Sidebar reads C_ROLE_MENU_MAP).
INSERT INTO C_ROLE_MENU_MAP (ROLE_ID, MENU_ID, CREATED_AT, CREATED_BY)
SELECT r.ROLE_ID, m.MENU_ID, SYSTIMESTAMP, 'MIGRATION'
  FROM RECON_MENU_MASTER m
  JOIN RCN_RECON_USER u ON u.BANK_ID = m.BANK_ID
  JOIN RECON_ROLE_MASTER r ON r.ROLE_ID = u.ROLE_ID
                          AND (r.ROLE_NAME LIKE 'Bank Admin - %' OR r.ROLE_NAME LIKE 'Branch Admin - %')
 WHERE m.MENU_NAME = 'Approval Request History' AND m.CREATED_BY = 'MIGRATION'
   AND NOT EXISTS (SELECT 1 FROM C_ROLE_MENU_MAP x WHERE x.ROLE_ID = r.ROLE_ID AND x.MENU_ID = m.MENU_ID);
COMMIT;

-- 3. Verify.
SELECT r.ROLE_NAME, m.MENU_NAME, m.MENU_URL,
       CASE WHEN x.MENU_ID IS NULL THEN 'NOT GRANTED' ELSE 'OK' END AS GRANT_STATUS
  FROM RECON_ROLE_MASTER r
  JOIN RCN_RECON_USER u ON u.ROLE_ID = r.ROLE_ID
  JOIN RECON_MENU_MASTER m ON m.BANK_ID = u.BANK_ID
                          AND m.MENU_NAME IN ('Handover & Delegation History', 'Approval Request History')
  LEFT JOIN C_ROLE_MENU_MAP x ON x.ROLE_ID = r.ROLE_ID AND x.MENU_ID = m.MENU_ID
 WHERE r.ROLE_NAME LIKE 'Bank Admin - %' OR r.ROLE_NAME LIKE 'Branch Admin - %'
 ORDER BY r.ROLE_NAME, m.MENU_NAME;
