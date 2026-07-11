-- =============================================================================
-- Fix UX_MENU_PRODUCT_NAME_PARENT.
--
-- The old index was UNIQUE on (PRODUCT_ID, MENU_NAME, MENU_PARENT) for any row with a product.
-- Three things are wrong with that:
--
--   1. BANK_ID is missing. An appended row is per-institution, so two different banks mapping the
--      SAME sub-menu for the SAME product collided — the second bank simply could not map it.
--      This is the bug that matters.
--   2. It keys on MENU_NAME. A display name is renameable; identity is SYSTEM_MENU_CODE.
--   3. MENU_PROCESS_ID is missing. A process-driven sub-menu is legitimately mapped once per
--      process, so those rows must not collide either.
--
-- The replacement is exactly the duplicate key the service enforces (resolveExistingMapping):
--      process-driven : BANK_ID + PRODUCT_ID + SYSTEM_MENU_CODE + MENU_PROCESS_ID
--      fixed-URL      : BANK_ID + PRODUCT_ID + SYSTEM_MENU_CODE   (process is NULL)
--
-- Catalog rows (PRODUCT_ID NULL) stay out of it — they are covered by UX_MENU_CATALOG_CODE.
-- =============================================================================
SET SERVEROUTPUT ON;
SET PAGESIZE 100 LINESIZE 130 FEEDBACK OFF;

PROMPT ==== Any rows that would violate the NEW key? (must be empty) ====
SELECT BANK_ID, PRODUCT_ID, SYSTEM_MENU_CODE, MENU_PROCESS_ID, COUNT(*) DUPES
  FROM RECON_MENU_MASTER
 WHERE PRODUCT_ID IS NOT NULL
 GROUP BY BANK_ID, PRODUCT_ID, SYSTEM_MENU_CODE, MENU_PROCESS_ID
HAVING COUNT(*) > 1;

DECLARE
  n NUMBER;
BEGIN
  SELECT COUNT(*) INTO n FROM user_indexes WHERE index_name = 'UX_MENU_PRODUCT_NAME_PARENT';
  IF n > 0 THEN
    EXECUTE IMMEDIATE 'DROP INDEX UX_MENU_PRODUCT_NAME_PARENT';
    DBMS_OUTPUT.PUT_LINE('Dropped UX_MENU_PRODUCT_NAME_PARENT (was missing BANK_ID).');
  END IF;

  SELECT COUNT(*) INTO n FROM user_indexes WHERE index_name = 'UX_MENU_BANK_PRODUCT_CODE';
  IF n = 0 THEN
    EXECUTE IMMEDIATE
      'CREATE UNIQUE INDEX UX_MENU_BANK_PRODUCT_CODE ON RECON_MENU_MASTER ('
      || 'CASE WHEN PRODUCT_ID IS NULL THEN NULL ELSE BANK_ID END, '
      || 'CASE WHEN PRODUCT_ID IS NULL THEN NULL ELSE PRODUCT_ID END, '
      || 'CASE WHEN PRODUCT_ID IS NULL THEN NULL ELSE SYSTEM_MENU_CODE END, '
      || 'CASE WHEN PRODUCT_ID IS NULL THEN NULL ELSE MENU_PROCESS_ID END)';
    DBMS_OUTPUT.PUT_LINE('Created UX_MENU_BANK_PRODUCT_CODE (bank + product + code + process).');
  ELSE
    DBMS_OUTPUT.PUT_LINE('UX_MENU_BANK_PRODUCT_CODE already present.');
  END IF;
END;
/

PROMPT ==== Unique indexes now on RECON_MENU_MASTER ====
COL INDEX_NAME FORMAT A30
SELECT index_name, uniqueness FROM user_indexes
 WHERE table_name = 'RECON_MENU_MASTER' AND uniqueness = 'UNIQUE' ORDER BY index_name;

EXIT;
