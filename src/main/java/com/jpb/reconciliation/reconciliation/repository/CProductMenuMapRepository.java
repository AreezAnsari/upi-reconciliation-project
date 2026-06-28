package com.jpb.reconciliation.reconciliation.repository;

import com.jpb.reconciliation.reconciliation.entity.CProductMenuMap;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface CProductMenuMapRepository extends JpaRepository<CProductMenuMap, CProductMenuMap.ProductMenuMapId> {

    @Query("SELECT p FROM CProductMenuMap p WHERE p.id.productId = :productId")
    List<CProductMenuMap> findByProductId(@Param("productId") Long productId);

    @Query("SELECT p FROM CProductMenuMap p WHERE p.id.menuId = :menuId")
    List<CProductMenuMap> findByMenuId(@Param("menuId") Long menuId);
}
