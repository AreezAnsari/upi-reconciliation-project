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
import java.util.LinkedHashMap;
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
import com.jpb.reconciliation.reconciliation.dto.BranchBankDTO;
import com.jpb.reconciliation.reconciliation.entity.BranchBank;
import com.jpb.reconciliation.reconciliation.entity.MainBank;
import com.jpb.reconciliation.reconciliation.entity.MainBankProduct;
import com.jpb.reconciliation.reconciliation.mapper.BranchBankMapper;
import com.jpb.reconciliation.reconciliation.mapper.MainBankMapper;
import com.jpb.reconciliation.reconciliation.entity.BranchBankProduct;
import com.jpb.reconciliation.reconciliation.repository.BranchAdminRepository;
import com.jpb.reconciliation.reconciliation.repository.BranchBankProductRepository;
import com.jpb.reconciliation.reconciliation.repository.BranchBankRepository;
import com.jpb.reconciliation.reconciliation.repository.MainBankProductRepository;
import com.jpb.reconciliation.reconciliation.repository.MainAdminRepository;
import com.jpb.reconciliation.reconciliation.repository.MainBankRepository;

@Service
public class MainBankServiceImpl implements MainBankService {

    private static final Logger logger = LoggerFactory.getLogger(MainBankServiceImpl.class);

    private static final String LOGO_UPLOAD_DIR = "/home/ec2-user/bank_logos/";

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
    private BranchBankProductRepository branchBankProductRepository;

    @Autowired
    private EmailService emailService;

    @Autowired
    private MainAdminRepository mainAdminRepository;

    @Autowired
    private BranchAdminRepository branchAdminRepository;

    @Autowired
    private PasswordEncoder passwordEncoder;

    @Value("${app.frontend.url:http://localhost:5173}")
    private String frontendUrl;

