package com.jpb.reconciliation.reconciliation.controller;

import java.time.LocalDateTime;
import java.util.Optional;

import javax.servlet.http.Cookie;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import com.jpb.reconciliation.reconciliation.constants.CommonConstants;
import com.jpb.reconciliation.reconciliation.dto.AuthResponse;
import com.jpb.reconciliation.reconciliation.dto.LoginRequestDto;
import com.jpb.reconciliation.reconciliation.dto.RestWithStatusList;
import com.jpb.reconciliation.reconciliation.dto.TokenRefreshRequest;
import com.jpb.reconciliation.reconciliation.entity.ReconUser;
import com.jpb.reconciliation.reconciliation.google.GoogleRequest;
import com.jpb.reconciliation.reconciliation.google.GoogleService;
import com.jpb.reconciliation.reconciliation.repository.ReconUserRepository;
import com.jpb.reconciliation.reconciliation.security.JwtHelper;
import com.jpb.reconciliation.reconciliation.security.TokenBlacklistService;
import com.jpb.reconciliation.reconciliation.service.CustomUserDetailService;
import com.jpb.reconciliation.reconciliation.service.ReconUserService;

import io.jsonwebtoken.ExpiredJwtException;
import io.jsonwebtoken.MalformedJwtException;
import io.jsonwebtoken.security.SignatureException;

@RestController
@RequestMapping("/auth")
public class AuthController {

	@Autowired
	ReconUserService reconUserService;

	@Autowired
	GoogleService googleService;

	@Autowired
	CustomUserDetailService customUserDetailService;

	@Autowired
	JwtHelper jwtHelper;

	@Autowired
	TokenBlacklistService tokenBlacklistService;

	@Autowired
	ReconUserRepository reconUserRepository;

	private Logger logger = LoggerFactory.getLogger(AuthController.class);

	@PostMapping(value = "/login", produces = CommonConstants.APPLICATION_JSON)
	public ResponseEntity<?> login(@RequestBody LoginRequestDto request, HttpServletResponse response) {
		return reconUserService.login(request, response);
		
	}

	@PostMapping(value = "/refresh-token", produces = CommonConstants.APPLICATION_JSON)
	public ResponseEntity<?> refreshToken(HttpServletRequest request, HttpServletResponse response,
			@RequestBody TokenRefreshRequest tokenRefreshRequest) {
		String refreshToken = null;

		Cookie[] cookies = request.getCookies();
		if (cookies != null) {
			for (Cookie cookie : cookies) {
				if ("refresh_token".equals(cookie.getName())) {
					refreshToken = cookie.getValue();
					break;
				}
			}
		}
		if (refreshToken == null || refreshToken.trim().isEmpty()) {
			logger.warn("Refresh token is null or empty in the cookie for /refresh-token endpoint.");
			return ResponseEntity.status(HttpStatus.UNAUTHORIZED).body("Refresh token not found.");
		}
		String userName = null;
		String refreshTokenJti = null;

		try {
			userName = jwtHelper.getUsernameFromToken(refreshToken);
			refreshTokenJti = jwtHelper.getJtiFromToken(refreshToken);
			logger.info("Successfully extracted username '{}' and JTI '{}' from refresh token.", userName,
					refreshTokenJti);

			if (tokenBlacklistService.isTokenBlacklisted(refreshTokenJti)) {
				logger.warn("Attempted to use a blacklisted refresh token with JTI: {} for user: {}", refreshTokenJti,
						userName);
				clearRefreshCookie(response);
				return ResponseEntity.status(HttpStatus.UNAUTHORIZED)
						.body("Invalid or blacklisted refresh token. Please log in again.");
			}

			UserDetails userDetails = customUserDetailService.loadUserByUsername(userName);

			if (!jwtHelper.validateToken(refreshToken, userName, false)) {
				logger.warn("Refresh token validation failed for user: {}", userName);
				clearRefreshCookie(response);
				return ResponseEntity.status(HttpStatus.UNAUTHORIZED)
						.body("Invalid or expired refresh token. Please log in again.");
			}

			String newAccessToken = jwtHelper.generateToken(userDetails);
			return ResponseEntity.ok(new AuthResponse(newAccessToken, refreshToken));
		} catch (UsernameNotFoundException e) {
			logger.warn("User '{}' found in refresh token but not in system. Clearing cookie.", userName);
			clearRefreshCookie(response);
			return ResponseEntity.status(HttpStatus.UNAUTHORIZED).body("User not found.");
		} catch (ExpiredJwtException e) {
			logger.warn("Refresh token for user '{}' is expired. Clearing cookie. Message: {}",
					e.getClaims().getSubject(), e.getMessage());
			return ResponseEntity.status(HttpStatus.UNAUTHORIZED).body("Refresh token expired. Please log in again.");
		} catch (SignatureException | MalformedJwtException e) {
			logger.warn("Refresh token is invalid or malformed for user: {}. Clearing cookie. Error: {}", userName,
					e.getMessage());
			clearRefreshCookie(response);
			return ResponseEntity.status(HttpStatus.UNAUTHORIZED).body("Invalid refresh token. Please log in again.");
		} catch (IllegalArgumentException e) {
			logger.error("Illegal argument provided for JWT parsing: {}. Clearing cookie. Full stack trace:",
					e.getMessage(), e);
			clearRefreshCookie(response);
			return ResponseEntity.status(HttpStatus.BAD_REQUEST).body("Invalid token format.");
		} catch (Exception e) {
			logger.error("An unexpected error occurred during token refresh for user: {}. Clearing cookie. Error: {}",
					userName, e.getMessage(), e);
			clearRefreshCookie(response);
			return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body("An internal error occurred.");
		}
	}

