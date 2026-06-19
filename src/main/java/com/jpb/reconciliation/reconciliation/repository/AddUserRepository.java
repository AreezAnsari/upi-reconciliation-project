package com.jpb.reconciliation.reconciliation.repository;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import com.jpb.reconciliation.reconciliation.entity.AddUser;

import java.time.LocalDateTime;
import java.util.Collection;
import java.util.List;
import java.util.Optional;

public interface AddUserRepository extends JpaRepository<AddUser, Long> {

    Optional<AddUser> findByUsername(String username);

    Optional<AddUser> findByEmail(String email);

    boolean existsByUsername(String username);

    boolean existsByEmail(String email);

    List<AddUser> findByCreatedBy(String createdBy);

    List<AddUser> findByBankCode(String bankCode);

    List<AddUser> findByBranchCode(String branchCode);

    List<AddUser> findByBankCodeAndBranchCodeIsNull(String bankCode);

    @Query("SELECT u FROM AddUser u WHERE u.createdBy = :createdBy AND " +
           "(LOWER(u.username) LIKE LOWER(CONCAT('%', :term, '%')) OR " +
           "LOWER(u.email) LIKE LOWER(CONCAT('%', :term, '%')))")
    List<AddUser> searchByCreator(@Param("createdBy") String createdBy, @Param("term") String term);

    // Scheduler queries for auto-transitions
    List<AddUser> findByStatusAndInactivateScheduledAtBefore(AddUser.UserStatus status, LocalDateTime cutoff);
    List<AddUser> findByStatusAndReactivateScheduledAtBefore(AddUser.UserStatus status, LocalDateTime cutoff);
    List<AddUser> findByStatusAndBlockScheduledAtBefore(AddUser.UserStatus status, LocalDateTime cutoff);

    // Quick existence check — used as scheduler pre-check to avoid full queries when nothing is pending
    boolean existsByStatusIn(Collection<AddUser.UserStatus> statuses);
}
