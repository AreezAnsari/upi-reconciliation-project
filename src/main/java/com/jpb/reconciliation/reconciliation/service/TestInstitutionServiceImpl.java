package com.jpb.reconciliation.reconciliation.service;

import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Random;
import java.util.UUID;
import java.util.stream.Collectors;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.multipart.MultipartFile;

import org.apache.poi.ss.usermodel.*;
import org.apache.poi.xssf.usermodel.XSSFWorkbook;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import com.jpb.reconciliation.reconciliation.dto.RestWithStatusList;
import com.jpb.reconciliation.reconciliation.dto.TestInstitutionDTO;
import com.jpb.reconciliation.reconciliation.entity.TestInstitution;
import com.jpb.reconciliation.reconciliation.mapper.TestInstitutionMapper;
import com.jpb.reconciliation.reconciliation.repository.TestInstitutionRepository;

@Service
public class TestInstitutionServiceImpl implements TestInstitutionService {

    private static final Logger logger = LoggerFactory.getLogger(TestInstitutionServiceImpl.class);

    private static final String LOGO_UPLOAD_DIR = "/home/ec2-user/institution_logos/";

    private static final List<String> ALLOWED_TYPES = Arrays.asList(
            "image/jpeg", "image/jpg", "image/tiff", "image/tif"
    );
    private static final long MAX_LOGO_SIZE = 2 * 1024 * 1024; // 2 MB

    @Autowired
    private TestInstitutionRepository testInstitutionRepository;

    @Autowired
    private EmailService emailService;

    @Value("${app.frontend.url:http://localhost:5173}")
    private String frontendUrl;

