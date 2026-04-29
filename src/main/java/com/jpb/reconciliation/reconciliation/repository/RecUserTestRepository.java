//package com.jpb.reconciliation.reconciliation.repository;
//
//import org.springframework.data.jpa.repository.JpaRepository;
//import org.springframework.data.jpa.repository.Query;
//import org.springframework.data.repository.query.Param;
//import org.springframework.stereotype.Repository;
//
//import com.jpb.reconciliation.reconciliation.entity.RecUserTest;
//
//import java.util.List;
//import java.util.Optional;
// 
//@Repository
//public interface RecUserTestRepository extends JpaRepository<RecUserTest, Long> {
// 
//    // Sirf active users — dropdown ke liye
//    List<RecUserTest> findByIsActiveOrderByFullNameAsc(Integer isActive);
// 
//    // Email se user dhundo
//    Optional<RecUserTest> findByEmail(String email);
// 
//    // Employee code se dhundo
//    Optional<RecUserTest> findByEmployeeCode(String employeeCode);
// 
//    // Email already exist karta hai?
//    boolean existsByEmail(String email);
// 
//    // Employee code already exist karta hai?
//    boolean existsByEmployeeCode(String employeeCode);
// 
//    // Name ya employee code se search (dropdown search ke liye)
//    @Query("SELECT u FROM RecUserTest u WHERE u.isActive = 1 AND " +
//           "(LOWER(u.fullName) LIKE LOWER(CONCAT('%', :keyword, '%')) OR " +
//           "LOWER(u.employeeCode) LIKE LOWER(CONCAT('%', :keyword, '%')) OR " +
//           "LOWER(u.email) LIKE LOWER(CONCAT('%', :keyword, '%')))")
//    List<RecUserTest> searchActiveUsers(@Param("keyword") String keyword);
// 
//    // Department ke saare users
//    List<RecUserTest> findByDepartmentAndIsActiveOrderByFullNameAsc(
//        String department, Integer isActive
//    );
//}
