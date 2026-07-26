-- Default Dashboard — a system fallback menu, plus the generic IS_SYSTEM_MENU framework it rides on.
--
-- Problem: a business/user role with zero granted menus (freshly created, or after every granted menu
-- is deleted / product-filtered out) leaves the user with an empty sidebar and no landing page — a
-- broken session. Default Dashboard is a guaranteed fallback: every business role always has EITHER
-- one+ application menu OR the Default Dashboard.
--
-- Design:
--   IS_SYSTEM_MENU='Y'  — a generic flag marking a row as a system menu (not a real application menu).
--                         System menus are hidden from every list/tree/count/sidebar and are immutable.
--                         Future system menus (Maintenance / License-Expired / …) reuse this same flag.
--   SYSTEM_MENU_TYPE    — which kind of system menu ('FALLBACK' for Default Dashboard). Lets the
--                         framework distinguish future system menus without new columns.
--   The Default Dashboard is ONE shared singleton row (BANK_ID NULL), granted per business role in
--   C_ROLE_MENU_MAP but never shown; the frontend simply falls back to /default-dashboard when a role
--   has zero accessible application menus.
--
-- Business roles only: RECON_USER / BANK_USER / BRANCH_USER. System/bootstrap roles
-- (KAL_ADMIN_DEFAULT / BANK_ADMIN_DEFAULT / BRANCH_ADMIN_DEFAULT) never receive it — they always
-- carry their own bootstrap menus.

-- ── Schema ───────────────────────────────────────────────────────────────────
-- Oracle backfills existing rows from the DEFAULT on ADD COLUMN; the UPDATE below is a no-op safety
-- net. SYSTEM_MENU_TYPE has no DEFAULT (NULL = "not a system menu", the vast majority).
ALTER TABLE RECON_MENU_MASTER ADD (IS_SYSTEM_MENU VARCHAR2(1) DEFAULT 'N');
ALTER TABLE RECON_MENU_MASTER ADD (SYSTEM_MENU_TYPE VARCHAR2(30));
UPDATE RECON_MENU_MASTER SET IS_SYSTEM_MENU = 'N' WHERE IS_SYSTEM_MENU IS NULL;
COMMIT;

-- ── Seed the Default Dashboard row (idempotent) ───────────────────────────────
-- MENU_ID from SEQ_MENU (same sequence the entity uses). Only inserted if a DEFAULT_DASHBOARD row
-- does not already exist, so re-running is safe.
INSERT INTO RECON_MENU_MASTER (
    MENU_ID, SYSTEM_MENU_CODE, MENU_TYPE, MENU_NAME, MENU_URL, STATUS,
    CREATED_BY, CREATED_DATE, INSERT_DATE, BANK_ID, PRODUCT_ID,
    MENU_SOURCE, IS_CLICKABLE, ACTIVE_YN, IS_SYSTEM_MENU, SYSTEM_MENU_TYPE
)
SELECT
    SEQ_MENU.NEXTVAL, 'DEFAULT_DASHBOARD', 'SYSTEM', 'Default Dashboard', '/default-dashboard', 'Y',
    'SYSTEM', SYSDATE, SYSDATE, NULL, NULL,
    'SYSTEM', 'Y', 'Y', 'Y', 'FALLBACK'
FROM DUAL
WHERE NOT EXISTS (
    SELECT 1 FROM RECON_MENU_MASTER WHERE SYSTEM_MENU_CODE = 'DEFAULT_DASHBOARD'
);
COMMIT;

-- Guard against a second Default Dashboard ever being created by manual SQL. A unique index on
-- SYSTEM_MENU_CODE would clash with the many existing catalog rows that legitimately SHARE a code
-- (BANK_ID-scoped copies) and the many rows with NULL codes — so scope the uniqueness to system
-- menus only via a function-based unique index: it indexes ONLY IS_SYSTEM_MENU='Y' rows (others map
-- to NULL and are not indexed), guaranteeing at most one row per system code.
CREATE UNIQUE INDEX UX_SYSTEM_MENU_CODE ON RECON_MENU_MASTER (
    CASE WHEN IS_SYSTEM_MENU = 'Y' THEN SYSTEM_MENU_CODE ELSE NULL END
);

