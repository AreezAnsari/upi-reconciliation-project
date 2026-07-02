-- ============================================================
-- Migration: drop dead columns + clear stale plaintext passwords
--
-- VERIFICATION_TOKEN / TOKEN_EXPIRY were never read anywhere in the
-- codebase (only ever declared on the entity) — dropping them.
--
-- DEFAULT_PASSWORD is now stored BCrypt-encoded going forward
-- (ReconBankMasterServiceImpl.createBank()). It is never compared against
-- in login logic (verifyCredentials compares against RCN_RECON_USER.PASSWORD_HASH,
-- not this column) — it exists only for potential future display, so any
-- existing PLAINTEXT values are cleared here rather than re-hashed via SQL
-- (Oracle SQL cannot compute BCrypt).
-- ============================================================

SET DEFINE OFF;

ALTER TABLE KAL_RECON.RECON_BANK_MASTER DROP COLUMN VERIFICATION_TOKEN;
ALTER TABLE KAL_RECON.RECON_BANK_MASTER DROP COLUMN TOKEN_EXPIRY;

UPDATE KAL_RECON.RECON_BANK_MASTER
SET DEFAULT_PASSWORD = NULL
WHERE DEFAULT_PASSWORD IS NOT NULL;

COMMIT;
