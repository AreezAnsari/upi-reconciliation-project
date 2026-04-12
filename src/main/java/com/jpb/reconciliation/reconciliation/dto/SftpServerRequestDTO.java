package com.jpb.reconciliation.reconciliation.dto;

import lombok.Data;

@Data
public class SftpServerRequestDTO {
    private String serverName;
    private String host;
    private Integer port = 22;
    private String defaultUsername;
    private String password;
    private String protocol;
    private String authType = "PASSWORD";  // PASSWORD or KEY_BASED
}