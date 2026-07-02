package com.jpb.reconciliation.reconciliation.repository.v2;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import com.jpb.reconciliation.reconciliation.entity.v2.ReconUser;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

@Repository
public interface ReconUserRepository extends JpaRepository<ReconUser, Long> {

    Optional<ReconUser> findByUsername(String username);

    Optional<ReconUser> findByEmail(String email);

    boolean existsByUsername(String username);

    boolean existsByEmail(String email);

    List<ReconUser> findByStatus(String status);

    List<ReconUser> findByBankId(Long bankId);

    List<ReconUser> findByUserType(String userType);

    List<ReconUser> findByApprovedYn(String approvedYn);

    List<ReconUser> findByBankIdAndStatus(Long bankId, String status);

    List<ReconUser> findByRoleId(Long roleId);

    Optional<ReconUser> findByUsernameAndStatus(String username, String status);

    Optional<ReconUser> findByUsernameAndApprovedYn(String username, String approvedYn);

    List<ReconUser> findByInactivateScheduledAtBefore(LocalDateTime time);

    List<ReconUser> findByReactivateScheduledAtBefore(LocalDateTime time);

    List<ReconUser> findByBlockScheduledAtBefore(LocalDateTime time);

    List<ReconUser> findByBankIdAndUserType(Long bankId, String userType);

    Optional<ReconUser> findByBankIdAndUserTypeAndContactRank(Long bankId, String userType, String contactRank);

    Optional<ReconUser> findByUsernameAndBankId(String username, Long bankId);

    List<ReconUser> findByParentUserId(Long parentUserId);

    Optional<ReconUser> findByEmailAndUserType(String email, String userType);
}
