package com.jpb.reconciliation.reconciliation.repository;

import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;
import com.jpb.reconciliation.reconciliation.entity.KalCreateUser;

@Repository
public interface KalUserRepo extends JpaRepository<KalCreateUser, Long> {

    Optional<KalCreateUser> findByUsername(String username);

    Optional<KalCreateUser> findByEmail(String email);
}
