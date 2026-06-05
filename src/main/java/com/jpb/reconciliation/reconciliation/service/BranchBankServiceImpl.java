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

import org.springframework.http.HttpHeaders;

import org.apache.poi.ss.usermodel.BorderStyle;
import org.apache.poi.ss.usermodel.Cell;
import org.apache.poi.ss.usermodel.CellStyle;
import org.apache.poi.ss.usermodel.FillPatternType;
import org.apache.poi.ss.usermodel.Font;
import org.apache.poi.ss.usermodel.HorizontalAlignment;
import org.apache.poi.ss.usermodel.IndexedColors;
import org.apache.poi.ss.usermodel.Row;
import org.apache.poi.ss.usermodel.Sheet;
import org.apache.poi.xssf.usermodel.XSSFWorkbook;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.multipart.MultipartFile;

import com.jpb.reconciliation.reconciliation.dto.BranchBankDTO;
import com.jpb.reconciliation.reconciliation.dto.BranchBankDTO.ProductDateEntry;
import com.jpb.reconciliation.reconciliation.dto.RestWithStatusList;
import com.jpb.reconciliation.reconciliation.entity.BranchBank;
import com.jpb.reconciliation.reconciliation.entity.BranchBankProduct;
import com.jpb.reconciliation.reconciliation.entity.MainAdmin;
import com.jpb.reconciliation.reconciliation.entity.MainBank;
import com.jpb.reconciliation.reconciliation.mapper.BranchBankMapper;
import com.jpb.reconciliation.reconciliation.repository.MainAdminRepository;
import com.jpb.reconciliation.reconciliation.repository.BranchBankProductRepository;
import com.jpb.reconciliation.reconciliation.repository.BranchBankRepository;
import com.jpb.reconciliation.reconciliation.repository.BranchAdminRepository;
import com.jpb.reconciliation.reconciliation.repository.MainBankRepository;
import org.springframework.security.crypto.password.PasswordEncoder;

@Service
public class BranchBankServiceImpl implements BranchBankService {

    private static final Logger logger = LoggerFactory.getLogger(BranchBankServiceImpl.class);

    private static final String LOGO_UPLOAD_DIR = "/home/ec2-user/institution_logos/";

    private static final List<String> ALLOWED_TYPES = Arrays.asList(
            "image/jpeg", "image/jpg", "image/tiff", "image/tif"
    );
    private static final long MAX_LOGO_SIZE = 2 * 1024 * 1024; // 2 MB

    @Autowired
    private BranchBankRepository branchBankRepository;

    @Autowired
    private BranchBankProductRepository branchBankProductRepository;

    @Autowired
    private MainAdminRepository mainAdminRepository;

    @Autowired
    private MainBankRepository mainBankRepository;

    @Autowired
    private BranchAdminRepository branchAdminRepository;

    @Autowired
    private PasswordEncoder passwordEncoder;

    @Autowired
    private EmailService emailService;

    @Value("${app.frontend.url:http://localhost:5173}")
    private String frontendUrl;

