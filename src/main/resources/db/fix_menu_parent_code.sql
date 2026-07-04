-- One-time data fix: createDefaultAdminMenus() used to store a Master menu's MENU_ID
-- (as a string) in its children's PARENT_MENU_CODE, instead of the Master's MENU_NAME
-- that every other part of the app expects (AddMenu.jsx, PrivilegesAssign, ViewRole,
-- ViewUser all filter children by parentMenuCode === master menuName). This is why
-- "Main" menus (Overview, Add User, Checker Dashboard, etc.) never showed up in the
-- Privileges tree for any bank/branch onboarded before this fix.
--
-- This UPDATE only touches rows whose PARENT_MENU_CODE currently equals some existing
-- Master menu's MENU_ID — rows already using a name (from AddMenu.jsx-created menus)
-- won't match and are left untouched.

UPDATE RECON_MENU_MASTER child
SET child.PARENT_MENU_CODE = (
    SELECT master.MENU_NAME
    FROM RECON_MENU_MASTER master
    WHERE master.MENU_TYPE = 'Master'
      AND TO_CHAR(master.MENU_ID) = child.PARENT_MENU_CODE
)
WHERE child.PARENT_MENU_CODE IN (
    SELECT TO_CHAR(MENU_ID) FROM RECON_MENU_MASTER WHERE MENU_TYPE = 'Master'
);

COMMIT;
