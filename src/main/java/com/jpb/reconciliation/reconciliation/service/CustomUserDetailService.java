package com.jpb.reconciliation.reconciliation.service;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import org.springframework.stereotype.Service;

import com.jpb.reconciliation.reconciliation.entity.CustomUserDetail;
import com.jpb.reconciliation.reconciliation.entity.ReconUser;
import com.jpb.reconciliation.reconciliation.repository.ReconUserRepository;

@Service
public class CustomUserDetailService implements UserDetailsService {

	Logger logger = LoggerFactory.getLogger(CustomUserDetailService.class);

	@Autowired
	private ReconUserRepository reconUserRepository;

	@Override
	public UserDetails loadUserByUsername(String username) throws UsernameNotFoundException {
		ReconUser reconUser = reconUserRepository.findByUserName(username)
				.orElseThrow(() -> new UsernameNotFoundException("User not found with given username " + username));
		return new CustomUserDetail(reconUser);
	}

	public UserDetails loadUserByUserEmail(String email) throws UsernameNotFoundException {
		ReconUser reconUser = reconUserRepository.findByEmailId(email)
				.orElseThrow(() -> new UsernameNotFoundException("User not found with given username " + email));
		return new CustomUserDetail(reconUser);
	}

}
