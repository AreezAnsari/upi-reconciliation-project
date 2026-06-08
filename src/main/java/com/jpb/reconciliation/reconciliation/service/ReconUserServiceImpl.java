package com.jpb.reconciliation.reconciliation.service;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

import javax.servlet.http.Cookie;
import javax.servlet.http.HttpServletResponse;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.BadCredentialsException;
import org.springframework.security.authentication.DisabledException;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;

import com.jpb.reconciliation.reconciliation.config.SchedulerConfig;
import com.jpb.reconciliation.reconciliation.constants.CommonConstants;
import com.jpb.reconciliation.reconciliation.constants.UserConstants;
import com.jpb.reconciliation.reconciliation.dto.AuthResponse;
import com.jpb.reconciliation.reconciliation.dto.LoginRequestDto;
import com.jpb.reconciliation.reconciliation.dto.ReconUserDto;
import com.jpb.reconciliation.reconciliation.dto.ReconUserResponseDto;
import com.jpb.reconciliation.reconciliation.dto.ResponseDto;
import com.jpb.reconciliation.reconciliation.dto.RestWithStatusList;
import com.jpb.reconciliation.reconciliation.dto.UserPasswordChangeRequest;
import com.jpb.reconciliation.reconciliation.entity.KalAdminPasswordManager;
import com.jpb.reconciliation.reconciliation.entity.KalAdmin;
import com.jpb.reconciliation.reconciliation.entity.Role;
import com.jpb.reconciliation.reconciliation.exception.ResourceNotFoundException;
import com.jpb.reconciliation.reconciliation.mapper.ReconUserMapper;
import com.jpb.reconciliation.reconciliation.repository.PasswordManagerRepository;
import com.jpb.reconciliation.reconciliation.repository.ProcessMasterRepository;
import com.jpb.reconciliation.reconciliation.repository.KalAdminRepository;
import com.jpb.reconciliation.reconciliation.repository.RoleManageRepository;
import com.jpb.reconciliation.reconciliation.repository.RoleRepository;
import com.jpb.reconciliation.reconciliation.security.JwtHelper;

import io.jsonwebtoken.JwtException;

@Service
public class ReconUserServiceImpl implements ReconUserService {

	private final ProcessMasterRepository processMasterRepository;

	private final SchedulerConfig schedulerConfig;

	@Autowired
	KalAdminRepository KalAdminRepository;

	@Autowired
	RoleRepository roleRepository;

	@Autowired
	PasswordEncoder passwordEncoder;

	@Autowired
	CustomUserDetailService customUserDetailService;

	@Autowired
	AuditLogManagerService auditLogManagerService;

	@Autowired
	JwtHelper helper;

	@Autowired
	PasswordManagerRepository passwordManagerRepository;

	@Autowired
	RoleManageRepository roleManageRepository;

	@Autowired
	private AuthenticationManager manager;

	private Logger logger = LoggerFactory.getLogger(ReconUserServiceImpl.class);

	ReconUserServiceImpl(SchedulerConfig schedulerConfig, ProcessMasterRepository processMasterRepository) {
		this.schedulerConfig = schedulerConfig;
		this.processMasterRepository = processMasterRepository;
	}

