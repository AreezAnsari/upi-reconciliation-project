package com.jpb.reconciliation.reconciliation.repository;

import java.util.List;
import java.util.Optional;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import com.jpb.reconciliation.reconciliation.entity.KalAdmin;

@Repository
public interface KalAdminRepository extends JpaRepository<KalAdmin, Long> {

	Optional<KalAdmin> findByUserId(Long userId);

	Optional<KalAdmin> findByUserName(String username);

	Boolean existsByUserName(String userName);

	Boolean existsByEmailId(String emailId);

	Boolean existsByUserNameAndEmailId(String userName, String emailId);

	Optional<KalAdmin> findByEmailId(String email);

	List<KalAdmin> findByApprovedYn(String approvedYN);

	Optional<KalAdmin> findByUserNameAndApprovedYn(String username, String string);

	Optional<KalAdmin> findByUserNameAndUserStatus(String userName, String userStatus);

}
