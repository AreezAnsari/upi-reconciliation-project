-- =============================================================================
-- PHASE 1 / STEP 0 — SYSTEM_MENU_CODE migration.
--
-- SYSTEM_MENU_CODE is the PERMANENT business identity of a menu. MENU_NAME is a
-- display label and may be renamed / localised / re-branded at any time, so nothing
-- may ever key on it. Codes are therefore EXPLICIT and hand-defined — never generated
-- (slugged) from the display name.
--
-- This one-time migration maps the menus that already exist in production onto their
-- predefined codes via the fixed lookup below (the lookup reads the current name only
-- to *identify* the row once; from here on the code is authoritative).
--
-- A menu keeps the same code across every bank — a bank-owned row is a per-bank copy
-- of the same logical menu — so SYSTEM_MENU_CODE is unique only among catalog rows
-- (BANK_ID IS NULL); bank rows legitimately repeat a code.
--
-- Idempotent: the column add is guarded, and the UPDATE only fills NULLs.
-- =============================================================================
SET DEFINE OFF;
SET SERVEROUTPUT ON;
SET PAGESIZE 200 LINESIZE 140 FEEDBACK OFF;

-- ── 1. Add the column if it isn't there yet ──────────────────────────────────
DECLARE
  n NUMBER;
BEGIN
  SELECT COUNT(*) INTO n FROM user_tab_columns
   WHERE table_name = 'RECON_MENU_MASTER' AND column_name = 'SYSTEM_MENU_CODE';
  IF n = 0 THEN
    EXECUTE IMMEDIATE 'ALTER TABLE RECON_MENU_MASTER ADD (SYSTEM_MENU_CODE VARCHAR2(60))';
    DBMS_OUTPUT.PUT_LINE('SYSTEM_MENU_CODE column added.');
  ELSE
    DBMS_OUTPUT.PUT_LINE('SYSTEM_MENU_CODE column already present.');
  END IF;
END;
/

-- ── 2. Assign the predefined codes to the existing (My Organization /
--       Administration / Dashboard) menus. Only fills rows still NULL. ────────
UPDATE RECON_MENU_MASTER
   SET SYSTEM_MENU_CODE =
       CASE
         -- Masters
         WHEN MENU_TYPE = 'Master' AND MENU_NAME = 'My Organization'      THEN 'ORG'
         WHEN MENU_TYPE = 'Master' AND MENU_NAME = 'Administration'       THEN 'ADM'
         WHEN MENU_TYPE = 'Master' AND MENU_NAME = 'Dashboard'            THEN 'DASH'
         -- My Organization mains
         WHEN MENU_NAME = 'Overview'                     THEN 'ORG_OVERVIEW'
         WHEN MENU_NAME = 'My Hierarchy'                 THEN 'ORG_HIERARCHY'
         WHEN MENU_NAME = 'Admin Status'                 THEN 'ORG_ADMIN_STATUS'
         WHEN MENU_NAME = 'Branch Admin Status'          THEN 'ORG_BRANCH_ADMIN_STATUS'
         WHEN MENU_NAME = 'Banks & Branches'             THEN 'ORG_BANKS_BRANCHES'
         WHEN MENU_NAME = 'Bank Onboarding'              THEN 'ORG_BANK_ONBOARDING'
         WHEN MENU_NAME = 'Branch Onboarding'            THEN 'ORG_BRANCH_ONBOARDING'
         WHEN MENU_NAME = 'Branches'                     THEN 'ORG_BRANCHES'
         WHEN MENU_NAME = 'User Status'                  THEN 'ORG_USER_STATUS'
         WHEN MENU_NAME = 'My Queue'                     THEN 'ORG_MY_QUEUE'
         -- Administration mains
         WHEN MENU_NAME = 'Add Role'                     THEN 'ADM_ADD_ROLE'
         WHEN MENU_NAME = 'Add User'                     THEN 'ADM_ADD_USER'
         WHEN MENU_NAME = 'Add Menu'                     THEN 'ADM_ADD_MENU'
         WHEN MENU_NAME = 'Role List'                    THEN 'ADM_ROLE_LIST'
         WHEN MENU_NAME = 'User List'                    THEN 'ADM_USER_LIST'
         WHEN MENU_NAME = 'Menu List'                    THEN 'ADM_MENU_LIST'
         WHEN MENU_NAME = 'User Management'              THEN 'ADM_USER_MGMT'
         WHEN MENU_NAME = 'Handover & Delegation History' THEN 'ADM_HANDOVER_HISTORY'
         WHEN MENU_NAME = 'Approval Request History'     THEN 'ADM_APPROVAL_HISTORY'
         WHEN MENU_NAME = 'Maker Dashboard'              THEN 'ADM_MAKER_DASH'
         WHEN MENU_NAME = 'Checker Dashboard'            THEN 'ADM_CHECKER_DASH'
         ELSE NULL
       END
 WHERE SYSTEM_MENU_CODE IS NULL;
COMMIT;

-- ── 3. Verify: nothing may be left without a code ────────────────────────────
PROMPT ==== Rows still missing a SYSTEM_MENU_CODE (must be EMPTY) ====
COL MENU_NAME FORMAT A32;
SELECT MENU_ID, MENU_TYPE, MENU_NAME, BANK_ID
  FROM RECON_MENU_MASTER WHERE SYSTEM_MENU_CODE IS NULL ORDER BY MENU_ID;

PROMPT ==== Code assignment summary ====
COL SYSTEM_MENU_CODE FORMAT A26;
SELECT SYSTEM_MENU_CODE, MENU_TYPE, COUNT(*) CNT
  FROM RECON_MENU_MASTER
 GROUP BY SYSTEM_MENU_CODE, MENU_TYPE
 ORDER BY MENU_TYPE, SYSTEM_MENU_CODE;
