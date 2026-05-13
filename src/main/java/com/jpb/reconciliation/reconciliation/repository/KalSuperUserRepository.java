package com.jpb.reconciliation.reconciliation.repository;

<<<<<<< HEAD
import java.util.Optional;
=======
import java.util.Optional; 

>>>>>>> origin/feature/areez-ui
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;
import com.jpb.reconciliation.reconciliation.entity.KalSuperUser;

@Repository
public interface KalSuperUserRepository extends JpaRepository<KalSuperUser, Long> {

    // Login aur setup ke liye — main lookup
    Optional<KalSuperUser> findByInstitutionCodeAndSuperUserId(
            String institutionCode,
            String superUserId
    );

<<<<<<< HEAD
    // ✅ Forgot password — email se dhundho (findFirst = multiple records ho to pehla lo)
    Optional<KalSuperUser> findFirstByEmail(String email);

    // superUserId se dhundho (backward compat)
    Optional<KalSuperUser> findBySuperUserId(String superUserId);
=======
    // Used by OtpController to activate institution after first login
    Optional<KalSuperUser>
    findByEmail(String email);    
>>>>>>> origin/feature/areez-ui
}