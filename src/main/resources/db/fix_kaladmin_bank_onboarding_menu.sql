-- ============================================================
-- Fix: menu placement for onboarding items
--   KAL_ADMIN   -> ONLY "Bank Onboarding"   (Main, under My Organization)
--   BANK_ADMIN* -> ONLY "Branch Onboarding" (Main, under My Organization)
-- (*BANK_ADMIN roles are per-bank, e.g. "BANK_ADMIN43238675" — matched by prefix)
-- ============================================================

SET DEFINE OFF;

-- 1. KAL_ADMIN: move "Bank Onboarding" under "My Organization", Master -> Main
UPDATE KAL_RECON.RECON_MENU_MASTER
SET MENU_TYPE   = 'Main',
    MENU_PARENT = (
        SELECT TO_CHAR(m.MENU_ID)
        FROM KAL_RECON.RECON_MENU_MASTER m
        WHERE m.MENU_NAME = 'My Organization'
          AND m.ROLE_ID = (SELECT ROLE_ID FROM KAL_RECON.RECON_ROLE_MASTER WHERE ROLE_CODE = 'KAL_ADMIN')
    )
WHERE MENU_NAME = 'Bank Onboarding'
  AND ROLE_ID = (SELECT ROLE_ID FROM KAL_RECON.RECON_ROLE_MASTER WHERE ROLE_CODE = 'KAL_ADMIN');

-- 2. KAL_ADMIN: remove any "Branch Onboarding" mistakenly added under KAL_ADMIN
DELETE FROM KAL_RECON.RECON_MENU_MASTER
WHERE MENU_NAME = 'Branch Onboarding'
  AND ROLE_ID = (SELECT ROLE_ID FROM KAL_RECON.RECON_ROLE_MASTER WHERE ROLE_CODE = 'KAL_ADMIN');

-- 3. BANK_ADMIN*: move existing standalone "Branch Onboarding" (Master) under
--    "My Organization" (Main), for every bank-admin role that has one.
UPDATE KAL_RECON.RECON_MENU_MASTER bo
SET MENU_TYPE   = 'Main',
    MENU_PARENT = (
        SELECT TO_CHAR(m.MENU_ID)
        FROM KAL_RECON.RECON_MENU_MASTER m
        WHERE m.MENU_NAME = 'My Organization'
          AND m.ROLE_ID = bo.ROLE_ID
    )
WHERE bo.MENU_NAME = 'Branch Onboarding'
  AND bo.ROLE_ID IN (
        SELECT ROLE_ID FROM KAL_RECON.RECON_ROLE_MASTER WHERE ROLE_CODE LIKE 'BANK_ADMIN%'
  )
  AND EXISTS (
        SELECT 1 FROM KAL_RECON.RECON_MENU_MASTER m
        WHERE m.MENU_NAME = 'My Organization'
          AND m.ROLE_ID = bo.ROLE_ID
  );

COMMIT;
