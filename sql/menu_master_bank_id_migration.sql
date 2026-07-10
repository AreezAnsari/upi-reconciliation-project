-- =============================================================================
-- RECON_MENU_MASTER : ROLE_ID -> BANK_ID  (+ PRODUCT_ID)
--
-- DO NOT EXECUTE AS-IS. Run STEP 0, read the output, take the backup in STEP 1, then go.
--
-- Why: ROLE_ID answered "which bank does this menu belong to" the long way round
-- (bank -> users -> roleIds -> menus). It never meant "which role may see this menu" —
-- that is, and always was, C_ROLE_MENU_MAP. And PRODUCT_ID cannot replace it either:
-- RECON_PRODUCT_MASTER.PRODUCT_NAME is UNIQUE, so one product row is shared by every bank
-- that bought it. A product identifies the product, never the owner.
--
-- After this migration the three questions have three separate answers:
--     RECON_MENU_MASTER.BANK_ID    -> which institution owns the row
--     RECON_MENU_MASTER.PRODUCT_ID -> which product it belongs to (NULL = unrestricted)
--     C_ROLE_MENU_MAP              -> which roles may see it
--
-- Catalog (reference) rows carry BANK_ID, PRODUCT_ID and MENU_PROCESS_ID all NULL, and are
-- never granted. Kal Admin's bootstrap menus also have a NULL BANK_ID (KAL_ADMIN has no
-- bank) — they are told apart by having C_ROLE_MENU_MAP grants.
--
-- Hibernate runs with ddl-auto: update, so it will ADD BANK_ID/PRODUCT_ID by itself on the
-- next boot but will never DROP ROLE_ID. Run STEP 2 before restarting on the new code, or
-- every existing menu will have a NULL owner and no bank will see its own menus.
-- =============================================================================


-- -----------------------------------------------------------------------------
-- STEP 0 — Inventory. READ-ONLY.
-- -----------------------------------------------------------------------------

-- 0a. How many menu rows, and how many already resolve to exactly one bank?
--     A menu's bank is the bank of the users who hold its owning role.
SELECT COUNT(*) AS TOTAL_MENUS,
       SUM(CASE WHEN m.ROLE_ID IS NULL THEN 1 ELSE 0 END) AS NO_ROLE
  FROM RECON_MENU_MASTER m;

-- 0b. Roles whose users span more than one bank. Each must be resolved by hand before
--     STEP 2, because a menu can only have one owner. Expect zero rows.
SELECT u.ROLE_ID, COUNT(DISTINCT u.BANK_ID) AS BANKS
  FROM RECON_USER u
 WHERE u.ROLE_ID IS NOT NULL AND u.BANK_ID IS NOT NULL
 GROUP BY u.ROLE_ID
HAVING COUNT(DISTINCT u.BANK_ID) > 1;

-- 0c. Menus whose role has no user at all — these cannot be resolved to a bank.
--     Expect Kal Admin's bootstrap rows (role "KalInfotech Admin") and nothing else.
SELECT m.MENU_ID, m.MENU_NAME, m.ROLE_ID, r.ROLE_NAME
  FROM RECON_MENU_MASTER m
  LEFT JOIN RECON_ROLE_MASTER r ON r.ROLE_ID = m.ROLE_ID
 WHERE m.ROLE_ID IS NOT NULL
   AND NOT EXISTS (SELECT 1 FROM RECON_USER u WHERE u.ROLE_ID = m.ROLE_ID AND u.BANK_ID IS NOT NULL);


-- -----------------------------------------------------------------------------
-- STEP 1 — Backup.
-- -----------------------------------------------------------------------------
CREATE TABLE BKP_MENU_BEFORE_BANKID AS SELECT * FROM RECON_MENU_MASTER;


-- -----------------------------------------------------------------------------
-- STEP 2 — Schema + backfill. Run BEFORE restarting the app on the new code.
-- -----------------------------------------------------------------------------

ALTER TABLE RECON_MENU_MASTER ADD (BANK_ID NUMBER(19));
ALTER TABLE RECON_MENU_MASTER ADD (PRODUCT_ID NUMBER(19));

COMMENT ON COLUMN RECON_MENU_MASTER.BANK_ID IS
  'Institution that owns this menu row. A branch is its own RECON_BANK_MASTER row. NULL = catalog row or Kal Admin bootstrap menu.';
COMMENT ON COLUMN RECON_MENU_MASTER.PRODUCT_ID IS
  'Product this menu belongs to. NULL = no product restriction (platform menu / catalog row).';

ALTER TABLE RECON_MENU_MASTER
  ADD CONSTRAINT FK_MENU_BANK FOREIGN KEY (BANK_ID) REFERENCES RECON_BANK_MASTER (BANK_ID);
ALTER TABLE RECON_MENU_MASTER
  ADD CONSTRAINT FK_MENU_PRODUCT FOREIGN KEY (PRODUCT_ID) REFERENCES RECON_PRODUCT_MASTER (PRODUCT_ID);

CREATE INDEX IX_MENU_BANK_ID    ON RECON_MENU_MASTER (BANK_ID);
CREATE INDEX IX_MENU_PRODUCT_ID ON RECON_MENU_MASTER (PRODUCT_ID);

