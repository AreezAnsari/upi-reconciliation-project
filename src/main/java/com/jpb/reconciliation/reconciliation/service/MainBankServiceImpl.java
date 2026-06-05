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
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.multipart.MultipartFile;

import org.apache.poi.ss.usermodel.*;
import org.apache.poi.xssf.usermodel.XSSFWorkbook;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import com.jpb.reconciliation.reconciliation.dto.RestWithStatusList;
import com.jpb.reconciliation.reconciliation.dto.MainBankDTO;
import com.jpb.reconciliation.reconciliation.dto.MainBankDTO.ProductDateEntry;
import com.jpb.reconciliation.reconciliation.entity.BranchBank;
import com.jpb.reconciliation.reconciliation.entity.MainBank;
import com.jpb.reconciliation.reconciliation.entity.MainBankProduct;
import com.jpb.reconciliation.reconciliation.mapper.MainBankMapper;
import com.jpb.reconciliation.reconciliation.repository.BranchBankRepository;
import com.jpb.reconciliation.reconciliation.repository.MainBankProductRepository;
import com.jpb.reconciliation.reconciliation.repository.MainAdminRepository;
import com.jpb.reconciliation.reconciliation.repository.MainBankRepository;

@Service
public class MainBankServiceImpl implements MainBankService {

    private static final Logger logger = LoggerFactory.getLogger(MainBankServiceImpl.class);

    private static final String LOGO_UPLOAD_DIR = "/home/ec2-user/institution_logos/";

    private static final List<String> ALLOWED_TYPES = Arrays.asList(
            "image/jpeg", "image/jpg", "image/tiff", "image/tif"
    );
    private static final long MAX_LOGO_SIZE = 2 * 1024 * 1024; // 2 MB

    @Autowired
    private MainBankRepository mainBankRepository;

    @Autowired
    private BranchBankRepository branchBankRepository;

    @Autowired
    private MainBankProductRepository mainBankProductRepository;

    @Autowired
    private EmailService emailService;

    @Autowired
    private MainAdminRepository mainAdminRepository;

    @Autowired
    private PasswordEncoder passwordEncoder;

    @Value("${app.frontend.url:http://localhost:5173}")
    private String frontendUrl;

    // ─────────────────────────────────────────────────────────────────────────
    // CREATE
    // ─────────────────────────────────────────────────────────────────────────
    @Override
    @Transactional
    public ResponseEntity<RestWithStatusList> createInstitution(MainBankDTO dto, String createdBy) {

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


        if (mainBankRepository.existsByPrimaryEmail(dto.getPrimaryEmail().trim())) {
            return bad("An institution with email '" + dto.getPrimaryEmail() + "' is already registered.");
        }

        // ── Institution code: frontend sends backend-generated code (via /generate-code API) ──
        // Validate format + uniqueness as final safety check
        String institutionCode = dto.getInstitutionCode();

        if (institutionCode == null || !institutionCode.matches("\\d{8}")) {
            institutionCode = generateInstitutionCode(); // fallback: generate if missing
        }

        if (mainBankRepository.findByInstitutionCode(institutionCode).isPresent()) {
            institutionCode = generateInstitutionCode(); // duplicate? generate fresh
        }
        logger.info("Institution code: {}", institutionCode);

        // Generate Super User ID — unique within this institution
        String superUserId = generateSuperUserId(dto.getPrimaryFullName(), institutionCode);

        // Generate Default Password
        String defaultPassword = generateDefaultPassword();

        // DTO → Entity
        MainBank institution =
                MainBankMapper.mapToEntity(dto, new MainBank());

        institution.setInstitutionCode(institutionCode);
        institution.setStatus("REQUEST");
        institution.setCreatedAt(LocalDateTime.now());
        institution.setCreatedBy(createdBy);  // logged-in admin username from JWT

        // Super User Credentials
        institution.setSuperUserId(superUserId);
        institution.setDefaultPassword(passwordEncoder.encode(defaultPassword)); // BCrypt stored

        // Verification Token
        String token = UUID.randomUUID().toString();

        institution.setVerificationToken(token);
        institution.setTokenExpiry(LocalDateTime.now().plusHours(48));

        // Save Institution
        mainBankRepository.save(institution);

        // =========================================================
        // SEND WELCOME EMAIL
        // KAL_SUPER_USER insert happens only after Super User sets new password
        // =========================================================

        String verifyLink =
                frontendUrl
                + "/verify-email?institutionCode="
                + institutionCode
                + "&username="
                + superUserId;

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

            logger.info(
                    "Welcome email dispatched to: {} | userId: {} | institution: {}",
                    dto.getPrimaryEmail(),
                    superUserId,
                    dto.getInstitutionNameFull()
            );

        } catch (Exception e) {

            logger.warn(
                    "Institution saved but welcome email failed for {}: {}",
                    dto.getPrimaryEmail(),
                    e.getMessage()
            );
        }

