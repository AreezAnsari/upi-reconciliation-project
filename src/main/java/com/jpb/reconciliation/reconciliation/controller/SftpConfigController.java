//
//import java.util.List;
//
//import javax.validation.Valid;
//
//import org.springframework.http.ResponseEntity;
//import org.springframework.web.bind.annotation.DeleteMapping;
//import org.springframework.web.bind.annotation.GetMapping;
//import org.springframework.web.bind.annotation.PathVariable;
//import org.springframework.web.bind.annotation.PostMapping;
//import org.springframework.web.bind.annotation.RequestBody;
//import org.springframework.web.bind.annotation.RequestMapping;
//import org.springframework.web.bind.annotation.RestController;
//
//import com.jpb.reconciliation.reconciliation.dto.RestWithStatusList;
//import com.kalinfotech.reconciliation.service.SftpConfigService;
//
//import io.swagger.v3.oas.annotations.Operation;
//import io.swagger.v3.oas.annotations.tags.Tag;
//import lombok.RequiredArgsConstructor;
//import lombok.experimental.var;
//
///**
// * SFTP / file-delivery config per template.
// *
// * URL base  : /api/v1/templates/{templateId}/sftp-config
// * Entity    : SftpConfig  (tbl_sftp_config)
// * Service   : SftpConfigService  (impl: SftpConfigServiceImpl)
// * Response  : RestWithStatusList – project-wide standard wrapper
//// *
// * Request DTO  – SftpConfigRequest:
// *   sftpServerRef        Long    (optional FK to sftp server master)
// *   host                 String
// *   port                 Integer (default 22)
// *   username             String
// *   authType             String  PASSWORD | KEY_BASED
// *   credentialRef        String  vault/KMS path – never a plain password
// *   remoteDirectory      String
// *   filePattern          String  e.g. CBS_TXN_*.csv
// *   pollingFrequencyCron String  cron expression
// *   archiveAfterPickup   Boolean (default true)
// *   archiveDirectory     String
// *   deliveryMode         DeliveryMode  SFTP_AUTO | MANUAL
// *
// * Response DTO – SftpConfigResponse:
// *   sftpConfigId, templateId, sftpServerRef, host, port, username,
// *   authType, remoteDirectory, filePattern, pollingFrequencyCron,
// *   archiveAfterPickup, archiveDirectory, deliveryMode,
// *   lastTestedAt, lastTestStatus
// */
//@RestController
//@RequestMapping("/api/v1/templates/{templateId}/sftp-config")
//@RequiredArgsConstructor
//@Tag(name = "SFTP Configuration",
//        description = "File delivery configuration (SFTP / manual) per template")
//public class SftpConfigController {
//
//    // Injected via @RequiredArgsConstructor – interface only, no impl reference
//    private final SftpConfigService sftpConfigService;
//
//    // ─────────────────────────────────────────────────────────────────────────
//    // POST  /api/v1/templates/{templateId}/sftp-config
//    // Upsert: creates the row if absent, updates it if already exists.
//    // ─────────────────────────────────────────────────────────────────────────
//    @PostMapping
//    @Operation(summary = "Save or update SFTP config for a template")
//    public ResponseEntity<RestWithStatusList> save(
//            @PathVariable Long templateId,
//            @Valid @RequestBody SftpConfigRequest request) {
//
//        return ok("SFTP config saved successfully.",
//                sftpConfigService.saveSftpConfig(templateId, request));
//    }
//
//    // ─────────────────────────────────────────────────────────────────────────
//    // GET   /api/v1/templates/{templateId}/sftp-config
//    // ─────────────────────────────────────────────────────────────────────────
//    @GetMapping
//    @Operation(summary = "Get SFTP config for a template")
//    public ResponseEntity<RestWithStatusList> get(
//            @PathVariable Long templateId) {
//
//        return ok("SFTP config fetched.",
//                sftpConfigService.getSftpConfig(templateId));
//    }
//
//    // ─────────────────────────────────────────────────────────────────────────
//    // POST  /api/v1/templates/{templateId}/sftp-config/test-connection
//    // Performs a live connection test and persists lastTestedAt/lastTestStatus.
//    // Returns SftpTestResult { success, message, testedAt }.
//    // ─────────────────────────────────────────────────────────────────────────
//    @PostMapping("/test-connection")
//    @Operation(summary = "Test SFTP connectivity — persists lastTestedAt and lastTestStatus")
//    public ResponseEntity<RestWithStatusList> testConnection(
//            @PathVariable Long templateId) {
//
//        var result = sftpConfigService.testConnection(templateId);
//        String msg = result.isSuccess()
//                ? "SFTP connection successful."
//                : "SFTP connection failed: " + result.getMessage();
//        return ok(msg, result);
//    }
//
//    // ─────────────────────────────────────────────────────────────────────────
//    // DELETE /api/v1/templates/{templateId}/sftp-config
//    // Hard-deletes the SFTP config row for this template.
//    // The template will have no SFTP config until a new POST is made.
//    // ─────────────────────────────────────────────────────────────────────────
//    @DeleteMapping
//    @Operation(summary = "Delete SFTP config for a template")
//    public ResponseEntity<RestWithStatusList> delete(
//            @PathVariable Long templateId) {
//
//        sftpConfigService.deleteSftpConfig(templateId);
//        return ok("SFTP config deleted successfully.", null);
//    }
//
//    // ─────────────────────────────────────────────────────────────────────────
//    // private helper — keeps every endpoint consistent
//    // ─────────────────────────────────────────────────────────────────────────
//    private ResponseEntity<RestWithStatusList> ok(String msg, Object data) {
//        return ResponseEntity.ok(RestWithStatusList.builder()
//                .status("SUCCESS")
//                .statusMsg(msg)
//                .data(data != null ? List.of(data) : List.of())
//                .build());
//    }
//}