    // ─────────────────────────────────────────────────────────────────────────
    // CREATE
    // ─────────────────────────────────────────────────────────────────────────
    @Override
    @Transactional
    public ResponseEntity<RestWithStatusList> createInstitution(TestInstitutionDTO dto) {

        if (dto.getInstitutionNameFull() == null || dto.getInstitutionNameFull().trim().isEmpty()) {
            return bad("Institution full name is required.");
        }
        if (dto.getRegAddressLine1() == null || dto.getRegAddressLine1().trim().isEmpty()) {
            return bad("Registered address line 1 is required.");
        }
        if (dto.getRegCity() == null || dto.getRegCity().trim().isEmpty()) {
            return bad("Registered city is required.");
        }
        if (dto.getPrimaryFullName() == null || dto.getPrimaryFullName().trim().isEmpty()) {
            return bad("Primary contact full name is required.");
        }
        if (dto.getPrimaryEmail() == null || dto.getPrimaryEmail().trim().isEmpty()) {
            return bad("Primary contact email is required.");
        }
        if (dto.getPrimaryMobile() == null || dto.getPrimaryMobile().trim().isEmpty()) {
            return bad("Primary contact mobile is required.");
        }

        if (testInstitutionRepository.existsByInstitutionNameFullIgnoreCase(dto.getInstitutionNameFull().trim())) {
            logger.warn("Institution already exists: {}", dto.getInstitutionNameFull());
            return bad("Institution with name '" + dto.getInstitutionNameFull() + "' already exists.");
        }
        
        if (testInstitutionRepository.existsByPrimaryEmail(dto.getPrimaryEmail().trim())) {
            return bad("An institution with email '" + dto.getPrimaryEmail() + "' is already registered.");
        }

        // ── Institution code: accept from frontend — validate uniqueness only ──
        // Frontend generates pure 8-digit code and sends in payload
        // Backend: validate format + uniqueness → save as-is
        String institutionCode = dto.getInstitutionCode();
        if (institutionCode == null || !institutionCode.matches("\\d{8}")) {
            return bad("Institution code must be exactly 8 digits.");
        }
        if (testInstitutionRepository.findByInstitutionCode(institutionCode).isPresent()) {
            return bad("Institution code already exists. Please regenerate and try again.");
        }
        logger.info("Institution code accepted from frontend: {}", institutionCode);

        // ── Generate Super User ID — rule: firstname.lastname all lowercase ──
        // e.g. "Rajesh Kumar" → "rajesh.kumar"
        String superUserId = generateSuperUserId(dto.getPrimaryFullName());

        // ── Generate default password — rule: min 8 chars, 1 upper, 1 special, 1 number ──
        // e.g. "Recon@1234" pattern with random suffix
        String defaultPassword = generateDefaultPassword();

        // Map DTO → Entity
        TestInstitution institution = TestInstitutionMapper.mapToEntity(dto, new TestInstitution());
        institution.setInstitutionCode(institutionCode);
        institution.setStatus("REQUEST");  // Sir's rule: REQUEST when onboarded by Kal Admin
        institution.setCreatedAt(LocalDateTime.now());

        // Save Super User credentials in institution record
        institution.setSuperUserId(superUserId);
        institution.setDefaultPassword(defaultPassword);

        // Generate verification token — valid for 48 hours
        String token = UUID.randomUUID().toString();
        institution.setVerificationToken(token);
        institution.setTokenExpiry(LocalDateTime.now().plusHours(48));

        testInstitutionRepository.save(institution);
        logger.info("Institution created: {} | Code: {} | SuperUserId: {}",
                    dto.getInstitutionNameFull(), institutionCode, superUserId);

        // ── Send welcome email with Institution Code, User ID, Default Password ──
        String verifyLink = frontendUrl + "/verify-email?token=" + token;
        try {
            emailService.sendSuperUserWelcome(
                dto.getPrimaryEmail(),
                dto.getPrimaryFullName(),
                dto.getInstitutionNameFull(),
                institutionCode,
                superUserId,
                defaultPassword,
                verifyLink
            );
            logger.info("Welcome email dispatched to: {} | userId: {} | institution: {}",
                        dto.getPrimaryEmail(), superUserId, dto.getInstitutionNameFull());
        } catch (Exception e) {
            // Email failure should NOT rollback the onboarding — just log the warning
            logger.warn("Institution saved but welcome email failed for {}: {}",
                        dto.getPrimaryEmail(), e.getMessage());
        }

        List<Object> data = new ArrayList<>();
        data.add(TestInstitutionMapper.mapToDTO(institution));

        return ResponseEntity.status(HttpStatus.CREATED)
                .body(new RestWithStatusList("SUCCESS",
                        "Institution '" + dto.getInstitutionNameFull() + "' onboarded successfully.", data));
    }

    // ─────────────────────────────────────────────────────────────────────────
    // GET ALL
    // ─────────────────────────────────────────────────────────────────────────
    @Override
    @Transactional(readOnly = true)
    public ResponseEntity<RestWithStatusList> getAllInstitutions() {
        List<TestInstitution> list = testInstitutionRepository.findAll();

        if (list.isEmpty()) {
            return ResponseEntity.ok(new RestWithStatusList("SUCCESS", "No institutions found.", new ArrayList<>()));
        }

        List<Object> data = list.stream()
                .map(TestInstitutionMapper::mapToDTO)
                .collect(Collectors.toList());
        logger.info("Fetched {} institutions", list.size());

        return ResponseEntity.ok(new RestWithStatusList("SUCCESS",
                list.size() + " institution(s) fetched successfully.", data));
    }

    // ─────────────────────────────────────────────────────────────────────────
    // GET BY ID
    // ─────────────────────────────────────────────────────────────────────────
    @Override
    @Transactional(readOnly = true)
    public ResponseEntity<RestWithStatusList> getInstitutionById(Long institutionId) {
        Optional<TestInstitution> optional = testInstitutionRepository.findByInstitutionId(institutionId);

        if (!optional.isPresent()) {
            logger.warn("Institution not found: {}", institutionId);
            return bad("Institution not found with ID: " + institutionId);
        }

        List<Object> data = new ArrayList<>();
        data.add(TestInstitutionMapper.mapToDTO(optional.get()));

        return ResponseEntity.ok(new RestWithStatusList("SUCCESS", "Institution fetched successfully.", data));
    }

