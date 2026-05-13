//package com.jpb.reconciliation.reconciliation.repository;
//
//import org.springframework.data.jpa.repository.JpaRepository;
//import org.springframework.data.jpa.repository.Query;
//import org.springframework.data.repository.query.Param;
//import org.springframework.stereotype.Repository;
//
//import com.jpb.reconciliation.reconciliation.entity.RecUserRoleMappingTest;
//
//import java.util.List;
//import java.util.Optional;
// 
//@Repository
//public interface RecUserRoleMappingTestRepository
//        extends JpaRepository<RecUserRoleMappingTest, Long> {
// 
//    // Duplicate check — same user same role
//    boolean existsByUserUserIdAndRoleId(Long userId, Long roleId);
// 
//    // Ek user ke saare role assignments
//    List<RecUserRoleMappingTest> findByUserUserIdOrderByCreatedAtDesc(Long userId);
// 
//    // Ek role ke saare assigned users
//    List<RecUserRoleMappingTest> findByRoleIdOrderByCreatedAtDesc(Long roleId);
// 
//    // Specific mapping find karo
//    Optional<RecUserRoleMappingTest> findByUserUserIdAndRoleId(
//        Long userId, Long roleId
//    );
// 
//    // Active assignments only
//    List<RecUserRoleMappingTest> findByUserUserIdAndStatus(
//        Long userId, String status
//    );
// 
//    // User + Role ka full detail ek query mein
//    @Query("SELECT m FROM RecUserRoleMappingTest m " +
//           "JOIN FETCH m.user u " +
//           "JOIN FETCH m.role r " +
//           "WHERE u.userId = :userId")
//    List<RecUserRoleMappingTest> findByUserIdWithDetails(@Param("userId") Long userId);
//}