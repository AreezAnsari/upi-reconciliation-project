//package com.jpb.reconciliation.reconciliation.repository;
//
//import org.springframework.data.jpa.repository.JpaRepository;
//import org.springframework.stereotype.Repository;
//
//import com.jpb.reconciliation.reconciliation.entity.JpbRole;
//
//import java.util.List;
//
//@Repository
//public interface JpbRoleRepository extends JpaRepository<JpbRole, Long> {
//    List<JpbRole> findByStatus(String status);
//    List<JpbRole> findByRoleType(String roleType);
//    boolean existsByRoleName(String roleName);
//}
