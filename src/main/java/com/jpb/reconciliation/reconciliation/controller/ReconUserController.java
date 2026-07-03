package com.jpb.reconciliation.reconciliation.controller;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import com.jpb.reconciliation.reconciliation.constants.CommonConstants;
import com.jpb.reconciliation.reconciliation.dto.ReconUserDto;
import com.jpb.reconciliation.reconciliation.dto.ResponseDto;
import com.jpb.reconciliation.reconciliation.dto.RestWithStatusList;
import com.jpb.reconciliation.reconciliation.dto.UserPasswordChangeRequest;
import com.jpb.reconciliation.reconciliation.entity.v2.ReconBankMaster;
import com.jpb.reconciliation.reconciliation.entity.v2.ReconUser;
import com.jpb.reconciliation.reconciliation.repository.v2.ReconBankMasterRepository;
import com.jpb.reconciliation.reconciliation.repository.v2.ReconUserRepository;
import com.jpb.reconciliation.reconciliation.service.v2.DelegationService;
import com.jpb.reconciliation.reconciliation.service.v2.ReconUserService;

import io.swagger.v3.oas.annotations.Operation;

import java.util.ArrayList;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;

@RestController
@RequestMapping(path = "/api/v1/user")
public class ReconUserController {

	Logger logger = LoggerFactory.getLogger(ReconUserController.class);

	@Autowired
	ReconUserService reconUserService;

	@Autowired
	ReconUserRepository reconUserRepository;

	@Autowired
	ReconBankMasterRepository reconBankMasterRepository;

	@Autowired
	DelegationService delegationService;

	@PostMapping(value = "/create-user", produces = CommonConstants.APPLICATION_JSON)
	public ResponseEntity<RestWithStatusList> createUser(@RequestBody ReconUserDto reconUserDto) {
		RestWithStatusList restWithStatusList = null;
//		if (reconUserDto.getRoleName().equalsIgnoreCase("MAKER")) {
			return reconUserService.createUser(reconUserDto);
//		} else {
//			restWithStatusList = new RestWithStatusList("FAILURE", "You do not have the rights to create a user.",
//					null);
//			return new ResponseEntity<RestWithStatusList>(restWithStatusList, HttpStatus.BAD_REQUEST);
//		}

	}

	@GetMapping(value = "/getuserby-userid/{userId}", produces = CommonConstants.APPLICATION_JSON)
	public ResponseEntity<RestWithStatusList> getUser(@PathVariable Long userId) {
		return reconUserService.getUserByUserId(userId);
	}

	@GetMapping(value = "/getuser", produces = CommonConstants.APPLICATION_JSON)
	public ResponseEntity<?> getUserDetails(@AuthenticationPrincipal UserDetails userDetails) {
		return reconUserService.getUserData(userDetails.getUsername());
	}

	@DeleteMapping(value = "/remove-user/{userId}", produces = CommonConstants.APPLICATION_JSON)
	public ResponseEntity<ResponseDto> removedUser(@PathVariable Long userId) {
		return reconUserService.removeUser(userId);
	}

	@PostMapping(value = "/change-password", produces = CommonConstants.APPLICATION_JSON)
	public ResponseEntity<ResponseDto> changePassword(@RequestBody UserPasswordChangeRequest changePasswordRequest) {
		logger.info("User Request for changing password" + changePasswordRequest);
		return reconUserService.changePassword(changePasswordRequest);
	}

	@PutMapping(path = "/update-user", produces = CommonConstants.APPLICATION_JSON)
	public ResponseEntity<RestWithStatusList> updateUser(@RequestBody ReconUserDto userUpdateRequest) {
		return reconUserService.updateUser(userUpdateRequest);
	}

	@GetMapping(value = "/get-approved-users", produces = CommonConstants.APPLICATION_JSON)
	public ResponseEntity<RestWithStatusList> getApprovedUsers(@RequestParam String approvedYN) {
		return reconUserService.getApprovedUSers(approvedYN);
	}

	@PostMapping(value = "/approve-reject-user", produces = CommonConstants.APPLICATION_JSON)
	public ResponseEntity<RestWithStatusList> approveOrRejectUser(@RequestBody ReconUserDto approveUserRequest,@AuthenticationPrincipal UserDetails userDetails) {
		return reconUserService.approveOrRejectUser(approveUserRequest, userDetails);
	}

