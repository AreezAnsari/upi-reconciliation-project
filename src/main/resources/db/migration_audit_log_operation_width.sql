-- ============================================================
-- Fix: AUDIT_LOG.OPERATION was VARCHAR2(10), but the application
-- writes values like "STATUS_CHANGE" (13 chars) and "CANCEL_SCHEDULE"
-- (15 chars). Oracle rejects the insert (ORA-12899), which corrupts
-- the Hibernate session mid-transaction — even though the failure is
-- caught locally, Spring still marks the enclosing @Transactional as
-- rollback-only, so the ENTIRE parent operation (e.g. Undo/cancel a
-- scheduled block) fails with UnexpectedRollbackException even though
-- the actual status change had already succeeded in memory.
-- ============================================================

SET DEFINE OFF;

ALTER TABLE KAL_RECON.AUDIT_LOG MODIFY (OPERATION VARCHAR2(30));

COMMIT;
