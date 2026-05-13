//package com.jpb.reconciliation.reconciliation.service;
//
//
//import lombok.RequiredArgsConstructor;
//import lombok.extern.slf4j.Slf4j;
//import org.springframework.stereotype.Service;
//import org.springframework.transaction.annotation.Transactional;
//
//import com.jpb.reconciliation.reconciliation.dto.AssignUserRequestDTO;
//import com.jpb.reconciliation.reconciliation.dto.AssignUserResponseDTO;
//import com.jpb.reconciliation.reconciliation.entity.RecRole;
//import com.jpb.reconciliation.reconciliation.entity.RecUserRoleMappingTest;
//import com.jpb.reconciliation.reconciliation.entity.RecUserTest;
//import com.jpb.reconciliation.reconciliation.exception.ResourceNotFoundException;
//import com.jpb.reconciliation.reconciliation.repository.RecRoleTestRepository;
//import com.jpb.reconciliation.reconciliation.repository.RecUserRoleMappingTestRepository;
//import com.jpb.reconciliation.reconciliation.repository.RecUserTestRepository;
//
//import java.time.LocalDateTime;
//import java.util.List;
//import java.util.stream.Collectors;
// 
//@Service
//@RequiredArgsConstructor
//@Slf4j
//public class RecUserRoleMappingService {
// 
//    private final RecUserTestRepository            userRepo;
//    private final RecRoleTestRepository            roleRepo;
//    private final RecUserRoleMappingTestRepository mappingRepo;
// 
//    // ── 1. Active roles — dropdown ke liye ────────────────────
//    public List<RecRoleDropdownDTO> getActiveRolesForDropdown() {
//        log.info("Fetching active roles for dropdown");
//        return roleRepo.findByStatusOrderByRoleNameAsc("ACTIVE")
//            .stream()
//            .map(r -> RoleDropdownDTO.builder()
//                .roleId(r.getId())
//                .roleCode(r.getRoleCode())
//                .roleName(r.getRoleName())
//                .roleType(r.getRoleType())
//                .status(r.getStatus())
//                .build())
//            .collect(Collectors.toList());
//    }
// 
//    // ── 2. MAIN TASK — User ko Role assign karo ───────────────
//    @Transactional
//    public AssignUserResponseDTO assignRoleToUser(AssignUserRequestDTO request) {
//        log.info("Assigning role {} to user {}", request.getRoleId(), request.getUserId());
// 
//        // Step 1: Duplicate check
//        if (mappingRepo.existsByUserUserIdAndRoleId(
//                request.getUserId(), request.getRoleId())) {
//            throw new DuplicateAssignmentException(
//                "This role is already assigned to the selected user. " +
//                "Duplicate assignment is not allowed."
//            );
//        }
// 
//        // Step 2: User exist karta hai?
//        RecUserTest user = userRepo.findById(request.getUserId())
//            .orElseThrow(() -> new ResourceNotFoundException(
//                "User not found with ID: " + request.getUserId()
//            ));
// 
//        // Step 3: User active hai?
//        if (user.getIsActive() != 1) {
//            throw new IllegalArgumentException(
//                "Cannot assign role to inactive user: " + user.getFullName()
//            );
//        }
// 
//        // Step 4: Role exist karta hai?
//        RecRole role = roleRepo.findById(request.getRoleId())
//            .orElseThrow(() -> new ResourceNotFoundException(
//                "Role not found with ID: " + request.getRoleId()
//            ));
// 
//        // Step 5: Role ACTIVE hai?
//        if (!"ACTIVE".equalsIgnoreCase(role.getStatus())) {
//            throw new IllegalArgumentException(
//                "Only ACTIVE roles can be assigned. Current status: " + role.getStatus()
//            );
//        }
// 
//        // Step 6: Mapping table mein INSERT karo
//        RecUserRoleMappingTest mapping = RecUserRoleMappingTest.builder()
//            .user(user)
//            .role(role)
//            .moduleAssignment(
//                request.getModuleAssignment() != null
//                    ? request.getModuleAssignment()
//                    : "ROLE_DEFAULTS"
//            )
//            .status("ACTIVE")
//            .assignedBy(
//                request.getAssignedBy() != null
//                    ? request.getAssignedBy()
//                    : "SYSTEM"
//            )
//            .assignedAt(LocalDateTime.now())
//            .remarks(request.getRemarks())
//            .build();
// 
//        RecUserRoleMappingTest saved = mappingRepo.save(mapping);
//        log.info("Mapping created: ID={}", saved.getMappingId());
// 
//        // Step 7: Response return karo
//        return AssignUserResponseDTO.builder()
//            .mappingId(saved.getMappingId())
//            .userId(user.getUserId())
//            .userName(user.getFullName())
//            .employeeCode(user.getEmployeeCode())
//            .userEmail(user.getEmail())
//            .roleId(role.getId())
//            .roleName(role.getRoleName())
//            .roleCode(role.getRoleCode())
//            .moduleAssignment(saved.getModuleAssignment())
//            .status(saved.getStatus())
//            .assignedBy(saved.getAssignedBy())
//            .assignedAt(saved.getAssignedAt())
//            .message("Role '" + role.getRoleName() + "' successfully assigned to "
//                     + user.getFullName())
//            .build();
//    }
// 
//    // ── 3. Ek user ke saare assignments dekho ─────────────────
//    public List<AssignUserResponseDTO> getUserAssignments(Long userId) {
//        log.info("Fetching assignments for user ID: {}", userId);
// 
//        // User exist karta hai?
//        if (!userRepo.existsById(userId)) {
//            throw new ResourceNotFoundException(
//                "User not found with ID: " + userId
//            );
//        }
// 
//        return mappingRepo.findByUserIdWithDetails(userId)
//            .stream()
//            .map(m -> AssignUserResponseDTO.builder()
//                .mappingId(m.getMappingId())
//                .userId(m.getUser().getUserId())
//                .userName(m.getUser().getFullName())
//                .employeeCode(m.getUser().getEmployeeCode())
//                .userEmail(m.getUser().getEmail())
//                .roleId(m.getRole().getId())
//                .roleName(m.getRole().getRoleName())
//                .roleCode(m.getRole().getRoleCode())
//                .moduleAssignment(m.getModuleAssignment())
//                .status(m.getStatus())
//                .assignedBy(m.getAssignedBy())
//                .assignedAt(m.getAssignedAt())
//                .build())
//            .collect(Collectors.toList());
//    }
// 
//    // ── 4. Assignment revoke karo ──────────────────────────────
//    @Transactional
//    public AssignUserResponseDTO revokeAssignment(Long mappingId) {
//        log.info("Revoking assignment ID: {}", mappingId);
// 
//        RecUserRoleMappingTest mapping = mappingRepo.findById(mappingId)
//            .orElseThrow(() -> new ResourceNotFoundException(
//                "Mapping not found with ID: " + mappingId
//            ));
// 
//        mapping.setStatus("REVOKED");
//        RecUserRoleMappingTest updated = mappingRepo.save(mapping);
// 
//        return AssignUserResponseDTO.builder()
//            .mappingId(updated.getMappingId())
//            .userName(updated.getUser().getFullName())
//            .roleName(updated.getRole().getRoleName())
//            .status(updated.getStatus())
//            .message("Assignment revoked successfully")
//            .build();
//    }
//}
// 