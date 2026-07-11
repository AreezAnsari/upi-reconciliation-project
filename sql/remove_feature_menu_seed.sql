-- =============================================================================
-- PHASE 1 / STEP 1 — Remove the bad feature-menu seed.
--
-- WHY: sql/seed_feature_menus.sql (now deleted) inserted the feature menus as
-- BANK-OWNED rows (BANK_ID = 45/47) *and* granted them to the admin roles. That
-- violates the design (catalog rows must be BANK_ID NULL and are never granted)
-- and is why feature menus leaked into the admin privilege tree + sidebar.
--
-- SAFETY: those rows are the ONLY ones with CREATED_BY='CATALOG' AND BANK_ID NOT NULL.
-- Every legitimate onboarded row carries CREATED_BY = an admin username (aadil.ansari,
-- areez.ansari, wajdan.ansari, mudassir.khan, raquib.shaikh) or 'SYSTEM'. The pre-check
-- below proves that before anything is deleted.
--
-- Idempotent: re-running after a successful run deletes nothing.
-- =============================================================================
SET DEFINE OFF;
SET PAGESIZE 200 LINESIZE 140 FEEDBACK OFF;
COL CREATED_BY FORMAT A16;

PROMPT ==== PRE-CHECK 1: rows that WILL be deleted (bad seed only) ====
SELECT BANK_ID, MENU_TYPE, COUNT(*) CNT
  FROM RECON_MENU_MASTER
 WHERE CREATED_BY = 'CATALOG' AND BANK_ID IS NOT NULL
 GROUP BY BANK_ID, MENU_TYPE ORDER BY BANK_ID, MENU_TYPE;

SELECT COUNT(*) AS GRANTS_TO_DELETE FROM C_ROLE_MENU_MAP WHERE CREATED_BY = 'CATALOG';

PROMPT ==== PRE-CHECK 2: legitimate rows that MUST SURVIVE (never CREATED_BY='CATALOG') ====
SELECT BANK_ID, CREATED_BY, COUNT(*) CNT
  FROM RECON_MENU_MASTER
 WHERE NOT (CREATED_BY = 'CATALOG' AND BANK_ID IS NOT NULL)
 GROUP BY BANK_ID, CREATED_BY ORDER BY BANK_ID NULLS FIRST, CREATED_BY;

-- ── Delete grants first (FK-safe), then the bank-owned seed rows ─────────────
DELETE FROM C_ROLE_MENU_MAP WHERE CREATED_BY = 'CATALOG';
DELETE FROM RECON_MENU_MASTER WHERE CREATED_BY = 'CATALOG' AND BANK_ID IS NOT NULL;
COMMIT;

PROMPT ==== POST-CHECK: both must be 0 ====
SELECT COUNT(*) AS BAD_MENU_ROWS_LEFT FROM RECON_MENU_MASTER
 WHERE CREATED_BY = 'CATALOG' AND BANK_ID IS NOT NULL;
SELECT COUNT(*) AS BAD_GRANTS_LEFT FROM C_ROLE_MENU_MAP WHERE CREATED_BY = 'CATALOG';

PROMPT ==== POST-CHECK: legitimate rows intact ====
SELECT BANK_ID, CREATED_BY, COUNT(*) CNT
  FROM RECON_MENU_MASTER
 GROUP BY BANK_ID, CREATED_BY ORDER BY BANK_ID NULLS FIRST, CREATED_BY;