    // ─────────────────────────────────────────────────────────────────────────
    // GET BY STATUS
    // ─────────────────────────────────────────────────────────────────────────
    @Override
    @Transactional(readOnly = true)
    public ResponseEntity<RestWithStatusList> getInstitutionsByStatus(String status) {
        List<TestInstitution> list = testInstitutionRepository.findByStatus(status.toUpperCase());

        List<Object> data = list.stream()
                .map(TestInstitutionMapper::mapToDTO)
                .collect(Collectors.toList());
        logger.info("Fetched {} institutions with status: {}", list.size(), status);

        return ResponseEntity.ok(new RestWithStatusList("SUCCESS",
                list.size() + " institution(s) with status '" + status + "' fetched.", data));
    }

    // ─────────────────────────────────────────────────────────────────────────
    // FULL UPDATE
    // ─────────────────────────────────────────────────────────────────────────
    @Override
    @Transactional
    public ResponseEntity<RestWithStatusList> updateInstitution(Long institutionId, TestInstitutionDTO dto) {
        Optional<TestInstitution> optional = testInstitutionRepository.findByInstitutionId(institutionId);

        if (!optional.isPresent()) {
            return bad("Institution not found with ID: " + institutionId);
        }

        TestInstitution institution = optional.get();

        // Preserve system-generated fields
        String existingCode             = institution.getInstitutionCode();
        String existingStatus           = institution.getStatus();
        String existingLogo             = institution.getLogoPath();
        String existingSuperUserId      = institution.getSuperUserId();
        String existingDefaultPassword  = institution.getDefaultPassword();
        LocalDateTime existingCreatedAt = institution.getCreatedAt();
        String existingCreatedBy        = institution.getCreatedBy();

        TestInstitutionMapper.mapToEntity(dto, institution);

        // Restore protected fields
        institution.setInstitutionCode(existingCode);
        institution.setStatus(existingStatus);
        institution.setLogoPath(existingLogo);
        institution.setSuperUserId(existingSuperUserId);
        institution.setDefaultPassword(existingDefaultPassword);
        institution.setCreatedAt(existingCreatedAt);
        institution.setCreatedBy(existingCreatedBy);
        institution.setUpdatedAt(LocalDateTime.now());

        testInstitutionRepository.save(institution);
        logger.info("Institution updated: {}", institutionId);

        List<Object> data = new ArrayList<>();
        data.add(TestInstitutionMapper.mapToDTO(institution));

        return ResponseEntity.ok(new RestWithStatusList("SUCCESS", "Institution updated successfully.", data));
    }

