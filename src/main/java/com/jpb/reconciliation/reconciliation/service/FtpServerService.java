package com.jpb.reconciliation.reconciliation.service;


import org.springframework.http.ResponseEntity;

import com.jpb.reconciliation.reconciliation.dto.FtpServerDTO;
import com.jpb.reconciliation.reconciliation.dto.RestWithMapStatusList;

public interface FtpServerService {

    ResponseEntity<RestWithMapStatusList> createFtpServer(FtpServerDTO dto);

    ResponseEntity<RestWithMapStatusList> getFtpServerById(Long id);

    ResponseEntity<RestWithMapStatusList> getAllFtpServers();

    ResponseEntity<RestWithMapStatusList> updateFtpServer(Long id, FtpServerDTO dto);

    ResponseEntity<RestWithMapStatusList> deleteFtpServer(Long id);

    ResponseEntity<RestWithMapStatusList> getFtpServersByIp(String serverIp);
}