	@Override
	public ResponseEntity<RestWithStatusList> createUser(ReconUserDto reconUserDto) {
		RestWithStatusList restWithStatusList = null;

		Boolean existsUser = KalAdminRepository.existsByUserNameAndEmailId(reconUserDto.getUserName(),
				reconUserDto.getEmailId());
		logger.info("Check user present into records :::::::::" + existsUser);
		if (existsUser) {
			restWithStatusList = new RestWithStatusList("FAILURE", "User Already Exists", null);
			return new ResponseEntity<>(restWithStatusList, HttpStatus.BAD_REQUEST);
		} else {
			Role userRole = roleManageRepository.findByRoleId(reconUserDto.getRoleId());
			KalAdmin KalAdmin = ReconUserMapper.mapToReconUser(reconUserDto, new KalAdmin());
//			Role role = saveUserRole(reconUserDto.getRoleName());
//			roleRepository.save(role);
			KalAdmin.setRole(userRole);
			KalAdminPasswordManager KalAdminPasswordManager = saveUserPasswordData(reconUserDto.getUserPassword());
			KalAdminPasswordManager.setKalAdmin(KalAdmin);
			KalAdmin.setPasswordManager(KalAdminPasswordManager);
			logger.info("User data :::::::::" + KalAdmin.toString());
			if (KalAdmin != null) {
				KalAdminRepository.save(KalAdmin);
				KalAdminRepository.flush();
				restWithStatusList = new RestWithStatusList("SUCCESS", "User Created Succussfully", null);
			}
		}
		return new ResponseEntity<>(restWithStatusList, HttpStatus.OK);
	}

//	private Role saveUserRole(String roleName) {
//		Role role = new Role();
//		role.setRoleName(roleName);
//		if (roleName.equalsIgnoreCase("JPB")) {
//			role.setRoleCode("ROLE_JPB");
//		} else if (roleName.equalsIgnoreCase("bankadmin")) {
//			role.setRoleCode("ROLE_ADMIN");
//		} else if (roleName.equalsIgnoreCase("VIEW")) {
//			role.setRoleCode("ROLE_VIEW");
//		} else if (roleName.equalsIgnoreCase("AppSupport")) {
//			role.setRoleCode("ROLE_APPSUPPORT");
//		} else if (roleName.equalsIgnoreCase("RPSL")) {
//			role.setRoleCode("ROLE_RPSL");
//		}
//		role.setCreatedAt(LocalDateTime.now());
//		return role;
//	}

	private KalAdminPasswordManager saveUserPasswordData(String userPassword) {
		KalAdminPasswordManager KalAdminPasswordManager = new KalAdminPasswordManager();
		KalAdminPasswordManager.setUserPassword(passwordEncoder.encode(userPassword));
		KalAdminPasswordManager.setExpirationDate(LocalDateTime.now());
		KalAdminPasswordManager.setCreatedAt(LocalDateTime.now());
		return KalAdminPasswordManager;
	}

	@Override
	public ResponseEntity<RestWithStatusList> getUserByUserId(Long userId) {
		RestWithStatusList restWithStatusList;
		List<Object> addReconUser = new ArrayList<>();

		KalAdmin KalAdmin = KalAdminRepository.findByUserId(userId)
				.orElseThrow(() -> new ResourceNotFoundException("USER NOT FOUND"));

		ReconUserResponseDto reconUserResponseDto = ReconUserMapper.mapToReconUserResponseDto(KalAdmin,
				new ReconUserResponseDto());
		logger.info("User found in given user id :::" + userId + KalAdmin);
		if (KalAdmin != null) {
			addReconUser.add(reconUserResponseDto);
			restWithStatusList = new RestWithStatusList("SUCCESS", "User found", addReconUser);
			return new ResponseEntity<>(restWithStatusList, HttpStatus.OK);
		}
		restWithStatusList = new RestWithStatusList("FAILURE", "User Not Found", null);
		return new ResponseEntity<>(restWithStatusList, HttpStatus.NOT_FOUND);
	}

	@Override
	public ResponseEntity<?> login(LoginRequestDto request, HttpServletResponse response) {
		RestWithStatusList restWithStatusList;
		this.doAuthenticate(request.getUserName(), request.getUserPassword());
		UserDetails userDetails = customUserDetailService.loadUserByUsername(request.getUserName());
		System.out.println("Login User Details ::::::::::" + userDetails);

		String token = this.helper.generateToken(userDetails);
		Optional<KalAdmin> KalAdmin = KalAdminRepository.findByUserName(userDetails.getUsername());
		System.out.println("User Details ::::::::::" + KalAdmin);
		KalAdmin user = KalAdmin.get();

		if ("Y".equals(user.getApprovedYn()) && user.getUserStatus().equalsIgnoreCase("active")) {
			KalAdminPasswordManager KalAdminPasswordManager = user.getPasswordManager();
			KalAdminPasswordManager.setToken(token);
			user.setPasswordManager(KalAdminPasswordManager);
			KalAdminRepository.save(user);

			try {
				String userName = helper.getUsernameFromToken(token);
				String refreshToken = helper.generateTokenForRefresh(userName);
				Cookie refreshCookie = new Cookie("refresh_token", refreshToken);
				refreshCookie.setHttpOnly(true);
				refreshCookie.setMaxAge((int) (JwtHelper.JWT_TOKEN_REFRESH / 1000));
				refreshCookie.setPath("/");
				response.addCookie(refreshCookie);
				auditLogManagerService.loginAudit(user, token, refreshToken);
				return ResponseEntity.ok(new AuthResponse(token, refreshToken));
			} catch (JwtException e) {
				return ResponseEntity.status(403).body("Invalid or expired refresh token");
			}
		} else {
//			restWithStatusList = new RestWithStatusList("SUCCESS", "User is not approved or inactive", null);
			ResponseDto responseDto = new ResponseDto(CommonConstants.STATUS_400, CommonConstants.MESSAGE_400);
			return new ResponseEntity<>(responseDto, HttpStatus.BAD_REQUEST);
		}
	}

