package com.jpb.reconciliation.reconciliation.repository;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import com.jpb.reconciliation.reconciliation.entity.AddUser;

import java.util.List;
import java.util.Optional;

public interface AddUserRepository extends JpaRepository<AddUser, Long> {

    Optional<AddUser> findByUsername(String username);

    Optional<AddUser> findByEmail(String email);

    boolean existsByUsername(String username);

    boolean existsByEmail(String email);

    @Query("SELECT u FROM AddUser u WHERE u.bankCode = :bankCode AND " +
           "(LOWER(u.username) LIKE LOWER(CONCAT('%', :term, '%')) OR " +
           "LOWER(u.email) LIKE LOWER(CONCAT('%', :term, '%')))")
    List<AddUser> searchUsers(@Param("bankCode") String bankCode, @Param("term") String term);
}
