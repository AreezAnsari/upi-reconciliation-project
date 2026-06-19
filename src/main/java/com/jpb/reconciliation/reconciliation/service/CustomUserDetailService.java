package com.jpb.reconciliation.reconciliation.service;

import java.util.Collections;
import java.util.Optional;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.userdetails.User;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import org.springframework.stereotype.Service;

import com.jpb.reconciliation.reconciliation.entity.AddUser;
import com.jpb.reconciliation.reconciliation.entity.BranchAdmin;
import com.jpb.reconciliation.reconciliation.entity.CustomUserDetail;
import com.jpb.reconciliation.reconciliation.entity.KalAdmin;
import com.jpb.reconciliation.reconciliation.entity.MainAdmin;
import com.jpb.reconciliation.reconciliation.repository.AddUserRepository;
import com.jpb.reconciliation.reconciliation.repository.BranchAdminRepository;
import com.jpb.reconciliation.reconciliation.repository.MainAdminRepository;
import com.jpb.reconciliation.reconciliation.repository.KalAdminRepository;

@Service
public class CustomUserDetailService implements UserDetailsService {

	Logger logger = LoggerFactory.getLogger(CustomUserDetailService.class);

	@Autowired
	private KalAdminRepository KalAdminRepository;

	@Autowired
	private MainAdminRepository mainAdminRepository;

	@Autowired
	private BranchAdminRepository branchAdminRepository;

	@Autowired
	private AddUserRepository addUserRepository;

	/**
	 * Called by JwtAuthenticationFilter to validate every request's Bearer token.
	 *
	 * Strategy:
	 * 1. Try regular-user table by username
	 * 2. Try regular-user table by email (some tokens use email as subject)
	 * 3. Try BANK_ADMIN table by email (super-user OTP tokens use email as subject)
	 * 4. Try BANK_ADMIN table by username
	 *
	 * If none found → UsernameNotFoundException → 401
	 */
	@Override
	public UserDetails loadUserByUsername(String username) throws UsernameNotFoundException {

		// ── Step 1: Regular user — by username ──
		Optional<KalAdmin> byUsername = KalAdminRepository.findByUserName(username);
		if (byUsername.isPresent()) {
			return new CustomUserDetail(byUsername.get());
		}

		// ── Step 2: Regular user — by email (some tokens store email as subject) ──
		try {
			Optional<KalAdmin> byEmail = KalAdminRepository.findByEmailId(username);
			if (byEmail.isPresent()) {
				return new CustomUserDetail(byEmail.get());
			}
		} catch (Exception ignored) {
			// findByEmailId may throw if column doesn't match — safe to ignore
		}

		// ── Step 3: Super-user — by email (OTP token uses email as subject) ──
		Optional<MainAdmin> superByEmail = mainAdminRepository.findFirstByEmail(username);
		if (superByEmail.isPresent()) {
			MainAdmin su = superByEmail.get();
			logger.debug("loadUserByUsername — super-user found by email: {}", username);
			return buildSuperUserDetails(username, su);
		}

		// ── Step 4: Super-user — by username (fallback) ──
		Optional<MainAdmin> superByUsername = mainAdminRepository.findFirstByUsername(username);
		if (superByUsername.isPresent()) {
			MainAdmin su = superByUsername.get();
			logger.debug("loadUserByUsername — super-user found by username: {}", username);
			return buildSuperUserDetails(username, su);
		}

		// ── Step 5: Branch Admin — by email (OTP token uses email as subject) ──
		Optional<BranchAdmin> branchAdminByEmail = branchAdminRepository.findFirstByEmail(username);
		if (branchAdminByEmail.isPresent()) {
			BranchAdmin ba = branchAdminByEmail.get();
			logger.debug("loadUserByUsername — branch-admin found by email: {}", username);
			return User.builder()
					.username(username)
					.password(ba.getPassword() != null ? ba.getPassword() : "")
					.authorities(Collections.singletonList(
							new SimpleGrantedAuthority("ROLE_BRANCH_ADMIN")))
					.build();
		}

		// ── Step 6: Branch Admin — by username (fallback) ──
		Optional<BranchAdmin> branchAdminByUsername = branchAdminRepository.findFirstByUsername(username);
		if (branchAdminByUsername.isPresent()) {
			BranchAdmin ba = branchAdminByUsername.get();
			logger.debug("loadUserByUsername — branch-admin found by username: {}", username);
			return User.builder()
					.username(username)
					.password(ba.getPassword() != null ? ba.getPassword() : "")
					.authorities(Collections.singletonList(
							new SimpleGrantedAuthority("ROLE_BRANCH_ADMIN")))
					.build();
		}

		// ── Step 7: Org user (AddUser) — by username ──
		Optional<AddUser> addUserOpt = addUserRepository.findByUsername(username);
		if (addUserOpt.isPresent()) {
			AddUser au = addUserOpt.get();
			logger.debug("loadUserByUsername — org-user found by username: {}", username);
			return User.builder()
					.username(au.getUsername())
					.password(au.getDefaultPassword() != null ? au.getDefaultPassword() : "")
					.authorities(Collections.singletonList(new SimpleGrantedAuthority("ROLE_USER")))
					.build();
		}

		throw new UsernameNotFoundException("User not found: " + username);
	}

	public UserDetails loadUserByUserEmail(String email) throws UsernameNotFoundException {
		KalAdmin KalAdmin = KalAdminRepository.findByEmailId(email)
				.orElseThrow(() -> new UsernameNotFoundException("User not found with given email " + email));
		return new CustomUserDetail(KalAdmin);
	}

	// ── Build a minimal UserDetails for super-user (no DB password needed for JWT) ──
	private UserDetails buildSuperUserDetails(String subject, MainAdmin su) {
		return User.builder()
				.username(subject)
				.password(su.getPassword() != null ? su.getPassword() : "")
				.authorities(Collections.singletonList(
						new SimpleGrantedAuthority("ROLE_BANK_ADMIN")))
				.build();
	}

}
