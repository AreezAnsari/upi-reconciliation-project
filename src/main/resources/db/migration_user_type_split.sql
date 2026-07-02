-- ============================================================
-- Migration: split USER_TYPE (institution role) from employment
-- classification (Internal/External), which was never persisted before.
--
-- After this: USER_TYPE holds one of KAL_ADMIN / BANK_ADMIN / BRANCH_ADMIN /
-- BANK_USER / BRANCH_USER. EMPLOYMENT_TYPE holds INTERNAL / EXTERNAL for
-- regular (non-admin) users only.
-- ============================================================

SET DEFINE OFF;

ALTER TABLE KAL_RECON.RCN_RECON_USER ADD (
    EMPLOYMENT_TYPE           VARCHAR2(20),
    EXTERNAL_DEPARTMENT_NAME  VARCHAR2(200),
    EXTERNAL_SUPERVISOR_NAME  VARCHAR2(100),
    EXTERNAL_SUPERVISOR_EMAIL VARCHAR2(150),
    EXTERNAL_SUPERVISOR_PHONE VARCHAR2(20)
);

-- Backfill: any existing regular user rows that were mistakenly created with
-- USER_TYPE = INTERNAL/EXTERNAL (bug in old AddUser.jsx) — move that value to
-- EMPLOYMENT_TYPE, and reset USER_TYPE based on whether they belong to a
-- top-level bank or a branch (via RECON_BANK_MASTER.PARENT_BANK_ID).
UPDATE KAL_RECON.RCN_RECON_USER u
SET u.EMPLOYMENT_TYPE = u.USER_TYPE
WHERE u.USER_TYPE IN ('INTERNAL', 'EXTERNAL');

UPDATE KAL_RECON.RCN_RECON_USER u
SET u.USER_TYPE = (
    SELECT CASE WHEN b.PARENT_BANK_ID IS NOT NULL THEN 'BRANCH_USER' ELSE 'BANK_USER' END
    FROM KAL_RECON.RECON_BANK_MASTER b
    WHERE b.BANK_ID = u.BANK_ID AND b.CONTACT_RANK = 'PRIMARY'
    AND ROWNUM = 1
)
WHERE u.USER_TYPE IN ('INTERNAL', 'EXTERNAL')
  AND EXISTS (
    SELECT 1 FROM KAL_RECON.RECON_BANK_MASTER b
    WHERE b.BANK_ID = u.BANK_ID AND b.CONTACT_RANK = 'PRIMARY'
  );

COMMIT;
