-- ============================================================
-- Fix: default menu structure for KAL_ADMIN / BANK_ADMIN* / BRANCH_ADMIN*
--
-- KAL_ADMIN   -> ONLY "My Organization" (Overview, Bank Branches, Bank
--                Onboarding, My Hierarchy, Admin Status, User Status).
--                No Dashboard, no Administration section.
-- BANK_ADMIN* -> My Organization: Overview, Branches, Branch Onboarding,
--                My Hierarchy, User Status, Branch Admin Status (no My Queue).
--                Administration unchanged. No Dashboard (unchanged).
-- BRANCH_ADMIN* -> My Organization: Overview, My Hierarchy, User Status
--                (My Queue removed). Dashboard + Administration unchanged.
-- ============================================================

SET DEFINE OFF;

-- ============ 1. KAL_ADMIN ============

-- Remove Administration children, then the Administration master item
DELETE FROM KAL_RECON.RECON_MENU_MASTER
WHERE ROLE_ID = (SELECT ROLE_ID FROM KAL_RECON.RECON_ROLE_MASTER WHERE ROLE_CODE = 'KAL_ADMIN')
  AND MENU_PARENT = (
        SELECT TO_CHAR(MENU_ID) FROM KAL_RECON.RECON_MENU_MASTER
        WHERE MENU_NAME = 'Administration'
          AND ROLE_ID = (SELECT ROLE_ID FROM KAL_RECON.RECON_ROLE_MASTER WHERE ROLE_CODE = 'KAL_ADMIN')
  );

DELETE FROM KAL_RECON.RECON_MENU_MASTER
WHERE MENU_NAME = 'Administration'
  AND MENU_PARENT IS NULL
  AND ROLE_ID = (SELECT ROLE_ID FROM KAL_RECON.RECON_ROLE_MASTER WHERE ROLE_CODE = 'KAL_ADMIN');

-- Remove standalone Dashboard
DELETE FROM KAL_RECON.RECON_MENU_MASTER
WHERE MENU_NAME = 'Dashboard'
  AND MENU_PARENT IS NULL
  AND ROLE_ID = (SELECT ROLE_ID FROM KAL_RECON.RECON_ROLE_MASTER WHERE ROLE_CODE = 'KAL_ADMIN');

-- (My Organization list is already Overview/Bank Branches/Bank Onboarding/
--  My Hierarchy/Admin Status/User Status from the previous fix — untouched.)


-- ============ 2. BANK_ADMIN* ============

-- Delete all current My Organization children (so we can reinsert in order)
DELETE FROM KAL_RECON.RECON_MENU_MASTER m
WHERE m.ROLE_ID IN (SELECT ROLE_ID FROM KAL_RECON.RECON_ROLE_MASTER WHERE ROLE_CODE LIKE 'BANK_ADMIN%')
  AND m.MENU_PARENT IN (
        SELECT TO_CHAR(mo.MENU_ID) FROM KAL_RECON.RECON_MENU_MASTER mo
        WHERE mo.MENU_NAME = 'My Organization'
          AND mo.ROLE_ID = m.ROLE_ID
  );

INSERT INTO KAL_RECON.RECON_MENU_MASTER
    (MENU_TYPE, MENU_NAME, MENU_URL, MENU_PARENT, SUB_MENU_FLAG, STATUS, ROLE_ID, CREATED_BY, CREATED_DATE, INSERT_DATE)
SELECT 'Main', 'Overview', '/bank-admin/my-organization/overview', TO_CHAR(mo.MENU_ID), 'N', 'Y', mo.ROLE_ID, 'SYSTEM', SYSDATE, SYSDATE
FROM KAL_RECON.RECON_MENU_MASTER mo
WHERE mo.MENU_NAME = 'My Organization'
  AND mo.ROLE_ID IN (SELECT ROLE_ID FROM KAL_RECON.RECON_ROLE_MASTER WHERE ROLE_CODE LIKE 'BANK_ADMIN%');

INSERT INTO KAL_RECON.RECON_MENU_MASTER
    (MENU_TYPE, MENU_NAME, MENU_URL, MENU_PARENT, SUB_MENU_FLAG, STATUS, ROLE_ID, CREATED_BY, CREATED_DATE, INSERT_DATE)
