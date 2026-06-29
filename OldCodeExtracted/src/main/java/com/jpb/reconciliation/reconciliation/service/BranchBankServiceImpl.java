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
import com.jpb.reconciliation.reconciliation.entity.BranchAdmin;
import com.jpb.reconciliation.reconciliation.entity.BranchBank;
import com.jpb.reconciliation.reconciliation.entity.BranchBankProduct;
import com.jpb.reconciliation.reconciliation.entity.MainAdmin;
import com.jpb.reconciliation.reconciliation.entity.MainBank;
import com.jpb.reconciliation.reconciliation.mapper.BranchBankMapper;
import com.jpb.reconciliation.reconciliation.entity.AdminReplacement;
import com.jpb.reconciliation.reconciliation.repository.AdminReplacementRepository;
import com.jpb.reconciliation.reconciliation.repository.MainAdminRepository;
import com.jpb.reconciliation.reconciliation.repository.BranchBankProductRepository;
import com.jpb.reconciliation.reconciliation.repository.BranchBankRepository;
import com.jpb.reconciliation.reconciliation.repository.BranchAdminRepository;
import com.jpb.reconciliation.reconciliation.repository.AddUserRepository;
import com.jpb.reconciliation.reconciliation.repository.MainBankRepository;
import com.jpb.reconciliation.reconciliation.entity.AddUser;
import org.springframework.security.crypto.password.PasswordEncoder;

@Service
public class BranchBankServiceImpl implements BranchBankService {

    private static final Logger logger = LoggerFactory.getLogger(BranchBankServiceImpl.class);

    private static final String LOGO_UPLOAD_DIR = "/home/ec2-user/bank _logos/";

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
    private AddUserRepository addUserRepository;

