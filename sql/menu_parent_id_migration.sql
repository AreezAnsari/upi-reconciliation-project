-- =============================================================================
-- RECON_MENU_MASTER : add PARENT_MENU_ID (internal, numeric, self-referencing)
--
-- DO NOT EXECUTE AS-IS. Run STEP 0, read the output, take the backup in STEP 1, then go.
--
-- Why: the hierarchy (Master -> Main -> Submenu) is resolved entirely through two string
-- columns today: MENU_PARENT (Java parentMenuCode) and MASTER_MENU_PARENT (Java
-- masterMenuParent). These hold menu NAMES (or, for legacy KAL_ADMIN rows, a stringified
-- MENU_ID — a pre-existing inconsistency; see STEP 0b). Name-based resolution is fragile:
-- MENU_NAME is not unique across banks (every Bank/Branch Admin, and Kal Admin, has its own
-- "My Organization"/"Administration" Master row with the same name).
--
-- MENU_PARENT / MASTER_MENU_PARENT are NOT being removed or changed by this migration.
-- The React frontend sends/reads them as plain NAME strings (AddMenu.jsx POSTs them
-- verbatim; Sidebar.jsx/Navbar.jsx/RoleManagement.jsx/PrivilegesAssign.jsx/MenuList.jsx/
-- ViewMenu.jsx/ViewRole.jsx/ViewUser.jsx/CheckerQueue.jsx/MakerQueue.jsx read them from API
-- responses), and four endpoints (/api/v1/menu/visible, /api/v1/getallmenu,
-- /api/v1/menu/by-bank/{bankId}, /api/v1/menu/checker-queue) serialize the raw entity, so
-- those Java field names are the wire contract. PARENT_MENU_ID is purely an ADDITIVE,
-- internal numeric mirror, used going forward by the backend only.
--
-- Hibernate runs with ddl-auto: update, so it will ADD the bare PARENT_MENU_ID column itself
-- on the next boot once the entity field exists, but it will never backfill data or add the
-- FK/index. Run this file (through STEP 6 verification) BEFORE deploying the code that
-- populates/reads PARENT_MENU_ID, or every existing row will have a NULL parent pointer.
-- =============================================================================


-- -----------------------------------------------------------------------------
-- STEP 0 — Inventory. READ-ONLY.
-- -----------------------------------------------------------------------------

-- 0a. Row counts by MENU_TYPE x (BANK_ID NULL/SET).
SELECT MENU_TYPE,
       CASE WHEN BANK_ID IS NULL THEN 'NULL' ELSE 'SET' END AS BANK_ID_STATE,
       COUNT(*) AS ROWS_
  FROM RECON_MENU_MASTER
 GROUP BY MENU_TYPE, CASE WHEN BANK_ID IS NULL THEN 'NULL' ELSE 'SET' END
 ORDER BY 1, 2;

-- 0b. Main rows whose MENU_PARENT is a stringified MENU_ID instead of a name — expect
--     exactly the KAL_ADMIN bootstrap Main rows (BANK_ID IS NULL, CREATED_BY <> 'CATALOG').
--     See KalAdminAuthServiceImpl.createDefaultKalAdminMenus(): it passes
--     String.valueOf(myOrg.getMenuId()) as the parent code instead of a name.
SELECT MENU_ID, MENU_NAME, MENU_PARENT, BANK_ID, CREATED_BY
  FROM RECON_MENU_MASTER
 WHERE MENU_TYPE = 'Main'
   AND MENU_PARENT IS NOT NULL
   AND REGEXP_LIKE(MENU_PARENT, '^[0-9]+$');

-- 0c. Main rows whose (non-numeric) MENU_PARENT name does not resolve to any Master in the
--     same BANK_ID scope. Expect zero.
SELECT m.MENU_ID, m.MENU_NAME, m.MENU_PARENT, m.BANK_ID
  FROM RECON_MENU_MASTER m
 WHERE m.MENU_TYPE = 'Main'
   AND m.MENU_PARENT IS NOT NULL
   AND NOT REGEXP_LIKE(m.MENU_PARENT, '^[0-9]+$')
   AND NOT EXISTS (
         SELECT 1 FROM RECON_MENU_MASTER p
          WHERE p.MENU_TYPE = 'Master' AND p.MENU_NAME = m.MENU_PARENT
            AND ((m.BANK_ID IS NULL AND p.BANK_ID IS NULL) OR p.BANK_ID = m.BANK_ID));

