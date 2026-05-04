package com.jpb.reconciliation.reconciliation.repository;

import java.util.List; 
import java.util.Optional;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import com.jpb.reconciliation.reconciliation.entity.TestUser;

@Repository
public interface TestUserRepository extends JpaRepository<TestUser, Long> {
	Optional<TestUser> findByUserId(Long userId);

	Optional<TestUser> findByUserName(String username);

	Boolean existsByUserNameAndEmailId(String userName, String emailId);

	Optional<TestUser> findByEmailId(String email);

	List<TestUser> findByApprovedYn(String approvedYN);

	Optional<TestUser> findByUserNameAndApprovedYn(String username, String string);

	Optional<TestUser> findByUserNameAndUserStatus(String userName, String userStatus);
}