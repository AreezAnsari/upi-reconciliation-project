-- =============================================================================
-- RECON_MENU_MASTER — catalog (reference) rows
--
-- These rows are REFERENCE DATA. Add Menu validates against them and never modifies them.
-- A catalog row is identified by BANK_ID IS NULL (it belongs to no institution) and is
-- therefore never returned by getMenusByBankId() and never granted in C_ROLE_MENU_MAP —
-- it can neither appear in a sidebar nor in the Privileges tree.
--
--   Master  : MENU_PARENT NULL,            MASTER_MENU_PARENT NULL
--   Main    : MENU_PARENT = <master name>, MASTER_MENU_PARENT NULL
--   Submenu : MENU_PARENT = <main name>,   MASTER_MENU_PARENT = <master name>
--
-- MENU_PROCESS_ID and PRODUCT_ID stay NULL here. Add Menu supplies both when it appends a
-- bank-owned copy of a submenu. Until at least one submenu under a Main is mapped, that Main
-- (and its Master, if all its Mains are unmapped) stays hidden from Privileges — Case A.
--
-- Safe to re-run: every insert is guarded by NOT EXISTS.
-- =============================================================================

-- ── Master: Configuration ────────────────────────────────────────────────────
INSERT INTO RECON_MENU_MASTER (MENU_ID, MENU_TYPE, MENU_NAME, DESCRIPTION, MENU_PARENT,
       MASTER_MENU_PARENT, SUB_MENU_FLAG, MENU_URL, MENU_PROCESS_ID, PROCESS_TYPE,
       STATUS, BANK_ID, PRODUCT_ID, IS_PORTAL_TWIN, CREATED_BY, CREATED_DATE, INSERT_DATE)
SELECT SEQ_MENU.NEXTVAL, 'Master', 'Configuration', 'Configuration root', NULL,
       NULL, 'N', NULL, NULL, NULL, 'Y', NULL, NULL, 'N', 'CATALOG', SYSDATE, SYSDATE
  FROM dual
 WHERE NOT EXISTS (SELECT 1 FROM RECON_MENU_MASTER
                    WHERE MENU_NAME = 'Configuration' AND MENU_TYPE = 'Master' AND BANK_ID IS NULL);

-- ── Main: Configuration > Template Management ────────────────────────────────
INSERT INTO RECON_MENU_MASTER (MENU_ID, MENU_TYPE, MENU_NAME, DESCRIPTION, MENU_PARENT,
       MASTER_MENU_PARENT, SUB_MENU_FLAG, MENU_URL, MENU_PROCESS_ID, PROCESS_TYPE,
       STATUS, BANK_ID, PRODUCT_ID, IS_PORTAL_TWIN, CREATED_BY, CREATED_DATE, INSERT_DATE)
SELECT SEQ_MENU.NEXTVAL, 'Main', 'Template Management', 'Templates', 'Configuration',
       NULL, 'N', NULL, NULL, NULL, 'Y', NULL, NULL, 'N', 'CATALOG', SYSDATE, SYSDATE
  FROM dual
 WHERE NOT EXISTS (SELECT 1 FROM RECON_MENU_MASTER
                    WHERE MENU_NAME = 'Template Management' AND MENU_TYPE = 'Main' AND BANK_ID IS NULL);

-- ── Submenus: Configuration > Template Management > … ────────────────────────
-- PROCESS_TYPE decides the URL Add Menu generates:
--   EXTRACTION     -> /extraction/fileProcessing.extr?processid={id}
--   RECONCILIATION -> /reconciliation/fileProcessing.extr?processid={id}
INSERT INTO RECON_MENU_MASTER (MENU_ID, MENU_TYPE, MENU_NAME, DESCRIPTION, MENU_PARENT,
       MASTER_MENU_PARENT, SUB_MENU_FLAG, MENU_URL, MENU_PROCESS_ID, PROCESS_TYPE,
       STATUS, BANK_ID, PRODUCT_ID, IS_PORTAL_TWIN, CREATED_BY, CREATED_DATE, INSERT_DATE)
SELECT SEQ_MENU.NEXTVAL, 'Submenu', 'Define Extraction', 'Extraction template', 'Template Management',
       'Configuration', 'Y', NULL, NULL, 'EXTRACTION', 'Y', NULL, NULL, 'N', 'CATALOG', SYSDATE, SYSDATE
  FROM dual
 WHERE NOT EXISTS (SELECT 1 FROM RECON_MENU_MASTER
                    WHERE MENU_NAME = 'Define Extraction' AND MENU_TYPE = 'Submenu' AND BANK_ID IS NULL);

INSERT INTO RECON_MENU_MASTER (MENU_ID, MENU_TYPE, MENU_NAME, DESCRIPTION, MENU_PARENT,
       MASTER_MENU_PARENT, SUB_MENU_FLAG, MENU_URL, MENU_PROCESS_ID, PROCESS_TYPE,
       STATUS, BANK_ID, PRODUCT_ID, IS_PORTAL_TWIN, CREATED_BY, CREATED_DATE, INSERT_DATE)
SELECT SEQ_MENU.NEXTVAL, 'Submenu', 'Define Reconciliation', 'Reconciliation template', 'Template Management',
       'Configuration', 'Y', NULL, NULL, 'RECONCILIATION', 'Y', NULL, NULL, 'N', 'CATALOG', SYSDATE, SYSDATE
  FROM dual
 WHERE NOT EXISTS (SELECT 1 FROM RECON_MENU_MASTER
                    WHERE MENU_NAME = 'Define Reconciliation' AND MENU_TYPE = 'Submenu' AND BANK_ID IS NULL);

COMMIT;

-- ── Verify ───────────────────────────────────────────────────────────────────
SELECT MENU_ID, MENU_TYPE, MENU_NAME, MENU_PARENT, MASTER_MENU_PARENT, PROCESS_TYPE
  FROM RECON_MENU_MASTER
 WHERE BANK_ID IS NULL AND CREATED_BY = 'CATALOG'
 ORDER BY DECODE(MENU_TYPE, 'Master', 1, 'Main', 2, 3), MENU_ID;