    // ─────────────────────────────────────────────────────────────────────────
    // STATUS UPDATE ONLY
    // ─────────────────────────────────────────────────────────────────────────
    @Override
    @Transactional
    public ResponseEntity<RestWithStatusList> updateStatus(Long institutionId, String status) {

        // Valid statuses per sir's rules
        List<String> validStatuses = Arrays.asList(
            "REQUEST", "VERIFIED", "ACTIVE", "INACTIVE", "BLOCKED", "RETIRED"
        );
        if (!validStatuses.contains(status.toUpperCase())) {
            return bad("Invalid status. Allowed: REQUEST, VERIFIED, ACTIVE, INACTIVE, BLOCKED, RETIRED.");
        }

        Optional<TestInstitution> optional = testInstitutionRepository.findByInstitutionId(institutionId);
        if (!optional.isPresent()) {
            return bad("Institution not found with ID: " + institutionId);
        }

        TestInstitution institution = optional.get();
        String currentStatus = institution.getStatus();

        // ── RETIRED is permanent — cannot be changed by anyone ──
        if ("RETIRED".equals(currentStatus)) {
            return bad("This institution is RETIRED. Its status cannot be changed by anyone.");
        }
        
     // ── Transition validation ──
        Map<String, List<String>> allowedTransitions = new HashMap<>();
        allowedTransitions.put("ACTIVE",   Arrays.asList("INACTIVE", "BLOCKED", "RETIRED"));
        allowedTransitions.put("INACTIVE", Arrays.asList("ACTIVE", "BLOCKED", "RETIRED"));  // ← ACTIVE add
        allowedTransitions.put("BLOCKED",  Arrays.asList("ACTIVE", "RETIRED"));              // ← ACTIVE add
        allowedTransitions.put("PENDING",  Arrays.asList("ACTIVE", "INACTIVE", "BLOCKED", "RETIRED"));
        allowedTransitions.put("VERIFIED", Arrays.asList("ACTIVE", "INACTIVE", "BLOCKED", "RETIRED"));

        List<String> allowed = allowedTransitions.getOrDefault(currentStatus, new ArrayList<>());
        if (!allowed.contains(status.toUpperCase())) {
            return bad("Cannot change status from '" + currentStatus 
                + "' to '" + status.toUpperCase() + "'. "
                + "Allowed transitions: " + allowed);
        }

        // ── Cannot set back to RETIRED from code (only forward transition allowed) ──
        // RETIRED can only be set if current status is not already RETIRED
        // (admin can set any → RETIRED but not out of RETIRED)

        institution.setStatus(status.toUpperCase());
        institution.setUpdatedAt(LocalDateTime.now());
        testInstitutionRepository.save(institution);

        logger.info("Institution {} status updated: {} → {}", institutionId, currentStatus, status.toUpperCase());

        return ResponseEntity.ok(new RestWithStatusList("SUCCESS",
                "Institution status updated to '" + status.toUpperCase() + "'.", new ArrayList<>()));
    }

    // ─────────────────────────────────────────────────────────────────────────
    // SOFT DELETE
    // ─────────────────────────────────────────────────────────────────────────
    @Override
    @Transactional
    public ResponseEntity<RestWithStatusList> deleteInstitution(Long institutionId) {
        Optional<TestInstitution> optional = testInstitutionRepository.findByInstitutionId(institutionId);

        if (!optional.isPresent()) {
            return bad("Institution not found with ID: " + institutionId);
        }

        TestInstitution institution = optional.get();
        institution.setStatus("INACTIVE");
        institution.setUpdatedAt(LocalDateTime.now());
        testInstitutionRepository.save(institution);

        logger.info("Institution {} soft-deleted (status → INACTIVE)", institutionId);

        return ResponseEntity.ok(new RestWithStatusList("SUCCESS",
                "Institution deactivated successfully.", new ArrayList<>()));
    }

    // ─────────────────────────────────────────────────────────────────────────
    // LOGO UPLOAD
    // ─────────────────────────────────────────────────────────────────────────
    @Override
    @Transactional
    public ResponseEntity<RestWithStatusList> uploadLogo(Long institutionId, MultipartFile file, String logoUploader) {

        Optional<TestInstitution> optional = testInstitutionRepository.findByInstitutionId(institutionId);
        if (!optional.isPresent()) {
            return bad("Institution not found with ID: " + institutionId);
        }

        if (file == null || file.isEmpty()) {
            return bad("No file provided.");
        }
        if (!ALLOWED_TYPES.contains(file.getContentType())) {
            return bad("Invalid file type. Accepted formats: JPG, TIF.");
        }
        if (file.getSize() > MAX_LOGO_SIZE) {
            return bad("File size exceeds 2 MB limit.");
        }

        try {
            File uploadDir = new File(LOGO_UPLOAD_DIR);
            if (!uploadDir.exists()) {
                uploadDir.mkdirs();
            }

            TestInstitution institution = optional.get();
            String originalFilename = file.getOriginalFilename();
            String extension = originalFilename != null && originalFilename.contains(".")
                    ? originalFilename.substring(originalFilename.lastIndexOf("."))
                    : ".jpg";
            String savedFilename = institution.getInstitutionCode() + "_logo" + extension;

            Path filePath = Paths.get(LOGO_UPLOAD_DIR + savedFilename);
            Files.write(filePath, file.getBytes());

            institution.setLogoPath(filePath.toString());
            institution.setUpdatedAt(LocalDateTime.now());
            institution.setUpdatedBy(logoUploader);
            testInstitutionRepository.save(institution);

            logger.info("Logo uploaded for institution {}: {}", institutionId, filePath);

            List<Object> data = new ArrayList<>();
            data.add(TestInstitutionMapper.mapToDTO(institution));

            return ResponseEntity.ok(new RestWithStatusList("SUCCESS",
                    "Logo uploaded successfully.", data));

        } catch (IOException e) {
            logger.error("Logo upload failed for institution {}: {}", institutionId, e.getMessage());
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(new RestWithStatusList("FAILURE", "Logo upload failed. Please try again.", new ArrayList<>()));
        }
    }

