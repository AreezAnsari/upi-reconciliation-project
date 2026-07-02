-- ============================================================
-- Fix: RECON_ROLE_MASTER.ROLE_CODE was VARCHAR2(20), but
-- "BRANCH_ADMIN_" + an 8-digit branch code = 21 characters,
-- overflowing the column (ORA-12899) and silently corrupting the
-- Hibernate session for the rest of createBank() — surfaced as:
--   "AssertionFailure: null id in ReconRoleMaster entry
--    (don't flush the Session after an exception occurs)"
--
-- "BANK_ADMIN_" + 8 digits = 19 chars, which is why Bank onboarding
-- never hit this but Branch onboarding always did.
-- ============================================================

SET DEFINE OFF;

ALTER TABLE KAL_RECON.RECON_ROLE_MASTER MODIFY ROLE_CODE VARCHAR2(30);

COMMIT;
