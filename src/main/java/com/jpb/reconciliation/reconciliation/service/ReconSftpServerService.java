package com.jpb.reconciliation.reconciliation.service;

import org.springframework.http.ResponseEntity;

import com.jpb.reconciliation.reconciliation.dto.RestWithMapStatusList;
import com.jpb.reconciliation.reconciliation.dto.SftpServerRequestDTO;
import com.jpb.reconciliation.reconciliation.dto.SftpTestConnectionRequestDTO;

public interface ReconSftpServerService {

    ResponseEntity<RestWithMapStatusList> createServer(SftpServerRequestDTO request, String createdBy);

    ResponseEntity<RestWithMapStatusList> updateServer(Long serverId, SftpServerRequestDTO request, String updatedBy);

    ResponseEntity<RestWithMapStatusList> getServerById(Long serverId);

    ResponseEntity<RestWithMapStatusList> getAllServers(boolean activeOnly);

    ResponseEntity<RestWithMapStatusList> deleteServer(Long serverId, String updatedBy);

    ResponseEntity<RestWithMapStatusList> testConnection(SftpTestConnectionRequestDTO request);
}