    // ─────────────────────────────────────────────────────────────────────────
    // VERIFY EMAIL
    // ─────────────────────────────────────────────────────────────────────────
    @Override
    @Transactional
    public ResponseEntity<RestWithStatusList> verifyEmail(String token) {
        Optional<TestInstitution> optional = 
            testInstitutionRepository.findByVerificationToken(token);

        // Token nahi mila DB mein
        if (!optional.isPresent()) {
            return bad("Invalid or expired verification link.");
        }

        TestInstitution institution = optional.get();

        // Token expiry check — 48 hrs baad expire
        if (institution.getTokenExpiry() != null &&
            LocalDateTime.now().isAfter(institution.getTokenExpiry())) {
            return bad("Verification link has expired. Please contact KalInfotech Admin.");
        }

        // ── Already VERIFIED or ACTIVE — link clicked again ──
        // Sir's rule: VERIFIED status is set ONLY when user successfully enters
        // Institution Code + UserID + Default Password (Step 1 of SuperUserLogin)
        // Email link click just validates the token and redirects to login page.
        if ("VERIFIED".equals(institution.getStatus()) ||
            "ACTIVE".equals(institution.getStatus())) {
            return ResponseEntity.ok(new RestWithStatusList(
                "ALREADY_VERIFIED",
                "Link already used. Please proceed to login.",
                new ArrayList<>()
            ));
        }

        // ── Status stays REQUEST — do NOT change to VERIFIED here ──
        // VERIFIED is set in KalSuperServiceImp.setNewPassword() after Step 2 success
        // Token stays valid for 36 hrs so user can come back if Step 1/2 fails
        logger.info("Email link clicked for institution {} — status remains REQUEST, redirecting to login",
                institution.getInstitutionCode());

        return ResponseEntity.ok(new RestWithStatusList(
            "SUCCESS",
            "Link is valid. Please proceed to login with your credentials.",
            new ArrayList<>()
        ));
    }

    // ─────────────────────────────────────────────────────────────────────────
    // PRIVATE HELPERS
    // ─────────────────────────────────────────────────────────────────────────

    // Institution code is generated by frontend (pure 8 digits)
    // Backend only validates format and uniqueness — no generation needed here

    // Super User ID: firstname.lastname all lowercase
    // e.g. "Rajesh Kumar Sharma" → "rajesh.kumar"  (first 2 words only)
    // e.g. "Rajesh Kumar"        → "rajesh.kumar"
    // e.g. "Rajesh"              → "rajesh"
    private String generateSuperUserId(String fullName) {
        if (fullName == null || fullName.trim().isEmpty()) return "user";
        String[] parts = fullName.trim().toLowerCase().split("\\s+");
        if (parts.length == 1) return parts[0];
        return parts[0] + "." + parts[1];
    }

    // Default Password: "Recon@" + 4 random digits
    // e.g. "Recon@4821" — satisfies: 8+ chars, 1 uppercase, 1 special, 1 number
    private String generateDefaultPassword() {
        int digits = 1000 + new Random().nextInt(9000);
        return "Recon@" + digits;
    }

    /** Convenience — 400 Bad Request */
    private ResponseEntity<RestWithStatusList> bad(String message) {
        return ResponseEntity.status(HttpStatus.BAD_REQUEST)
                .body(new RestWithStatusList("FAILURE", message, new ArrayList<>()));
    }
    
