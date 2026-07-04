-- Lets a Role be scoped to a specific Bank Type (Issuer/Acquirer) when the owning
-- Bank/Branch itself supports both — e.g. a MAKER role that can only work Issuer-side
-- transactions even though the bank is registered as both Issuer and Acquirer.
-- If the bank/branch only has ONE bank type, this is irrelevant and the Privileges
-- screen hides the selector entirely (defaults to that one type).

ALTER TABLE RECON_ROLE_MASTER ADD (BANK_TYPE_SCOPE VARCHAR2(50 CHAR));
