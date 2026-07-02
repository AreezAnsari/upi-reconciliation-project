-- ============================================================
-- Gap 2 fix: reactivation cooldown (matches old MainBankServiceImpl)
-- Adds INACTIVATED_AT — set whenever a bank/branch becomes INACTIVE,
-- used to block going back ACTIVE within 30 seconds.
-- ============================================================

SET DEFINE OFF;

ALTER TABLE KAL_RECON.RECON_BANK_MASTER ADD (INACTIVATED_AT TIMESTAMP);

COMMIT;
