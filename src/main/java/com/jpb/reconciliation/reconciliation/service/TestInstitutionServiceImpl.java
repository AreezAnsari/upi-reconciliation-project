package com.jpb.reconciliation.reconciliation.service;

import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
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

        if (testInstitutionRepository.existsByInstitutionNameFull(dto.getInstitutionNameFull().trim())) {
            logger.warn("Institution already exists: {}", dto.getInstitutionNameFull());
            return bad("Institution with name '" + dto.getInstitutionNameFull() + "' already exists.");
        }

        // Generate unique institution code
        String institutionCode = generateInstitutionCode(dto.getInstitutionNameFull());
        logger.info("Generated institution code: {}", institutionCode);

        // ── Generate Super User ID — rule: firstname.lastname all lowercase ──
        // e.g. "Rajesh Kumar" → "rajesh.kumar"
        String superUserId = generateSuperUserId(dto.getPrimaryFullName());

        // ── Generate default password — rule: min 8 chars, 1 upper, 1 special, 1 number ──
        // e.g. "Recon@1234" pattern with random suffix
        String defaultPassword = generateDefaultPassword();

        // Map DTO → Entity
        TestInstitution institution = TestInstitutionMapper.mapToEntity(dto, new TestInstitution());
        institution.setInstitutionCode(institutionCode);
        institution.setStatus("PENDING");
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

        List<String> validStatuses = Arrays.asList("ACTIVE", "INACTIVE", "PENDING", "BLOCKED");
        if (!validStatuses.contains(status.toUpperCase())) {
            return bad("Invalid status. Allowed: ACTIVE, INACTIVE, PENDING, BLOCKED.");
        }

        Optional<TestInstitution> optional = testInstitutionRepository.findByInstitutionId(institutionId);
        if (!optional.isPresent()) {
            return bad("Institution not found with ID: " + institutionId);
        }

        TestInstitution institution = optional.get();
        institution.setStatus(status.toUpperCase());
        institution.setUpdatedAt(LocalDateTime.now());
        testInstitutionRepository.save(institution);

        logger.info("Institution {} status updated to: {}", institutionId, status);

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

        // ── Already ACTIVE hai — second time click ──
        if ("ACTIVE".equals(institution.getStatus())) {
            return ResponseEntity.ok(new RestWithStatusList(
                "ALREADY_VERIFIED",
                "Your email is already verified. Please proceed to login.",
                new ArrayList<>()
            ));
        }

        // ── First time — PENDING → ACTIVE ──
        institution.setStatus("ACTIVE");
        // Token DELETE MAT KARO — 48 hrs tak valid rahega
        institution.setUpdatedAt(LocalDateTime.now());
        testInstitutionRepository.save(institution);

        logger.info("Institution {} verified and ACTIVE", institution.getInstitutionCode());

        return ResponseEntity.ok(new RestWithStatusList(
            "SUCCESS",
            "Email verified successfully! Please proceed to login.",
            new ArrayList<>()
        ));
    }

    // ─────────────────────────────────────────────────────────────────────────
    // PRIVATE HELPERS
    // ─────────────────────────────────────────────────────────────────────────

    // Institution code: first 4 letters of name + 4 random digits e.g. AXIS4298
    private String generateInstitutionCode(String institutionNameFull) {
        String cleaned = institutionNameFull.replaceAll("[^a-zA-Z]", "").toUpperCase();
        String prefix  = cleaned.length() >= 4 ? cleaned.substring(0, 4) : cleaned;
        while (prefix.length() < 4) prefix += "_";

        String code;
        do {
            int digits = 1000 + new Random().nextInt(9000);
            code = prefix + digits;
        } while (testInstitutionRepository.findByInstitutionCode(code).isPresent());

        return code;
    }

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
}