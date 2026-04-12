package com.jpb.reconciliation.reconciliation.dto;

import lombok.Data;

@Data
public class SftpTestConnectionRequestDTO {
    private String host;
    private Integer port;
    private String username;
    private String password;
    private String remotePath;
}