 // ─────────────────────────────────────────────────────────────────────────
 // CHECK NAME EXISTS — Step 1 real-time validation
 // ─────────────────────────────────────────────────────────────────────────
 @Override
 public ResponseEntity<RestWithStatusList> checkNameExists(String name) {
     if (name == null || name.trim().isEmpty()) {
         return bad("Institution name is required.");
     }
     boolean exists = testInstitutionRepository
             .existsByInstitutionNameFullIgnoreCase(name.trim());
     if (exists) {
         return ResponseEntity.ok(
                 new RestWithStatusList("EXISTS",
                         "Institution '" + name.trim() + "' is already registered.",
                         new ArrayList<>()));
     }
     return ResponseEntity.ok(
             new RestWithStatusList("AVAILABLE",
                     "Institution name is available.",
                     new ArrayList<>()));
 }

    // ─────────────────────────────────────────────────────────────────────────
    // CHECK NAME EXISTS — Step 1 real-time validation
    // ─────────────────────────────────────────────────────────────────────────
    @Override
    public ResponseEntity<RestWithStatusList> checkEmailExists(String email) {
        if (email == null || email.trim().isEmpty()) {
            return bad("Email is required.");
        }
        boolean exists = testInstitutionRepository.existsByPrimaryEmail(email.trim());
        if (exists) {
            return ResponseEntity.ok(
                    new RestWithStatusList("EXISTS",
                            "Email '" + email.trim() + "' is already registered.",
                            new ArrayList<>()));
        }
        return ResponseEntity.ok(
                new RestWithStatusList("AVAILABLE",
                        "Email is available.",
                        new ArrayList<>()));
    }

    // ─────────────────────────────────────────────────────────────────────────
    // EXPORT TO EXCEL
    // ─────────────────────────────────────────────────────────────────────────
    @Override
    public ResponseEntity<byte[]> exportToExcel() throws java.io.IOException {

        List<TestInstitution> institutions = testInstitutionRepository.findAll();
        DateTimeFormatter fmt = DateTimeFormatter.ofPattern("dd-MM-yyyy HH:mm:ss");

        try (XSSFWorkbook workbook = new XSSFWorkbook();
             ByteArrayOutputStream out = new ByteArrayOutputStream()) {

            Sheet sheet = workbook.createSheet("Institutions");

            CellStyle headerStyle = workbook.createCellStyle();
            Font headerFont = workbook.createFont();
            headerFont.setBold(true);
            headerFont.setColor(IndexedColors.WHITE.getIndex());
            headerStyle.setFont(headerFont);
            headerStyle.setFillForegroundColor(IndexedColors.DARK_BLUE.getIndex());
            headerStyle.setFillPattern(FillPatternType.SOLID_FOREGROUND);
            headerStyle.setAlignment(HorizontalAlignment.CENTER);

            CellStyle dataStyle = workbook.createCellStyle();
            dataStyle.setBorderBottom(BorderStyle.THIN);
            dataStyle.setBorderLeft(BorderStyle.THIN);
            dataStyle.setBorderRight(BorderStyle.THIN);

            CellStyle altStyle = workbook.createCellStyle();
            altStyle.cloneStyleFrom(dataStyle);
            altStyle.setFillForegroundColor(IndexedColors.LIGHT_CORNFLOWER_BLUE.getIndex());
            altStyle.setFillPattern(FillPatternType.SOLID_FOREGROUND);

            String[] headers = {
                "S.No", "Institution Code", "Institution Name (Full)",
                "Institution Name (Short)", "Bank Type", "Super User ID",
                "Primary Email", "Primary Mobile", "Status", "Created At"
            };

            Row headerRow = sheet.createRow(0);
            for (int i = 0; i < headers.length; i++) {
                Cell cell = headerRow.createCell(i);
                cell.setCellValue(headers[i]);
                cell.setCellStyle(headerStyle);
            }

            int rowNum = 1;
            for (TestInstitution inst : institutions) {
                Row row = sheet.createRow(rowNum);
                CellStyle style = (rowNum % 2 == 0) ? altStyle : dataStyle;
                setCell(row, 0, String.valueOf(rowNum), style);
                setCell(row, 1, inst.getInstitutionCode(), style);
                setCell(row, 2, inst.getInstitutionNameFull(), style);
                setCell(row, 3, inst.getInstitutionNameShort(), style);
                setCell(row, 4, inst.getBankType() != null ? inst.getBankType() : "", style);
                setCell(row, 5, inst.getSuperUserId(), style);
                setCell(row, 6, inst.getPrimaryEmail(), style);
                setCell(row, 7, inst.getPrimaryMobile(), style);
                setCell(row, 8, inst.getStatus(), style);
                setCell(row, 9, inst.getCreatedAt() != null
                        ? inst.getCreatedAt().format(fmt) : "", style);
                rowNum++;
            }

            for (int i = 0; i < headers.length; i++) {
                sheet.autoSizeColumn(i);
            }

            workbook.write(out);
            byte[] data = out.toByteArray();

            String filename = "Institutions_" +
                    LocalDateTime.now().format(DateTimeFormatter.ofPattern("yyyyMMdd_HHmmss")) + ".xlsx";

            return ResponseEntity.ok()
                    .header(HttpHeaders.CONTENT_DISPOSITION,
                            "attachment; filename=\"" + filename + "\"")
                    .contentType(MediaType.parseMediaType(
                            "application/vnd.openxmlformats-officedocument.spreadsheetml.sheet"))
                    .contentLength(data.length)
                    .body(data);
        }
    }

