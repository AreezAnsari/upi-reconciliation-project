-- ============================================================
-- Dummy test data: 2 child users nested UNDER the previously inserted
-- test.bankuser1 / test.branchuser1 (dummy_users_test.sql) — to test
-- the Hierarchy tree's parent → child nesting.
--
-- PARENT_USER_ID + BANK_ID are resolved dynamically by looking up the
-- parent's USERNAME, so no manual ID lookup needed. Run
-- dummy_users_test.sql FIRST if you haven't already.
-- ============================================================

SET DEFINE OFF;

-- ── Child of test.bankuser1 (same bank, BANK_USER) ──
INSERT INTO KAL_RECON.RCN_RECON_USER
    (BANK_ID, PARENT_USER_ID, USERNAME, FULL_NAME, EMAIL, MOBILE_NUMBER, USER_TYPE, CONTACT_RANK,
     PASSWORD_HASH, PASSWORD_SET, STATUS, APPROVED_YN, EMPLOYMENT_TYPE, CREATED_AT, CREATED_BY)
SELECT p.BANK_ID, p.USER_ID, 'test.bankuser1.child1', 'Test Bank User Child One',
       'test.bankuser1.child1@example.com', '9876543214', 'BANK_USER', 'PRIMARY',
       '$2a$10$dummyBcryptHashPlaceholderXXXXXXXXXXXXXXXXXXXXXXXXX', 1,
       'ACTIVE', 'Y', 'INTERNAL', SYSTIMESTAMP, 'SYSTEM'
FROM KAL_RECON.RCN_RECON_USER p
WHERE p.USERNAME = 'test.bankuser1' AND ROWNUM = 1;

-- ── Child of test.branchuser1 (same branch, BRANCH_USER) ──
INSERT INTO KAL_RECON.RCN_RECON_USER
    (BANK_ID, PARENT_USER_ID, USERNAME, FULL_NAME, EMAIL, MOBILE_NUMBER, USER_TYPE, CONTACT_RANK,
     PASSWORD_HASH, PASSWORD_SET, STATUS, APPROVED_YN, EMPLOYMENT_TYPE, CREATED_AT, CREATED_BY)
SELECT p.BANK_ID, p.USER_ID, 'test.branchuser1.child1', 'Test Branch User Child One',
       'test.branchuser1.child1@example.com', '9876543215', 'BRANCH_USER', 'PRIMARY',
       '$2a$10$dummyBcryptHashPlaceholderXXXXXXXXXXXXXXXXXXXXXXXXX', 1,
       'ACTIVE', 'Y', 'EXTERNAL', SYSTIMESTAMP, 'SYSTEM'
FROM KAL_RECON.RCN_RECON_USER p
WHERE p.USERNAME = 'test.branchuser1' AND ROWNUM = 1;

COMMIT;

-- Verify parent → child chain:
-- SELECT c.USER_ID, c.USERNAME, c.PARENT_USER_ID, p.USERNAME AS PARENT_USERNAME, c.USER_TYPE
-- FROM KAL_RECON.RCN_RECON_USER c
-- JOIN KAL_RECON.RCN_RECON_USER p ON p.USER_ID = c.PARENT_USER_ID
-- WHERE c.USERNAME LIKE 'test.%child%';
