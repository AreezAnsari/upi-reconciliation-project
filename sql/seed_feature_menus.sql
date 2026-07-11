-- =============================================================================
-- Seed the application's FEATURE menu tree (everything except My Organization
-- and Administration, which are already seeded per bank).
--
-- Rows are BANK-OWNED (BANK_ID set) so they surface in:
--   * PrivilegesAssign  -> getMenusByBankId(bankId)  (the grantable tree)
--   * Add Menu parents  -> getMenuByRole(roleId)      (needs the role grant below)
--   * the admin sidebar -> C_ROLE_MENU_MAP grant
--
-- Structure: each MENU_CONFIG group = Master, each item = Main with a MENU_URL.
-- These Mains have no sub-menus, so they show normally (Case: no-submenu Mains are
-- always visible). The reconciliation/extraction PROCESS-mapped submenu flow is
-- exercised later via Add Menu, which appends new Submenu rows under these Masters.
--
-- Feature routes are registered top-level in App.jsx (/setup/*, /process/* …), NOT
-- portal-prefixed, so the same MENU_URL works for admin and /user portals alike
-- (rewriteToUserUrl only twins /bank-admin|/branch-admin|/admin URLs).
--
-- Idempotent: guarded by name+type+bank; grants guarded by role+menu.
-- Seeds bank 45 (role 147) and branch 47 (role 148). Adjust the two seed() calls
-- for other institutions.
-- =============================================================================
SET DEFINE OFF;
SET SERVEROUTPUT ON;

DECLARE
  TYPE t_row IS RECORD (master VARCHAR2(60), main VARCHAR2(60), url VARCHAR2(200));
  TYPE t_tab IS TABLE OF t_row;
  rows t_tab := t_tab(
    -- Configuration
    t_row('Configuration', 'File Configuration',            '/setup/file-config'),
    t_row('Configuration', 'Recon Config',                  '/setup/recon-config'),
    t_row('Configuration', 'Report Configuration',          '/setup/report-config'),
    t_row('Configuration', 'FTP Configuration',             '/setup/ftp-config'),
    t_row('Configuration', 'Force Match Config',            '/setup/force-match'),
    t_row('Configuration', 'Template Config',               '/setup/template-config'),
    t_row('Configuration', 'Process Definition',            '/setup/process-definition'),
    -- Process
    t_row('Process', 'Extraction Transaction Search',       '/process/extraction-search'),
    t_row('Process', 'Bulk Process Match',                  '/process/bulk-match'),
    t_row('Process', 'File Upload',                         '/process/file-upload'),
    t_row('Process', 'Split Transaction',                   '/process/split-transaction'),
    t_row('Process', 'Recon Transaction Search',            '/process/recon-search'),
    -- Reports
    t_row('Reports', 'Generate Report',                     '/reports/generate'),
    t_row('Reports', 'TTUM Report',                         '/reports/ttum'),
    t_row('Reports', 'Reconciliation Report',               '/reports/reconciliation'),
    t_row('Reports', 'Job Details',                         '/reports/extraction-details'),
    -- Dispute Management
    t_row('Dispute Management', 'Dispute Dashboard',        '/dispute/dashboard'),
    t_row('Dispute Management', 'Dispute Action Center',    '/dispute/action-center'),
    t_row('Dispute Management', 'Maker Upload',             '/dispute/maker-upload'),
    t_row('Dispute Management', 'Checker Approval',         '/dispute/checker-approval'),
    -- Reconciliation (new-config definition tools)
    t_row('Reconciliation', 'Add New Template',            '/new-config/add-template'),
    t_row('Reconciliation', 'Define Reconciliation',        '/new-config/reconciliation'),
    t_row('Reconciliation', 'Reconciliation Ops Center',    '/new-config/ops-center'),
    -- UPI Reconciliation
    t_row('UPI Reconciliation', 'UPI Recon Dashboard',      '/upi-recon/dashboard'),
    -- NEFT Reconciliation
    t_row('NEFT Reconciliation', 'NEFT Cycle Management',   '/neft-recon/dashboard')
  );

  PROCEDURE grant_menu(p_role NUMBER, p_menu NUMBER) IS
  BEGIN
    INSERT INTO C_ROLE_MENU_MAP (ROLE_ID, MENU_ID, CREATED_AT, CREATED_BY)
    SELECT p_role, p_menu, SYSTIMESTAMP, 'CATALOG' FROM dual
     WHERE NOT EXISTS (SELECT 1 FROM C_ROLE_MENU_MAP WHERE ROLE_ID = p_role AND MENU_ID = p_menu);
  END;

  PROCEDURE seed(p_bank NUMBER, p_role NUMBER) IS
    v_master_id NUMBER;
    v_main_id   NUMBER;
  BEGIN
    FOR i IN 1 .. rows.COUNT LOOP
      -- Master (once per name+bank)
      BEGIN
        SELECT MENU_ID INTO v_master_id FROM RECON_MENU_MASTER
         WHERE MENU_NAME = rows(i).master AND MENU_TYPE = 'Master' AND BANK_ID = p_bank AND ROWNUM = 1;
      EXCEPTION WHEN NO_DATA_FOUND THEN
        v_master_id := SEQ_MENU.NEXTVAL;
        INSERT INTO RECON_MENU_MASTER (MENU_ID, MENU_TYPE, MENU_NAME, MENU_PARENT, SUB_MENU_FLAG,
               STATUS, BANK_ID, CREATED_BY, CREATED_DATE, INSERT_DATE)
        VALUES (v_master_id, 'Master', rows(i).master, NULL, 'N', 'Y', p_bank, 'CATALOG', SYSDATE, SYSDATE);
      END;
      grant_menu(p_role, v_master_id);

      -- Main (once per name+bank)
      BEGIN
        SELECT MENU_ID INTO v_main_id FROM RECON_MENU_MASTER
         WHERE MENU_NAME = rows(i).main AND MENU_TYPE = 'Main' AND BANK_ID = p_bank AND ROWNUM = 1;
      EXCEPTION WHEN NO_DATA_FOUND THEN
        v_main_id := SEQ_MENU.NEXTVAL;
        INSERT INTO RECON_MENU_MASTER (MENU_ID, MENU_TYPE, MENU_NAME, MENU_PARENT, SUB_MENU_FLAG,
               MENU_URL, STATUS, BANK_ID, CREATED_BY, CREATED_DATE, INSERT_DATE)
        VALUES (v_main_id, 'Main', rows(i).main, rows(i).master, 'N', rows(i).url, 'Y', p_bank, 'CATALOG', SYSDATE, SYSDATE);
      END;
      grant_menu(p_role, v_main_id);
    END LOOP;
    DBMS_OUTPUT.PUT_LINE('Seeded feature menus for bank ' || p_bank || ' / role ' || p_role);
  END;

BEGIN
  seed(45, 147);  -- Bank Admin
  seed(47, 148);  -- Branch Admin
  COMMIT;
END;
/

-- Verify
SET PAGESIZE 200 LINESIZE 160 FEEDBACK OFF
COL MENU_NAME FORMAT A28
COL MENU_PARENT FORMAT A22
COL MENU_URL FORMAT A34
SELECT BANK_ID, MENU_TYPE, MENU_NAME, MENU_PARENT, MENU_URL
  FROM RECON_MENU_MASTER
 WHERE CREATED_BY = 'CATALOG' AND BANK_ID IN (45, 47)
 ORDER BY BANK_ID, DECODE(MENU_TYPE, 'Master', 1, 'Main', 2, 3), MENU_PARENT, MENU_NAME;