    // ─────────────────────────────────────────────────────────────────────────
    // EXPORT TO CSV
    // ─────────────────────────────────────────────────────────────────────────
    @Override
    public ResponseEntity<byte[]> exportToCsv() {

        List<TestInstitution> institutions = testInstitutionRepository.findAll();
        DateTimeFormatter fmt = DateTimeFormatter.ofPattern("dd-MM-yyyy HH:mm:ss");

        StringBuilder csv = new StringBuilder();
        csv.append("S.No,Institution Code,Institution Name (Full),Institution Name (Short),")
           .append("Bank Type,Super User ID,Primary Email,Primary Mobile,Status,Created At\n");

        int sno = 1;
        for (TestInstitution inst : institutions) {
            csv.append(sno++).append(",")
               .append(safeCsv(inst.getInstitutionCode())).append(",")
               .append(safeCsv(inst.getInstitutionNameFull())).append(",")
               .append(safeCsv(inst.getInstitutionNameShort())).append(",")
               .append(safeCsv(inst.getBankType())).append(",")
               .append(safeCsv(inst.getSuperUserId())).append(",")
               .append(safeCsv(inst.getPrimaryEmail())).append(",")
               .append(safeCsv(inst.getPrimaryMobile())).append(",")
               .append(safeCsv(inst.getStatus())).append(",")
               .append(safeCsv(inst.getCreatedAt() != null
                       ? inst.getCreatedAt().format(fmt) : ""))
               .append("\n");
        }

        byte[] data = csv.toString().getBytes();
        String filename = "Institutions_" +
                LocalDateTime.now().format(DateTimeFormatter.ofPattern("yyyyMMdd_HHmmss")) + ".csv";

        return ResponseEntity.ok()
                .header(HttpHeaders.CONTENT_DISPOSITION,
                        "attachment; filename=\"" + filename + "\"")
                .contentType(MediaType.parseMediaType("text/csv"))
                .contentLength(data.length)
                .body(data);
    }

    private void setCell(Row row, int col, String value, CellStyle style) {
        Cell cell = row.createCell(col);
        cell.setCellValue(value != null ? value : "");
        cell.setCellStyle(style);
    }

    private String safeCsv(String val) {
        if (val == null) return "";
        if (val.contains(",")) return "\"" + val + "\"";
        return val;
    }
}