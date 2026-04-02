package com.jpb.reconciliation.reconciliation.entity;


import java.time.LocalDateTime;
import javax.persistence.*;
import lombok.*;

@Entity
@Table(name = "recon_sftp_server_mast")
@Data @NoArgsConstructor @AllArgsConstructor
@EqualsAndHashCode(onlyExplicitlyIncluded = true)
public class ReconSftpServerMast {

    @Id
    @GeneratedValue(strategy = GenerationType.SEQUENCE, generator = "SEQ_SFTP_SERVER_MAST")
    @SequenceGenerator(name = "SEQ_SFTP_SERVER_MAST", sequenceName = "seq_sftp_server_mast", allocationSize = 1)
    @Column(name = "sftp_server_id")
    @EqualsAndHashCode.Include
    private Long sftpServerId;

    @Column(name = "server_name", nullable = false, length = 100)
    private String serverName;

    @Column(name = "host", nullable = false, length = 200)
    private String host;

    @Column(name = "port", nullable = false)
    private Integer port = 22;

    @Column(name = "default_username", length = 100)
    private String defaultUsername;

    @Column(name = "auth_type", nullable = false, length = 20)
    private String authType = "PASSWORD";

    @Column(name = "credential_ref", length = 200)
    private String credentialRef;

    @Column(name = "is_active", length = 1)
    private String isActive = "Y";

    @Column(name = "created_by", nullable = false, length = 50)
    private String createdBy;

    @Column(name = "created_at", nullable = false, updatable = false)
    private LocalDateTime createdAt;

    @Column(name = "updated_by", length = 50)
    private String updatedBy;

    @Column(name = "updated_at")
    private LocalDateTime updatedAt;

    // Legacy aliases for old FtpServer code
    public String getFtpServerName() { return this.serverName; }
    public String getServerIp()      { return this.host; }
    public String getUserName()      { return this.defaultUsername; }
    public Long getId()              { return this.sftpServerId; }
}
