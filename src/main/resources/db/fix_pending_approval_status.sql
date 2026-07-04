-- One-time data fix: before this session's fix, a Maker-created user (awaiting Checker
-- approval) was given STATUS='ACTIVE_PENDING' — the same literal value used for the
-- unrelated "Inactive -> reactivating" scheduling flow. Now that the creation flow uses
-- STATUS='PENDING_APPROVAL' instead, any existing rows from before this fix need updating.
--
-- Disambiguates by REACTIVATE_SCHEDULED_AT: a genuine reactivation-in-progress row always
-- has this timestamp set (by UserStatusServiceImpl.scheduleReactivate()); a Maker-created
-- user pending Checker approval never does. Only rows where it's NULL are touched.

UPDATE RCN_RECON_USER
SET STATUS = 'PENDING_APPROVAL'
WHERE STATUS = 'ACTIVE_PENDING'
  AND REACTIVATE_SCHEDULED_AT IS NULL;

COMMIT;