-- 0d. Submenu rows whose MENU_PARENT name does not resolve to any Main in the same BANK_ID
--     scope. Expect zero.
SELECT m.MENU_ID, m.MENU_NAME, m.MENU_PARENT, m.BANK_ID
  FROM RECON_MENU_MASTER m
 WHERE m.MENU_TYPE = 'Submenu'
   AND m.MENU_PARENT IS NOT NULL
   AND NOT EXISTS (
         SELECT 1 FROM RECON_MENU_MASTER p
          WHERE p.MENU_TYPE = 'Main' AND p.MENU_NAME = m.MENU_PARENT
            AND ((m.BANK_ID IS NULL AND p.BANK_ID IS NULL) OR p.BANK_ID = m.BANK_ID));


-- -----------------------------------------------------------------------------
-- STEP 1 — Backup.
-- -----------------------------------------------------------------------------
CREATE TABLE BKP_MENU_BEFORE_PARENTID AS SELECT * FROM RECON_MENU_MASTER;


-- -----------------------------------------------------------------------------
-- STEP 2 — Schema. Run BEFORE restarting the app on the new code.
-- -----------------------------------------------------------------------------

ALTER TABLE RECON_MENU_MASTER ADD (PARENT_MENU_ID NUMBER(19));

COMMENT ON COLUMN RECON_MENU_MASTER.PARENT_MENU_ID IS
  'Internal numeric hierarchy pointer (self-FK to MENU_ID). Added alongside MENU_PARENT/
   MASTER_MENU_PARENT, which remain the wire contract and are not altered by this column.
   NULL for a Master row (root) and for any row the resolver could not match.';

ALTER TABLE RECON_MENU_MASTER
  ADD CONSTRAINT FK_MENU_PARENT_ID FOREIGN KEY (PARENT_MENU_ID) REFERENCES RECON_MENU_MASTER (MENU_ID);

CREATE INDEX IX_MENU_PARENT_ID ON RECON_MENU_MASTER (PARENT_MENU_ID);


-- -----------------------------------------------------------------------------
-- STEP 3 — Backfill Main rows (parent = Master). Same scoping/order the runtime uses in
-- MenuMasterServiceImpl.getMenusByRolePrivileges(): name match first, scoped by BANK_ID
-- (NULL=NULL for catalog/KAL_ADMIN rows), falling back to a numeric MENU_ID parse only for
-- the legacy KAL_ADMIN rows identified in STEP 0b.
-- -----------------------------------------------------------------------------

-- 3a. Name-based.
UPDATE RECON_MENU_MASTER m
   SET m.PARENT_MENU_ID = (
         SELECT MIN(p.MENU_ID) FROM RECON_MENU_MASTER p
          WHERE p.MENU_TYPE = 'Master' AND p.STATUS = 'Y' AND p.MENU_NAME = m.MENU_PARENT
            AND ((m.BANK_ID IS NULL AND p.BANK_ID IS NULL) OR p.BANK_ID = m.BANK_ID))
 WHERE m.MENU_TYPE = 'Main' AND m.MENU_PARENT IS NOT NULL
   AND NOT REGEXP_LIKE(m.MENU_PARENT, '^[0-9]+$');

-- 3b. KAL_ADMIN legacy numeric-ID fallback — only where 3a left NULL, mirroring the
-- runtime's try-name-then-try-ID order.
UPDATE RECON_MENU_MASTER m
   SET m.PARENT_MENU_ID = TO_NUMBER(m.MENU_PARENT)
 WHERE m.MENU_TYPE = 'Main' AND m.PARENT_MENU_ID IS NULL
   AND m.MENU_PARENT IS NOT NULL AND REGEXP_LIKE(m.MENU_PARENT, '^[0-9]+$')
   AND EXISTS (SELECT 1 FROM RECON_MENU_MASTER p WHERE p.MENU_ID = TO_NUMBER(m.MENU_PARENT));