	@GetMapping(value = "/getallusers", produces = CommonConstants.APPLICATION_JSON)
	public ResponseEntity<RestWithStatusList> getAllUserDetails() {
		return reconUserService.getAllUsers();
	}

	@Operation(summary = "Delegate a user's direct reports to an ancestor (the 'Confirm Delegation' flow) — "
			+ "transfers children immediately and schedules the delegator's own inactivation")
	@PostMapping(value = "/{id}/delegate", produces = CommonConstants.APPLICATION_JSON)
	public ResponseEntity<RestWithStatusList> delegateUser(
			@PathVariable Long id,
			@RequestParam Long delegateeId,
			@RequestParam(required = false) String reason,
			@AuthenticationPrincipal UserDetails userDetails) {
		String triggeredBy = userDetails != null ? userDetails.getUsername() : "UNKNOWN";
		return delegationService.delegateNow(id, delegateeId, reason, triggeredBy);
	}

	@GetMapping(value = "/get-by-bank/{bankCode}", produces = CommonConstants.APPLICATION_JSON)
	public ResponseEntity<RestWithStatusList> getUsersByBankCode(@PathVariable String bankCode) {
		Optional<ReconBankMaster> bankOpt = reconBankMasterRepository.findByBankCode(bankCode);
		if (!bankOpt.isPresent()) {
			return ResponseEntity.ok(new RestWithStatusList("SUCCESS", "No data", Collections.emptyList()));
		}
		Long mainBankId = bankOpt.get().getBankId();
		List<Map<String, Object>> result = new ArrayList<>();

		// Users directly under the bank itself (not under any branch)
		List<ReconUser> directUsers = reconUserRepository.findByBankId(mainBankId);
		for (ReconUser u : directUsers) {
			if (!"BANK_USER".equals(u.getUserType())) continue;
			result.add(buildUserRow(u, bankCode));
		}

		// Users under each of this bank's branches
		List<ReconBankMaster> branches = reconBankMasterRepository.findByParentBankId(mainBankId);
		for (ReconBankMaster branch : branches) {
			String bCode = branch.getBankCode();
			List<ReconUser> users = reconUserRepository.findByBankId(branch.getBankId());
			for (ReconUser u : users) {
				if (!"BRANCH_USER".equals(u.getUserType())) continue;
				result.add(buildUserRow(u, bCode));
			}
		}
		return ResponseEntity.ok(new RestWithStatusList("SUCCESS", "Users fetched", result));
	}

	@GetMapping(value = "/get-by-branch/{branchCode}", produces = CommonConstants.APPLICATION_JSON)
	public ResponseEntity<RestWithStatusList> getUsersByBranchCode(@PathVariable String branchCode) {
		Optional<ReconBankMaster> branchOpt = reconBankMasterRepository.findByBankCode(branchCode);
		if (!branchOpt.isPresent()) {
			return ResponseEntity.ok(new RestWithStatusList("SUCCESS", "No data", Collections.emptyList()));
		}
		Long branchBankId = branchOpt.get().getBankId();
		List<ReconUser> users = reconUserRepository.findByBankId(branchBankId);
		List<Map<String, Object>> result = new ArrayList<>();
		for (ReconUser u : users) {
			if (!"BRANCH_USER".equals(u.getUserType())) continue;
			result.add(buildUserRow(u, branchCode));
		}
		return ResponseEntity.ok(new RestWithStatusList("SUCCESS", "Branch users fetched", result));
	}

	private Map<String, Object> buildUserRow(ReconUser u, String branchCode) {
		Map<String, Object> row = new LinkedHashMap<>();
		row.put("id", u.getUserId());
		row.put("fullName", u.getFullName());
		row.put("username", u.getUsername());
		row.put("email", u.getEmail());
		row.put("mobileNumber", u.getMobileNumber());
		row.put("role", u.getUserType());
		row.put("status", u.getStatus());
		row.put("branchCode", branchCode);
		row.put("blockReason", u.getBlockReason());
		row.put("blockScheduledAt", u.getBlockScheduledAt());
		row.put("inactivateScheduledAt", u.getInactivateScheduledAt());
		row.put("reactivateScheduledAt", u.getReactivateScheduledAt());
		row.put("preBlockStatus", u.getPreBlockStatus());
		row.put("replacementStatus", null);
		row.put("replacedByUsername", null);
		row.put("replacementAdminRow", false);
		row.put("parentId", u.getParentUserId());
		return row;
	}

