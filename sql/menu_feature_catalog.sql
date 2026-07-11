-- =============================================================================
-- PHASE 1 / STEP 2 — The immutable system menu catalog (feature modules).
--
-- These rows are SYSTEM METADATA, not business data:
--   BANK_ID = NULL, PRODUCT_ID = NULL, MENU_PROCESS_ID = NULL, STATUS = 'ACTIVE'
--
-- They exist ONLY so Add Menu can validate that a requested menu is registered in
-- the system. They are never granted, never bank-assigned, never process-assigned,
-- never edited/deleted/approved from any screen, and can never make a menu visible.
-- Add Menu VALIDATES against a catalog row and then APPENDS a separate bank/product/
-- process-specific row — that appended row is the only thing that ever goes live.
--
-- SYSTEM_MENU_CODE is the permanent identity. MENU_NAME is a display label and may be
-- renamed at any time, so no logic may key on it. Codes here are explicit and hand-
-- defined — never generated from the name.
--
-- Two kinds of sub-menu:
--   * process-driven : MENU_URL NULL, PROCESS_TYPE = EXTRACTION | RECONCILIATION.
--                      Add Menu requires a process/template; the URL is derived from it.
--   * fixed-URL      : MENU_URL = the real route, PROCESS_TYPE NULL.
--                      Add Menu needs no process map; the appended row inherits this URL.
--
-- My Organization / Administration / Dashboard are NOT part of this catalog — they keep
-- their existing bootstrap architecture untouched.
--
-- Idempotent: every insert is guarded on (SYSTEM_MENU_CODE, BANK_ID IS NULL).
-- =============================================================================
SET DEFINE OFF;
SET SERVEROUTPUT ON;
SET PAGESIZE 300 LINESIZE 160 FEEDBACK OFF;