-- 2a. Backfill the owner: a menu belongs to the bank of the users who hold its owning role.
--     MIN() is safe only because STEP 0b returned no rows; if it did not, fix those roles first.
UPDATE RECON_MENU_MASTER m
   SET m.BANK_ID = (SELECT MIN(u.BANK_ID)
                      FROM RECON_USER u
                     WHERE u.ROLE_ID = m.ROLE_ID
                       AND u.BANK_ID IS NOT NULL)
 WHERE m.ROLE_ID IS NOT NULL
   AND EXISTS (SELECT 1 FROM RECON_USER u WHERE u.ROLE_ID = m.ROLE_ID AND u.BANK_ID IS NOT NULL);

-- 2b. Roles created during onboarding may still have no user yet (the admin account is
--     created moments later). Fall back to the bank named in the role: the bootstrap roles are
--     "Bank Admin - <bankCode>" / "Branch Admin - <bankCode>".
UPDATE RECON_MENU_MASTER m
   SET m.BANK_ID = (
         SELECT b.BANK_ID FROM RECON_BANK_MASTER b
          WHERE b.BANK_CODE = (SELECT REGEXP_SUBSTR(r.ROLE_NAME, '[^ ]+$')
                                 FROM RECON_ROLE_MASTER r WHERE r.ROLE_ID = m.ROLE_ID))
 WHERE m.BANK_ID IS NULL
   AND m.ROLE_ID IS NOT NULL
   AND EXISTS (SELECT 1 FROM RECON_ROLE_MASTER r
                WHERE r.ROLE_ID = m.ROLE_ID
                  AND (r.ROLE_NAME LIKE 'Bank Admin - %' OR r.ROLE_NAME LIKE 'Branch Admin - %'));

COMMIT;

-- 2c. PRODUCT_ID stays NULL everywhere. Nothing populated the old C_PRODUCT_MENU_MAP, so
--     there is no product mapping to carry over, and NULL already means "every product".
--     Add Menu sets it going forward.


-- -----------------------------------------------------------------------------
-- STEP 3 — Verification. (a) and (b) must return zero rows.
-- -----------------------------------------------------------------------------

-- (a) Every menu that had an owner still has one. Kal Admin's rows are the only allowed
--     exception: KAL_ADMIN has no bank, and its menus are reachable via C_ROLE_MENU_MAP.
SELECT m.MENU_ID, m.MENU_NAME, m.ROLE_ID, r.ROLE_NAME
  FROM RECON_MENU_MASTER m
  LEFT JOIN RECON_ROLE_MASTER r ON r.ROLE_ID = m.ROLE_ID
 WHERE m.ROLE_ID IS NOT NULL
   AND m.BANK_ID IS NULL
   AND (r.ROLE_NAME IS NULL OR r.ROLE_NAME <> 'KalInfotech Admin');

-- (b) No menu was handed to a bank that does not exist.
SELECT m.MENU_ID, m.BANK_ID FROM RECON_MENU_MASTER m
 WHERE m.BANK_ID IS NOT NULL
   AND NOT EXISTS (SELECT 1 FROM RECON_BANK_MASTER b WHERE b.BANK_ID = m.BANK_ID);

-- (c) Sanity: menus per bank, before (via role) and after (via BANK_ID). Should match.
SELECT b.BANK_CODE,
       (SELECT COUNT(*) FROM BKP_MENU_BEFORE_BANKID o
         WHERE o.ROLE_ID IN (SELECT u.ROLE_ID FROM RECON_USER u WHERE u.BANK_ID = b.BANK_ID)) AS BEFORE_,
       (SELECT COUNT(*) FROM RECON_MENU_MASTER n WHERE n.BANK_ID = b.BANK_ID)                 AS AFTER_
  FROM RECON_BANK_MASTER b ORDER BY b.BANK_CODE;

-- (d) Grants are untouched — C_ROLE_MENU_MAP was never part of this migration.
SELECT COUNT(*) AS GRANT_ROWS FROM C_ROLE_MENU_MAP;


-- -----------------------------------------------------------------------------
-- STEP 4 — Per-bank duplicate guard.
--
-- Two banks may now each own a submenu with the same name bound to the same template.
-- Within one bank that is still forbidden. Catalog rows (BANK_ID NULL) are excluded.
-- -----------------------------------------------------------------------------
CREATE UNIQUE INDEX UX_MENU_BANK_NAME_PROCESS ON RECON_MENU_MASTER (
  CASE WHEN BANK_ID IS NULL THEN NULL ELSE BANK_ID END,
  CASE WHEN BANK_ID IS NULL THEN NULL ELSE MENU_NAME END,
  CASE WHEN BANK_ID IS NULL THEN NULL ELSE MENU_PROCESS_ID END
);


-- -----------------------------------------------------------------------------
-- STEP 5 — Retire ROLE_ID. Safe now: no Java code reads RECON_MENU_MASTER.ROLE_ID.
-- Keep the column unread for one release so a rollback stays possible, then drop it.
-- -----------------------------------------------------------------------------
-- ALTER TABLE RECON_MENU_MASTER DROP COLUMN ROLE_ID;


-- -----------------------------------------------------------------------------
-- STEP 6 — Retire C_PRODUCT_MENU_MAP. Its entity and repository are deleted; nothing ever
-- wrote to it. ddl-auto: update will not drop the table, so do it by hand.
-- -----------------------------------------------------------------------------
SELECT COUNT(*) AS ROWS_TO_LOSE FROM C_PRODUCT_MENU_MAP;   -- expect 0
-- DROP TABLE C_PRODUCT_MENU_MAP PURGE;
