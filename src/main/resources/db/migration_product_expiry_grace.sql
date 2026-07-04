-- ============================================================
-- Feature: Product-validity expiry cascade with a grace period.
--
-- When a product's validity (C_BANK_PRODUCT_MAP.VALID_TO) lapses:
--   1. All Maker/Checker capability rows for that product go SUSPENDED
--      (not immediately REVOKED) — SUSPENDED_AT records when.
--   2. If a bank has ZERO products left with valid (non-expired) dates,
--      the bank's ALL_PRODUCTS_EXPIRED_AT is stamped, and every
--      RCN_RECON_USER under that bank is put on PRODUCT_EXPIRY_HOLD
--      (their prior status is remembered in PRE_PRODUCT_HOLD_STATUS
--      so it can be restored exactly).
--   3. If validity is renewed within the grace period (see
--      ProductExpiryConstants.GRACE_PERIOD_MINUTES), everything is
--      restored automatically. If the grace period elapses first,
--      SUSPENDED capabilities become REVOKED and held users become
--      INACTIVE permanently (manual re-grant/reactivation required).
-- ============================================================

SET DEFINE OFF;

ALTER TABLE KAL_RECON.RECON_PRODUCT_CAPABILITY_MAP ADD (SUSPENDED_AT TIMESTAMP);

ALTER TABLE KAL_RECON.RECON_BANK_MASTER ADD (ALL_PRODUCTS_EXPIRED_AT TIMESTAMP);

ALTER TABLE KAL_RECON.RCN_RECON_USER ADD (PRE_PRODUCT_HOLD_STATUS VARCHAR2(20));

COMMIT;
