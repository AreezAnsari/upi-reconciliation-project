package com.jpb.reconciliation.reconciliation.repository;

import java.util.List;
import java.util.Optional;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import com.jpb.reconciliation.reconciliation.entity.ReconMenuMaster;

@Repository
public interface MenuMasterRepository extends JpaRepository<ReconMenuMaster, Long> {

    Optional<ReconMenuMaster> findByMenuId(Long menuId);

    List<ReconMenuMaster> getByInsertUserId(Long userId);

    ReconMenuMaster findByMenuName(String menuName);

    List<ReconMenuMaster> findByParentMenuCode(String menuType);

    ReconMenuMaster findByMenuNameAndInsertUserId(String menuName, Long userId);

    // Bank scoping. A branch is its own RECON_BANK_MASTER row, so this serves both.
    // Replaces findByRoleIdIn(bank -> users -> roleIds), which needed two extra hops.
    List<ReconMenuMaster> findByBankId(Long bankId);

    ReconMenuMaster findByMenuNameAndBankId(String menuName, Long bankId);

    // Duplicate guard for Add Menu: the same menu name may exist under the same parent in a
    // DIFFERENT bank, but never twice within one.
    ReconMenuMaster findByMenuNameAndBankIdAndParentMenuCode(String menuName, Long bankId, String parentMenuCode);

    // Identifies a /user-portal "twin" of an Admin-portal menu regardless of which role first
    // created it, so it can be reused across the custom roles OF THE SAME BANK instead of
    // duplicated. Bank-scoped on purpose: /user/add-user is the same URL for every institution,
    // so a name+url lookup alone handed a Branch's role the parent Bank's menu row.
    Optional<ReconMenuMaster> findByMenuNameAndMenuUrlAndBankId(String menuName, String menuUrl, Long bankId);

    // Master menu names ("My Organization"/"Administration") aren't unique — Kal Admin, each
    // Bank Admin, and each Branch Admin all have their own bootstrap row with the same name.
    // Returns every match so the caller can pick any active one purely for nesting purposes.
    List<ReconMenuMaster> findAllByMenuNameAndMenuType(String menuName, String menuType);

    // ── Catalog (reference) rows ────────────────────────────────────────────────
    // A catalog row belongs to no institution: BANK_ID IS NULL. It is never granted and never
    // reaches a sidebar; Add Menu only reads it to answer "is this menu registered?".
    //
    // Kal Admin's bootstrap menus also carry a NULL BANK_ID, so name+type alone would collide
    // with them. Every query below is additionally keyed on the hierarchy, which those rows
    // never satisfy for a Submenu.

    @Query("SELECT m FROM ReconMenuMaster m WHERE m.bankId IS NULL AND m.menuType = 'Submenu' "
         + "AND m.menuName = :menuName AND m.parentMenuCode = :mainName AND m.masterMenuParent = :masterName")
    Optional<ReconMenuMaster> findCatalogSubmenu(@Param("menuName") String menuName,
                                                 @Param("mainName") String mainName,
                                                 @Param("masterName") String masterName);

    @Query("SELECT m FROM ReconMenuMaster m WHERE m.bankId IS NULL AND m.menuType = :menuType "
         + "AND m.menuName = :menuName")
    Optional<ReconMenuMaster> findCatalogByNameAndType(@Param("menuName") String menuName,
                                                       @Param("menuType") String menuType);

    /** Names of the Mains that the catalog declares at least one Submenu under. Drives Case A. */
    @Query("SELECT DISTINCT m.parentMenuCode FROM ReconMenuMaster m "
         + "WHERE m.bankId IS NULL AND m.menuType = 'Submenu'")
    List<String> findCatalogMainsWithSubmenus();

    /** The catalog entry for a permanent identity code. The authoritative lookup — display names
     *  are renameable, the code is not. */
    @Query("SELECT m FROM ReconMenuMaster m WHERE m.bankId IS NULL AND m.systemMenuCode = :code")
    Optional<ReconMenuMaster> findCatalogByCode(@Param("code") String code);

    /** Every bank-owned mapping this bank already holds for one logical (catalog) menu. Drives the
     *  duplicate / reactivate check — the caller narrows further by product and process. */
    @Query("SELECT m FROM ReconMenuMaster m WHERE m.bankId = :bankId AND m.systemMenuCode = :code")
    List<ReconMenuMaster> findBankMappingsByCode(@Param("bankId") Long bankId, @Param("code") String code);

    /** Appended rows a bank holds for one product — used to cascade when a product is deactivated. */
    List<ReconMenuMaster> findByBankIdAndProductId(Long bankId, Long productId);
}
