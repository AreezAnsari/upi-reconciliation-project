package com.jpb.reconciliation.reconciliation.repository;

import com.jpb.reconciliation.reconciliation.entity.UserDelegation;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;

public interface UserDelegationRepository extends JpaRepository<UserDelegation, Long> {

    Optional<UserDelegation> findByDelegatorUserIdAndStatus(Long delegatorUserId, String status);

    boolean existsByDelegatorUserIdAndStatus(Long delegatorUserId, String status);

    Optional<UserDelegation> findFirstByDelegateeUserIdAndStatus(Long delegateUserId, String status);
}
