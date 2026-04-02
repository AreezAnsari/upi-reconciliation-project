package com.jpb.reconciliation.reconciliation.repository;


import java.util.List;
import java.util.Optional;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import com.jpb.reconciliation.reconciliation.entity.ReconSftpServerMast;

@Repository
public interface ReconSftpServerMastRepository extends JpaRepository<ReconSftpServerMast, Long> {

    List<ReconSftpServerMast> findByIsActive(String isActive);

    Optional<ReconSftpServerMast> findByHost(String host);

    List<ReconSftpServerMast> findByHostContainingIgnoreCase(String host);

    // Legacy aliases — keeps FtpServerServiceImpl compiling
    default List<ReconSftpServerMast> findByServerIp(String ip) {
        return findByHostContainingIgnoreCase(ip);
    }
}
