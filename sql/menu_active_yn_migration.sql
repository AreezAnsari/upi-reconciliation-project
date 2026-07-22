-- ACTIVE_YN: soft-delete flag for RECON_MENU_MASTER, deliberately separate from STATUS
-- (maker-checker lifecycle: DRAFT/PENDING/ACTIVE/Y/REJECTED). STATUS already carries mixed
-- "active" spellings today ('ACTIVE' and 'Y' both appear live) and is mid-approval-flow for some
-- rows, so it cannot safely double as a plain soft-delete flag without colliding with in-flight
-- approvals — hence a dedicated column instead of reusing STATUS.
--
-- Y = normal (default). N = soft-deleted: invisible to Menu List and to every duplicate-validation
-- check (custom-menu duplicate guard, resolveExistingMapping, validateProcessIdUnique), so the
-- same menu name / process can be freely recreated once "deleted" — exactly as if it never
-- existed. Never a hard DELETE — Delete in Menu List now flips this to 'N' instead of removing
-- the row (see MenuMasterServiceImpl.removeMenu).

-- Inventory (informational)
SELECT COUNT(*) AS TOTAL_ROWS FROM RECON_MENU_MASTER;

-- Schema — Oracle backfills existing rows from the DEFAULT automatically on ADD COLUMN, so the
-- UPDATE below is a no-op safety net, not the primary backfill mechanism.
ALTER TABLE RECON_MENU_MASTER ADD (ACTIVE_YN VARCHAR2(1) DEFAULT 'Y');
UPDATE RECON_MENU_MASTER SET ACTIVE_YN = 'Y' WHERE ACTIVE_YN IS NULL;
COMMIT;

-- Verify (must return 0 rows)
SELECT COUNT(*) AS NULL_ACTIVE_YN FROM RECON_MENU_MASTER WHERE ACTIVE_YN IS NULL;