        logger.info(
                "Institution created: {} | Code: {} | SuperUserId: {}",
                dto.getInstitutionNameFull(),
                institutionCode,
                superUserId
        );
        saveProductDates(institution.getInstitutionId(), dto, createdBy);

        List<Object> data = new ArrayList<>();

        // defaultPassword is sent to Super User via email — admin response should not expose it
        MainBankDTO responseDto = MainBankMapper.mapToDTO(institution);
        responseDto.setDefaultPassword("--");
        data.add(responseDto);

        return ResponseEntity
                .status(HttpStatus.CREATED)
                .body(
                        new RestWithStatusList(
                                "SUCCESS",
                                "Institution '" +
                                        dto.getInstitutionNameFull() +
                                        "' onboarded successfully.",
                                data
                        )
                );
    }    // ─────────────────────────────────────────────────────────────────────────
    // GET ALL
    // ─────────────────────────────────────────────────────────────────────────
    @Override
    @Transactional(readOnly = true)
    public ResponseEntity<RestWithStatusList> getAllInstitutions() {
        List<MainBank> list = mainBankRepository.findAll();

        if (list.isEmpty()) {
            return ResponseEntity.ok(new RestWithStatusList("SUCCESS", "No institutions found.", new ArrayList<>()));
        }

        List<Object> data = list.stream()
                .map(MainBankMapper::mapToDTO)
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
        Optional<MainBank> optional = mainBankRepository.findByInstitutionId(institutionId);

        if (!optional.isPresent()) {
            logger.warn("Institution not found: {}", institutionId);
            return bad("Institution not found with ID: " + institutionId);
        }

        MainBankDTO dto = MainBankMapper.mapToDTO(optional.get());

        // Populate productDates from TEST_INST_PRODUCT table
        List<MainBankProduct> products = mainBankProductRepository.findByInstitutionId(institutionId);
        if (!products.isEmpty()) {
            Map<String, ProductDateEntry> productDates = new java.util.LinkedHashMap<>();
            for (MainBankProduct p : products) {
                ProductDateEntry entry = new ProductDateEntry();
                entry.setValidFrom(p.getValidFrom());
                entry.setValidTo(p.getValidTo());
                productDates.put(p.getProductName(), entry);
            }
            dto.setProductDates(productDates);
        }

        List<Object> data = new ArrayList<>();
        data.add(dto);

        return ResponseEntity.ok(new RestWithStatusList("SUCCESS", "Institution fetched successfully.", data));
    }

    // ─────────────────────────────────────────────────────────────────────────
    // GET BY STATUS
    // ─────────────────────────────────────────────────────────────────────────
    @Override
    @Transactional(readOnly = true)
    public ResponseEntity<RestWithStatusList> getInstitutionsByStatus(String status) {
        List<MainBank> list = mainBankRepository.findByStatus(status.toUpperCase());

        List<Object> data = list.stream()
                .map(MainBankMapper::mapToDTO)
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
    public ResponseEntity<RestWithStatusList> updateInstitution(Long institutionId, MainBankDTO dto) {
        Optional<MainBank> optional = mainBankRepository.findByInstitutionId(institutionId);

        if (!optional.isPresent()) {
            return bad("Institution not found with ID: " + institutionId);
        }

        MainBank institution = optional.get();

        // Preserve system-generated fields
        String existingCode             = institution.getInstitutionCode();
        String existingStatus           = institution.getStatus();
        String existingLogo             = institution.getLogoPath();
        String existingSuperUserId      = institution.getSuperUserId();
        String existingDefaultPassword  = institution.getDefaultPassword();
        LocalDateTime existingCreatedAt = institution.getCreatedAt();
        String existingCreatedBy        = institution.getCreatedBy();

        MainBankMapper.mapToEntity(dto, institution);

        // Restore protected fields
        institution.setInstitutionCode(existingCode);
        institution.setStatus(existingStatus);
        institution.setLogoPath(existingLogo);
        institution.setSuperUserId(existingSuperUserId);
        institution.setDefaultPassword(existingDefaultPassword);
        institution.setCreatedAt(existingCreatedAt);
        institution.setCreatedBy(existingCreatedBy);
        institution.setUpdatedAt(LocalDateTime.now());

        mainBankRepository.save(institution);

        // Sync email to KAL_SUPER_USER if primary_email changed
        try {
            mainAdminRepository.findByInstitutionCodeAndUsername(
                    institution.getInstitutionCode(), institution.getSuperUserId())
                .ifPresent(superUser -> {
                    superUser.setEmail(institution.getPrimaryEmail());
                    superUser.setUpdatedAt(LocalDateTime.now());
                    superUser.setUpdatedBy(getCurrentUsername());
                    mainAdminRepository.save(superUser);
                    logger.info("KAL_SUPER_USER email synced for institution: {}", institution.getInstitutionCode());
                });
        } catch (Exception e) {
            logger.warn("updateInstitution: KAL_SUPER_USER email sync failed for {}: {}",
                    institution.getInstitutionCode(), e.getMessage());
        }

        saveProductDates(institutionId, dto, institution.getCreatedBy());
        logger.info("Institution updated: {}", institutionId);

        List<Object> data = new ArrayList<>();
        data.add(MainBankMapper.mapToDTO(institution));

        return ResponseEntity.ok(new RestWithStatusList("SUCCESS", "Institution updated successfully.", data));
    }

    // ─────────────────────────────────────────────────────────────────────────
    // STATUS UPDATE ONLY
    // ─────────────────────────────────────────────────────────────────────────
    @Override
    @Transactional
    public ResponseEntity<RestWithStatusList> updateStatus(Long institutionId, String status) {

        // Valid statuses
        List<String> validStatuses = Arrays.asList(
            "REQUEST", "VERIFIED", "ACTIVE", "INACTIVE", "BLOCKED", "BLOCK_PENDING"
        );
        if (!validStatuses.contains(status.toUpperCase())) {
            return bad("Invalid status. Allowed: REQUEST, VERIFIED, ACTIVE, INACTIVE, BLOCKED, BLOCK_PENDING.");
        }

        Optional<MainBank> optional = mainBankRepository.findByInstitutionId(institutionId);
        if (!optional.isPresent()) {
            return bad("Institution not found with ID: " + institutionId);
        }

        MainBank institution = optional.get();
        String currentStatus = institution.getStatus();

        // ── BLOCKED is permanent — cannot be changed by anyone ──
        if ("BLOCKED".equals(currentStatus)) {
            return bad("This institution is permanently BLOCKED. Its status cannot be changed.");
        }

        // ── Transition validation ──
        Map<String, List<String>> allowedTransitions = new HashMap<>();
        allowedTransitions.put("ACTIVE",        Arrays.asList("INACTIVE", "BLOCK_PENDING"));
        allowedTransitions.put("INACTIVE",       Arrays.asList("ACTIVE",   "BLOCK_PENDING"));
        allowedTransitions.put("BLOCK_PENDING",  Arrays.asList("ACTIVE",   "INACTIVE",  "BLOCKED")); // BLOCKED = auto-block after countdown
        allowedTransitions.put("PENDING",        Arrays.asList("ACTIVE",   "INACTIVE", "BLOCK_PENDING"));
        allowedTransitions.put("VERIFIED",       Arrays.asList("ACTIVE",   "INACTIVE", "BLOCK_PENDING"));

        List<String> allowed = allowedTransitions.getOrDefault(currentStatus, new ArrayList<>());
        if (!allowed.contains(status.toUpperCase())) {
            return bad("Cannot change status from '" + currentStatus
                + "' to '" + status.toUpperCase() + "'. "
                + "Allowed transitions: " + allowed);
        }

        String upperStatus = status.toUpperCase();

        // ── ACTIVE restriction: cannot go ACTIVE within 30s of becoming INACTIVE ──
        // DEMO: 30s — change to plusMinutes(30) for production
        if ("ACTIVE".equals(upperStatus) && "INACTIVE".equals(currentStatus)) {
            LocalDateTime inactivatedAt = institution.getInactivatedAt();
            if (inactivatedAt != null) {
                LocalDateTime allowedAfter = inactivatedAt.plusSeconds(30); // DEMO: 30s — change to plusMinutes(30) for production
                if (LocalDateTime.now().isBefore(allowedAfter)) {
                    long secsLeft = java.time.Duration.between(LocalDateTime.now(), allowedAfter).getSeconds();
                    logger.warn("[ACTIVE-BLOCK] Institution {} — only {}s since inactivation (need 30s)", institutionId, secsLeft);
                    return bad("Cannot mark Active yet. Institution was recently made Inactive. Please wait " + secsLeft + " more second(s).");
                    // PRODUCTION msg: return bad("Please wait " + (secsLeft / 60 + 1) + " more minute(s) before re-activating.");
                }
            }
        }

        // ── Set inactivatedAt when going INACTIVE ──
        if ("INACTIVE".equals(upperStatus)) {
            institution.setInactivatedAt(LocalDateTime.now());
        }

        institution.setStatus(upperStatus);
        institution.setUpdatedAt(LocalDateTime.now());
        mainBankRepository.save(institution);

        // Sync status to KAL_SUPER_USER (record exists only after Super User sets password)
        String updatedByUser = getCurrentUsername();
        try {
            mainAdminRepository.findByInstitutionCodeAndUsername(
                    institution.getInstitutionCode(), institution.getSuperUserId())
                .ifPresent(superUser -> {
                    superUser.setStatus(upperStatus);
                    superUser.setUpdatedAt(LocalDateTime.now());
                    superUser.setUpdatedBy(updatedByUser);
                    mainAdminRepository.save(superUser);
                    logger.info("KAL_SUPER_USER status synced: {} → {}", institution.getInstitutionCode(), upperStatus);
                });
        } catch (Exception e) {
            logger.warn("TEST_INSTITUTION status updated but KAL_SUPER_USER sync failed for {}: {}",
                    institution.getInstitutionCode(), e.getMessage());
        }

        logger.info("Institution {} status updated: {} → {}", institutionId, currentStatus, upperStatus);

        // ── Send status change notification email to Super User ──
        try {
            if (institution.getPrimaryEmail() != null && !institution.getPrimaryEmail().isEmpty()) {
                emailService.sendStatusChangeNotification(
                    institution.getPrimaryEmail(),
                    institution.getPrimaryFullName() != null ? institution.getPrimaryFullName() : "Super User",
                    institution.getInstitutionNameFull(),
                    institution.getInstitutionCode(),
                    currentStatus,
                    upperStatus
                );
            }
        } catch (Exception e) {
            logger.warn("Status updated but notification email failed for institution {}: {}",
                        institution.getInstitutionCode(), e.getMessage());
        }

        // Individual block only — no cascade to sub-institutes
        return ResponseEntity.ok(new RestWithStatusList("SUCCESS",
                "Institution status updated to '" + upperStatus + "'.", new ArrayList<>()));
    }

    // ─────────────────────────────────────────────────────────────────────────
    // SOFT DELETE
    // ─────────────────────────────────────────────────────────────────────────
    @Override
    @Transactional
    public ResponseEntity<RestWithStatusList> deleteInstitution(Long institutionId) {
        Optional<MainBank> optional = mainBankRepository.findByInstitutionId(institutionId);

        if (!optional.isPresent()) {
            return bad("Institution not found with ID: " + institutionId);
        }

        MainBank institution = optional.get();
        institution.setStatus("INACTIVE");
        institution.setUpdatedAt(LocalDateTime.now());
        mainBankRepository.save(institution);

        // Sync INACTIVE to KAL_SUPER_USER
        String deletedByUser = getCurrentUsername();
        try {
            mainAdminRepository.findByInstitutionCodeAndUsername(
                    institution.getInstitutionCode(), institution.getSuperUserId())
                .ifPresent(superUser -> {
                    superUser.setStatus("INACTIVE");
                    superUser.setUpdatedAt(LocalDateTime.now());
                    superUser.setUpdatedBy(deletedByUser);
                    mainAdminRepository.save(superUser);
                });
        } catch (Exception e) {
            logger.warn("deleteInstitution: KAL_SUPER_USER sync failed for {}: {}",
                    institution.getInstitutionCode(), e.getMessage());
        }

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

        Optional<MainBank> optional = mainBankRepository.findByInstitutionId(institutionId);
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

            MainBank institution = optional.get();
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
            mainBankRepository.save(institution);

            logger.info("Logo uploaded for institution {}: {}", institutionId, filePath);

            List<Object> data = new ArrayList<>();
            data.add(MainBankMapper.mapToDTO(institution));

            return ResponseEntity.ok(new RestWithStatusList("SUCCESS",
                    "Logo uploaded successfully.", data));

        } catch (IOException e) {
            logger.error("Logo upload failed for institution {}: {}", institutionId, e.getMessage());
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(new RestWithStatusList("FAILURE", "Logo upload failed. Please try again.", new ArrayList<>()));
        }
    }

    // ─────────────────────────────────────────────────────────────────────────
    // SERVE LOGO IMAGE
    // ─────────────────────────────────────────────────────────────────────────
    @Override
    public ResponseEntity<byte[]> getLogoImage(String institutionCode) {
        Optional<MainBank> optional = mainBankRepository.findByInstitutionCode(institutionCode);
        if (!optional.isPresent()) {
            return ResponseEntity.notFound().build();
        }
        String logoPath = optional.get().getLogoPath();
        if (logoPath == null || logoPath.trim().isEmpty()) {
            return ResponseEntity.notFound().build();
        }
        try {
            // Strip surrounding quotes if path was stored with them
            String cleanLogoPath = logoPath.trim();
            cleanLogoPath = cleanLogoPath.replaceAll("^\"|\"$", "");
            if (cleanLogoPath.startsWith("'") && cleanLogoPath.endsWith("'")) {
                cleanLogoPath = cleanLogoPath.substring(1, cleanLogoPath.length() - 1);
            }
            Path path = Paths.get(cleanLogoPath);
            if (!Files.exists(path)) {
                logger.warn("Logo file not found on disk for institution {}: {}", institutionCode, cleanLogoPath);
                return ResponseEntity.notFound().build();
            }
            byte[] imageBytes = Files.readAllBytes(path);
            String contentType = Files.probeContentType(path);
            if (contentType == null) contentType = "image/jpeg";
            return ResponseEntity.ok()
                    .header(HttpHeaders.CONTENT_TYPE, contentType)
                    .header(HttpHeaders.CACHE_CONTROL, "max-age=3600")
                    .body(imageBytes);

        } catch (IOException e) {
            logger.error("Failed to serve logo for institution {}: {}", institutionCode, e.getMessage());
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).build();
        }
    }

    // ─────────────────────────────────────────────────────────────────────────
    // GET BY CODE — used by SuperUser sidebar to fetch bank logo + short name
    // ─────────────────────────────────────────────────────────────────────────
    @Override
    @Transactional(readOnly = true)
    public ResponseEntity<RestWithStatusList> getInstitutionByCode(String institutionCode) {
        try {
            Optional<MainBank> optional = mainBankRepository.findByInstitutionCode(institutionCode);
            if (!optional.isPresent()) {
                return bad("Institution not found with code: " + institutionCode);
            }
            List<Object> data = new ArrayList<>();
            data.add(MainBankMapper.mapToDTO(optional.get()));
            return ResponseEntity.ok(new RestWithStatusList("SUCCESS", "Institution fetched.", data));
        } catch (Exception e) {
            logger.error("Error fetching institution by code {}: {}", institutionCode, e.getMessage());
            return bad("Failed to fetch institution: " + e.getMessage());
        }
    }

    // ─────────────────────────────────────────────────────────────────────────
    // VERIFY EMAIL
    // ─────────────────────────────────────────────────────────────────────────
    @Override
    @Transactional
    public ResponseEntity<RestWithStatusList> verifyEmail(String token) {
        Optional<MainBank> optional =
            mainBankRepository.findByVerificationToken(token);

        // Token nahi mila DB mein
        if (!optional.isPresent()) {
            return bad("Invalid or expired verification link.");
        }

        MainBank institution = optional.get();

        // Token expiry check — 48 hrs baad expire
        if (institution.getTokenExpiry() != null &&
            LocalDateTime.now().isAfter(institution.getTokenExpiry())) {
            return bad("Verification link has expired. Please contact KalInfotech Admin.");
        }

        // ── Already VERIFIED or ACTIVE — link clicked again ──
        if ("VERIFIED".equals(institution.getStatus()) ||
            "ACTIVE".equals(institution.getStatus())) {
            return ResponseEntity.ok(new RestWithStatusList(
                "ALREADY_VERIFIED",
                "Link already used. Please proceed to login.",
                new ArrayList<>()
            ));
        }

        // ── Status stays REQUEST — do NOT change to VERIFIED here ──
        logger.info("Email link clicked for institution {} — status remains REQUEST, redirecting to login",
                institution.getInstitutionCode());

        return ResponseEntity.ok(new RestWithStatusList(
            "SUCCESS",
            "Link is valid. Please proceed to login with your credentials.",
            new ArrayList<>()
        ));
    }

    // ─────────────────────────────────────────────────────────────────────────
    // GENERATE CODE (public — called by controller for preview on form open)
    // ─────────────────────────────────────────────────────────────────────────
    @Override
    public ResponseEntity<RestWithStatusList> generateCode() {
        String code = generateInstitutionCode();
        logger.info("Pre-generated institution code: {}", code);
        List<Object> data = new ArrayList<>();
        data.add(code);
        return ResponseEntity.ok(new RestWithStatusList("SUCCESS", "Institution code generated.", data));
    }

    // ─────────────────────────────────────────────────────────────────────────
    // PRIVATE HELPERS
    // ─────────────────────────────────────────────────────────────────────────

    // Institution code: Epoch seconds (last 6 digits) + 2 random digits = always 8 digits
    private String generateInstitutionCode() {
        String code;
        do {
            long epochPart = (System.currentTimeMillis() / 1000) % 1_000_000L;
            int  randomPart = 10 + new Random().nextInt(90);
            code = String.format("%06d%02d", epochPart, randomPart);
        } while (mainBankRepository.findByInstitutionCode(code).isPresent());
        return code;
    }

    // Super User ID: firstname.lastname — unique within the given institution
    private String generateSuperUserId(String fullName, String institutionCode) {
        if (fullName == null || fullName.trim().isEmpty()) return "user";

        String[] parts = fullName.trim().toLowerCase()
                .replaceAll("[^a-z\\s]", "").split("\\s+");

        java.util.List<String> candidates = new java.util.ArrayList<>();

        if (parts.length == 1) {
            candidates.add(parts[0]);
        } else {
            candidates.add(parts[0] + "." + parts[1]);
            if (parts.length >= 3)
                candidates.add(parts[0] + "." + parts[1] + "." + parts[2]);
        }

        for (String candidate : candidates) {
            if (!mainAdminRepository.existsByInstitutionCodeAndUsername(institutionCode, candidate)) {
                return candidate;
            }
        }

        String instWord = institutionCode.replaceAll("[^a-z]", "");
        String base = candidates.get(0);
        String fallback = base + (instWord.isEmpty() ? ".x" : "." + instWord);
        return fallback;
    }

    // Default Password: "Recon@" + 4 random digits
    private String generateDefaultPassword() {
        int digits = 1000 + new Random().nextInt(9000);
        return "Recon@" + digits;
    }

    /**
     * Fire-and-forget email to sub-institute primary contact after cascade status change.
     */
    private void sendSubCascadeEmail(
            BranchBank sub,
            String oldStatus, String newStatus,
            MainBank parent) {
        try {
            if (sub.getPrimaryEmail() == null || sub.getPrimaryEmail().isEmpty()) {
                logger.warn("[CASCADE-EMAIL] No email for sub-institute: {} ({})",
                        sub.getInstitutionId(), sub.getInstitutionCode());
                return;
            }
            emailService.sendSubInstituteStatusNotification(
                    sub.getPrimaryEmail(),
                    sub.getPrimaryFullName() != null ? sub.getPrimaryFullName() : "Contact",
                    sub.getInstitutionNameFull() != null ? sub.getInstitutionNameFull() : sub.getInstitutionCode(),
                    sub.getInstitutionCode(),
                    oldStatus,
                    newStatus,
                    parent.getInstitutionNameFull(),
                    parent.getInstitutionCode()
            );
        } catch (Exception e) {
            logger.warn("[CASCADE-EMAIL] Failed for sub-institute {} ({}): {}",
                    sub.getInstitutionId(), sub.getInstitutionCode(), e.getMessage());
        }
    }

    private String getCurrentUsername() {
        try {
            return org.springframework.security.core.context.SecurityContextHolder
                    .getContext().getAuthentication().getName();
        } catch (Exception e) {
            return "SYSTEM";
        }
    }

    /** Convenience — 400 Bad Request */
    private ResponseEntity<RestWithStatusList> bad(String message) {
        return ResponseEntity.status(HttpStatus.BAD_REQUEST)
                .body(new RestWithStatusList("FAILURE", message, new ArrayList<>()));
    }

    // ─────────────────────────────────────────────────────────────────────────
    // CHECK NAME EXISTS — Step 1 real-time uniqueness validation
    // ─────────────────────────────────────────────────────────────────────────
    @Override
    public ResponseEntity<RestWithStatusList> checkNameExists(String name) {
        if (name == null || name.trim().isEmpty()) {
            return bad("Institution name is required.");
        }
        boolean exists = mainBankRepository.existsByInstitutionNameFull(name.trim());
        if (exists) {
            return ResponseEntity.ok(
                    new RestWithStatusList("EXISTS",
                            "Institution name '" + name.trim() + "' is already registered.",
                            new ArrayList<>()));
        }
        return ResponseEntity.ok(
                new RestWithStatusList("AVAILABLE",
                        "Institution name is available.",
                        new ArrayList<>()));
    }

    // ─────────────────────────────────────────────────────────────────────────
    // CHECK EMAIL EXISTS
    // ─────────────────────────────────────────────────────────────────────────
    @Override
    public ResponseEntity<RestWithStatusList> checkEmailExists(String email) {
        if (email == null || email.trim().isEmpty()) {
            return bad("Email is required.");
        }
        boolean exists = mainBankRepository.existsByPrimaryEmail(email.trim());
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

        List<MainBank> institutions = mainBankRepository.findAll();
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
            for (MainBank inst : institutions) {
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

        List<MainBank> institutions = mainBankRepository.findAll();
        DateTimeFormatter fmt = DateTimeFormatter.ofPattern("dd-MM-yyyy HH:mm:ss");

        StringBuilder csv = new StringBuilder();
        csv.append("S.No,Institution Code,Institution Name (Full),Institution Name (Short),")
           .append("Bank Type,Super User ID,Primary Email,Primary Mobile,Status,Created At\n");

        int sno = 1;
        for (MainBank inst : institutions) {
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

    @Override
    public ResponseEntity<RestWithStatusList> getInstitutionsByCreatedBy(String username) {
        List<MainBank> list = mainBankRepository.findByCreatedBy(username);
        List<MainBankDTO> dtos = list.stream()
            .map(inst -> MainBankMapper.mapToDTO(inst))
            .collect(Collectors.toList());
        return new ResponseEntity<>(
            new RestWithStatusList("SUCCESS", "Institutions fetched.", new ArrayList<>(dtos)),
            HttpStatus.OK);
    }

    // ─────────────────────────────────────────────────────────────────────────
    // SAVE PRODUCT DATES — delete-and-reinsert per institution
    // ─────────────────────────────────────────────────────────────────────────
    private void saveProductDates(Long institutionId, MainBankDTO dto, String createdBy) {
        mainBankProductRepository.deleteByInstitutionId(institutionId);
        if (dto.getProductDates() == null || dto.getProductDates().isEmpty()) return;
        List<MainBankProduct> products = new ArrayList<>();
        dto.getProductDates().forEach((productName, entry) -> {
            MainBankProduct p = new MainBankProduct();
            p.setInstitutionId(institutionId);
            p.setProductName(productName);
            p.setValidFrom(entry.getValidFrom());
            p.setValidTo(entry.getValidTo());
            p.setCreatedBy(createdBy);
            p.setCreatedAt(LocalDateTime.now());
            products.add(p);
        });
        mainBankProductRepository.saveAll(products);
        logger.info("Saved {} product date(s) for institution {}", products.size(), institutionId);
    }

    @Override
    public ResponseEntity<RestWithStatusList> getSubInstitutes(Long parentInstitutionId) {
        List<BranchBank> subs = branchBankRepository.findByParentInstitutionId(parentInstitutionId);
        List<MainBankDTO> dtos = subs.stream().map(sub -> {
            MainBankDTO dto = new MainBankDTO();
            dto.setInstitutionId(sub.getInstitutionId());
            dto.setInstitutionCode(sub.getInstitutionCode());
            dto.setInstitutionNameFull(sub.getInstitutionNameFull());
            dto.setRegCity(sub.getRegCity());
            dto.setRegState(sub.getRegState());
            dto.setRegCountry(sub.getRegCountry());
            dto.setPrimaryFullName(sub.getPrimaryFullName());
            dto.setPrimaryEmail(sub.getPrimaryEmail());
            dto.setPrimaryMobile(sub.getPrimaryMobile());
            dto.setStatus(sub.getStatus());
            return dto;
        }).collect(Collectors.toList());
        return new ResponseEntity<>(
            new RestWithStatusList("SUCCESS", dtos.size() + " sub-institute(s) found.", new ArrayList<>(dtos)),
            HttpStatus.OK);
    }
}