	private void doAuthenticate(String userName, String userPassword) {

		UsernamePasswordAuthenticationToken authentication = new UsernamePasswordAuthenticationToken(userName,
				userPassword);
		logger.info("UsernamePasswordAuthenticationToken Response is ::::::::::::::::::::::::::::::" + authentication);
		try {
			Authentication authData = manager.authenticate(authentication);
			logger.info("AUTHENTICATION DATA :::::::::::::::::" + authData);
		} catch (BadCredentialsException e) {
			throw new BadCredentialsException("Invalid Username or Password !");
		} catch (DisabledException e) {
			throw new DisabledException("User is not approved.");
		}
	}

	@Override
	public ResponseEntity<?> getUserData(String username) {
		RestWithStatusList restWithStatusList;
		List<Object> addReconUser = new ArrayList<>();

		KalAdmin KalAdmin = KalAdminRepository.findByUserName(username)
				.orElseThrow(() -> new ResourceNotFoundException("USER NOT FOUND :" + username));

		ReconUserResponseDto reconUserResponseDto = ReconUserMapper.mapToReconUserResponseDto(KalAdmin,
				new ReconUserResponseDto());

		logger.info("User found in given user id :::" + username + KalAdmin);
		addReconUser.add(reconUserResponseDto);
		restWithStatusList = new RestWithStatusList("SUCCESS", "USER FOUND SUCCESSFULLY", addReconUser);
		return new ResponseEntity<>(restWithStatusList, HttpStatus.OK);
	}

	@Override
	public ResponseEntity<ResponseDto> removeUser(Long userId) {
		KalAdmin KalAdmin = KalAdminRepository.findByUserId(userId)
				.orElseThrow(() -> new ResourceNotFoundException("User not found"));
		KalAdminRepository.deleteById(KalAdmin.getUserId());
		roleRepository.deleteById(userId);
		return new ResponseEntity<>(new ResponseDto(UserConstants.STATUS_200, "User Successfully Deleted"),
				HttpStatus.OK);
	}

	@Override
	public ResponseEntity<ResponseDto> changePassword(UserPasswordChangeRequest changePasswordRequest) {

		Optional<KalAdmin> findUser = KalAdminRepository.findByUserId(changePasswordRequest.getUserId());
		logger.info("USER BY USER ID :::::::::::::::::::::::::::::::" + findUser.get());
		KalAdmin UserData = findUser.get();

		KalAdminPasswordManager password = UserData.getPasswordManager();

		if (password != null) {
			logger.info("USER PASSWORD :::::::::" + password.getUserPassword());
			logger.info("OLD PASSWORD ::::::::::::::" + changePasswordRequest.getOldPassword());
			logger.info("NEW PASSWORD :::::::::" + changePasswordRequest.getNewPassword());
			logger.info("NEW CONFIRM PASSWORD :::::::::::::" + changePasswordRequest.getConfirmNewPassword());

			boolean checkPassword = passwordEncoder.matches(changePasswordRequest.getOldPassword(),
					password.getUserPassword());
			logger.info("Checking password :::::::::::" + checkPassword);
			if (checkPassword) {
				String newEncodedPassword = passwordEncoder.encode(changePasswordRequest.getNewPassword());
				password.setUserPassword(newEncodedPassword);
				password.setExpirationDate(LocalDateTime.now());
				password.setCreatedAt(LocalDateTime.now());
				UserData.setPasswordManager(password);
				KalAdminRepository.save(UserData);
			} else {
				return ResponseEntity.status(HttpStatus.BAD_REQUEST)
						.body(new ResponseDto("400", "Please enter valid old password!!"));
			}
		}
		return ResponseEntity.status(HttpStatus.OK).body(new ResponseDto("200", "New Password Changed Successfully"));
	}