-- 3c. Catalog-only fallback — only where 3a/3b left NULL. Catalog rows (BANK_ID IS NULL,
-- CREATED_BY='CATALOG', e.g. menu_feature_catalog.sql) use STATUS='ACTIVE', not 'Y' —
-- a different status vocabulary from bank-onboarded/KAL_ADMIN rows. 3a's STATUS='Y' filter
-- (mirroring the runtime's getMenusByRolePrivileges, which never resolves a catalog row this
-- way since catalog rows are never granted) correctly excludes catalog Masters, so their own
-- catalog Main children resolve here instead — for completeness of the migration only.
-- Nothing added by this refactor reads parentMenuId on a catalog row, so this fallback is
-- risk-free: it only fills in data nobody's runtime path consults yet.
UPDATE RECON_MENU_MASTER m
   SET m.PARENT_MENU_ID = (
         SELECT MIN(p.MENU_ID) FROM RECON_MENU_MASTER p
          WHERE p.MENU_TYPE = 'Master' AND p.BANK_ID IS NULL AND p.CREATED_BY = 'CATALOG'
            AND p.MENU_NAME = m.MENU_PARENT)
 WHERE m.MENU_TYPE = 'Main' AND m.PARENT_MENU_ID IS NULL
   AND m.BANK_ID IS NULL AND m.CREATED_BY = 'CATALOG'
   AND m.MENU_PARENT IS NOT NULL;

COMMIT;


-- -----------------------------------------------------------------------------
-- STEP 4 — Backfill Submenu rows (parent = Main), same BANK_ID scoping.
-- -----------------------------------------------------------------------------

UPDATE RECON_MENU_MASTER m
   SET m.PARENT_MENU_ID = (
         SELECT MIN(p.MENU_ID) FROM RECON_MENU_MASTER p
          WHERE p.MENU_TYPE = 'Main' AND p.MENU_NAME = m.MENU_PARENT
            AND ((m.BANK_ID IS NULL AND p.BANK_ID IS NULL) OR p.BANK_ID = m.BANK_ID))
 WHERE m.MENU_TYPE = 'Submenu' AND m.MENU_PARENT IS NOT NULL;

COMMIT;


-- -----------------------------------------------------------------------------
-- STEP 5 — Verification. (a), (b), (c), (d) must all return ZERO rows.
-- -----------------------------------------------------------------------------

-- (a) Main/Submenu with a non-null MENU_PARENT but still-null PARENT_MENU_ID.
SELECT MENU_ID, MENU_NAME, MENU_TYPE, MENU_PARENT, BANK_ID
  FROM RECON_MENU_MASTER
 WHERE MENU_TYPE IN ('Main', 'Submenu')
   AND MENU_PARENT IS NOT NULL
   AND PARENT_MENU_ID IS NULL;

-- (b) Master rows must never get a PARENT_MENU_ID (they are roots).
SELECT MENU_ID, MENU_NAME FROM RECON_MENU_MASTER
 WHERE MENU_TYPE = 'Master' AND PARENT_MENU_ID IS NOT NULL;

-- (c) Sanity: the resolved parent's name should equal MENU_PARENT, EXCEPT the KAL_ADMIN
--     numeric-ID case, where MENU_PARENT should equal TO_CHAR(resolved parent's MENU_ID).
SELECT m.MENU_ID, m.MENU_NAME, m.MENU_PARENT, m.PARENT_MENU_ID, p.MENU_NAME AS RESOLVED_PARENT_NAME
  FROM RECON_MENU_MASTER m
  JOIN RECON_MENU_MASTER p ON p.MENU_ID = m.PARENT_MENU_ID
 WHERE m.MENU_PARENT <> p.MENU_NAME
   AND m.MENU_PARENT <> TO_CHAR(p.MENU_ID);

-- (d) No row may point at itself.
SELECT MENU_ID FROM RECON_MENU_MASTER WHERE PARENT_MENU_ID = MENU_ID;


-- -----------------------------------------------------------------------------
-- Idempotency notes
-- -----------------------------------------------------------------------------
-- STEP 2's ALTER TABLE ADD errors harmlessly if re-run after the column already exists
-- (same "already ran" signal as menu_master_bank_id_migration.sql). STEPs 3/4 are natural
-- idempotent UPDATEs (recompute the same values) — safe to re-run this whole file any time,
-- e.g. after running menu_catalog_seed.sql / menu_feature_catalog.sql / any backfill_*.sql,
-- to sweep newly inserted rows' PARENT_MENU_ID into place. No new uniqueness constraint is
-- introduced here (FK + plain index only), so this migration does not interact with
-- UX_MENU_BANK_PRODUCT_CODE or UX_MENU_BANK_NAME_PROCESS.