DECLARE
  -- code, type, name, parent(main/master name), masterParent, url, processType
  TYPE t_row IS RECORD (
    code    VARCHAR2(60),
    mtype   VARCHAR2(20),
    mname   VARCHAR2(100),
    parent  VARCHAR2(100),
    master  VARCHAR2(100),
    url     VARCHAR2(200),
    ptype   VARCHAR2(30)
  );
  TYPE t_tab IS TABLE OF t_row;

  rows t_tab := t_tab(
    -- ── 1. Configuration ────────────────────────────────────────────────────
    t_row('CFG',                  'Master',  'Configuration',               NULL,                       NULL,             NULL,                          NULL),
    t_row('CFG_TPL',              'Main',    'Template Management',         'Configuration',            NULL,             NULL,                          NULL),
    t_row('CFG_TPL_EXTR',         'Submenu', 'Define Extraction',           'Template Management',      'Configuration',  NULL,                          'EXTRACTION'),
    t_row('CFG_TPL_RECON',        'Submenu', 'Define Reconciliation',       'Template Management',      'Configuration',  NULL,                          'RECONCILIATION'),
    t_row('CFG_SETUP',            'Main',    'System Setup',                'Configuration',            NULL,             NULL,                          NULL),
    t_row('CFG_SETUP_FILE',       'Submenu', 'File Configuration',          'System Setup',             'Configuration',  '/setup/file-config',          NULL),
    t_row('CFG_SETUP_RECON',      'Submenu', 'Recon Config',                'System Setup',             'Configuration',  '/setup/recon-config',         NULL),
    t_row('CFG_SETUP_REPORT',     'Submenu', 'Report Configuration',        'System Setup',             'Configuration',  '/setup/report-config',        NULL),
    t_row('CFG_SETUP_FTP',        'Submenu', 'FTP Configuration',           'System Setup',             'Configuration',  '/setup/ftp-config',           NULL),
    t_row('CFG_SETUP_FORCE',      'Submenu', 'Force Match Config',          'System Setup',             'Configuration',  '/setup/force-match',          NULL),
    t_row('CFG_SETUP_TEMPLATE',   'Submenu', 'Template Config',             'System Setup',             'Configuration',  '/setup/template-config',      NULL),
    t_row('CFG_SETUP_PROCDEF',    'Submenu', 'Process Definition',          'System Setup',             'Configuration',  '/setup/process-definition',   NULL),

    -- ── 2. Process ──────────────────────────────────────────────────────────
    t_row('PROC',                 'Master',  'Process',                     NULL,                       NULL,             NULL,                          NULL),
    t_row('PROC_TXN',             'Main',    'Transaction Processing',      'Process',                  NULL,             NULL,                          NULL),
    t_row('PROC_TXN_EXTR_SEARCH', 'Submenu', 'Extraction Transaction Search','Transaction Processing',  'Process',        '/process/extraction-search',  NULL),
    t_row('PROC_TXN_BULK_MATCH',  'Submenu', 'Bulk Process Match',          'Transaction Processing',   'Process',        '/process/bulk-match',         NULL),
    t_row('PROC_TXN_FILE_UPLOAD', 'Submenu', 'File Upload',                 'Transaction Processing',   'Process',        '/process/file-upload',        NULL),
    t_row('PROC_TXN_SPLIT',       'Submenu', 'Split Transaction',           'Transaction Processing',   'Process',        '/process/split-transaction',  NULL),
    t_row('PROC_TXN_RECON_SEARCH','Submenu', 'Recon Transaction Search',    'Transaction Processing',   'Process',        '/process/recon-search',       NULL),

    -- ── 3. Extraction (process-driven) ──────────────────────────────────────
    t_row('EXTR',                 'Master',  'Extraction',                  NULL,                       NULL,             NULL,                          NULL),
    t_row('EXTR_PROC',            'Main',    'Extraction Processing',       'Extraction',               NULL,             NULL,                          NULL),
    t_row('EXTR_PROC_FILE',       'Submenu', 'File Processing',             'Extraction Processing',    'Extraction',     NULL,                          'EXTRACTION'),
    t_row('EXTR_PROC_DATA',       'Submenu', 'Data Extraction',             'Extraction Processing',    'Extraction',     NULL,                          'EXTRACTION'),

    -- ── 4. Reconciliation ───────────────────────────────────────────────────
    t_row('RECON',                'Master',  'Reconciliation',              NULL,                       NULL,             NULL,                          NULL),
    t_row('RECON_PROC',           'Main',    'Reconciliation Processing',   'Reconciliation',           NULL,             NULL,                          NULL),
    t_row('RECON_PROC_AUTO',      'Submenu', 'Auto Reconciliation',         'Reconciliation Processing','Reconciliation', NULL,                          'RECONCILIATION'),
    t_row('RECON_PROC_MANUAL',    'Submenu', 'Manual Reconciliation',       'Reconciliation Processing','Reconciliation', NULL,                          'RECONCILIATION'),
    t_row('RECON_PROC_STATUS',    'Submenu', 'Reconciliation Status',       'Reconciliation Processing','Reconciliation', '/reconciliation/status',      NULL),
    t_row('RECON_SETUP',          'Main',    'Reconciliation Setup',        'Reconciliation',           NULL,             NULL,                          NULL),
    t_row('RECON_SETUP_TEMPLATE', 'Submenu', 'Add New Template',            'Reconciliation Setup',     'Reconciliation', '/new-config/add-template',    NULL),
    t_row('RECON_SETUP_RULE',     'Submenu', 'Define Reconciliation Rule',  'Reconciliation Setup',     'Reconciliation', '/new-config/reconciliation',  NULL),
    t_row('RECON_SETUP_OPS',      'Submenu', 'Reconciliation Ops Center',   'Reconciliation Setup',     'Reconciliation', '/new-config/ops-center',      NULL),

    -- ── 5. Reports ──────────────────────────────────────────────────────────
    t_row('RPT',                  'Master',  'Reports',                     NULL,                       NULL,             NULL,                          NULL),
    t_row('RPT_MAIN',             'Main',    'Reporting',                   'Reports',                  NULL,             NULL,                          NULL),
    t_row('RPT_GENERATE',         'Submenu', 'Generate Report',             'Reporting',                'Reports',        '/reports/generate',           NULL),
    t_row('RPT_TTUM',             'Submenu', 'TTUM Report',                 'Reporting',                'Reports',        '/reports/ttum',               NULL),
    t_row('RPT_RECON',            'Submenu', 'Reconciliation Report',       'Reporting',                'Reports',        '/reports/reconciliation',     NULL),
    t_row('RPT_JOB_DETAILS',      'Submenu', 'Job Details',                 'Reporting',                'Reports',        '/reports/extraction-details', NULL),

    -- ── 6. Dispute Management ───────────────────────────────────────────────
    t_row('DSP',                  'Master',  'Dispute Management',          NULL,                       NULL,             NULL,                          NULL),
    t_row('DSP_MAIN',             'Main',    'Dispute Handling',            'Dispute Management',       NULL,             NULL,                          NULL),
    t_row('DSP_DASHBOARD',        'Submenu', 'Dispute Dashboard',           'Dispute Handling',         'Dispute Management', '/dispute/dashboard',      NULL),
    t_row('DSP_ACTION_CENTER',    'Submenu', 'Dispute Action Center',       'Dispute Handling',         'Dispute Management', '/dispute/action-center',  NULL),
    t_row('DSP_MAKER_UPLOAD',     'Submenu', 'Maker Upload',                'Dispute Handling',         'Dispute Management', '/dispute/maker-upload',   NULL),
    t_row('DSP_CHECKER_APPROVAL', 'Submenu', 'Checker Approval',            'Dispute Handling',         'Dispute Management', '/dispute/checker-approval', NULL),

    -- ── 7. UPI Reconciliation ───────────────────────────────────────────────
    t_row('UPI',                  'Master',  'UPI Reconciliation',          NULL,                       NULL,             NULL,                          NULL),
    t_row('UPI_MAIN',             'Main',    'UPI Recon',                   'UPI Reconciliation',       NULL,             NULL,                          NULL),
    t_row('UPI_DASHBOARD',        'Submenu', 'UPI Recon Dashboard',         'UPI Recon',                'UPI Reconciliation', '/upi-recon/dashboard',    NULL),

    -- ── 8. NEFT Reconciliation ──────────────────────────────────────────────
    t_row('NEFT',                 'Master',  'NEFT Reconciliation',         NULL,                       NULL,             NULL,                          NULL),
    t_row('NEFT_MAIN',            'Main',    'NEFT Recon',                  'NEFT Reconciliation',      NULL,             NULL,                          NULL),
    t_row('NEFT_CYCLE',           'Submenu', 'NEFT Cycle Management',       'NEFT Recon',               'NEFT Reconciliation', '/neft-recon/dashboard',  NULL)
  );

  v_exists NUMBER;
  v_added  NUMBER := 0;
