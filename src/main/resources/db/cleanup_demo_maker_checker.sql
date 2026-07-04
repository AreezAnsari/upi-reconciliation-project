-- ============================================================
-- Cleanup: removes all rows inserted by dummy_maker_checker_test.sql
-- (safe to run even if only some of those rows made it in, or if
-- some errored out partway through). Run this BEFORE re-running
-- the fixed dummy script.
-- ============================================================

SET DEFINE OFF;

DELETE FROM KAL_RECON.RECON_PRODUCT_CAPABILITY_MAP
WHERE USER_ID IN (SELECT USER_ID FROM KAL_RECON.RCN_RECON_USER WHERE USERNAME LIKE 'demo.%');

DELETE FROM KAL_RECON.C_ROLE_MENU_MAP
WHERE ROLE_ID IN (SELECT ROLE_ID FROM KAL_RECON.RECON_ROLE_MASTER WHERE ROLE_CODE LIKE 'DEMO_%');

DELETE FROM KAL_RECON.RCN_RECON_USER WHERE USERNAME LIKE 'demo.%';

DELETE FROM KAL_RECON.RECON_MENU_MASTER WHERE MENU_NAME LIKE 'Demo %';

DELETE FROM KAL_RECON.RECON_ROLE_MASTER WHERE ROLE_CODE LIKE 'DEMO_%';

COMMIT;