	@Override
	public ResponseEntity<RestWithStatusList> updateUser(ReconUserDto userUpdateRequest) {
		RestWithStatusList restWithStatusList = null;
		Optional<KalAdmin> userDetails = KalAdminRepository.findByUserId(userUpdateRequest.getUserId());

		KalAdmin user = userDetails.get();
		if (user != null) {
			KalAdmin updatedUserDetails = ReconUserMapper.mapToReconUserUpdate(userUpdateRequest, user);
			Optional<Role> getRole = roleRepository.findById(userUpdateRequest.getRoleId());
			Role getRoleData = getRole.get();
			updatedUserDetails.setRole(getRoleData);
			KalAdminRepository.save(updatedUserDetails);
			restWithStatusList = new RestWithStatusList("SUCCESS", "User Update Successfully", null);
		} else {
			restWithStatusList = new RestWithStatusList("FAILURE", "Please enter valid user details", null);
			return new ResponseEntity<RestWithStatusList>(restWithStatusList, HttpStatus.NOT_FOUND);
		}
		return new ResponseEntity<RestWithStatusList>(restWithStatusList, HttpStatus.OK);
	}

	@Override
	public ResponseEntity<RestWithStatusList> getApprovedUSers(String approvedYN) {
		RestWithStatusList restWithStatusList = null;
		List<Object> userDetailsList = new ArrayList<>();
		List<KalAdmin> allUsersIsPresent = KalAdminRepository.findByApprovedYn(approvedYN);
		logger.info("USer Details ::::::::" + allUsersIsPresent);
		if (allUsersIsPresent.isEmpty()) {
			restWithStatusList = new RestWithStatusList("FAILURE", "User Details Is Not Found", userDetailsList);
			return new ResponseEntity<RestWithStatusList>(restWithStatusList, HttpStatus.NOT_FOUND);
		} else {
			List<ReconUserResponseDto> userDetailsListData = ReconUserMapper
					.mapToReconUsersResponseDto(allUsersIsPresent);
			userDetailsList.addAll(userDetailsListData);
			restWithStatusList = new RestWithStatusList("SUCCESS", "User Update Successfully", userDetailsList);
		}
		return new ResponseEntity<RestWithStatusList>(restWithStatusList, HttpStatus.OK);
	}

	@Override
	public ResponseEntity<RestWithStatusList> approveOrRejectUser(ReconUserDto approveUserRequest,UserDetails userDetails) {
		RestWithStatusList restWithStatusList = null;
		if (approveUserRequest == null) {
			restWithStatusList = new RestWithStatusList("FAILURE", "Please select user to approve", null);
			return new ResponseEntity<RestWithStatusList>(restWithStatusList, HttpStatus.BAD_REQUEST);
		} else {
			Optional<KalAdmin> user = KalAdminRepository.findByUserId(approveUserRequest.getUserId());
			KalAdmin getUser = user.get();
			approveUserRequest.setApprovedBy(userDetails.getUsername());
			KalAdmin approvedOrRejectUser = ReconUserMapper.mapToApproveRejectReconUser(approveUserRequest, getUser);
			logger.info("User approved or reject user :::::::::::" + approvedOrRejectUser);
			KalAdminRepository.save(approvedOrRejectUser);
			restWithStatusList = new RestWithStatusList("SUCCESS", "User Approved Successfully", null);
		}
		return new ResponseEntity<RestWithStatusList>(restWithStatusList, HttpStatus.OK);
	}

	@Override
	public ResponseEntity<RestWithStatusList> getAllUsers() {
		RestWithStatusList restWithStatusList;
		List<Object> userList = new ArrayList<>();

		List<KalAdmin> allUserExists = KalAdminRepository.findAll();
		List<ReconUserResponseDto> users = ReconUserMapper.mapToReconUsersResponseDto(allUserExists);
		if (!allUserExists.isEmpty()) {
			userList.addAll(users);
			restWithStatusList = new RestWithStatusList("SUCCESS", "Users found in the system", userList);
		} else {
			restWithStatusList = new RestWithStatusList("FAILURE", "No users found in the system", userList);
			return new ResponseEntity<RestWithStatusList>(restWithStatusList, HttpStatus.NOT_FOUND);
		}
		return new ResponseEntity<RestWithStatusList>(restWithStatusList, HttpStatus.OK);
	}

}
