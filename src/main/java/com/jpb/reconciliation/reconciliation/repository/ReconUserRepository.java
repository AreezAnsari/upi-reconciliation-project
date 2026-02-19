package com.jpb.reconciliation.reconciliation.repository;

import java.util.List;
import java.util.Optional;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import com.jpb.reconciliation.reconciliation.entity.ReconUser;

@Repository
public interface ReconUserRepository extends JpaRepository<ReconUser, Long> {

	Optional<ReconUser> findByUserId(Long userId);

	Optional<ReconUser> findByUserName(String username);

	Boolean existsByUserNameAndEmailId(String userName, String emailId);

	Optional<ReconUser> findByEmailId(String email);

	List<ReconUser> findByApprovedYn(String approvedYN);

	Optional<ReconUser> findByUserNameAndApprovedYn(String username, String string);

	Optional<ReconUser> findByUserNameAndUserStatus(String userName, String userStatus);

}
