package com.jpb.reconciliation.reconciliation.config;

import com.jpb.reconciliation.reconciliation.entity.ReconUser;
import com.jpb.reconciliation.reconciliation.repository.ReconUserRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.CommandLineRunner;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Component;

import java.time.LocalDateTime;
import java.util.List;

@Component
public class DataSeeder implements CommandLineRunner {

    private static final Logger logger = LoggerFactory.getLogger(DataSeeder.class);

    @Autowired
    private ReconUserRepository reconUserRepository;

    @Autowired
    private PasswordEncoder passwordEncoder;

    @Override
    public void run(String... args) {
        seedKalAdmin();
    }

    private void seedKalAdmin() {
        List<ReconUser> existing = reconUserRepository.findByUserType("KAL_ADMIN");
        if (!existing.isEmpty()) {
            logger.info("[DataSeeder] KAL_ADMIN already exists — skipping seed.");
            return;
        }

        String defaultPassword = "Admin@1234";

        ReconUser admin = new ReconUser();
        admin.setUsername("kal_admin");
        admin.setFullName("KAL Super Admin");
        admin.setEmail("akkalinfo@gmail.com");
        admin.setMobileNumber("9999999999");
        admin.setUserType("KAL_ADMIN");
        admin.setPasswordHash(passwordEncoder.encode(defaultPassword));
        admin.setPasswordSet(1);
        admin.setPasswordUpdatedAt(LocalDateTime.now());
        admin.setStatus("ACTIVE");
        admin.setApprovedYn("Y");
        admin.setApprovedBy("SYSTEM");
        admin.setCreatedAt(LocalDateTime.now());
        admin.setCreatedBy("SYSTEM");

        reconUserRepository.save(admin);
        logger.info("[DataSeeder] KAL_ADMIN seeded — username: kal_admin, password: {}", defaultPassword);
    }
}