BEGIN
  FOR i IN 1 .. rows.COUNT LOOP
    SELECT COUNT(*) INTO v_exists
      FROM RECON_MENU_MASTER
     WHERE BANK_ID IS NULL AND SYSTEM_MENU_CODE = rows(i).code;

    IF v_exists = 0 THEN
      INSERT INTO RECON_MENU_MASTER (
             MENU_ID, SYSTEM_MENU_CODE, MENU_TYPE, MENU_NAME,
             MENU_PARENT, MASTER_MENU_PARENT, SUB_MENU_FLAG,
             MENU_URL, PROCESS_TYPE,
             MENU_PROCESS_ID, BANK_ID, PRODUCT_ID,
             STATUS, IS_PORTAL_TWIN, CREATED_BY, CREATED_DATE, INSERT_DATE)
      VALUES (SEQ_MENU.NEXTVAL, rows(i).code, rows(i).mtype, rows(i).mname,
             rows(i).parent, rows(i).master,
             CASE WHEN rows(i).mtype = 'Submenu' THEN 'Y' ELSE 'N' END,
             rows(i).url, rows(i).ptype,
             NULL, NULL, NULL,
             'ACTIVE', 'N', 'CATALOG', SYSDATE, SYSDATE);
      v_added := v_added + 1;
    END IF;
  END LOOP;
  COMMIT;
  DBMS_OUTPUT.PUT_LINE('Catalog rows inserted: ' || v_added || ' (of ' || rows.COUNT || ' defined)');
END;
/

-- ── Verify ───────────────────────────────────────────────────────────────────
PROMPT ==== Catalog (BANK_ID NULL, CREATED_BY='CATALOG') ====
COL SYSTEM_MENU_CODE FORMAT A22
COL MENU_NAME        FORMAT A30
COL MENU_PARENT      FORMAT A26
COL MENU_URL         FORMAT A30
COL PROCESS_TYPE     FORMAT A15
SELECT SYSTEM_MENU_CODE, MENU_TYPE, MENU_NAME, MENU_PARENT, MENU_URL, PROCESS_TYPE
  FROM RECON_MENU_MASTER
 WHERE BANK_ID IS NULL AND CREATED_BY = 'CATALOG'
 ORDER BY DECODE(MENU_TYPE,'Master',1,'Main',2,3), SYSTEM_MENU_CODE;

PROMPT ==== Catalog integrity: must be 0 rows (no bank/product/process on catalog) ====
SELECT COUNT(*) AS CATALOG_ROWS_WITH_SCOPE
  FROM RECON_MENU_MASTER
 WHERE CREATED_BY = 'CATALOG'
   AND (BANK_ID IS NOT NULL OR PRODUCT_ID IS NOT NULL OR MENU_PROCESS_ID IS NOT NULL);