-- ── Backfill grants for existing business roles ───────────────────────────────
-- Every RECON_USER / BANK_USER / BRANCH_USER role that has no Default Dashboard mapping yet gets one.
-- Idempotent (WHERE NOT EXISTS). System roles are intentionally excluded.
INSERT INTO C_ROLE_MENU_MAP (ROLE_ID, MENU_ID, CREATED_AT, CREATED_BY)
SELECT r.ROLE_ID, dd.MENU_ID, SYSTIMESTAMP, 'SYSTEM'
FROM RECON_ROLE_MASTER r
CROSS JOIN (SELECT MENU_ID FROM RECON_MENU_MASTER WHERE SYSTEM_MENU_CODE = 'DEFAULT_DASHBOARD') dd
WHERE r.ROLE_TYPE IN ('RECON_USER', 'BANK_USER', 'BRANCH_USER')
  AND NOT EXISTS (
      SELECT 1 FROM C_ROLE_MENU_MAP m
      WHERE m.ROLE_ID = r.ROLE_ID AND m.MENU_ID = dd.MENU_ID
  );
COMMIT;

-- ── Verify (each must return 0 / expected) ────────────────────────────────────
SELECT COUNT(*) AS DD_ROW_COUNT              FROM RECON_MENU_MASTER WHERE SYSTEM_MENU_CODE = 'DEFAULT_DASHBOARD';   -- expect 1
SELECT COUNT(*) AS NULL_IS_SYSTEM_MENU       FROM RECON_MENU_MASTER WHERE IS_SYSTEM_MENU IS NULL;                   -- expect 0
SELECT COUNT(*) AS BUSINESS_ROLES_WITHOUT_DD
FROM RECON_ROLE_MASTER r
WHERE r.ROLE_TYPE IN ('RECON_USER', 'BANK_USER', 'BRANCH_USER')
  AND NOT EXISTS (
      SELECT 1 FROM C_ROLE_MENU_MAP m
      JOIN RECON_MENU_MASTER dd ON dd.MENU_ID = m.MENU_ID
      WHERE m.ROLE_ID = r.ROLE_ID AND dd.SYSTEM_MENU_CODE = 'DEFAULT_DASHBOARD'
  );   -- expect 0

-- ============================================================================
-- ROLLBACK (non-destructive default — run only if reverting this feature)
-- ============================================================================
-- Removes the Default Dashboard row and its grants. Does NOT drop the columns, because they are the
-- generic system-menu framework: other system menus (Maintenance / License-Expired / …) may already
-- use them. Dropping the columns is safe ONLY if no other IS_SYSTEM_MENU='Y' row exists.
--
--   DELETE FROM C_ROLE_MENU_MAP
--   WHERE MENU_ID = (SELECT MENU_ID FROM RECON_MENU_MASTER WHERE SYSTEM_MENU_CODE = 'DEFAULT_DASHBOARD');
--   DELETE FROM RECON_MENU_MASTER WHERE SYSTEM_MENU_CODE = 'DEFAULT_DASHBOARD';
--   DROP INDEX UX_SYSTEM_MENU_CODE;
--   COMMIT;
--
--   -- Column drop — ONLY if this query returns 0 (no other system menu exists):
--   --   SELECT COUNT(*) FROM RECON_MENU_MASTER WHERE IS_SYSTEM_MENU = 'Y';
--   -- ALTER TABLE RECON_MENU_MASTER DROP COLUMN SYSTEM_MENU_TYPE;
--   -- ALTER TABLE RECON_MENU_MASTER DROP COLUMN IS_SYSTEM_MENU;
--   -- COMMIT;