	@PostMapping(value = "/revoke-token", produces = CommonConstants.APPLICATION_JSON)
	public ResponseEntity<?> logout(@RequestBody TokenRefreshRequest request, HttpServletResponse response) {
		String refreshToken = request.getRefreshToken();
		String userNameFromToken = jwtHelper.getUsernameFromToken(refreshToken);
		logger.info("User name from refresh token::" + userNameFromToken);
		if (!userNameFromToken.isEmpty()) {
			Optional<ReconUser> user = reconUserRepository.findByUserName(userNameFromToken);
			if (user.isPresent()) {
				ReconUser getUser = user.get();
				getUser.setLastLoginDateTime(LocalDateTime.now());
				reconUserRepository.save(getUser);
			} else {
				logger.info("User is not found");
			}
		}

//		String refreshToken = tokenRefreshRequest.getRefreshToken();
		if (refreshToken == null || refreshToken.trim().isEmpty()) {
			logger.warn("Revoke token request received with null or empty refresh token in body.");
			return ResponseEntity.status(HttpStatus.BAD_REQUEST).body("Refresh token is missing from request.");
		}

		String jti = null;
		try {
			jti = jwtHelper.getJtiFromToken(refreshToken);
			tokenBlacklistService.blacklistToken(jti);
			logger.info("Successfully blacklisted refresh token with JTI: {}", jti);
			clearRefreshCookie(response);

		} catch (ExpiredJwtException e) {
			logger.warn("Attempted to revoke an already expired refresh token. User: {}");
			clearRefreshCookie(response);
			return ResponseEntity.status(HttpStatus.OK).body("Token already expired, but cleared from client.");
		} catch (Exception e) {
			logger.error("Error revoking token with JTI: {}. Error: {}", jti);
			return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body("Error revoking token.");
		}
		return new ResponseEntity<>("Token revoked and cookie cleared", HttpStatus.OK);
	}

	private void clearRefreshCookie(HttpServletResponse response) {
		Cookie cookie = new Cookie("refresh_token", null);
		cookie.setHttpOnly(true);
		cookie.setPath("/");
		cookie.setMaxAge(0);
		response.addCookie(cookie);
		logger.info("Refresh token cookie cleared.");
	}

	@PostMapping(value = "/google", produces = CommonConstants.APPLICATION_JSON)
	public ResponseEntity<RestWithStatusList> loginWithGoogle(@RequestBody GoogleRequest googleRequest,
			HttpServletResponse response) {
		return googleService.authenticateWithGoogle(googleRequest, response);
	}

}