//package com.jpb.reconciliation.reconciliation.repository;
//
//import org.springframework.data.jpa.repository.JpaRepository;
//import org.springframework.stereotype.Repository;
//
//import com.jpb.reconciliation.reconciliation.entity.RecRole;
//
//import java.util.List;
// 
//@Repository
//public interface RecRoleTestRepository extends JpaRepository<RecRole, Long> {
// 
//    // Sirf ACTIVE roles — assign karne ke liye
//    List<RecRole> findByStatusOrderByRoleNameAsc(String status);
// 
//    // Role code se dhundo
//    boolean existsByRoleCode(String roleCode);
//}
