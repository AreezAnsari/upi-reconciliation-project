//package com.jpb.reconciliation.reconciliation.service;
//
//import lombok.RequiredArgsConstructor;
//import lombok.extern.slf4j.Slf4j;
//import org.springframework.stereotype.Service;
//import org.springframework.transaction.annotation.Transactional;
//
//import com.jpb.reconciliation.reconciliation.dto.RecUserRequestDTO;
//import com.jpb.reconciliation.reconciliation.dto.RecUserResponseDTO;
//import com.jpb.reconciliation.reconciliation.dto.UserDropdownDTO;
//import com.jpb.reconciliation.reconciliation.entity.RecUserTest;
//import com.jpb.reconciliation.reconciliation.exception.DuplicateResourceException;
//import com.jpb.reconciliation.reconciliation.exception.ResourceNotFoundException;
//import com.jpb.reconciliation.reconciliation.repository.RecUserTestRepository;
//
//import java.util.List;
//import java.util.stream.Collectors;
//
//@Service
//@RequiredArgsConstructor
//@Slf4j
//public class RecUserService {
//
//    private final RecUserTestRepository userRepo;
//
//    // ── 1. Get all active users —  for dropdown  ──────────────
//    public List<UserDropdownDTO> getActiveUsersForDropdown() {
//        log.info("Fetching active users for dropdown");
//        return userRepo.findByIsActiveOrderByFullNameAsc(1)
//            .stream()
//            .map(this::toDropdownDTO)
//            .collect(Collectors.toList());
//    }
//
//    // ── 2. Get all users list (admin view) ──────────────────────
//    public List<RecUserResponseDTO> getAllUsers() {
//        log.info("Fetching all users");
//        return userRepo.findAll()
//            .stream()
//            .map(this::toResponseDTO)
//            .collect(Collectors.toList());
//    }
//
//    // ── 3. Single user by ID ───────────────────────────────────
//    public RecUserResponseDTO getUserById(Long userId) {
//        log.info("Fetching user by ID: {}", userId);
//        RecUserTest user = userRepo.findById(userId)
//            .orElseThrow(() -> new ResourceNotFoundException(
//                "User not found with ID: " + userId
//            ));
//        return toResponseDTO(user);
//    }
//
//    // ── 4.create new user ───────────────────────────────
//    @Transactional
//    public RecUserResponseDTO createUser(RecUserRequestDTO request) {
//        log.info("Creating new user: {}", request.getEmployeeCode());
//
//        // Duplicate email check
//        if (userRepo.existsByEmail(request.getEmail())) {
//            throw new DuplicateResourceException(
//                "Email already exists: " + request.getEmail()
//            );
//        }
//
//        // Duplicate employee code check
//        if (userRepo.existsByEmployeeCode(request.getEmployeeCode())) {
//            throw new DuplicateResourceException(
//                "Employee code already exists: " + request.getEmployeeCode()
//            );
//        }
//
//        RecUserTest user = RecUserTest.builder()
//            .employeeCode(request.getEmployeeCode())
//            .fullName(request.getFullName())
//            .email(request.getEmail())
//            .mobile(request.getMobile())
//            .department(request.getDepartment())
//            .designation(request.getDesignation())
//            .isActive(request.getIsActive() != null ? request.getIsActive() : 1)
//            .build();
//
//        RecUserTest saved = userRepo.save(user);
//        log.info("User created with ID: {}", saved.getUserId());
//        return toResponseDTO(saved);
//    }
//
//    // ── 5. Update User ────────────────────────────────────
//    @Transactional
//    public RecUserResponseDTO updateUser(Long userId, RecUserRequestDTO request) {
//        log.info("Updating user ID: {}", userId);
//
//        RecUserTest user = userRepo.findById(userId)
//            .orElseThrow(() -> new ResourceNotFoundException(
//                "User not found with ID: " + userId
//            ));
//
//        // Email change ho raha hai aur naya email already exists?
//        if (!user.getEmail().equals(request.getEmail())
//                && userRepo.existsByEmail(request.getEmail())) {
//            throw new DuplicateResourceException(
//                "Email already in use: " + request.getEmail()
//            );
//        }
//
//        user.setFullName(request.getFullName());
//        user.setEmail(request.getEmail());
//        user.setMobile(request.getMobile());
//        user.setDepartment(request.getDepartment());
//        user.setDesignation(request.getDesignation());
//        if (request.getIsActive() != null) {
//            user.setIsActive(request.getIsActive());
//        }
//
//        RecUserTest updated = userRepo.save(user);
//        log.info("User updated: {}", updated.getUserId());
//        return toResponseDTO(updated);
//    }
//
//    // ── 6. For deactivate user  (soft delete) ─────────────────
//    @Transactional
//    public RecUserResponseDTO deactivateUser(Long userId) {
//        log.info("Deactivating user ID: {}", userId);
//        RecUserTest user = userRepo.findById(userId)
//            .orElseThrow(() -> new ResourceNotFoundException(
//                "User not found with ID: " + userId
//            ));
//        user.setIsActive(0);
//        return toResponseDTO(userRepo.save(user));
//    }
//
//    // ── 7. Search users ────────────────────────────────────────
//    public List<UserDropdownDTO> searchUsers(String keyword) {
//        log.info("Searching users with keyword: {}", keyword);
//        return userRepo.searchActiveUsers(keyword)
//            .stream()
//            .map(this::toDropdownDTO)
//            .collect(Collectors.toList());
//    }
//
//    // ── Private mappers ────────────────────────────────────────
//    private RecUserResponseDTO toResponseDTO(RecUserTest u) {
//        return RecUserResponseDTO.builder()
//            .userId(u.getUserId())
//            .employeeCode(u.getEmployeeCode())
//            .fullName(u.getFullName())
//            .email(u.getEmail())
//            .mobile(u.getMobile())
//            .department(u.getDepartment())
//            .designation(u.getDesignation())
//            .isActive(u.getIsActive())
//            .createdAt(u.getCreatedAt())
//            .updatedAt(u.getUpdatedAt())
//            .build();
//    }
//
//    private UserDropdownDTO toDropdownDTO(RecUserTest u) {
//        return UserDropdownDTO.builder()
//            .userId(u.getUserId())
//            .employeeCode(u.getEmployeeCode())
//            .fullName(u.getFullName())
//            .email(u.getEmail())
//            .department(u.getDepartment())
//            .designation(u.getDesignation())
//            .build();
//    }
//}
//