    // ─────────────────────────────────────────────────────────────────────────
    // CREATE
    // ─────────────────────────────────────────────────────────────────────────
    @Override
    @Transactional
    public ResponseEntity<RestWithStatusList> createbank(MainBankDTO dto, String createdBy) {

        if (dto.getBankNameFull() == null || dto.getBankNameFull().trim().isEmpty()) {
            return bad("Bank full name is required.");
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


        // Allow re-onboarding when the existing record with this email is BLOCKED
        // (BLOCKED = permanently blocked — effectively removed from active use).
        if (mainBankRepository.existsByPrimaryEmailAndStatusNot(dto.getPrimaryEmail().trim(), "BLOCKED")) {
            return bad("An bank with email '" + dto.getPrimaryEmail() + "' is already registered.");
        }

        // ── Bank code: frontend sends backend-generated code (via /generate-code API) ──
        // Validate format + uniqueness as final safety check
        String bankCode = dto.getBankCode();

        if (bankCode == null || !bankCode.matches("\\d{8}")) {
            bankCode = generateBankCode(); // fallback: generate if missing
        }

        if (mainBankRepository.findByBankCode(bankCode).isPresent()) {
            bankCode = generateBankCode(); // duplicate? generate fresh
        }
        logger.info("Bank code: {}", bankCode);

        // Generate Super User ID — unique within this bank
        String superUserId = generateBankAdminId(dto.getPrimaryFullName(), bankCode);

        // Generate Default Password
        String defaultPassword = generateDefaultPassword();

        // DTO → Entity
        MainBank bank =
                MainBankMapper.mapToEntity(dto, new MainBank());

        bank.setBankCode(bankCode);
        bank.setStatus("REQUEST");
        bank.setCreatedAt(LocalDateTime.now());
        bank.setCreatedBy(createdBy);  // logged-in admin username from JWT

        // Super User Credentials
        bank.setBankAdminId(superUserId);
        bank.setDefaultPassword(passwordEncoder.encode(defaultPassword)); // BCrypt stored

        // Verification Token
        String token = UUID.randomUUID().toString();

        bank.setVerificationToken(token);
        bank.setTokenExpiry(LocalDateTime.now().plusHours(48));

        // Save Bank
        mainBankRepository.save(bank);

        // =========================================================
        // SEND WELCOME EMAIL
        // BANK_ADMIN insert happens only after Super User sets new password
        // =========================================================

        String verifyLink =
                frontendUrl
                + "/verify-email?bankCode="
                + bankCode
                + "&username="
                + superUserId;

        try {

            emailService.sendBankAdminWelcome(
                    dto.getPrimaryEmail(),
                    dto.getPrimaryFullName(),
                    dto.getBankNameFull(),
                    bankCode,
                    superUserId,
                    defaultPassword,
                    verifyLink
            );

            logger.info(
                    "Welcome email dispatched to: {} | userId: {} | bank: {}",
                    dto.getPrimaryEmail(),
                    superUserId,
                    dto.getBankNameFull()
            );

        } catch (Exception e) {

            logger.warn(
                    "Bank saved but welcome email failed for {}: {}",
                    dto.getPrimaryEmail(),
                    e.getMessage()
            );
        }

        logger.info(
                "Bank created: {} | Code: {} | BankAdminId: {}",
                dto.getBankNameFull(),
                bankCode,
                superUserId
        );
        saveProductDates(bank.getBankId(), dto, createdBy);

        List<Object> data = new ArrayList<>();

        // defaultPassword is sent to Super User via email — admin response should not expose it
        MainBankDTO responseDto = MainBankMapper.mapToDTO(bank);
        responseDto.setDefaultPassword("--");
        data.add(responseDto);

        return ResponseEntity
                .status(HttpStatus.CREATED)
                .body(
                        new RestWithStatusList(
                                "SUCCESS",
                                "Bank '" +
                                        dto.getBankNameFull() +
                                        "' onboarded successfully.",
                                data
                        )
                );
    }    // ─────────────────────────────────────────────────────────────────────────
    // GET ALL
    // ─────────────────────────────────────────────────────────────────────────
    @Override
    @Transactional(readOnly = true)
    public ResponseEntity<RestWithStatusList> getAllBanks() {
        List<MainBank> list = mainBankRepository.findAll();

        if (list.isEmpty()) {
            return ResponseEntity.ok(new RestWithStatusList("SUCCESS", "No banks found.", new ArrayList<>()));
        }

        List<Object> data = list.stream()
                .map(MainBankMapper::mapToDTO)
                .collect(Collectors.toList());
        logger.info("Fetched {} banks", list.size());

        return ResponseEntity.ok(new RestWithStatusList("SUCCESS",
                list.size() + " bank(s) fetched successfully.", data));
    }

    // ─────────────────────────────────────────────────────────────────────────
    // GET BY ID
    // ─────────────────────────────────────────────────────────────────────────
    @Override
    @Transactional(readOnly = true)
    public ResponseEntity<RestWithStatusList> getBankById(Long bankId) {
        Optional<MainBank> optional = mainBankRepository.findByBankId(bankId);

        if (!optional.isPresent()) {
            logger.warn("Bank not found: {}", bankId);
            return bad("Bank not found with ID: " + bankId);
        }

        MainBankDTO dto = MainBankMapper.mapToDTO(optional.get());

        // Populate productDates from TEST_INST_PRODUCT table
        List<MainBankProduct> products = mainBankProductRepository.findByBankId(bankId);
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

        return ResponseEntity.ok(new RestWithStatusList("SUCCESS", "Bank fetched successfully.", data));
    }

    // ─────────────────────────────────────────────────────────────────────────
    // GET BY STATUS
    // ─────────────────────────────────────────────────────────────────────────
    @Override
    @Transactional(readOnly = true)
    public ResponseEntity<RestWithStatusList> getBanksByStatus(String status) {
        List<MainBank> list = mainBankRepository.findByStatus(status.toUpperCase());

        List<Object> data = list.stream()
                .map(MainBankMapper::mapToDTO)
                .collect(Collectors.toList());
        logger.info("Fetched {} banks with status: {}", list.size(), status);

        return ResponseEntity.ok(new RestWithStatusList("SUCCESS",
                list.size() + " bank(s) with status '" + status + "' fetched.", data));
    }

    // ─────────────────────────────────────────────────────────────────────────
    // FULL UPDATE
    // ─────────────────────────────────────────────────────────────────────────
    @Override
    @Transactional
    public ResponseEntity<RestWithStatusList> updateBank(Long bankId, MainBankDTO dto) {
        Optional<MainBank> optional = mainBankRepository.findByBankId(bankId);

        if (!optional.isPresent()) {
            return bad("Bank not found with ID: " + bankId);
        }

        MainBank bank = optional.get();

        // ── Snapshot old values for change-detection (captured BEFORE any modification) ──
        final String oldRegAddr1    = bank.getRegAddressLine1();
        final String oldRegAddr2    = bank.getRegAddressLine2();
        final String oldRegAddr3    = bank.getRegAddressLine3();
        final String oldRegCity     = bank.getRegCity();
        final String oldRegState    = bank.getRegState();
        final String oldRegCountry  = bank.getRegCountry();
        final String oldRegPhCode   = bank.getRegPhoneCode();
        final String oldRegCityCode = bank.getRegCityCode();
        final String oldRegPhone    = bank.getRegPhone();
        final String oldSameAsReg   = bank.getSameAsRegistered();
        final String oldCommAddr1   = bank.getCommAddressLine1();
        final String oldCommAddr2   = bank.getCommAddressLine2();
        final String oldCommAddr3   = bank.getCommAddressLine3();
        final String oldCommCity    = bank.getCommCity();
        final String oldCommState   = bank.getCommState();
        final String oldCommCountry = bank.getCommCountry();
        final String oldCommPhone   = bank.getCommPhone();
        final String oldPrimaryName = bank.getPrimaryFullName();
        final String oldPriAltCode  = bank.getPrimaryAltMobileCode();
        final String oldPriAltMob   = bank.getPrimaryAltMobile();
        final String oldSecName     = bank.getSecondaryFullName();
        final String oldSecAltCode  = bank.getSecondaryAltMobileCode();
        final String oldSecAltMob   = bank.getSecondaryAltMobile();
        final String oldMfa         = bank.getEnableMfa();
        final String oldHrms        = bank.getEnableHrms();
        final String oldOtp         = bank.getEnableOtp();
        // Load old products before saveProductDates wipes them
        final List<MainBankProduct> oldProducts = mainBankProductRepository.findByBankId(bankId);

        // Preserve system-generated fields
        String existingCode             = bank.getBankCode();
        String existingStatus           = bank.getStatus();
        String existingLogo             = bank.getLogoPath();
        String existingBankAdminId      = bank.getBankAdminId();
        String existingDefaultPassword  = bank.getDefaultPassword();
        LocalDateTime existingCreatedAt = bank.getCreatedAt();
        String existingCreatedBy        = bank.getCreatedBy();

        MainBankMapper.mapToEntity(dto, bank);

        // Restore protected fields
        bank.setBankCode(existingCode);
        bank.setStatus(existingStatus);
        bank.setLogoPath(existingLogo);
        bank.setBankAdminId(existingBankAdminId);
        bank.setDefaultPassword(existingDefaultPassword);
        bank.setCreatedAt(existingCreatedAt);
        bank.setCreatedBy(existingCreatedBy);
        bank.setUpdatedAt(LocalDateTime.now());

        mainBankRepository.save(bank);

        // Sync email to BANK_ADMIN if primary_email changed
        try {
            mainAdminRepository.findByBankCodeAndUsername(
                    bank.getBankCode(), bank.getBankAdminId())
                .ifPresent(superUser -> {
                    superUser.setEmail(bank.getPrimaryEmail());
                    superUser.setUpdatedAt(LocalDateTime.now());
                    superUser.setUpdatedBy(getCurrentUsername());
                    mainAdminRepository.save(superUser);
                    logger.info("BANK_ADMIN email synced for bank: {}", bank.getBankCode());
                });
        } catch (Exception e) {
            logger.warn("updateBank: BANK_ADMIN email sync failed for {}: {}",
                    bank.getBankCode(), e.getMessage());
        }

        saveProductDates(bankId, dto, bank.getCreatedBy());
        logger.info("Bank updated: {}", bankId);

        // ── Build per-section change map and send notification email (async, non-blocking) ──
        try {
            Map<String, List<String>> sections = new LinkedHashMap<>();

            // Address
            List<String> addrChg = new ArrayList<>();
            diffF(addrChg, "Reg. Address Line 1", oldRegAddr1,   bank.getRegAddressLine1());
            diffF(addrChg, "Reg. Address Line 2", oldRegAddr2,   bank.getRegAddressLine2());
            diffF(addrChg, "Reg. Address Line 3", oldRegAddr3,   bank.getRegAddressLine3());
            diffF(addrChg, "Reg. City",           oldRegCity,    bank.getRegCity());
            diffF(addrChg, "Reg. State",          oldRegState,   bank.getRegState());
            diffF(addrChg, "Reg. Country",        oldRegCountry, bank.getRegCountry());
            diffF(addrChg, "Reg. Phone",          oldRegPhone,   bank.getRegPhone());
            if (!"Y".equalsIgnoreCase(bank.getSameAsRegistered())) {
                diffF(addrChg, "Comm. Address Line 1", oldCommAddr1,   bank.getCommAddressLine1());
                diffF(addrChg, "Comm. Address Line 2", oldCommAddr2,   bank.getCommAddressLine2());
                diffF(addrChg, "Comm. Address Line 3", oldCommAddr3,   bank.getCommAddressLine3());
                diffF(addrChg, "Comm. City",           oldCommCity,    bank.getCommCity());
                diffF(addrChg, "Comm. State",          oldCommState,   bank.getCommState());
                diffF(addrChg, "Comm. Country",        oldCommCountry, bank.getCommCountry());
                diffF(addrChg, "Comm. Phone",          oldCommPhone,   bank.getCommPhone());
            }
            if (!addrChg.isEmpty()) sections.put("Registered & Communication Address", addrChg);

            // Contact
            List<String> ctcChg = new ArrayList<>();
            diffF(ctcChg, "Primary Contact Name", oldPrimaryName, bank.getPrimaryFullName());
            diffF(ctcChg, "Primary Alternate Mobile",
                (strV(oldPriAltCode) + " " + strV(oldPriAltMob)).trim(),
                (strV(bank.getPrimaryAltMobileCode()) + " " + strV(bank.getPrimaryAltMobile())).trim());
            diffF(ctcChg, "Secondary Contact Name", oldSecName, bank.getSecondaryFullName());
            diffF(ctcChg, "Secondary Alternate Mobile",
                (strV(oldSecAltCode) + " " + strV(oldSecAltMob)).trim(),
                (strV(bank.getSecondaryAltMobileCode()) + " " + strV(bank.getSecondaryAltMobile())).trim());
            if (!ctcChg.isEmpty()) sections.put("Contact Details", ctcChg);

            // Security
            List<String> secChg = new ArrayList<>();
            diffB(secChg, "Multi-Factor Authentication (MFA)", oldMfa,  bank.getEnableMfa());
            diffB(secChg, "HRMS Integration",                  oldHrms, bank.getEnableHrms());
            diffB(secChg, "OTP Verification",                  oldOtp,  bank.getEnableOtp());
            if (!secChg.isEmpty()) sections.put("Security & Compliance Settings", secChg);

            // Products
            List<String> prodChg = diffProducts(oldProducts, dto.getProductDates());
            if (!prodChg.isEmpty()) sections.put("Product Subscriptions & Validity Dates", prodChg);

            if (!sections.isEmpty()) {
                String formattedAt = bank.getUpdatedAt()
                    .format(DateTimeFormatter.ofPattern("dd MMM yyyy, hh:mm a"));
                emailService.sendBankUpdateNotification(
                    bank.getPrimaryEmail(),
                    bank.getPrimaryFullName(),
                    bank.getBankNameFull(),
                    bank.getBankCode(),
                    formattedAt, sections
                );
            }
        } catch (Exception e) {
            logger.warn("updateBank: change-notification email failed for {}: {}",
                    bank.getBankCode(), e.getMessage());
        }

        List<Object> data = new ArrayList<>();
        data.add(MainBankMapper.mapToDTO(bank));

        return ResponseEntity.ok(new RestWithStatusList("SUCCESS", "Bank updated successfully.", data));
    }

    // ─────────────────────────────────────────────────────────────────────────
    // STATUS UPDATE ONLY
    // ─────────────────────────────────────────────────────────────────────────
    @Override
    @Transactional
    public ResponseEntity<RestWithStatusList> updateStatus(Long bankId, String status) {

        // Valid statuses
        List<String> validStatuses = Arrays.asList(
            "REQUEST", "VERIFIED", "ACTIVE", "INACTIVE", "BLOCKED", "BLOCK_PENDING"
        );
        if (!validStatuses.contains(status.toUpperCase())) {
            return bad("Invalid status. Allowed: REQUEST, VERIFIED, ACTIVE, INACTIVE, BLOCKED, BLOCK_PENDING.");
        }

        Optional<MainBank> optional = mainBankRepository.findByBankId(bankId);
        if (!optional.isPresent()) {
            return bad("Bank not found with ID: " + bankId);
        }

        MainBank bank = optional.get();
        String currentStatus = bank.getStatus();

        // ── BLOCKED is permanent — cannot be changed by anyone ──
        if ("BLOCKED".equals(currentStatus)) {
            return bad("This bank is permanently BLOCKED. Its status cannot be changed.");
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
            LocalDateTime inactivatedAt = bank.getInactivatedAt();
            if (inactivatedAt != null) {
                LocalDateTime allowedAfter = inactivatedAt.plusSeconds(30); // DEMO: 30s — change to plusMinutes(30) for production
                if (LocalDateTime.now().isBefore(allowedAfter)) {
                    long secsLeft = java.time.Duration.between(LocalDateTime.now(), allowedAfter).getSeconds();
                    logger.warn("[ACTIVE-BLOCK] Bank {} — only {}s since inactivation (need 30s)", bankId, secsLeft);
                    return bad("Cannot mark Active yet. Bank was recently made Inactive. Please wait " + secsLeft + " more second(s).");
                    // PRODUCTION msg: return bad("Please wait " + (secsLeft / 60 + 1) + " more minute(s) before re-activating.");
                }
            }
        }

        // ── Set inactivatedAt when going INACTIVE ──
        if ("INACTIVE".equals(upperStatus)) {
            bank.setInactivatedAt(LocalDateTime.now());
        }

        bank.setStatus(upperStatus);
        bank.setUpdatedAt(LocalDateTime.now());
        mainBankRepository.save(bank);

        // Sync status to BANK_ADMIN (record exists only after Super User sets password)
        String updatedByUser = getCurrentUsername();
        try {
            mainAdminRepository.findByBankCodeAndUsername(
                    bank.getBankCode(), bank.getBankAdminId())
                .ifPresent(superUser -> {
                    superUser.setStatus(upperStatus);
                    superUser.setUpdatedAt(LocalDateTime.now());
                    superUser.setUpdatedBy(updatedByUser);
                    mainAdminRepository.save(superUser);
                    logger.info("BANK_ADMIN status synced: {} → {}", bank.getBankCode(), upperStatus);
                });
        } catch (Exception e) {
            logger.warn("TEST_bank status updated but BANK_ADMIN sync failed for {}: {}",
                    bank.getBankCode(), e.getMessage());
        }

        logger.info("bank {} status updated: {} → {}", bankId, currentStatus, upperStatus);

        // ── Send status change notification email to Super User ──
        try {
            if (bank.getPrimaryEmail() != null && !bank.getPrimaryEmail().isEmpty()) {
                emailService.sendStatusChangeNotification(
                    bank.getPrimaryEmail(),
                    bank.getPrimaryFullName() != null ? bank.getPrimaryFullName() : "Super User",
                    bank.getBankNameFull(),
                    bank.getBankCode(),
                    currentStatus,
                    upperStatus
                );
            }
        } catch (Exception e) {
            logger.warn("Status updated but notification email failed for bank {}: {}",
                        bank.getBankCode(), e.getMessage());
        }

        // Individual block only — no cascade to branch-banks
        return ResponseEntity.ok(new RestWithStatusList("SUCCESS",
                "Bank status updated to '" + upperStatus + "'.", new ArrayList<>()));
    }

    // ─────────────────────────────────────────────────────────────────────────
    // SOFT DELETE
    // ─────────────────────────────────────────────────────────────────────────
    @Override
    @Transactional
    public ResponseEntity<RestWithStatusList> deleteBank(Long bankId) {
        Optional<MainBank> optional = mainBankRepository.findByBankId(bankId);

        if (!optional.isPresent()) {
            return bad("Bank not found with ID: " + bankId);
        }

        MainBank bank = optional.get();
        bank.setStatus("BLOCKED");
        bank.setUpdatedAt(LocalDateTime.now());
        mainBankRepository.save(bank);

        // Sync INACTIVE to BANK_ADMIN (admin can be INACTIVE, bank entity cannot)
        String deletedByUser = getCurrentUsername();
        try {
            mainAdminRepository.findByBankCodeAndUsername(
                    bank.getBankCode(), bank.getBankAdminId())
                .ifPresent(superUser -> {
                    superUser.setStatus("INACTIVE");
                    superUser.setUpdatedAt(LocalDateTime.now());
                    superUser.setUpdatedBy(deletedByUser);
                    mainAdminRepository.save(superUser);
                });
        } catch (Exception e) {
            logger.warn("deleteBank: BANK_ADMIN sync failed for {}: {}",
                    bank.getBankCode(), e.getMessage());
        }

        logger.info("Bank {} soft-deleted (status → BLOCKED)", bankId);

        return ResponseEntity.ok(new RestWithStatusList("SUCCESS",
                "Bank deactivated successfully.", new ArrayList<>()));
    }

    // ─────────────────────────────────────────────────────────────────────────
    // LOGO UPLOAD
    // ─────────────────────────────────────────────────────────────────────────
    @Override
    @Transactional
    public ResponseEntity<RestWithStatusList> uploadLogo(Long bankId, MultipartFile file, String logoUploader) {

        Optional<MainBank> optional = mainBankRepository.findByBankId(bankId);
        if (!optional.isPresent()) {
            return bad("Bank not found with ID: " + bankId);
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

            MainBank bank = optional.get();
            String originalFilename = file.getOriginalFilename();
            String extension = originalFilename != null && originalFilename.contains(".")
                    ? originalFilename.substring(originalFilename.lastIndexOf("."))
                    : ".jpg";
            String savedFilename = bank.getBankCode() + "_logo" + extension;

            Path filePath = Paths.get(LOGO_UPLOAD_DIR + savedFilename);
            Files.write(filePath, file.getBytes());

            bank.setLogoPath(filePath.toString());
            bank.setUpdatedAt(LocalDateTime.now());
            bank.setUpdatedBy(logoUploader);
            mainBankRepository.save(bank);

            logger.info("Logo uploaded for bank {}: {}", bankId, filePath);

            List<Object> data = new ArrayList<>();
            data.add(MainBankMapper.mapToDTO(bank));

            return ResponseEntity.ok(new RestWithStatusList("SUCCESS",
                    "Logo uploaded successfully.", data));

        } catch (IOException e) {
            logger.error("Logo upload failed for bank {}: {}", bankId, e.getMessage());
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(new RestWithStatusList("FAILURE", "Logo upload failed. Please try again.", new ArrayList<>()));
        }
    }

    // ─────────────────────────────────────────────────────────────────────────
    // SERVE LOGO IMAGE
    // ─────────────────────────────────────────────────────────────────────────
    @Override
    public ResponseEntity<byte[]> getLogoImage(String bankCode) {
        Optional<MainBank> optional = mainBankRepository.findByBankCode(bankCode);
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
                logger.warn("Logo file not found on disk for bank {}: {}", bankCode, cleanLogoPath);
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
            logger.error("Failed to serve logo for bank {}: {}", bankCode, e.getMessage());
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).build();
        }
    }

    // ─────────────────────────────────────────────────────────────────────────
    // GET BY CODE — used by SuperUser sidebar to fetch bank logo + short name
    // ─────────────────────────────────────────────────────────────────────────
    @Override
    @Transactional(readOnly = true)
    public ResponseEntity<RestWithStatusList> getBankByCode(String bankCode) {
        try {
            Optional<MainBank> optional = mainBankRepository.findByBankCode(bankCode);
            if (!optional.isPresent()) {
                return bad("Bank not found with code: " + bankCode);
            }
            MainBank bank = optional.get();
            MainBankDTO dto = MainBankMapper.mapToDTO(bank);
            mainAdminRepository.findByBankCodeAndUsername(bank.getBankCode(), bank.getBankAdminId())
                .ifPresent(admin -> dto.setAdminStatus(admin.getStatus()));
            List<Object> data = new ArrayList<>();
            data.add(dto);
            return ResponseEntity.ok(new RestWithStatusList("SUCCESS", "Bank fetched.", data));
        } catch (Exception e) {
            logger.error("Error fetching bank by code {}: {}", bankCode, e.getMessage());
            return bad("Failed to fetch bank: " + e.getMessage());
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

        MainBank bank = optional.get();

        // Token expiry check — 48 hrs baad expire
        if (bank.getTokenExpiry() != null &&
            LocalDateTime.now().isAfter(bank.getTokenExpiry())) {
            return bad("Verification link has expired. Please contact KalInfotech Admin.");
        }

        // ── Already VERIFIED or ACTIVE — link clicked again ──
        if ("VERIFIED".equals(bank.getStatus()) ||
            "ACTIVE".equals(bank.getStatus())) {
            return ResponseEntity.ok(new RestWithStatusList(
                "ALREADY_VERIFIED",
                "Link already used. Please proceed to login.",
                new ArrayList<>()
            ));
        }

        // ── Status stays REQUEST — do NOT change to VERIFIED here ──
        logger.info("Email link clicked for bank {} — status remains REQUEST, redirecting to login",
                bank.getBankCode());

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
        String code = generateBankCode();
        logger.info("Pre-generated bank code: {}", code);
        List<Object> data = new ArrayList<>();
        data.add(code);
        return ResponseEntity.ok(new RestWithStatusList("SUCCESS", "Bank code generated.", data));
    }

    // ─────────────────────────────────────────────────────────────────────────
    // PRIVATE HELPERS
    // ─────────────────────────────────────────────────────────────────────────

    // Bank code: Epoch seconds (last 6 digits) + 2 random digits = always 8 digits
    private String generateBankCode() {
        String code;
        do {
            long epochPart = (System.currentTimeMillis() / 1000) % 1_000_000L;
            int  randomPart = 10 + new Random().nextInt(90);
            code = String.format("%06d%02d", epochPart, randomPart);
        } while (mainBankRepository.findByBankCode(code).isPresent());
        return code;
    }

    // Super User ID: firstname.lastname — unique within the given bank
    private String generateBankAdminId(String fullName, String bankCode) {
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
            if (!mainAdminRepository.existsByBankCodeAndUsername(bankCode, candidate)) {
                return candidate;
            }
        }

        String bnkWord = bankCode.replaceAll("[^a-z]", "");
        String base = candidates.get(0);
        String fallback = base + (bnkWord.isEmpty() ? ".x" : "." + bnkWord);
        return fallback;
    }

    // Default Password: "Recon@" + 4 random digits
    private String generateDefaultPassword() {
        int digits = 1000 + new Random().nextInt(9000);
        return "Recon@" + digits;
    }

    /**
     * Fire-and-forget email to branch-bank primary contact after cascade status change.
     */
    private void sendSubCascadeEmail(
            BranchBank branch,
            String oldStatus, String newStatus,
            MainBank parent) {
        try {
            if (branch.getPrimaryEmail() == null || branch.getPrimaryEmail().isEmpty()) {
                logger.warn("[CASCADE-EMAIL] No email for branch-bank: {} ({})",
                        branch.getBranchId(), branch.getBranchCode());
                return;
            }
            emailService.sendBranchBankStatusNotification(
                    branch.getPrimaryEmail(),
                    branch.getPrimaryFullName() != null ? branch.getPrimaryFullName() : "Contact",
                    branch.getBranchNameFull() != null ? branch.getBranchNameFull() : branch.getBranchCode(),
                    branch.getBranchCode(),
                    oldStatus,
                    newStatus,
                    parent.getBankNameFull(),
                    parent.getBankCode()
            );
        } catch (Exception e) {
            logger.warn("[CASCADE-EMAIL] Failed for branch-bank {} ({}): {}",
                    branch.getBranchId(), branch.getBranchCode(), e.getMessage());
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
    // CHECK NAME EXISTS
    // Bank name uniqueness is NOT enforced — different legal entities may
    // share a name, and BLOCKED banks should not block fresh onboarding.
    // Always returns AVAILABLE so the form is never rejected on name alone.
    // ─────────────────────────────────────────────────────────────────────────
    @Override
    public ResponseEntity<RestWithStatusList> checkNameExists(String name) {
        if (name == null || name.trim().isEmpty()) {
            return bad("Bank name is required.");
        }
        // Name uniqueness is intentionally not checked — always allow.
        return ResponseEntity.ok(
                new RestWithStatusList("AVAILABLE",
                        "Bank name is available.",
                        new ArrayList<>()));
    }

    // ─────────────────────────────────────────────────────────────────────────
    // CHECK EMAIL EXISTS
    // Returns EXISTS only when a NON-BLOCKED bank already has this email.
    // A BLOCKED bank's email is treated as free — the bank was permanently
    // blocked and the admin should be allowed to re-onboard with the same address.
    // ─────────────────────────────────────────────────────────────────────────
    @Override
    public ResponseEntity<RestWithStatusList> checkEmailExists(String email) {
        if (email == null || email.trim().isEmpty()) {
            return bad("Email is required.");
        }
        boolean existsActive = mainBankRepository.existsByPrimaryEmailAndStatusNot(email.trim(), "BLOCKED");
        if (existsActive) {
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

        List<MainBank> banks = mainBankRepository.findAll();
        DateTimeFormatter fmt = DateTimeFormatter.ofPattern("dd-MM-yyyy HH:mm:ss");

        try (XSSFWorkbook workbook = new XSSFWorkbook();
             ByteArrayOutputStream out = new ByteArrayOutputStream()) {

            Sheet sheet = workbook.createSheet("Banks");

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
                "S.No", "Bank Code", "Bank Name (Full)",
                "Bank Name (Short)", "Bank Type", "Super User ID",
                "Primary Email", "Primary Mobile", "Status", "Created At"
            };

            Row headerRow = sheet.createRow(0);
            for (int i = 0; i < headers.length; i++) {
                Cell cell = headerRow.createCell(i);
                cell.setCellValue(headers[i]);
                cell.setCellStyle(headerStyle);
            }

            int rowNum = 1;
            for (MainBank bnk : banks) {
                Row row = sheet.createRow(rowNum);
                CellStyle style = (rowNum % 2 == 0) ? altStyle : dataStyle;
                setCell(row, 0, String.valueOf(rowNum), style);
                setCell(row, 1, bnk.getBankCode(), style);
                setCell(row, 2, bnk.getBankNameFull(), style);
                setCell(row, 3, bnk.getBankNameShort(), style);
                setCell(row, 4, bnk.getBankType() != null ? bnk.getBankType() : "", style);
                setCell(row, 5, bnk.getBankAdminId(), style);
                setCell(row, 6, bnk.getPrimaryEmail(), style);
                setCell(row, 7, bnk.getPrimaryMobile(), style);
                setCell(row, 8, bnk.getStatus(), style);
                setCell(row, 9, bnk.getCreatedAt() != null
                        ? bnk.getCreatedAt().format(fmt) : "", style);
                rowNum++;
            }

            for (int i = 0; i < headers.length; i++) {
                sheet.autoSizeColumn(i);
            }

            workbook.write(out);
            byte[] data = out.toByteArray();

            String filename = "Banks_" +
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

        List<MainBank> banks = mainBankRepository.findAll();
        DateTimeFormatter fmt = DateTimeFormatter.ofPattern("dd-MM-yyyy HH:mm:ss");

        StringBuilder csv = new StringBuilder();
        csv.append("S.No,Bank Code,Bank Name (Full),Bank Name (Short),")
           .append("Bank Type,Super User ID,Primary Email,Primary Mobile,Status,Created At\n");

        int sno = 1;
        for (MainBank bnk : banks) {
            csv.append(sno++).append(",")
               .append(safeCsv(bnk.getBankCode())).append(",")
               .append(safeCsv(bnk.getBankNameFull())).append(",")
               .append(safeCsv(bnk.getBankNameShort())).append(",")
               .append(safeCsv(bnk.getBankType())).append(",")
               .append(safeCsv(bnk.getBankAdminId())).append(",")
               .append(safeCsv(bnk.getPrimaryEmail())).append(",")
               .append(safeCsv(bnk.getPrimaryMobile())).append(",")
               .append(safeCsv(bnk.getStatus())).append(",")
               .append(safeCsv(bnk.getCreatedAt() != null
                       ? bnk.getCreatedAt().format(fmt) : ""))
               .append("\n");
        }

        byte[] data = csv.toString().getBytes();
        String filename = "Banks_" +
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
    public ResponseEntity<RestWithStatusList> getBanksByCreatedBy(String username) {
        List<MainBank> list = mainBankRepository.findByCreatedBy(username);
        List<MainBankDTO> dtos = new ArrayList<>();
        for (MainBank bnk : list) {
            MainBankDTO dto = MainBankMapper.mapToDTO(bnk);
            mainAdminRepository.findByBankCodeAndUsername(
                    bnk.getBankCode(), bnk.getBankAdminId())
                .ifPresent(admin -> dto.setAdminStatus(admin.getStatus()));
            List<MainBankProduct> prods = mainBankProductRepository.findByBankId(bnk.getBankId());
            if (!prods.isEmpty()) {
                Map<String, ProductDateEntry> productDates = new java.util.LinkedHashMap<>();
                for (MainBankProduct p : prods) {
                    ProductDateEntry entry = new ProductDateEntry();
                    entry.setValidFrom(p.getValidFrom());
                    entry.setValidTo(p.getValidTo());
                    productDates.put(p.getProductName(), entry);
                }
                dto.setProductDates(productDates);
            }
            dtos.add(dto);
        }
        return new ResponseEntity<>(
            new RestWithStatusList("SUCCESS", "Banks fetched.", new ArrayList<>(dtos)),
            HttpStatus.OK);
    }

    // ─────────────────────────────────────────────────────────────────────────
    // SAVE PRODUCT DATES — delete-and-reinsert per bank
    // ─────────────────────────────────────────────────────────────────────────
    private void saveProductDates(Long bankId, MainBankDTO dto, String createdBy) {
        mainBankProductRepository.deleteByBankId(bankId);
        if (dto.getProductDates() == null || dto.getProductDates().isEmpty()) return;
        List<MainBankProduct> products = new ArrayList<>();
        dto.getProductDates().forEach((productName, entry) -> {
            MainBankProduct p = new MainBankProduct();
            p.setBankId(bankId);
            p.setProductName(productName);
            p.setValidFrom(entry.getValidFrom());
            p.setValidTo(entry.getValidTo());
            p.setCreatedBy(createdBy);
            p.setCreatedAt(LocalDateTime.now());
            products.add(p);
        });
        mainBankProductRepository.saveAll(products);
        logger.info("Saved {} product date(s) for bank {}", products.size(), bankId);
    }

    // ─────────────────────────────────────────────────────────────────────────
    // CHANGE-DETECTION HELPERS (used by updateBank to build the diff map)
    // ─────────────────────────────────────────────────────────────────────────

    /** Compare two String fields; if different, append "Label: old → new" to the list. */
    private void diffF(List<String> out, String label, String oldVal, String newVal) {
        String o = oldVal == null ? "" : oldVal.trim();
        String n = newVal == null ? "" : newVal.trim();
        if (!o.equals(n)) out.add(label + ": " + (o.isEmpty() ? "—" : o) + " → " + (n.isEmpty() ? "—" : n));
    }

    /** Compare two Y/N boolean fields; if different, append "Label: Enabled/Disabled → Enabled/Disabled". */
    private void diffB(List<String> out, String label, String oldYN, String newYN) {
        String o = "Y".equalsIgnoreCase(oldYN) ? "Enabled" : "Disabled";
        String n = "Y".equalsIgnoreCase(newYN) ? "Enabled" : "Disabled";
        if (!o.equals(n)) out.add(label + ": " + o + " → " + n);
    }

    /** Null-safe trim. */
    private String strV(String s) { return s == null ? "" : s.trim(); }

    /** Format LocalDate as "dd MMM yyyy", or "—" if null. */
    private String fmtD(java.time.LocalDate d) {
        return d != null ? d.format(DateTimeFormatter.ofPattern("dd MMM yyyy")) : "—";
    }

    /**
     * Compares old product rows from DB vs the new DTO product-date map.
     * Returns human-readable change strings:
     *   "Added: NEFT (Valid: 01 Jan 2025 to 31 Dec 2025)"
     *   "Removed: UPI"
     *   "RTGS Valid To: 30 Jun 2025 → 31 Dec 2025"
     */
    private List<String> diffProducts(List<MainBankProduct> oldProds,
                                       Map<String, MainBankDTO.ProductDateEntry> newMap) {
        List<String> changes = new ArrayList<>();
        Map<String, MainBankProduct> oldMap = new LinkedHashMap<>();
        for (MainBankProduct p : oldProds) oldMap.put(p.getProductName(), p);
        if (newMap == null) newMap = new LinkedHashMap<>();

        // Removed products
        for (String name : oldMap.keySet()) {
            if (!newMap.containsKey(name)) changes.add("Removed: " + name);
        }
        // Added products
        for (Map.Entry<String, MainBankDTO.ProductDateEntry> e : newMap.entrySet()) {
            if (!oldMap.containsKey(e.getKey())) {
                MainBankDTO.ProductDateEntry d = e.getValue();
                changes.add("Added: " + e.getKey()
                    + " (Valid: " + fmtD(d.getValidFrom()) + " to " + fmtD(d.getValidTo()) + ")");
            }
        }
        // Date changes on existing products
        for (Map.Entry<String, MainBankDTO.ProductDateEntry> e : newMap.entrySet()) {
            String name = e.getKey();
            if (oldMap.containsKey(name)) {
                MainBankProduct old = oldMap.get(name);
                MainBankDTO.ProductDateEntry nd = e.getValue();
                String oldFrom = fmtD(old.getValidFrom()), newFrom = fmtD(nd.getValidFrom());
                String oldTo   = fmtD(old.getValidTo()),   newTo   = fmtD(nd.getValidTo());
                if (!oldFrom.equals(newFrom)) changes.add(name + " Valid From: " + oldFrom + " → " + newFrom);
                if (!oldTo.equals(newTo))     changes.add(name + " Valid To: "   + oldTo   + " → " + newTo);
            }
        }
        return changes;
    }

    @Override
    public ResponseEntity<RestWithStatusList> getBranchBank(Long parentBankId) {
        List<BranchBank> branchs = branchBankRepository.findByParentBankId(parentBankId);
        List<Object> data = new ArrayList<>();
        for (BranchBank branch : branchs) {
            BranchBankDTO dto = BranchBankMapper.mapToDTO(branch);
            // Resolve numeric PK of BRANCH_ADMIN record
            try {
                branchAdminRepository.findByBranchCodeAndUsername(
                        branch.getBranchCode(), branch.getBranchAdminId())
                    .ifPresent(ba -> { dto.setAdminId(ba.getId()); dto.setAdminStatus(ba.getStatus()); });
            } catch (Exception e) {
                logger.warn("getBranchBank: adminId lookup failed for {}: {}",
                        branch.getBranchCode(), e.getMessage());
            }
            // Populate product validity dates
            try {
                List<BranchBankProduct> prods = branchBankProductRepository.findByBranchId(branch.getBranchId());
                if (!prods.isEmpty()) {
                    java.util.Map<String, BranchBankDTO.ProductDateEntry> productDates = new java.util.LinkedHashMap<>();
                    for (BranchBankProduct p : prods) {
                        BranchBankDTO.ProductDateEntry entry = new BranchBankDTO.ProductDateEntry();
                        entry.setValidFrom(p.getValidFrom());
                        entry.setValidTo(p.getValidTo());
                        productDates.put(p.getProductName(), entry);
                    }
                    dto.setProductDates(productDates);
                }
            } catch (Exception e) {
                logger.warn("getBranchBank: productDates lookup failed for {}: {}",
                        branch.getBranchCode(), e.getMessage());
            }
            data.add(dto);
        }
        return new ResponseEntity<>(
            new RestWithStatusList("SUCCESS", data.size() + " branch-bank(s) found.", data),
            HttpStatus.OK);
    }
}
