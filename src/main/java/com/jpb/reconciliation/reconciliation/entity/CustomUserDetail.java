package com.jpb.reconciliation.reconciliation.entity;

import java.util.Collection;

import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.userdetails.UserDetails;

public class CustomUserDetail implements UserDetails {

	private static final long serialVersionUID = 1L;
	private KalAdmin KalAdmin;

	public CustomUserDetail(KalAdmin KalAdmin) {
		this.KalAdmin = KalAdmin;
	}

	@Override
	public Collection<? extends GrantedAuthority> getAuthorities() {
		return null;
	}

	@Override
	public String getPassword() {
		KalAdminPasswordManager KalAdminPasswordManager = KalAdmin.getPasswordManager();
		System.out.println("!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!-------------" + KalAdminPasswordManager.getUserPassword() );
		return KalAdminPasswordManager.getUserPassword();
	}

	@Override
	public String getUsername() {
		return KalAdmin.getUserName();
	}

	@Override
	public boolean isAccountNonExpired() {
		// TODO Auto-generated method stub
		return true;
	}

	@Override
	public boolean isAccountNonLocked() {
		// TODO Auto-generated method stub
		return true;
	}

	@Override
	public boolean isCredentialsNonExpired() {
		// TODO Auto-generated method stub
		return true;
	}

	@Override
	public boolean isEnabled() {
		// TODO Auto-generated method stub
		return true;
	}
}