    @Autowired
    private AdminReplacementRepository replacementRepository;

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
    public ResponseEntity<RestWithStatusList> createBank(BranchBankDTO dto , String createdBy) {

        if (dto.getBranchNameFull() == null || dto.getBranchNameFull().trim().isEmpty()) {
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
        if (branchBankRepository.existsByPrimaryEmailAndStatusNot(dto.getPrimaryEmail().trim(), "BLOCKED")) {
            return bad("A bank with email '" + dto.getPrimaryEmail() + "' is already registered.");
        }

        // ── Branch Bank Code ───────────────────────────────────────────────
        String bankCode;
        String dtoCode = dto.getBranchCode();
        if (dtoCode != null && dtoCode.matches("\\d{8}")
                && !branchBankRepository.existsByBranchCode(dtoCode)) {
            bankCode = dtoCode;
            logger.info("[BranchBankCode] Using frontend pre-generated code: {}", bankCode);
        } else {
            // Fallback: generate fresh (covers missing / collided DTO code)
            bankCode = generateBranchBankCode(createdBy);
            if (bankCode == null) {
                return bad("Failed to generate a unique bank code. Please try again.");
            }
        }
        logger.info("Final branch bank code: {}", bankCode);

        // ── Generate Branch Admin ID — rule: firstname.lastname all lowercase ──
        String branchAdminId = generateBankAdmin(dto.getPrimaryFullName());

        // ── Generate default password ──
        String defaultPassword = generateDefaultPassword();

        // Map DTO → Entity
        BranchBank bank= BranchBankMapper.mapToEntity(dto, new BranchBank());
        bank .setBranchCode(bankCode);
        bank .setStatus("REQUEST");
        bank .setCreatedAt(LocalDateTime.now());

        // Save Branch Admin credentials in bankrecord (BCrypt stored, plaintext in email)
        bank .setBranchAdminId(branchAdminId);
        bank .setDefaultPassword(passwordEncoder.encode(defaultPassword));

        // ── Resolve parentbankId from the logged-in BranchAdmin ──
        // Use StatusNot("BLOCKED") email fallback so a re-onboarded BranchAdmin's code resolves correctly.
        // Also normalize createdBy to username (JWT subject may be email when logged in via email mode).
        try {
            Optional<MainAdmin> suOpt = mainAdminRepository.findFirstByUsername(createdBy);
            if (!suOpt.isPresent()) suOpt = mainAdminRepository.findFirstByEmailAndStatusNot(createdBy, "BLOCKED");
            if (suOpt.isPresent()) {
                MainAdmin su = suOpt.get();
                createdBy = su.getUsername();
                Optional<MainBank> parentOpt = Optional.empty();
                if (su.getBankCode() != null && !su.getBankCode().isEmpty()) {
                    parentOpt = mainBankRepository.findByBankCode(su.getBankCode());
                }
                if (!parentOpt.isPresent()) {
                    parentOpt = mainBankRepository.findFirstByBankAdminId(su.getUsername());
                }
                parentOpt.ifPresent(parent -> bank .setParentBankId(parent.getBankId()));
                logger.info("parentbankId resolved: {} for createdBy='{}'",
                        bank .getParentBankId(), createdBy);
            } else {
                logger.warn("createBank: Could not resolve parent for createdBy='{}'", createdBy);
            }
        } catch (Exception e) {
            logger.warn("createBank: parentbankId resolution failed: {}", e.getMessage());
        }

        bank .setCreatedBy(createdBy);

        // Generate verification token — valid for 48 hours
        String token = UUID.randomUUID().toString();
        bank .setVerificationToken(token);
        bank .setTokenExpiry(LocalDateTime.now().plusHours(48));

        branchBankRepository.save(bank );
        logger.info("Branch bank created: {} | Code: {} | BankAdmin: {}",
                    dto.getBranchNameFull(), bankCode, branchAdminId);

        // ── Save product validity dates (delete-and-reinsert — same pattern as admin) ──
        try {
            saveProductDates(bank .getBranchId(), dto, createdBy);
        } catch (Exception e) {
            logger.warn("Product dates save failed for {}: {}", bankCode, e.getMessage());
        }

        // ── Send welcome email with Bank Code, User ID, Default Password ──
        // Branch Admin verify link — separate from Branch Admin flow
        String verifyLink = frontendUrl + "/branch-verify-email?bankCode="
                + bankCode + "&username=" + branchAdminId;
        try {
            emailService.sendBankAdminWelcome(
                dto.getPrimaryEmail(),
                dto.getPrimaryFullName(),
                dto.getBranchNameFull(),
                bankCode,
                branchAdminId,
                defaultPassword,
                verifyLink
            );
            logger.info("Welcome email dispatched to: {} | userId: {} | bank : {}",
                        dto.getPrimaryEmail(), branchAdminId, dto.getBranchNameFull());
        } catch (Exception e) {
            // Email failure should NOT rollback the onboarding — just log the warning
            logger.warn("BranchBank saved but welcome email failed for {}: {}",
                        dto.getPrimaryEmail(), e.getMessage());
        }

        List<Object> data = new ArrayList<>();
        BranchBankDTO responseDto = BranchBankMapper.mapToDTO(bank );
        responseDto.setDefaultPassword("--"); // admin should not see the password — sent via email
        data.add(responseDto);

        return ResponseEntity.status(HttpStatus.CREATED)
                .body(new RestWithStatusList("SUCCESS",
                        "Bank '" + dto.getBranchNameFull() + "' onboarded successfully.", data));
    }


    // ─────────────────────────────────────────────────────────────────────────
    // GET ALL
    // ─────────────────────────────────────────────────────────────────────────
    // GET ALL — scoped to the logged-in MainBank BranchAdmin's parent bank .
    // Filter by parentbankId (DB primary key) — unique even when username/email
    // is reused after re-onboarding a BLOCKED bank .
    // Resolution chain: username → MainAdmin → bankCode → MainBank → bankId
    // ─────────────────────────────────────────────────────────────────────────
    @Override
    @Transactional(readOnly = true)
    public ResponseEntity<RestWithStatusList> getAllBanks(String loggedInUsername) {

        // ── Resolve the parent bank 's unique DB id ──
        Long parentbankId = null;
        try {
            // Step 1: find the logged-in MainAdmin (non-BLOCKED, newest)
            Optional<MainAdmin> suOpt = mainAdminRepository.findFirstByUsername(loggedInUsername);
            if (!suOpt.isPresent()) {
                suOpt = mainAdminRepository.findFirstByEmailAndStatusNot(loggedInUsername, "BLOCKED");
            }
            if (suOpt.isPresent()) {
                String bankCode = suOpt.get().getBankCode();
                // Step 2: find the parent MainBank by its bank code (unique per bank )
                Optional<MainBank> parentOpt = mainBankRepository.findByBankCode(bankCode);
                // Fallback for replacement admins: bankCode may not match directly
                if (!parentOpt.isPresent()) {
                    java.util.List<AdminReplacement> reps = replacementRepository
                            .findByReplacementEntityIdAndEntityTypeAndStatusIn(
                                    suOpt.get().getId(), "MAIN_ADMIN",
                                    java.util.Arrays.asList("ACTIVE", "PERMANENT"));
                    if (!reps.isEmpty()) {
                        Optional<com.jpb.reconciliation.reconciliation.entity.MainAdmin> origOpt =
                                mainAdminRepository.findById(reps.get(0).getOriginalEntityId());
                        if (origOpt.isPresent()) {
                            parentOpt = mainBankRepository.findFirstByBankAdminId(origOpt.get().getUsername());
                        }
                    }
                }
                if (parentOpt.isPresent()) {
                    parentbankId = parentOpt.get().getBankId();
                    logger.info("[GetAllBranchBanks] Resolved parentbankId={} for user='{}'",
                            parentbankId, loggedInUsername);
                }
            } else {
                // Replacement branch admin: not in MainAdmin — find them in BranchAdmin,
                // then resolve parentBankId via their BranchBank record.
                java.util.Optional<com.jpb.reconciliation.reconciliation.entity.BranchAdmin> branchAdminOpt =
                        branchAdminRepository.findFirstByUsername(loggedInUsername);
                if (!branchAdminOpt.isPresent()) {
                    branchAdminOpt = branchAdminRepository.findFirstByEmailAndStatusNot(loggedInUsername, "BLOCKED");
                }
                if (branchAdminOpt.isPresent()) {
                    String branchCode = branchAdminOpt.get().getBranchCode();
                    if (branchCode != null) {
                        Optional<com.jpb.reconciliation.reconciliation.entity.BranchBank> branchBankOpt =
                                branchBankRepository.findByBranchCode(branchCode);
                        if (branchBankOpt.isPresent() && branchBankOpt.get().getParentBankId() != null) {
                            parentbankId = branchBankOpt.get().getParentBankId();
                            logger.info("[GetAllBranchBanks] Resolved parentbankId={} via BranchAdmin for user='{}'",
                                    parentbankId, loggedInUsername);
                        }
                    }
                }
            }
        } catch (Exception e) {
            logger.warn("[GetAllBranchBanks] parentbankId resolution failed for user='{}': {}",
                    loggedInUsername, e.getMessage());
        }

        if (parentbankId == null) {
            logger.warn("[GetAllBranchBanks] Could not resolve parent bank for user='{}' — returning empty list",
                    loggedInUsername);
            return ResponseEntity.ok(new RestWithStatusList("SUCCESS", "No bank s found.", new ArrayList<>()));
        }

        // ── Fetch only branch banks belonging to this parent bank──
        List<BranchBank> list = branchBankRepository.findByParentBankId(parentbankId);

        if (list.isEmpty()) {
            return ResponseEntity.ok(new RestWithStatusList("SUCCESS", "No bank s found.", new ArrayList<>()));
        }

        List<Object> data = new ArrayList<>();
        for (BranchBank branch : list) {
            final BranchBankDTO dto = BranchBankMapper.mapToDTO(branch);
            final com.jpb.reconciliation.reconciliation.dto.BranchBankDTO[] repDtoHolder = { null };
            final BranchBank branchRef = branch;
            branchAdminRepository.findByBranchCodeAndUsername(
                    branch.getBranchCode(), branch.getBranchAdminId())
                .ifPresent(ba -> {
                    dto.setAdminId(ba.getId());
                    dto.setAdminStatus(ba.getStatus());
                    enrichReplacementInfo(ba.getId(), "BRANCH_ADMIN", dto);
                    java.util.List<com.jpb.reconciliation.reconciliation.entity.AdminReplacement> repRecs =
                        replacementRepository.findByOriginalEntityIdAndEntityTypeAndStatusIn(
                            ba.getId(), "BRANCH_ADMIN", Arrays.asList("ACTIVE", "PERMANENT"));
                    if (!repRecs.isEmpty()) {
                        com.jpb.reconciliation.reconciliation.entity.AdminReplacement repRec = repRecs.get(0);
                        if (repRec.getReplacementEntityId() != null && repRec.getReplacementEntityId() > 0L) {
                            branchAdminRepository.findById(repRec.getReplacementEntityId()).ifPresent(repAdmin -> {
                                com.jpb.reconciliation.reconciliation.dto.BranchBankDTO repDto = BranchBankMapper.mapToDTO(branchRef);
                                repDto.setAdminId(repAdmin.getId());
                                repDto.setBranchAdminId(repAdmin.getUsername());
                                repDto.setAdminStatus(repAdmin.getStatus());
                                repDto.setReplacementAdminRow(true);
                                repDto.setReplacementStatus(repRec.getStatus());
                                repDto.setPrimaryEmail(repAdmin.getEmail());
                                String repName = repRec.getPendingFullName() != null && !repRec.getPendingFullName().isEmpty()
                                    ? repRec.getPendingFullName() : repAdmin.getUsername();
                                repDto.setPrimaryFullName(repName);
                                if (repRec.getPendingMobile() != null) repDto.setPrimaryMobile(repRec.getPendingMobile());
                                repDtoHolder[0] = repDto;
                            });
                        }
                    }
                });
            data.add(dto);
            if (repDtoHolder[0] != null) data.add(repDtoHolder[0]);
        }
        logger.info("[GetAllBranchBanks] Fetched {} branch bank(s) for parentId={}", list.size(), parentbankId);

        return ResponseEntity.ok(new RestWithStatusList("SUCCESS",
                list.size() + " bank (s) fetched successfully.", data));
    }


    // ─────────────────────────────────────────────────────────────────────────
    // GET BY ID
    // ─────────────────────────────────────────────────────────────────────────
    @Override
    @Transactional(readOnly = true)
    public ResponseEntity<RestWithStatusList> getBankById(Long bankId) {
        Optional<BranchBank> optional = branchBankRepository.findByBranchId(bankId);

        if (!optional.isPresent()) {
            logger.warn("Bank not found: {}", bankId);
            return bad("Bank not found with ID: " + bankId);
        }

        BranchBankDTO dto = BranchBankMapper.mapToDTO(optional.get());

        // ── Load product validity dates (same pattern as admin getBankById) ──
        try {
            java.util.List<BranchBankProduct> products =
                    branchBankProductRepository.findByBranchId(bankId);
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
            logger.warn("Could not load product dates for bank{}: {}", bankId, e.getMessage());
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
        List<BranchBank> list = branchBankRepository.findByStatus(status.toUpperCase());

        List<Object> data = list.stream()
                .map(BranchBankMapper::mapToDTO)
                .collect(Collectors.toList());
        logger.info("Fetched {} bank s with status: {}", list.size(), status);

        return ResponseEntity.ok(new RestWithStatusList("SUCCESS",
                list.size() + " bank (s) with status '" + status + "' fetched.", data));
    }

    // ─────────────────────────────────────────────────────────────────────────
    // FULL UPDATE
    // ─────────────────────────────────────────────────────────────────────────
    @Override
    @Transactional
    public ResponseEntity<RestWithStatusList> updateBank(Long bankId, BranchBankDTO dto) {
        Optional<BranchBank> optional = branchBankRepository.findByBranchId(bankId);

        if (!optional.isPresent()) {
            return bad("Bank not found with ID: " + bankId);
        }

        BranchBank bank= optional.get();

        // ── Snapshot old values for change-detection (captured BEFORE any modification) ──
        final String oldRegAddr1    = bank .getRegAddressLine1();
        final String oldRegAddr2    = bank .getRegAddressLine2();
        final String oldRegAddr3    = bank .getRegAddressLine3();
        final String oldRegCity     = bank .getRegCity();
        final String oldRegState    = bank .getRegState();
        final String oldRegCountry  = bank .getRegCountry();
        final String oldRegPhone    = bank .getRegPhone();
        final String oldSameAsReg   = bank .getSameAsRegistered();
        final String oldCommAddr1   = bank .getCommAddressLine1();
        final String oldCommAddr2   = bank .getCommAddressLine2();
        final String oldCommAddr3   = bank .getCommAddressLine3();
        final String oldCommCity    = bank .getCommCity();
        final String oldCommState   = bank .getCommState();
        final String oldCommCountry = bank .getCommCountry();
        final String oldCommPhone   = bank .getCommPhone();
        final String oldPrimaryName = bank .getPrimaryFullName();
        final String oldPriAltCode  = bank .getPrimaryAltMobileCode();
        final String oldPriAltMob   = bank .getPrimaryAltMobile();
        final String oldSecName     = bank .getSecondaryFullName();
        final String oldSecAltCode  = bank .getSecondaryAltMobileCode();
        final String oldSecAltMob   = bank .getSecondaryAltMobile();
        final String oldMfa         = bank .getEnableMfa();
        final String oldHrms        = bank .getEnableHrms();
        final String oldOtp         = bank .getEnableOtp();
        // Load old products before saveProductDates wipes them
        final List<BranchBankProduct> oldProducts = branchBankProductRepository.findByBranchId(bankId);

        // Preserve system-generated fields
        String existingCode             = bank .getBranchCode();
        String existingStatus           = bank .getStatus();
        String existingLogo             = bank .getLogoPath();
        String existingBankAdmin      = bank .getBranchAdminId();
        String existingDefaultPassword  = bank .getDefaultPassword();
        LocalDateTime existingCreatedAt = bank .getCreatedAt();
        String existingCreatedBy        = bank .getCreatedBy();

        BranchBankMapper.mapToEntity(dto, bank );

        // Restore protected fields
        bank .setBranchCode(existingCode);
        bank .setStatus(existingStatus);
        bank .setLogoPath(existingLogo);
        bank .setBranchAdminId(existingBankAdmin);
        bank .setDefaultPassword(existingDefaultPassword);
        bank .setCreatedAt(existingCreatedAt);
        bank .setCreatedBy(existingCreatedBy);
        bank .setUpdatedAt(LocalDateTime.now());

        branchBankRepository.save(bank );
        logger.info("Bank updated: {}", bankId);

        // ── Update product validity dates (delete-and-reinsert) ──
        try {
            saveProductDates(bankId, dto, bank .getCreatedBy());
        } catch (Exception e) {
            logger.warn("Product dates update failed for bank{}: {}", bankId, e.getMessage());
        }

        // ── Build per-section change map and send notification email (async, non-blocking) ──
        try {
            String newSameAsReg = bank .getSameAsRegistered();
            String newMfa  = bank .getEnableMfa();
            String newHrms = bank .getEnableHrms();
            String newOtp  = bank .getEnableOtp();

            Map<String, List<String>> sections = new LinkedHashMap<>();

            // Address
            List<String> addrChg = new ArrayList<>();
            bDiffF(addrChg, "Reg. Address Line 1", oldRegAddr1,   bank .getRegAddressLine1());
            bDiffF(addrChg, "Reg. Address Line 2", oldRegAddr2,   bank .getRegAddressLine2());
            bDiffF(addrChg, "Reg. Address Line 3", oldRegAddr3,   bank .getRegAddressLine3());
            bDiffF(addrChg, "Reg. City",           oldRegCity,    bank .getRegCity());
            bDiffF(addrChg, "Reg. State",          oldRegState,   bank .getRegState());
            bDiffF(addrChg, "Reg. Country",        oldRegCountry, bank .getRegCountry());
            bDiffF(addrChg, "Reg. Phone",          oldRegPhone,   bank .getRegPhone());
            if (!"Y".equalsIgnoreCase(newSameAsReg)) {
                bDiffF(addrChg, "Comm. Address Line 1", oldCommAddr1,   bank .getCommAddressLine1());
                bDiffF(addrChg, "Comm. Address Line 2", oldCommAddr2,   bank .getCommAddressLine2());
                bDiffF(addrChg, "Comm. Address Line 3", oldCommAddr3,   bank .getCommAddressLine3());
                bDiffF(addrChg, "Comm. City",           oldCommCity,    bank .getCommCity());
                bDiffF(addrChg, "Comm. State",          oldCommState,   bank .getCommState());
                bDiffF(addrChg, "Comm. Country",        oldCommCountry, bank .getCommCountry());
                bDiffF(addrChg, "Comm. Phone",          oldCommPhone,   bank .getCommPhone());
            }
            if (!addrChg.isEmpty()) sections.put("Registered & Communication Address", addrChg);

            // Contact
            List<String> ctcChg = new ArrayList<>();
            bDiffF(ctcChg, "Primary Contact Name", oldPrimaryName, bank .getPrimaryFullName());
            bDiffF(ctcChg, "Primary Alternate Mobile",
                (bStrV(oldPriAltCode) + " " + bStrV(oldPriAltMob)).trim(),
                (bStrV(bank .getPrimaryAltMobileCode()) + " " + bStrV(bank .getPrimaryAltMobile())).trim());
            bDiffF(ctcChg, "Secondary Contact Name", oldSecName, bank .getSecondaryFullName());
            bDiffF(ctcChg, "Secondary Alternate Mobile",
                (bStrV(oldSecAltCode) + " " + bStrV(oldSecAltMob)).trim(),
                (bStrV(bank .getSecondaryAltMobileCode()) + " " + bStrV(bank .getSecondaryAltMobile())).trim());
            if (!ctcChg.isEmpty()) sections.put("Contact Details", ctcChg);

            // Security
            List<String> secChg = new ArrayList<>();
            bDiffB(secChg, "Multi-Factor Authentication (MFA)", oldMfa,  newMfa);
            bDiffB(secChg, "HRMS Integration",                  oldHrms, newHrms);
            bDiffB(secChg, "OTP Verification",                  oldOtp,  newOtp);
            if (!secChg.isEmpty()) sections.put("Security & Compliance Settings", secChg);

            // Products
            List<String> prodChg = bDiffProducts(oldProducts, dto.getProductDates());
            if (!prodChg.isEmpty()) sections.put("Product Subscriptions & Validity Dates", prodChg);

            if (!sections.isEmpty()) {
                String formattedAt = bank .getUpdatedAt()
                    .format(DateTimeFormatter.ofPattern("dd MMM yyyy, hh:mm a"));
                String[] branchContact = resolveBranchBankContact(bank);
                emailService.sendBankUpdateNotification(
                    branchContact[0],
                    branchContact[1],
                    bank .getBranchNameFull(),
                    bank .getBranchCode(),
                    formattedAt, sections
                );
            }
        } catch (Exception e) {
            logger.warn("updateBank: change-notification email failed for {}: {}",
                    bank .getBranchCode(), e.getMessage());
        }

        List<Object> data = new ArrayList<>();
        data.add(BranchBankMapper.mapToDTO(bank ));

        return ResponseEntity.ok(new RestWithStatusList("SUCCESS", "Bank updated successfully.", data));
    }

    // ─────────────────────────────────────────────────────────────────────────
    // STATUS UPDATE ONLY
    // ─────────────────────────────────────────────────────────────────────────
    @Override
    @Transactional
    public ResponseEntity<RestWithStatusList> updateStatus(Long bankId, String status) {

        List<String> validStatuses = Arrays.asList(
            "REQUEST", "VERIFIED", "ACTIVE", "INACTIVE", "BLOCKED", "BLOCK_PENDING"
        );
        if (!validStatuses.contains(status.toUpperCase())) {
            return bad("Invalid status. Allowed: REQUEST, VERIFIED, ACTIVE, INACTIVE, BLOCKED, BLOCK_PENDING.");
        }

        Optional<BranchBank> optional = branchBankRepository.findById(bankId);
        if (!optional.isPresent()) {
            return bad("Branch bank not found with ID: " + bankId);
        }

        BranchBank bank= optional.get();
        String currentStatus = bank .getStatus();
        String newStatus = status.toUpperCase();

        // ── BLOCKED is permanent ──
        if ("BLOCKED".equals(currentStatus)) {
            return bad("This branch bank is permanently BLOCKED. Its status cannot be changed.");
        }

        // ── Valid transitions (same rules as MainBank) ──
        Map<String, List<String>> allowedTransitions = new HashMap<>();
        allowedTransitions.put("ACTIVE",        Arrays.asList("INACTIVE", "BLOCK_PENDING"));
        allowedTransitions.put("INACTIVE",       Arrays.asList("ACTIVE",   "BLOCK_PENDING"));
        allowedTransitions.put("BLOCK_PENDING",  Arrays.asList("ACTIVE",   "INACTIVE",  "BLOCKED"));
        allowedTransitions.put("PENDING",        Arrays.asList("ACTIVE",   "INACTIVE", "BLOCK_PENDING"));
        allowedTransitions.put("VERIFIED",       Arrays.asList("ACTIVE",   "INACTIVE", "BLOCK_PENDING"));

        List<String> allowed = allowedTransitions.getOrDefault(currentStatus, new ArrayList<>());
        if (!allowed.contains(newStatus)) {
            return bad("Cannot change status from '" + currentStatus + "' to '" + newStatus
                    + "'. Allowed transitions: " + allowed);
        }

        bank .setStatus(newStatus);
        bank .setUpdatedAt(LocalDateTime.now());
        branchBankRepository.save(bank );

        // Sync status to BRANCH_ADMIN
        String updatedByUser = getCurrentUsername();
        try {
            branchAdminRepository.findByBranchCodeAndUsername(
                    bank .getBranchCode(), bank .getBranchAdminId())
                .ifPresent(ba -> {
                    ba.setStatus(newStatus);
                    ba.setUpdatedAt(LocalDateTime.now());
                    ba.setUpdatedBy(updatedByUser);
                    branchAdminRepository.save(ba);
                });
        } catch (Exception e) {
            logger.warn("updateStatus: BRANCH_ADMIN sync failed for {}: {}", bank .getBranchCode(), e.getMessage());
        }

        logger.info("Branch bank {} status updated: {} → {}", bankId, currentStatus, newStatus);

        // ── Send email notification on meaningful status transitions (routes to replacement if original is inactive) ──
        try {
            String[] branchContact = resolveBranchBankContact(bank);
            if (branchContact[0] != null && !branchContact[0].isEmpty()) {
                emailService.sendStatusChangeNotification(branchContact[0], branchContact[1],
                        bank .getBranchNameFull(), bank .getBranchCode(), currentStatus, newStatus);
                logger.info("[EMAIL] Status change notification sent to {} for branch bank {}",
                        branchContact[0], bank .getBranchCode());
            }
        } catch (Exception e) {
            logger.warn("[EMAIL] Status change notification failed for branch bank {}: {}",
                    bank .getBranchCode(), e.getMessage());
        }

        return ResponseEntity.ok(new RestWithStatusList("SUCCESS",
                "Branch bank status updated to '" + newStatus + "'.", new ArrayList<>()));
    }

    // ─────────────────────────────────────────────────────────────────────────
    // SOFT DELETE
    // ─────────────────────────────────────────────────────────────────────────
    @Override
    @Transactional
    public ResponseEntity<RestWithStatusList> deleteBank(Long bankId) {
        Optional<BranchBank> optional = branchBankRepository.findByBranchId(bankId);

        if (!optional.isPresent()) {
            return bad("Bank not found with ID: " + bankId);
        }

        BranchBank bank= optional.get();
        bank .setStatus("BLOCKED");
        bank .setUpdatedAt(LocalDateTime.now());
        branchBankRepository.save(bank );

        // Sync INACTIVE to BRANCH_ADMIN (admin can be INACTIVE, branch entity cannot)
        String deletedByUser = getCurrentUsername();
        try {
            branchAdminRepository.findByBranchCodeAndUsername(
                    bank .getBranchCode(), bank .getBranchAdminId())
                .ifPresent(ba -> {
                    ba.setStatus("INACTIVE");
                    ba.setUpdatedAt(LocalDateTime.now());
                    ba.setUpdatedBy(deletedByUser);
                    branchAdminRepository.save(ba);
                });
        } catch (Exception e) {
            logger.warn("delete: BRANCH_ADMIN sync failed for {}: {}", bank .getBranchCode(), e.getMessage());
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

        Optional<BranchBank> optional = branchBankRepository.findByBranchId(bankId);
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

            BranchBank bank= optional.get();
            String originalFilename = file.getOriginalFilename();
            String extension = originalFilename != null && originalFilename.contains(".")
                    ? originalFilename.substring(originalFilename.lastIndexOf("."))
                    : ".jpg";
            String savedFilename = bank .getBranchCode() + "_logo" + extension;

            Path filePath = Paths.get(LOGO_UPLOAD_DIR + savedFilename);
            Files.write(filePath, file.getBytes());

            bank .setLogoPath(filePath.toString());
            bank .setUpdatedAt(LocalDateTime.now());
            bank .setUpdatedBy(logoUploader);

            System.out.println("Original File Name: " + originalFilename);
            System.out.println("Saved File Name: " + savedFilename);
            System.out.println("File Path: " + filePath.toString());

            branchBankRepository.save(bank );

            System.out.println("DB Logo Path: " + bank .getLogoPath());

            logger.info("Logo uploaded for bank{}: {}", bankId, filePath);

            List<Object> data = new ArrayList<>();
            data.add(BranchBankMapper.mapToDTO(bank ));

            return ResponseEntity.ok(new RestWithStatusList("SUCCESS",
                    "Logo uploaded successfully.", data));

        } catch (IOException e) {
            logger.error("Logo upload failed for bank{}: {}", bankId, e.getMessage());
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(new RestWithStatusList("FAILURE", "Logo upload failed. Please try again.", new ArrayList<>()));
        }
    }

    // ─────────────────────────────────────────────────────────────────────────
    // VERIFY EMAIL
    // ─────────────────────────────────────────────────────────────────────────
    @Override
    @Transactional
    public ResponseEntity<RestWithStatusList> verifyEmail(String branchCode, String username) {
        Optional<BranchBank> optional =
            branchBankRepository.findByBranchCodeAndBranchAdminId(branchCode, username);

        if (!optional.isPresent()) {
            return bad("Invalid or expired verification link.");
        }

        BranchBank bank= optional.get();

        // Token expiry check — 48 hrs baad expire
        if (bank .getTokenExpiry() != null &&
            LocalDateTime.now().isAfter(bank .getTokenExpiry())) {
            return bad("Verification link has expired. Please contact KalInfotech Admin.");
        }

        // Already set password → OLD_USER (go to login), first time → NEW_USER (set password)
        String userStatus = ("VERIFIED".equals(bank .getStatus())
                || "ACTIVE".equals(bank .getStatus()))
                ? "OLD_USER" : "NEW_USER";

        logger.info("Email link clicked for branch bank {} — userStatus: {}",
                bank .getBranchCode(), userStatus);

        Map<String, String> payload = new HashMap<>();
        payload.put("userStatus",  userStatus);
        payload.put("branchCode",  bank .getBranchCode());
        payload.put("username",    bank .getBranchAdminId());

        List<Object> data = new ArrayList<>();
        data.add(payload);

        return ResponseEntity.ok(new RestWithStatusList(
            "SUCCESS",
            "Link is valid. Please proceed.",
            data
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

        // JWT subject = username → try by username first, then email.
        // Use StatusNot("BLOCKED") email fallback so a re-onboarded BranchAdmin's code prefix resolves correctly.
        Optional<MainAdmin> branchAdminOpt = mainAdminRepository.findFirstByUsername(createdBy);
        if (!branchAdminOpt.isPresent()) {
            branchAdminOpt = mainAdminRepository.findFirstByEmailAndStatusNot(createdBy, "BLOCKED");
        }

        if (branchAdminOpt.isPresent()) {
            MainAdmin su = branchAdminOpt.get();
            logger.info("[BranchBankCode] MainAdmin found — username='{}' bankCode='{}'",
                    su.getUsername(), su.getBankCode());

            if (su.getBankCode() != null && su.getBankCode().length() >= 4) {
                // Strategy 1
                parentPrefix = su.getBankCode().substring(0, 4);
                logger.info("[BranchBankCode] Strategy 1 HIT — prefix='{}'", parentPrefix);
            } else {
                // Strategy 2
                logger.warn("[BranchBankCode] Strategy 1 MISS — trying Strategy 2...");
                Optional<MainBank> parentInst =
                        mainBankRepository.findFirstByBankAdminId(su.getUsername());
                if (parentInst.isPresent() && parentInst.get().getBankCode() != null
                        && parentInst.get().getBankCode().length() >= 4) {
                    parentPrefix = parentInst.get().getBankCode().substring(0, 4);
                    logger.info("[BranchBankCode] Strategy 2 HIT — bankCode='{}' prefix='{}'",
                            parentInst.get().getBankCode(), parentPrefix);
                } else {
                    logger.warn("[BranchBankCode] Strategy 2 MISS for branchAdminId='{}'", su.getUsername());
                }
            }
        } else {
            // Strategy 3
            logger.warn("[BranchBankCode] MainAdmin not found, trying Strategy 3...");
            Optional<MainBank> parentInst =
                    mainBankRepository.findFirstByBankAdminId(createdBy);
            if (parentInst.isPresent() && parentInst.get().getBankCode() != null
                    && parentInst.get().getBankCode().length() >= 4) {
                parentPrefix = parentInst.get().getBankCode().substring(0, 4);
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
            if (!branchBankRepository.existsByBranchCode(candidate)) {
                return candidate;
            }
            logger.warn("[BranchBankCode] Collision on attempt {}: {}", attempt + 1, candidate);
        }
        return null; // caller handles null
    }

    // Branch Admin ID: firstname.lastname all lowercase
    private String generateBankAdmin(String fullName) {
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
    private void saveProductDates(Long bankId, BranchBankDTO dto, String savedBy) {
        // Delete existing entries first
        branchBankProductRepository.deleteByBranchId(bankId);

        if (dto.getProductDates() == null || dto.getProductDates().isEmpty()) return;

        List<BranchBankProduct> products = new ArrayList<>();
        dto.getProductDates().forEach((productName, entry) -> {
            BranchBankProduct p = new BranchBankProduct();
            p.setBranchId(bankId);
            p.setProductName(productName);
            p.setValidFrom(entry.getValidFrom());
            p.setValidTo(entry.getValidTo());
            p.setCreatedBy(savedBy != null ? savedBy : "UNKNOWN");
            p.setCreatedAt(LocalDateTime.now());
            p.setUpdatedAt(LocalDateTime.now());
            products.add(p);
        });

        branchBankProductRepository.saveAll(products);
        logger.info("[PRODUCT-DATES] Saved {} product date entries for bank{}",
                products.size(), bankId);
    }

    // ─────────────────────────────────────────────────────────────────────────
    // CHANGE-DETECTION HELPERS (used by updateBank to build the diff map)
    // Prefixed "b" to avoid name clash with any future shared utility class.
    // ─────────────────────────────────────────────────────────────────────────

    /** Compare two String fields; if different, append "Label: old → new" to the list. */
    private void bDiffF(List<String> out, String label, String oldVal, String newVal) {
        String o = oldVal == null ? "" : oldVal.trim();
        String n = newVal == null ? "" : newVal.trim();
        if (!o.equals(n)) out.add(label + ": " + (o.isEmpty() ? "—" : o) + " → " + (n.isEmpty() ? "—" : n));
    }

    /** Compare two Y/N boolean fields; if different, append "Label: Enabled/Disabled → Enabled/Disabled". */
    private void bDiffB(List<String> out, String label, String oldYN, String newYN) {
        String o = "Y".equalsIgnoreCase(oldYN) ? "Enabled" : "Disabled";
        String n = "Y".equalsIgnoreCase(newYN) ? "Enabled" : "Disabled";
        if (!o.equals(n)) out.add(label + ": " + o + " → " + n);
    }

    /** Null-safe trim. */
    private String bStrV(String s) { return s == null ? "" : s.trim(); }

    /** Format LocalDate as "dd MMM yyyy", or "—" if null. */
    private String bFmtD(java.time.LocalDate d) {
        return d != null ? d.format(DateTimeFormatter.ofPattern("dd MMM yyyy")) : "—";
    }

    /**
     * Compares old BranchBankProduct rows from DB vs the new DTO product-date map.
     * Returns human-readable change strings:
     *   "Added: NEFT (Valid: 01 Jan 2025 to 31 Dec 2025)"
     *   "Removed: UPI"
     *   "RTGS Valid To: 30 Jun 2025 → 31 Dec 2025"
     */
    private List<String> bDiffProducts(List<BranchBankProduct> oldProds,
                                        Map<String, BranchBankDTO.ProductDateEntry> newMap) {
        List<String> changes = new ArrayList<>();
        Map<String, BranchBankProduct> oldMap = new LinkedHashMap<>();
        for (BranchBankProduct p : oldProds) oldMap.put(p.getProductName(), p);
        if (newMap == null) newMap = new LinkedHashMap<>();

        // Removed
        for (String name : oldMap.keySet()) {
            if (!newMap.containsKey(name)) changes.add("Removed: " + name);
        }
        // Added
        for (Map.Entry<String, BranchBankDTO.ProductDateEntry> e : newMap.entrySet()) {
            if (!oldMap.containsKey(e.getKey())) {
                BranchBankDTO.ProductDateEntry d = e.getValue();
                changes.add("Added: " + e.getKey()
                    + " (Valid: " + bFmtD(d.getValidFrom()) + " to " + bFmtD(d.getValidTo()) + ")");
            }
        }
        // Date changes on existing products
        for (Map.Entry<String, BranchBankDTO.ProductDateEntry> e : newMap.entrySet()) {
            String name = e.getKey();
            if (oldMap.containsKey(name)) {
                BranchBankProduct old = oldMap.get(name);
                BranchBankDTO.ProductDateEntry nd = e.getValue();
                String oldFrom = bFmtD(old.getValidFrom()), newFrom = bFmtD(nd.getValidFrom());
                String oldTo   = bFmtD(old.getValidTo()),   newTo   = bFmtD(nd.getValidTo());
                if (!oldFrom.equals(newFrom)) changes.add(name + " Valid From: " + oldFrom + " → " + newFrom);
                if (!oldTo.equals(newTo))     changes.add(name + " Valid To: "   + oldTo   + " → " + newTo);
            }
        }
        return changes;
    }

    private String[] resolveBranchBankContact(BranchBank branch) {
        try {
            if (branch.getBranchAdminId() != null) {
                Optional<BranchAdmin> origOpt = branchAdminRepository.findByBranchCodeAndUsername(
                        branch.getBranchCode(), branch.getBranchAdminId());
                if (origOpt.isPresent()) {
                    BranchAdmin orig = origOpt.get();
                    List<AdminReplacement> reps = replacementRepository
                            .findByOriginalEntityIdAndEntityTypeAndStatusIn(
                                    orig.getId(), "BRANCH_ADMIN", Arrays.asList("ACTIVE", "PERMANENT"));
                    if (!reps.isEmpty() && reps.get(0).getReplacementEntityId() > 0) {
                        Optional<BranchAdmin> rep = branchAdminRepository.findById(reps.get(0).getReplacementEntityId());
                        if (rep.isPresent() && rep.get().getEmail() != null && !rep.get().getEmail().isEmpty()) {
                            return new String[]{rep.get().getEmail(), rep.get().getUsername()};
                        }
                    }
                }
            }
        } catch (Exception e) {
            logger.warn("resolveBranchBankContact failed for {}: {}", branch.getBranchCode(), e.getMessage());
        }
        return new String[]{branch.getPrimaryEmail(),
                branch.getPrimaryFullName() != null ? branch.getPrimaryFullName() : "Branch Admin"};
    }

    private String getCurrentUsername() {
        try {
            return org.springframework.security.core.context.SecurityContextHolder
                    .getContext().getAuthentication().getName();
        } catch (Exception e) {
            return "SYSTEM";
        }
    }

    // ─────────────────────────────────────────────────────────────────────────
    // GET BY CODE — used by BranchAdmin sidebar to fetch bank logo + short name
    // ─────────────────────────────────────────────────────────────────────────
    @Override
    @Transactional(readOnly = true)
    public ResponseEntity<RestWithStatusList> getBankByCode(String branchCode) {
        try {
            Optional<BranchBank> optional = branchBankRepository.findByBranchCode(branchCode);
            if (!optional.isPresent()) {
                logger.warn("getBankByCode: No record found in BRANCH_BANK for code '{}'", branchCode);
                return bad("Sub-banknot found with code: " + branchCode);
            }
            BranchBank branch = optional.get();
            logger.info("getBankByCode: Found '{}' for code '{}'", branch.getBranchNameFull(), branchCode);
            BranchBankDTO dto = BranchBankMapper.mapToDTO(branch);
            branchAdminRepository.findByBranchCodeAndUsername(branch.getBranchCode(), branch.getBranchAdminId())
                .ifPresent(admin -> {
                    // If an active replacement exists, report their status — not the (INACTIVE) original's
                    Optional<AdminReplacement> activeRep = replacementRepository
                            .findByOriginalEntityIdAndEntityTypeAndStatus(admin.getId(), "BRANCH_ADMIN", "ACTIVE");
                    if (activeRep.isPresent() && activeRep.get().getReplacementEntityId() != null) {
                        branchAdminRepository.findById(activeRep.get().getReplacementEntityId())
                                .ifPresent(rep -> dto.setAdminStatus(rep.getStatus()));
                    } else {
                        dto.setAdminStatus(admin.getStatus());
                    }
                });
            List<Object> data = new ArrayList<>();
            data.add(dto);
            return ResponseEntity.ok(new RestWithStatusList("SUCCESS", "Sub-bankfetched.", data));
        } catch (Exception e) {
            logger.error("Error fetching sub-bankby code {}: {}", branchCode, e.getMessage());
            return bad("Error fetching sub-bank .");
        }
    }

    // ─────────────────────────────────────────────────────────────────────────
    // GET BY ADMIN EMAIL — used by BranchAdmin sidebar (email is always in sync)
    // When multiple records share the same email (BLOCKED + active), the non-BLOCKED
    // record is preferred so the active branch admin's sidebar works correctly.
    // ─────────────────────────────────────────────────────────────────────────
    @Override
    @Transactional(readOnly = true)
    public ResponseEntity<RestWithStatusList> getBankByEmail(String email) {
        try {
            // Prefer the first non-BLOCKED record — handles re-onboarding duplicates
            Optional<BranchBank> optional =
                    branchBankRepository.findFirstByPrimaryEmailAndStatusNot(email, "BLOCKED");
            if (!optional.isPresent()) {
                // Fallback: all records for this email are BLOCKED — return the most recent one
                // Use findAll + stream to safely handle multiple BLOCKED rows (re-onboarding edge case)
                optional = branchBankRepository.findAllByPrimaryEmail(email)
                        .stream().findFirst();
            }
            if (!optional.isPresent()) {
                logger.warn("getBankByEmail: No record found for email '{}'", email);
                return bad("Sub-banknot found for email: " + email);
            }
            logger.info("getBankByEmail: Found '{}' (status={}) for email '{}'",
                    optional.get().getBranchNameFull(), optional.get().getStatus(), email);
            List<Object> data = new ArrayList<>();
            data.add(BranchBankMapper.mapToDTO(optional.get()));
            return ResponseEntity.ok(new RestWithStatusList("SUCCESS", "Sub-bankfetched.", data));
        } catch (Exception e) {
            logger.error("Error fetching sub-bankby email {}: {}", email, e.getMessage());
            return bad("Error fetching sub-bank .");
        }
    }

    private void enrichReplacementInfo(Long originalAdminId, String entityType, BranchBankDTO dto) {
        java.util.List<AdminReplacement> recs = replacementRepository
                .findByOriginalEntityIdAndEntityTypeAndStatusIn(
                        originalAdminId, entityType, Arrays.asList("ACTIVE", "PERMANENT"));
        if (recs.isEmpty()) return;
        AdminReplacement rec = recs.get(0);
        dto.setReplacementStatus(rec.getStatus());
        try {
            branchAdminRepository.findById(rec.getReplacementEntityId())
                .ifPresent(rep -> dto.setReplacedByUsername(rep.getUsername()));
        } catch (Exception e) {
            logger.warn("enrichReplacementInfo: could not resolve replacement username: {}", e.getMessage());
        }
    }

    /** Convenience — 400 Bad Request */
    private ResponseEntity<RestWithStatusList> bad(String message) {
        return ResponseEntity.status(HttpStatus.BAD_REQUEST)
                .body(new RestWithStatusList("FAILURE", message, new ArrayList<>()));
    }

    // ─────────────────────────────────────────────────────────────────────────
    // CHECK EMAIL EXISTS
    // Returns EXISTS only when a NON-BLOCKED branch bank already has this email.
    // A BLOCKED branch bank's email is treated as free — it was permanently
    // blocked and the Branch Admin should be allowed to re-onboard with the same address.
    // ─────────────────────────────────────────────────────────────────────────
    @Override
    public ResponseEntity<RestWithStatusList> checkEmailExists(String email) {
        if (email == null || email.trim().isEmpty()) {
            return bad("Email is required.");
        }
        boolean existsActive = branchBankRepository.existsByPrimaryEmailAndStatusNot(email.trim(), "BLOCKED");
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
    // CHECK NAME EXISTS
    // Branch bank name uniqueness is NOT enforced — a BLOCKED branch bank's name
    // should not prevent a fresh record from using the same name.
    // Always returns AVAILABLE.
    // ─────────────────────────────────────────────────────────────────────────
    @Override
    public ResponseEntity<RestWithStatusList> checkNameExists(String name) {
        if (name == null || name.trim().isEmpty()) {
            return bad("Name is required.");
        }
        // Name uniqueness is intentionally not checked — always allow.
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

        List<BranchBank> banks = branchBankRepository.findAll();
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
                "Branch Bank Name (Short)", "Bank Type", "Branch Admin ID",
                "Primary Email", "Primary Mobile", "Status", "Created At"
            };

            Row headerRow = sheet.createRow(0);
            for (int i = 0; i < headers.length; i++) {
                Cell cell = headerRow.createCell(i);
                cell.setCellValue(headers[i]);
                cell.setCellStyle(headerStyle);
            }

            int rowNum = 1;
            for (BranchBank bnk : banks) {
                Row row = sheet.createRow(rowNum);
                CellStyle style = (rowNum % 2 == 0) ? altStyle : dataStyle;
                setCell(row, 0, String.valueOf(rowNum), style);
                setCell(row, 1, bnk.getBranchCode(), style);
                setCell(row, 2, bnk.getBranchNameFull(), style);
                setCell(row, 3, bnk.getBranchNameShort(), style);
                setCell(row, 4, bnk.getBankType() != null ? bnk.getBankType() : "", style);
                setCell(row, 5, bnk.getBranchAdminId(), style);
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

        List<BranchBank> banks = branchBankRepository.findAll();
        DateTimeFormatter fmt = DateTimeFormatter.ofPattern("dd-MM-yyyy HH:mm:ss");

        StringBuilder csv = new StringBuilder();
        csv.append("S.No,Bank Code,Bank Name (Full),Bank Name (Short),")
           .append("Bank Type,Branch Admin ID,Primary Email,Primary Mobile,Status,Created At\n");

        int sno = 1;
        for (BranchBank bnk : banks) {
            csv.append(sno++).append(",")
               .append(safeCsv(bnk.getBranchCode())).append(",")
               .append(safeCsv(bnk.getBranchNameFull())).append(",")
               .append(safeCsv(bnk.getBranchNameShort())).append(",")
               .append(safeCsv(bnk.getBankType())).append(",")
               .append(safeCsv(bnk.getBranchAdminId())).append(",")
               .append(safeCsv(bnk.getPrimaryEmail())).append(",")
               .append(safeCsv(bnk.getPrimaryMobile())).append(",")
               .append(safeCsv(bnk.getStatus())).append(",")
               .append(safeCsv(bnk.getCreatedAt() != null
                       ? bnk.getCreatedAt().format(fmt) : ""))
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
    public ResponseEntity<RestWithStatusList> scheduleBlock(Long bankId, String scheduledBy, String reason) {

        Optional<BranchBank> opt = branchBankRepository.findById(bankId);
        if (!opt.isPresent()) {
            return bad("Branch bank not found with ID: " + bankId);
        }

        BranchBank bnk = opt.get();

        if ("BLOCKED".equals(bnk.getStatus())) {
            return bad("This branch bank is already permanently BLOCKED.");
        }
        if ("BLOCK_PENDING".equals(bnk.getStatus())) {
            return bad("Block is already scheduled for this branch bank.");
        }

        bnk.setPreBlockStatus(bnk.getStatus());
        bnk.setStatus("BLOCK_PENDING");
        bnk.setBlockScheduledAt(LocalDateTime.now());
        bnk.setBlockScheduledBy(scheduledBy);
        bnk.setBlockReason(reason);
        bnk.setUpdatedAt(LocalDateTime.now());
        branchBankRepository.save(bnk);

        // Sync BLOCK_PENDING to BRANCH_ADMIN (always)
        final String blockReasonVal = reason;
        try {
            branchAdminRepository.findByBranchCodeAndUsername(bnk.getBranchCode(), bnk.getBranchAdminId())
                .ifPresent(ba -> {
                    if (!"BLOCKED".equalsIgnoreCase(ba.getStatus()) && !"BLOCK_PENDING".equalsIgnoreCase(ba.getStatus())) {
                        ba.setPreBlockStatus(ba.getStatus());
                        ba.setStatus("BLOCK_PENDING");
                        ba.setBlockScheduledAt(LocalDateTime.now());
                        ba.setBlockScheduledBy(scheduledBy);
                        ba.setBlockReason(blockReasonVal);
                        ba.setInactivateScheduledAt(null);
                        ba.setInactivateScheduledBy(null);
                        ba.setReactivateScheduledAt(null);
                        ba.setReactivateScheduledBy(null);
                        ba.setUpdatedAt(LocalDateTime.now());
                        ba.setUpdatedBy(scheduledBy);
                        branchAdminRepository.save(ba);
                    }
                });
        } catch (Exception e) {
            logger.warn("scheduleBlock: BRANCH_ADMIN sync failed for {}: {}", bnk.getBranchCode(), e.getMessage());
        }

        // Check parent bank status — cascade to users if parent bank is ACTIVE
        boolean parentActive = false;
        if (bnk.getParentBankId() != null) {
            try {
                Optional<MainBank> parentBankOpt = mainBankRepository.findById(bnk.getParentBankId());
                parentActive = parentBankOpt.isPresent() && "ACTIVE".equalsIgnoreCase(parentBankOpt.get().getStatus());
            } catch (Exception e) {
                logger.warn("scheduleBlock: parent bank lookup failed for branch {}: {}", bnk.getBranchCode(), e.getMessage());
            }
        }
        logger.info("Block scheduled for branch bank {} by {} at {}",
                bankId, scheduledBy, bnk.getBlockScheduledAt());

        String blockAtFormatted = bnk.getBlockScheduledAt()
                .plusSeconds(30)   // DEMO: 30s — change to plusHours(24) for production
                .format(DateTimeFormatter.ofPattern("dd MMM yyyy, hh:mm a"));

        if (parentActive && "ACTIVE".equalsIgnoreCase(bnk.getPreBlockStatus())) {
            try {
                List<AddUser> branchUsers = addUserRepository.findByBranchCode(bnk.getBranchCode());
                for (AddUser user : branchUsers) {
                    if (user.getStatus() != AddUser.UserStatus.BLOCKED && user.getStatus() != AddUser.UserStatus.BLOCK_PENDING) {
                        user.setPreBlockStatus(user.getStatus().name());
                        user.setStatus(AddUser.UserStatus.BLOCK_PENDING);
                        user.setBlockScheduledAt(LocalDateTime.now());
                        user.setBlockScheduledBy(scheduledBy);
                        user.setBlockReason(reason);
                        user.setInactivateScheduledAt(null);
                        user.setInactivateScheduledBy(null);
                        user.setReactivateScheduledAt(null);
                        user.setReactivateScheduledBy(null);
                        addUserRepository.save(user);
                        try {
                            if (user.getEmail() != null && !user.getEmail().isEmpty()) {
                                emailService.sendBlockWarning(
                                        user.getEmail(),
                                        user.getFullName() != null ? user.getFullName() : user.getUsername(),
                                        bnk.getBranchNameFull(),
                                        bnk.getBranchCode(),
                                        blockAtFormatted
                                );
                            }
                        } catch (Exception emailEx) {
                            logger.warn("scheduleBlock: warning email failed for user {}: {}", user.getUsername(), emailEx.getMessage());
                        }
                    }
                }
                logger.info("scheduleBlock: cascaded BLOCK_PENDING to {} user(s) in branch {}", branchUsers.size(), bnk.getBranchCode());
            } catch (Exception e) {
                logger.warn("scheduleBlock: user cascade failed for branch {}: {}", bnk.getBranchCode(), e.getMessage());
            }
        }

        try {
            String[] branchContact = resolveBranchBankContact(bnk);
            if (branchContact[0] != null && !branchContact[0].isEmpty()) {
                emailService.sendBlockWarning(branchContact[0], branchContact[1],
                        bnk.getBranchNameFull(), bnk.getBranchCode(), blockAtFormatted);
                logger.info("[BLOCK-WARN] Warning email sent to: {}", branchContact[0]);
            }
        } catch (Exception e) {
            logger.warn("[BLOCK-WARN] Warning email failed for branch bank {}: {}", bnk.getBranchCode(), e.getMessage());
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
    public ResponseEntity<RestWithStatusList> undoBlock(Long bankId, String undoneBy) {

        Optional<BranchBank> opt = branchBankRepository.findById(bankId);
        if (!opt.isPresent()) {
            return bad("Branch bank not found with ID: " + bankId);
        }

        BranchBank bnk = opt.get();

        if (!"BLOCK_PENDING".equals(bnk.getStatus())) {
            return bad("No scheduled block found for this branch bank.");
        }

        if (bnk.getBlockScheduledAt() != null &&
                LocalDateTime.now().isAfter(bnk.getBlockScheduledAt().plusSeconds(30))) {   // DEMO: 30s
            return bad("Undo period has expired (30 seconds). Branch bank has been permanently blocked.");
        }

        String restoredStatus = bnk.getPreBlockStatus() != null ? bnk.getPreBlockStatus() : "INACTIVE";
        bnk.setStatus(restoredStatus);
        bnk.setBlockScheduledAt(null);
        bnk.setBlockScheduledBy(null);
        bnk.setBlockReason(null);
        bnk.setPreBlockStatus(null);
        bnk.setUpdatedAt(LocalDateTime.now());
        branchBankRepository.save(bnk);

        // Sync restored status to BRANCH_ADMIN
        final String finalRestored = restoredStatus;
        try {
            branchAdminRepository.findByBranchCodeAndUsername(bnk.getBranchCode(), bnk.getBranchAdminId())
                .ifPresent(ba -> { ba.setStatus(finalRestored); ba.setUpdatedAt(LocalDateTime.now()); ba.setUpdatedBy(undoneBy); branchAdminRepository.save(ba); });
        } catch (Exception e) {
            logger.warn("undoBlock: BRANCH_ADMIN sync failed for {}: {}", bnk.getBranchCode(), e.getMessage());
        }

        // Restore all BLOCK_PENDING users under this branch + send cancellation emails
        try {
            List<AddUser> branchUsers = addUserRepository.findByBranchCode(bnk.getBranchCode());
            for (AddUser user : branchUsers) {
                if (user.getStatus() == AddUser.UserStatus.BLOCK_PENDING) {
                    String userRestored = user.getPreBlockStatus() != null ? user.getPreBlockStatus() : "ACTIVE";
                    user.setStatus(AddUser.UserStatus.valueOf(userRestored));
                    user.setPreBlockStatus(null);
                    user.setBlockScheduledAt(null);
                    user.setBlockScheduledBy(null);
                    user.setBlockReason(null);
                    addUserRepository.save(user);
                    try {
                        if (user.getEmail() != null && !user.getEmail().isEmpty()) {
                            emailService.sendBlockCancelled(
                                    user.getEmail(),
                                    user.getFullName() != null ? user.getFullName() : user.getUsername(),
                                    bnk.getBranchNameFull(),
                                    bnk.getBranchCode(),
                                    userRestored
                            );
                        }
                    } catch (Exception emailEx) {
                        logger.warn("undoBlock: cancellation email failed for user {}: {}", user.getUsername(), emailEx.getMessage());
                    }
                }
            }
            logger.info("undoBlock: restored {} user(s) in branch {}", branchUsers.size(), bnk.getBranchCode());
        } catch (Exception e) {
            logger.warn("undoBlock: user restore failed for branch {}: {}", bnk.getBranchCode(), e.getMessage());
        }

        logger.info("Block undone for branch bank {} by {}. Restored to {}", bankId, undoneBy, restoredStatus);

        try {
            String[] branchContact = resolveBranchBankContact(bnk);
            if (branchContact[0] != null && !branchContact[0].isEmpty()) {
                emailService.sendBlockCancelled(branchContact[0], branchContact[1],
                        bnk.getBranchNameFull(), bnk.getBranchCode(), restoredStatus);
                logger.info("[UNDO-BLOCK] Cancellation email sent to: {}", branchContact[0]);
            }
        } catch (Exception e) {
            logger.warn("[UNDO-BLOCK] Cancellation email failed for {}: {}", bnk.getBranchCode(), e.getMessage());
        }

        return ResponseEntity.ok(new RestWithStatusList("SUCCESS",
                "Block has been cancelled. Branch bank status restored to '" + restoredStatus + "'.",
                new ArrayList<>()));
    }

    // ─────────────────────────────────────────────────────────────────────────
    // LOGO IMAGE SERVE
    // ─────────────────────────────────────────────────────────────────────────
    @Override
    public ResponseEntity<byte[]> getLogoImage(String branchCode) {
        Optional<BranchBank> optional = branchBankRepository.findByBranchCode(branchCode);
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
            // Normalize Windows backslashes → forward slashes (server runs on Linux)
            cleanPath = cleanPath.replace('\\', '/');
            Path path = Paths.get(cleanPath);
            if (!Files.exists(path)) {
                logger.warn("Logo file not found on disk for branch bank {}: {}", branchCode, cleanPath);
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
            logger.error("Failed to serve logo for branch bank {}: {}", branchCode, e.getMessage());
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).build();
        }
    }

}