    // ─────────────────────────────────────────────────────────────────────────
    // CREATE
    // ─────────────────────────────────────────────────────────────────────────
    @Override
    @Transactional
    public ResponseEntity<RestWithStatusList> createInstitution(BranchBankDTO dto , String createdBy) {

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

        if (branchBankRepository.existsByInstitutionNameFull(dto.getInstitutionNameFull().trim())) {
            logger.warn("Institution already exists: {}", dto.getInstitutionNameFull());
            return bad("Institution with name '" + dto.getInstitutionNameFull() + "' already exists.");
        }

        // ── Sub-Institution Code ───────────────────────────────────────────────
        String institutionCode;
        String dtoCode = dto.getInstitutionCode();
        if (dtoCode != null && dtoCode.matches("\\d{8}")
                && !branchBankRepository.existsByInstitutionCode(dtoCode)) {
            institutionCode = dtoCode;
            logger.info("[BranchBankCode] Using frontend pre-generated code: {}", institutionCode);
        } else {
            // Fallback: generate fresh (covers missing / collided DTO code)
            institutionCode = generateBranchBankCode(createdBy);
            if (institutionCode == null) {
                return bad("Failed to generate a unique institution code. Please try again.");
            }
        }
        logger.info("Final branch bank code: {}", institutionCode);

        // ── Generate Super User ID — rule: firstname.lastname all lowercase ──
        String superUserId = generateSuperUserId(dto.getPrimaryFullName());

        // ── Generate default password ──
        String defaultPassword = generateDefaultPassword();

        // Map DTO → Entity
        BranchBank institution = BranchBankMapper.mapToEntity(dto, new BranchBank());
        institution.setInstitutionCode(institutionCode);
        institution.setStatus("PENDING");
        institution.setCreatedAt(LocalDateTime.now());

        // Save Super User credentials in institution record (BCrypt stored, plaintext in email)
        institution.setSuperUserId(superUserId);
        institution.setDefaultPassword(passwordEncoder.encode(defaultPassword));
        institution.setCreatedBy(createdBy);

        // Generate verification token — valid for 48 hours
        String token = UUID.randomUUID().toString();
        institution.setVerificationToken(token);
        institution.setTokenExpiry(LocalDateTime.now().plusHours(48));

        branchBankRepository.save(institution);
        logger.info("Branch bank created: {} | Code: {} | SuperUserId: {}",
                    dto.getInstitutionNameFull(), institutionCode, superUserId);

        // ── Save product validity dates (delete-and-reinsert — same pattern as admin) ──
        try {
            saveProductDates(institution.getInstitutionId(), dto, createdBy);
        } catch (Exception e) {
            logger.warn("Product dates save failed for {}: {}", institutionCode, e.getMessage());
        }

        // ── Send welcome email with Institution Code, User ID, Default Password ──
        // Branch Admin verify link — separate from Super User flow
        String verifyLink = frontendUrl + "/branch-verify-email?institutionCode="
                + institutionCode + "&username=" + superUserId;
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
            logger.warn("BranchBank saved but welcome email failed for {}: {}",
                        dto.getPrimaryEmail(), e.getMessage());
        }

        List<Object> data = new ArrayList<>();
        BranchBankDTO responseDto = BranchBankMapper.mapToDTO(institution);
        responseDto.setDefaultPassword("--"); // admin should not see the password — sent via email
        data.add(responseDto);

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
        List<BranchBank> list = branchBankRepository.findAll();

        if (list.isEmpty()) {
            return ResponseEntity.ok(new RestWithStatusList("SUCCESS", "No institutions found.", new ArrayList<>()));
        }

        List<Object> data = list.stream()
                .map(BranchBankMapper::mapToDTO)
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
        Optional<BranchBank> optional = branchBankRepository.findByInstitutionId(institutionId);

        if (!optional.isPresent()) {
            logger.warn("Institution not found: {}", institutionId);
            return bad("Institution not found with ID: " + institutionId);
        }

        BranchBankDTO dto = BranchBankMapper.mapToDTO(optional.get());