SELECT 'Main', 'Branches', '/bank-admin/my-organization/branches', TO_CHAR(mo.MENU_ID), 'N', 'Y', mo.ROLE_ID, 'SYSTEM', SYSDATE, SYSDATE
FROM KAL_RECON.RECON_MENU_MASTER mo
WHERE mo.MENU_NAME = 'My Organization'
  AND mo.ROLE_ID IN (SELECT ROLE_ID FROM KAL_RECON.RECON_ROLE_MASTER WHERE ROLE_CODE LIKE 'BANK_ADMIN%');

INSERT INTO KAL_RECON.RECON_MENU_MASTER
    (MENU_TYPE, MENU_NAME, MENU_URL, MENU_PARENT, SUB_MENU_FLAG, STATUS, ROLE_ID, CREATED_BY, CREATED_DATE, INSERT_DATE)
SELECT 'Main', 'Branch Onboarding', '/bank-admin/branch-onboarding', TO_CHAR(mo.MENU_ID), 'N', 'Y', mo.ROLE_ID, 'SYSTEM', SYSDATE, SYSDATE
FROM KAL_RECON.RECON_MENU_MASTER mo
WHERE mo.MENU_NAME = 'My Organization'
  AND mo.ROLE_ID IN (SELECT ROLE_ID FROM KAL_RECON.RECON_ROLE_MASTER WHERE ROLE_CODE LIKE 'BANK_ADMIN%');

INSERT INTO KAL_RECON.RECON_MENU_MASTER
    (MENU_TYPE, MENU_NAME, MENU_URL, MENU_PARENT, SUB_MENU_FLAG, STATUS, ROLE_ID, CREATED_BY, CREATED_DATE, INSERT_DATE)
SELECT 'Main', 'My Hierarchy', '/bank-admin/my-organization/hierarchy', TO_CHAR(mo.MENU_ID), 'N', 'Y', mo.ROLE_ID, 'SYSTEM', SYSDATE, SYSDATE
FROM KAL_RECON.RECON_MENU_MASTER mo
WHERE mo.MENU_NAME = 'My Organization'
  AND mo.ROLE_ID IN (SELECT ROLE_ID FROM KAL_RECON.RECON_ROLE_MASTER WHERE ROLE_CODE LIKE 'BANK_ADMIN%');

INSERT INTO KAL_RECON.RECON_MENU_MASTER
    (MENU_TYPE, MENU_NAME, MENU_URL, MENU_PARENT, SUB_MENU_FLAG, STATUS, ROLE_ID, CREATED_BY, CREATED_DATE, INSERT_DATE)
SELECT 'Main', 'User Status', '/bank-admin/my-organization/user-status', TO_CHAR(mo.MENU_ID), 'N', 'Y', mo.ROLE_ID, 'SYSTEM', SYSDATE, SYSDATE
FROM KAL_RECON.RECON_MENU_MASTER mo
WHERE mo.MENU_NAME = 'My Organization'
  AND mo.ROLE_ID IN (SELECT ROLE_ID FROM KAL_RECON.RECON_ROLE_MASTER WHERE ROLE_CODE LIKE 'BANK_ADMIN%');

INSERT INTO KAL_RECON.RECON_MENU_MASTER
    (MENU_TYPE, MENU_NAME, MENU_URL, MENU_PARENT, SUB_MENU_FLAG, STATUS, ROLE_ID, CREATED_BY, CREATED_DATE, INSERT_DATE)
SELECT 'Main', 'Branch Admin Status', '/bank-admin/my-organization/admin-status', TO_CHAR(mo.MENU_ID), 'N', 'Y', mo.ROLE_ID, 'SYSTEM', SYSDATE, SYSDATE
FROM KAL_RECON.RECON_MENU_MASTER mo
WHERE mo.MENU_NAME = 'My Organization'
  AND mo.ROLE_ID IN (SELECT ROLE_ID FROM KAL_RECON.RECON_ROLE_MASTER WHERE ROLE_CODE LIKE 'BANK_ADMIN%');

-- (Administration section for BANK_ADMIN* is unchanged.)


-- ============ 3. BRANCH_ADMIN* ============

-- Just remove "My Queue" — removing it doesn't disturb the relative
-- MENU_ID order of the remaining items (Overview, My Hierarchy, User Status).
DELETE FROM KAL_RECON.RECON_MENU_MASTER
WHERE MENU_NAME = 'My Queue'
  AND ROLE_ID IN (SELECT ROLE_ID FROM KAL_RECON.RECON_ROLE_MASTER WHERE ROLE_CODE LIKE 'BRANCH_ADMIN%');

-- (Dashboard + Administration section for BRANCH_ADMIN* are unchanged.)

COMMIT;