	@Operation(summary = "Get reporting-hierarchy tree of users under a branch (nested by parentUserId)")
	@GetMapping(value = "/hierarchy/branch/{branchCode}", produces = CommonConstants.APPLICATION_JSON)
	public ResponseEntity<RestWithStatusList> getUserHierarchyByBranch(@PathVariable String branchCode) {
		Optional<ReconBankMaster> branchOpt = reconBankMasterRepository.findByBankCode(branchCode);
		if (!branchOpt.isPresent()) {
			return ResponseEntity.ok(new RestWithStatusList("SUCCESS", "No data", Collections.emptyList()));
		}
		List<Map<String, Object>> tree = buildUserHierarchyTree(branchOpt.get().getBankId(), branchCode);
		return ResponseEntity.ok(new RestWithStatusList("SUCCESS", "Branch hierarchy fetched", tree));
	}

	@Operation(summary = "Get reporting-hierarchy tree of users directly under a bank (not under any branch)")
	@GetMapping(value = "/hierarchy/bank/{bankCode}/direct", produces = CommonConstants.APPLICATION_JSON)
	public ResponseEntity<RestWithStatusList> getUserHierarchyByBankDirect(@PathVariable String bankCode) {
		Optional<ReconBankMaster> bankOpt = reconBankMasterRepository.findByBankCode(bankCode);
		if (!bankOpt.isPresent()) {
			return ResponseEntity.ok(new RestWithStatusList("SUCCESS", "No data", Collections.emptyList()));
		}
		List<Map<String, Object>> tree = buildUserHierarchyTree(bankOpt.get().getBankId(), bankCode);
		return ResponseEntity.ok(new RestWithStatusList("SUCCESS", "Bank direct hierarchy fetched", tree));
	}

	// Users of the given bankId, nested by parentUserId. Roots = users whose
	// parent isn't also a user of this same bankId (e.g. admin created by a KalAdmin/BankAdmin
	// who sits outside this institution's own user scope).
	private List<Map<String, Object>> buildUserHierarchyTree(Long bankId, String codeForRow) {
		List<ReconUser> allUsers = new ArrayList<>();
		for (ReconUser u : reconUserRepository.findByBankId(bankId)) {
			// Only regular (non-admin) users belong in the hierarchy tree.
			if ("BANK_USER".equals(u.getUserType()) || "BRANCH_USER".equals(u.getUserType())) {
				allUsers.add(u);
			}
		}
		java.util.Set<Long> idsInScope = new java.util.HashSet<>();
		for (ReconUser u : allUsers) idsInScope.add(u.getUserId());

		Map<Long, List<ReconUser>> byParent = new java.util.HashMap<>();
		List<ReconUser> roots = new ArrayList<>();
		for (ReconUser u : allUsers) {
			Long parentId = u.getParentUserId();
			if (parentId == null || !idsInScope.contains(parentId)) {
				roots.add(u);
			} else {
				byParent.computeIfAbsent(parentId, k -> new ArrayList<>()).add(u);
			}
		}

		List<Map<String, Object>> result = new ArrayList<>();
		for (ReconUser root : roots) {
			result.add(buildUserNode(root, byParent, codeForRow));
		}
		return result;
	}

	private Map<String, Object> buildUserNode(ReconUser u, Map<Long, List<ReconUser>> byParent, String codeForRow) {
		Map<String, Object> node = buildUserRow(u, codeForRow);
		List<ReconUser> children = byParent.getOrDefault(u.getUserId(), Collections.emptyList());
		List<Map<String, Object>> childNodes = new ArrayList<>();
		for (ReconUser c : children) {
			childNodes.add(buildUserNode(c, byParent, codeForRow));
		}
		node.put("children", childNodes);
		return node;
	}

}