        // ── Load product validity dates (same pattern as admin getInstitutionById) ──
        try {
            java.util.List<BranchBankProduct> products =
                    branchBankProductRepository.findByInstitutionId(institutionId);
            if (!products.isEmpty()) {
                java.util.Map<String, ProductDateEntry> productDates = new java.util.LinkedHashMap<>();
                for (BranchBankProduct p : products) {
                    ProductDateEntry entry = new ProductDateEntry();
                    entry.setValidFrom(p.getValidFrom());
                    entry.setValidTo(p.getValidTo());
                    productDates.put(p.getProductName(), entry);
                }
                dto.setProductDates(productDates);
            }
        } catch (Exception e) {
            logger.warn("Could not load product dates for institution {}: {}", institutionId, e.getMessage());
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
        List<BranchBank> list = branchBankRepository.findByStatus(status.toUpperCase());

        List<Object> data = list.stream()
                .map(BranchBankMapper::mapToDTO)
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
    public ResponseEntity<RestWithStatusList> updateInstitution(Long institutionId, BranchBankDTO dto) {
        Optional<BranchBank> optional = branchBankRepository.findByInstitutionId(institutionId);

        if (!optional.isPresent()) {
            return bad("Institution not found with ID: " + institutionId);
        }

        BranchBank institution = optional.get();

        // Preserve system-generated fields
        String existingCode             = institution.getInstitutionCode();
        String existingStatus           = institution.getStatus();
        String existingLogo             = institution.getLogoPath();
        String existingSuperUserId      = institution.getSuperUserId();
        String existingDefaultPassword  = institution.getDefaultPassword();
        LocalDateTime existingCreatedAt = institution.getCreatedAt();
        String existingCreatedBy        = institution.getCreatedBy();

        BranchBankMapper.mapToEntity(dto, institution);

        // Restore protected fields
        institution.setInstitutionCode(existingCode);
        institution.setStatus(existingStatus);
        institution.setLogoPath(existingLogo);
        institution.setSuperUserId(existingSuperUserId);
        institution.setDefaultPassword(existingDefaultPassword);
        institution.setCreatedAt(existingCreatedAt);
        institution.setCreatedBy(existingCreatedBy);
        institution.setUpdatedAt(LocalDateTime.now());

        branchBankRepository.save(institution);
        logger.info("Institution updated: {}", institutionId);

        // ── Update product validity dates (delete-and-reinsert — same as admin) ──
        try {
            saveProductDates(institutionId, dto, institution.getCreatedBy());
        } catch (Exception e) {
            logger.warn("Product dates update failed for institution {}: {}", institutionId, e.getMessage());
        }

        List<Object> data = new ArrayList<>();
        data.add(BranchBankMapper.mapToDTO(institution));

        return ResponseEntity.ok(new RestWithStatusList("SUCCESS", "Institution updated successfully.", data));
    }

    // ─────────────────────────────────────────────────────────────────────────
    // STATUS UPDATE ONLY
    // ─────────────────────────────────────────────────────────────────────────
    @Override
    @Transactional
    public ResponseEntity<RestWithStatusList> updateStatus(Long institutionId, String status) {

        List<String> validStatuses = Arrays.asList("ACTIVE", "INACTIVE", "PENDING", "BLOCKED", "BLOCK_PENDING");
        if (!validStatuses.contains(status.toUpperCase())) {
            return bad("Invalid status. Allowed: ACTIVE, INACTIVE, PENDING, BLOCKED, BLOCK_PENDING.");
        }

        Optional<BranchBank> optional = branchBankRepository.findById(institutionId);
        if (!optional.isPresent()) {
            return bad("Branch bank not found with ID: " + institutionId);
        }

        BranchBank institution = optional.get();
        String currentStatus = institution.getStatus();
        String newStatus = status.toUpperCase();

        // ── BLOCKED is permanent ──
        if ("BLOCKED".equals(currentStatus)) {
            return bad("This branch bank is permanently BLOCKED. Its status cannot be changed.");
        }

        // ── Valid transitions (same rules as MainBank) ──
        Map<String, List<String>> allowedTransitions = new HashMap<>();
        allowedTransitions.put("ACTIVE",        Arrays.asList("INACTIVE", "BLOCK_PENDING"));
        allowedTransitions.put("INACTIVE",       Arrays.asList("ACTIVE",   "BLOCK_PENDING"));
        allowedTransitions.put("BLOCK_PENDING",  Arrays.asList("ACTIVE",   "INACTIVE"));
        allowedTransitions.put("PENDING",        Arrays.asList("ACTIVE",   "INACTIVE", "BLOCK_PENDING"));
        allowedTransitions.put("VERIFIED",       Arrays.asList("ACTIVE",   "INACTIVE", "BLOCK_PENDING"));

        List<String> allowed = allowedTransitions.getOrDefault(currentStatus, new ArrayList<>());
        if (!allowed.contains(newStatus)) {
            return bad("Cannot change status from '" + currentStatus + "' to '" + newStatus
                    + "'. Allowed transitions: " + allowed);
        }

        // ── INACTIVE → ACTIVE: 30s cooldown (DEMO — change to 30 mins in production) ──
        if ("ACTIVE".equals(newStatus) && "INACTIVE".equals(currentStatus)) {
            LocalDateTime inactivatedAt = institution.getInactivatedAt();
            if (inactivatedAt != null) {
                LocalDateTime allowedAfter = inactivatedAt.plusSeconds(30);
                if (LocalDateTime.now().isBefore(allowedAfter)) {
                    long secsLeft = java.time.Duration.between(LocalDateTime.now(), allowedAfter).getSeconds();
                    logger.warn("[ACTIVE-BLOCK] Branch bank {} — only {}s since inactivation (need 30s)", institutionId, secsLeft);
                    return bad("Cannot mark Active yet. Branch bank was recently made Inactive. Please wait "
                            + secsLeft + " more second(s).");
                }
            }
        }

        // ── Set inactivatedAt when going INACTIVE ──
        if ("INACTIVE".equals(newStatus)) {
            institution.setInactivatedAt(LocalDateTime.now());
        }

        institution.setStatus(newStatus);
        institution.setUpdatedAt(LocalDateTime.now());
        branchBankRepository.save(institution);

        // Sync status to BRANCH_ADMIN
        String updatedByUser = getCurrentUsername();
        try {
            branchAdminRepository.findByInstitutionCodeAndUsername(
                    institution.getInstitutionCode(), institution.getSuperUserId())
                .ifPresent(ba -> {
                    ba.setStatus(newStatus);
                    ba.setUpdatedAt(LocalDateTime.now());
                    ba.setUpdatedBy(updatedByUser);
                    branchAdminRepository.save(ba);
                });
        } catch (Exception e) {
            logger.warn("updateStatus: BRANCH_ADMIN sync failed for {}: {}", institution.getInstitutionCode(), e.getMessage());
        }

        logger.info("Branch bank {} status updated: {} → {}", institutionId, currentStatus, newStatus);

        // ── Send email notification on meaningful status transitions ──
        try {
            if (institution.getPrimaryEmail() != null && !institution.getPrimaryEmail().isEmpty()
                    && !currentStatus.equals(newStatus)
                    && !newStatus.equals("PENDING")
                    && !newStatus.equals("BLOCK_PENDING")) {
                emailService.sendStatusChangeNotification(
                        institution.getPrimaryEmail(),
                        institution.getPrimaryFullName() != null ? institution.getPrimaryFullName() : "Super User",
                        institution.getInstitutionNameFull(),
                        institution.getInstitutionCode(),
                        currentStatus,
                        newStatus
                );
                logger.info("[EMAIL] Status change notification sent to {} for branch bank {}",
                        institution.getPrimaryEmail(), institution.getInstitutionCode());
            }
        } catch (Exception e) {
            logger.warn("[EMAIL] Status change notification failed for branch bank {}: {}",
                    institution.getInstitutionCode(), e.getMessage());
        }

        return ResponseEntity.ok(new RestWithStatusList("SUCCESS",
                "Branch bank status updated to '" + newStatus + "'.", new ArrayList<>()));
    }

    // ─────────────────────────────────────────────────────────────────────────
    // SOFT DELETE
    // ─────────────────────────────────────────────────────────────────────────
    @Override
    @Transactional
    public ResponseEntity<RestWithStatusList> deleteInstitution(Long institutionId) {
        Optional<BranchBank> optional = branchBankRepository.findByInstitutionId(institutionId);

        if (!optional.isPresent()) {
            return bad("Institution not found with ID: " + institutionId);
        }

        BranchBank institution = optional.get();
        institution.setStatus("INACTIVE");
        institution.setUpdatedAt(LocalDateTime.now());
        branchBankRepository.save(institution);

        // Sync INACTIVE to BRANCH_ADMIN
        String deletedByUser = getCurrentUsername();
        try {
            branchAdminRepository.findByInstitutionCodeAndUsername(
                    institution.getInstitutionCode(), institution.getSuperUserId())
                .ifPresent(ba -> {
                    ba.setStatus("INACTIVE");
                    ba.setUpdatedAt(LocalDateTime.now());
                    ba.setUpdatedBy(deletedByUser);
                    branchAdminRepository.save(ba);
                });
        } catch (Exception e) {
            logger.warn("delete: BRANCH_ADMIN sync failed for {}: {}", institution.getInstitutionCode(), e.getMessage());
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

        Optional<BranchBank> optional = branchBankRepository.findByInstitutionId(institutionId);
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

            BranchBank institution = optional.get();
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

            System.out.println("Original File Name: " + originalFilename);
            System.out.println("Saved File Name: " + savedFilename);
            System.out.println("File Path: " + filePath.toString());

            branchBankRepository.save(institution);

            System.out.println("DB Logo Path: " + institution.getLogoPath());

            logger.info("Logo uploaded for institution {}: {}", institutionId, filePath);

            List<Object> data = new ArrayList<>();
            data.add(BranchBankMapper.mapToDTO(institution));

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
        Optional<BranchBank> optional =
            branchBankRepository.findByVerificationToken(token);

        // Token nahi mila DB mein
        if (!optional.isPresent()) {
            return bad("Invalid or expired verification link.");
        }

        BranchBank institution = optional.get();

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
        branchBankRepository.save(institution);

        logger.info("Branch bank {} verified and ACTIVE", institution.getInstitutionCode());

        return ResponseEntity.ok(new RestWithStatusList(
            "SUCCESS",
            "Email verified successfully! Please proceed to login.",
            new ArrayList<>()
        ));
    }

    // ─────────────────────────────────────────────────────────────────────────
    // PRIVATE HELPERS
    // ─────────────────────────────────────────────────────────────────────────


    // ── PUBLIC: called by /generate-code endpoint ─────────────────────────────
    @Override
    public ResponseEntity<RestWithStatusList> generateCode(String createdBy) {
        String code = generateBranchBankCode(createdBy);
        if (code == null) {
            return bad("Failed to generate a unique branch bank code. Please try again.");
        }
        logger.info("[generateCode] Pre-generated branch bank code: {} for createdBy='{}'", code, createdBy);
        List<Object> data = new ArrayList<>();
        data.add(code);
        return ResponseEntity.ok(new RestWithStatusList("SUCCESS", "Code generated.", data));
    }

    // ── PRIVATE: resolve parent prefix + build unique 8-digit code ────────────
    private String generateBranchBankCode(String createdBy) {
        String parentPrefix = "0000";

        logger.info("[BranchBankCode] Resolving parent prefix for createdBy='{}'", createdBy);

        // JWT subject = username → try by username first, then email
        Optional<MainAdmin> superUserOpt = mainAdminRepository.findFirstByUsername(createdBy);
        if (!superUserOpt.isPresent()) {
            superUserOpt = mainAdminRepository.findFirstByEmail(createdBy);
        }

        if (superUserOpt.isPresent()) {
            MainAdmin su = superUserOpt.get();
            logger.info("[BranchBankCode] MainAdmin found — username='{}' institutionCode='{}'",
                    su.getUsername(), su.getInstitutionCode());

            if (su.getInstitutionCode() != null && su.getInstitutionCode().length() >= 4) {
                // Strategy 1
                parentPrefix = su.getInstitutionCode().substring(0, 4);
                logger.info("[BranchBankCode] Strategy 1 HIT — prefix='{}'", parentPrefix);
            } else {
                // Strategy 2
                logger.warn("[BranchBankCode] Strategy 1 MISS — trying Strategy 2...");
                Optional<MainBank> parentInst =
                        mainBankRepository.findFirstBySuperUserId(su.getUsername());
                if (parentInst.isPresent() && parentInst.get().getInstitutionCode() != null
                        && parentInst.get().getInstitutionCode().length() >= 4) {
                    parentPrefix = parentInst.get().getInstitutionCode().substring(0, 4);
                    logger.info("[BranchBankCode] Strategy 2 HIT — instCode='{}' prefix='{}'",
                            parentInst.get().getInstitutionCode(), parentPrefix);
                } else {
                    logger.warn("[BranchBankCode] Strategy 2 MISS for superUserId='{}'", su.getUsername());
                }
            }
        } else {
            // Strategy 3
            logger.warn("[BranchBankCode] MainAdmin not found, trying Strategy 3...");
            Optional<MainBank> parentInst =
                    mainBankRepository.findFirstBySuperUserId(createdBy);
            if (parentInst.isPresent() && parentInst.get().getInstitutionCode() != null
                    && parentInst.get().getInstitutionCode().length() >= 4) {
                parentPrefix = parentInst.get().getInstitutionCode().substring(0, 4);
                logger.info("[BranchBankCode] Strategy 3 HIT — prefix='{}'", parentPrefix);
            } else {
                logger.error("[BranchBankCode] ALL STRATEGIES FAILED for createdBy='{}' — using '0000'", createdBy);
            }
        }

        // Collision-safe: retry up to 10 times with shifted epoch
        for (int attempt = 0; attempt < 10; attempt++) {
            String epochStr = String.valueOf(System.currentTimeMillis() + attempt);
            String suffix   = epochStr.substring(epochStr.length() - 4);
            String candidate = parentPrefix + suffix;
            if (!branchBankRepository.existsByInstitutionCode(candidate)) {
                return candidate;
            }
            logger.warn("[BranchBankCode] Collision on attempt {}: {}", attempt + 1, candidate);
        }
        return null; // caller handles null
    }

    // Super User ID: firstname.lastname all lowercase
    private String generateSuperUserId(String fullName) {
        if (fullName == null || fullName.trim().isEmpty()) return "user";
        String[] parts = fullName.trim().toLowerCase().split("\\s+");
        if (parts.length == 1) return parts[0];
        return parts[0] + "." + parts[1];
    }

    // Default Password: "Recon@" + 4 random digits
    private String generateDefaultPassword() {
        int digits = 1000 + new Random().nextInt(9000);
        return "Recon@" + digits;
    }

    // ─────────────────────────────────────────────────────────────────────────
    // SAVE PRODUCT DATES — delete-and-reinsert (same pattern as admin)
    // ─────────────────────────────────────────────────────────────────────────
    private void saveProductDates(Long institutionId, BranchBankDTO dto, String savedBy) {
        // Delete existing entries first
        branchBankProductRepository.deleteByInstitutionId(institutionId);

        if (dto.getProductDates() == null || dto.getProductDates().isEmpty()) return;

        List<BranchBankProduct> products = new ArrayList<>();
        dto.getProductDates().forEach((productName, entry) -> {
            BranchBankProduct p = new BranchBankProduct();
            p.setInstitutionId(institutionId);
            p.setProductName(productName);
            p.setValidFrom(entry.getValidFrom());
            p.setValidTo(entry.getValidTo());
            p.setCreatedBy(savedBy != null ? savedBy : "UNKNOWN");
            p.setCreatedAt(LocalDateTime.now());
            p.setUpdatedAt(LocalDateTime.now());
            products.add(p);
        });

        branchBankProductRepository.saveAll(products);
        logger.info("[PRODUCT-DATES] Saved {} product date entries for institution {}",
                products.size(), institutionId);
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

    @Override
    public ResponseEntity<RestWithStatusList> checkEmailExists(String email) {
        if (email == null || email.trim().isEmpty()) {
            return bad("Email is required.");
        }
        boolean exists = branchBankRepository.existsByPrimaryEmail(email.trim());
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

    @Override
    public ResponseEntity<RestWithStatusList> checkNameExists(String name) {
        if (name == null || name.trim().isEmpty()) {
            return bad("Name is required.");
        }
        boolean exists = branchBankRepository.existsByInstitutionNameFull(name.trim());
        if (exists) {
            return ResponseEntity.ok(
                    new RestWithStatusList("EXISTS",
                            "Name '" + name.trim() + "' is already registered.",
                            new ArrayList<>()));
        }
        return ResponseEntity.ok(
                new RestWithStatusList("AVAILABLE",
                        "Name is available.",
                        new ArrayList<>()));
    }

    // ─────────────────────────────────────────────────────────────────────────
    // EXPORT TO EXCEL
    // ─────────────────────────────────────────────────────────────────────────
    @Override
    public ResponseEntity<byte[]> exportToExcel() throws java.io.IOException {

        List<BranchBank> institutions = branchBankRepository.findAll();
        DateTimeFormatter fmt = DateTimeFormatter.ofPattern("dd-MM-yyyy HH:mm:ss");

        try (XSSFWorkbook workbook = new XSSFWorkbook();
             ByteArrayOutputStream out = new ByteArrayOutputStream()) {

            Sheet sheet = workbook.createSheet("BranchBanks");

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
                "S.No", "Branch Bank Code", "Branch Bank Name (Full)",
                "Branch Bank Name (Short)", "Bank Type", "Super User ID",
                "Primary Email", "Primary Mobile", "Status", "Created At"
            };

            Row headerRow = sheet.createRow(0);
            for (int i = 0; i < headers.length; i++) {
                Cell cell = headerRow.createCell(i);
                cell.setCellValue(headers[i]);
                cell.setCellStyle(headerStyle);
            }

            int rowNum = 1;
            for (BranchBank inst : institutions) {
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

            String filename = "BranchBanks_" +
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

        List<BranchBank> institutions = branchBankRepository.findAll();
        DateTimeFormatter fmt = DateTimeFormatter.ofPattern("dd-MM-yyyy HH:mm:ss");

        StringBuilder csv = new StringBuilder();
        csv.append("S.No,Institution Code,Institution Name (Full),Institution Name (Short),")
           .append("Bank Type,Super User ID,Primary Email,Primary Mobile,Status,Created At\n");

        int sno = 1;
        for (BranchBank inst : institutions) {
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
        String filename = "BranchBanks_" +
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

    // ─────────────────────────────────────────────────────────────────────────
    // SCHEDULE BLOCK
    // ─────────────────────────────────────────────────────────────────────────
    @Override
    @Transactional
    public ResponseEntity<RestWithStatusList> scheduleBlock(Long institutionId, String scheduledBy) {

        Optional<BranchBank> opt = branchBankRepository.findById(institutionId);
        if (!opt.isPresent()) {
            return bad("Branch bank not found with ID: " + institutionId);
        }

        BranchBank inst = opt.get();

        if ("BLOCKED".equals(inst.getStatus())) {
            return bad("This branch bank is already permanently BLOCKED.");
        }
        if ("BLOCK_PENDING".equals(inst.getStatus())) {
            return bad("Block is already scheduled for this branch bank.");
        }

        inst.setPreBlockStatus(inst.getStatus());
        inst.setStatus("BLOCK_PENDING");
        inst.setBlockScheduledAt(LocalDateTime.now());
        inst.setBlockScheduledBy(scheduledBy);
        inst.setUpdatedAt(LocalDateTime.now());
        branchBankRepository.save(inst);

        // Sync BLOCK_PENDING to BRANCH_ADMIN
        try {
            branchAdminRepository.findByInstitutionCodeAndUsername(inst.getInstitutionCode(), inst.getSuperUserId())
                .ifPresent(ba -> { ba.setStatus("BLOCK_PENDING"); ba.setUpdatedAt(LocalDateTime.now()); ba.setUpdatedBy(scheduledBy); branchAdminRepository.save(ba); });
        } catch (Exception e) {
            logger.warn("scheduleBlock: BRANCH_ADMIN sync failed for {}: {}", inst.getInstitutionCode(), e.getMessage());
        }

        logger.info("Block scheduled for branch bank {} by {} at {}",
                institutionId, scheduledBy, inst.getBlockScheduledAt());

        String blockAtFormatted = inst.getBlockScheduledAt()
                .plusSeconds(30)   // DEMO: 30s — change to plusHours(24) for production
                .format(DateTimeFormatter.ofPattern("dd MMM yyyy, hh:mm a"));

        try {
            if (inst.getPrimaryEmail() != null && !inst.getPrimaryEmail().isEmpty()) {
                emailService.sendRetireWarning(
                        inst.getPrimaryEmail(),
                        inst.getPrimaryFullName() != null ? inst.getPrimaryFullName() : "Super User",
                        inst.getInstitutionNameFull(),
                        inst.getInstitutionCode(),
                        blockAtFormatted
                );
                logger.info("[BLOCK-WARN] Warning email sent to branch bank super user: {}", inst.getPrimaryEmail());
            }
        } catch (Exception e) {
            logger.warn("[BLOCK-WARN] Warning email failed for branch bank {}: {}", inst.getInstitutionCode(), e.getMessage());
        }

        return ResponseEntity.ok(new RestWithStatusList("SUCCESS",
                "Block scheduled. Branch bank will be permanently blocked in 30 seconds. You can undo this within 30 seconds.",
                new ArrayList<>()));
    }

    // ─────────────────────────────────────────────────────────────────────────
    // UNDO BLOCK
    // ─────────────────────────────────────────────────────────────────────────
    @Override
    @Transactional
    public ResponseEntity<RestWithStatusList> undoBlock(Long institutionId, String undoneBy) {

        Optional<BranchBank> opt = branchBankRepository.findById(institutionId);
        if (!opt.isPresent()) {
            return bad("Branch bank not found with ID: " + institutionId);
        }

        BranchBank inst = opt.get();

        if (!"BLOCK_PENDING".equals(inst.getStatus())) {
            return bad("No scheduled block found for this branch bank.");
        }

        if (inst.getBlockScheduledAt() != null &&
                LocalDateTime.now().isAfter(inst.getBlockScheduledAt().plusSeconds(30))) {   // DEMO: 30s
            return bad("Undo period has expired (30 seconds). Branch bank has been permanently blocked.");
        }

        String restoredStatus = inst.getPreBlockStatus() != null ? inst.getPreBlockStatus() : "INACTIVE";
        inst.setStatus(restoredStatus);
        inst.setBlockScheduledAt(null);
        inst.setBlockScheduledBy(null);
        inst.setPreBlockStatus(null);
        inst.setUpdatedAt(LocalDateTime.now());
        branchBankRepository.save(inst);

        // Sync restored status to BRANCH_ADMIN
        final String finalRestored = restoredStatus;
        try {
            branchAdminRepository.findByInstitutionCodeAndUsername(inst.getInstitutionCode(), inst.getSuperUserId())
                .ifPresent(ba -> { ba.setStatus(finalRestored); ba.setUpdatedAt(LocalDateTime.now()); ba.setUpdatedBy(undoneBy); branchAdminRepository.save(ba); });
        } catch (Exception e) {
            logger.warn("undoBlock: BRANCH_ADMIN sync failed for {}: {}", inst.getInstitutionCode(), e.getMessage());
        }

        logger.info("Block undone for branch bank {} by {}. Restored to {}", institutionId, undoneBy, restoredStatus);

        try {
            if (inst.getPrimaryEmail() != null && !inst.getPrimaryEmail().isEmpty()) {
                emailService.sendRetireCancelled(
                        inst.getPrimaryEmail(),
                        inst.getPrimaryFullName() != null ? inst.getPrimaryFullName() : "Super User",
                        inst.getInstitutionNameFull(),
                        inst.getInstitutionCode(),
                        restoredStatus
                );
                logger.info("[UNDO-BLOCK] Cancellation email sent to: {}", inst.getPrimaryEmail());
            }
        } catch (Exception e) {
            logger.warn("[UNDO-BLOCK] Cancellation email failed for {}: {}", inst.getInstitutionCode(), e.getMessage());
        }

        return ResponseEntity.ok(new RestWithStatusList("SUCCESS",
                "Block has been cancelled. Branch bank status restored to '" + restoredStatus + "'.",
                new ArrayList<>()));
    }

    // ─────────────────────────────────────────────────────────────────────────
    // LOGO IMAGE SERVE
    // ─────────────────────────────────────────────────────────────────────────
    @Override
    public ResponseEntity<byte[]> getLogoImage(String institutionCode) {
        Optional<BranchBank> optional = branchBankRepository.findByInstitutionCode(institutionCode);
        if (!optional.isPresent()) {
            return ResponseEntity.notFound().build();
        }
        String logoPath = optional.get().getLogoPath();
        if (logoPath == null || logoPath.trim().isEmpty()) {
            return ResponseEntity.notFound().build();
        }
        try {
            String cleanPath = logoPath.trim().replaceAll("^\"|\"$", "");
            if (cleanPath.startsWith("'") && cleanPath.endsWith("'")) {
                cleanPath = cleanPath.substring(1, cleanPath.length() - 1);
            }
            Path path = Paths.get(cleanPath);
            if (!Files.exists(path)) {
                logger.warn("Logo file not found on disk for branch bank {}: {}", institutionCode, cleanPath);
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
            logger.error("Failed to serve logo for branch bank {}: {}", institutionCode, e.getMessage());
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).build();
        }
    }

}
