package com.jpb.reconciliation.reconciliation.service;

import com.jpb.reconciliation.reconciliation.dto.AddUserRequest;
import com.jpb.reconciliation.reconciliation.dto.AddUserResponse;
import com.jpb.reconciliation.reconciliation.dto.RestWithStatusList;
import com.jpb.reconciliation.reconciliation.entity.AddUser;
import com.jpb.reconciliation.reconciliation.mapper.AddUserMapper;
import com.jpb.reconciliation.reconciliation.repository.AddUserRepository;

import lombok.RequiredArgsConstructor;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class AddUserServiceImpl implements AddUserService {

    private final AddUserRepository userRepository;
    private final PasswordEncoder passwordEncoder;

    @Override
    public RestWithStatusList createUser(AddUserRequest request, String createdBy, String instCode) {

        // 🔴 Validation
        if (userRepository.existsByUsername(request.getUsername())) {
            throw new RuntimeException("Username already exists");
        }

        if (userRepository.existsByEmail(request.getEmail())) {
            throw new RuntimeException("Email already exists");
        }

        // 🟢 Convert DTO → Entity
        AddUser user = AddUserMapper.toEntity(
                request,
                passwordEncoder,
                "SYSTEM",         // you can replace with logged-in user
                "DEFAULT_INST"
        );

        userRepository.save(user);

        // 🟢 Convert Entity → Response
        AddUserResponse response = AddUserMapper.toResponse(user);

        return RestWithStatusList.builder()
                .status("SUCCESS")
                .statusMsg("User created successfully")
                .data(java.util.Arrays.asList(response))
                .build();
    }

    @Override
    public RestWithStatusList getUserById(Long id) {

        AddUser user = userRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("User not found"));

        AddUserResponse response = AddUserMapper.toResponse(user);

        return RestWithStatusList.builder()
                .status("SUCCESS")
                .statusMsg("User fetched successfully")
                .data(java.util.Arrays.asList(response))
                .build();
    }

    @Override
    public RestWithStatusList getAllUsers() {

        List<AddUserResponse> users = userRepository.findAll()
                .stream()
                .map(AddUserMapper::toResponse)
                .collect(Collectors.toList());

        return RestWithStatusList.builder()
                .status("SUCCESS")
                .statusMsg("Users fetched successfully")
                .data(new java.util.ArrayList<>(users))
                .build();
    }

	@Override
	public RestWithStatusList updateUser(Long id, AddUserRequest request) {
		AddUser user = userRepository.findById(id)
	            .orElseThrow(() -> new RuntimeException("User not found"));

	    user.setUsername(request.getUsername());
	    user.setEmail(request.getEmail());
	    user.setDepartment(request.getDepartment());
	    user.setDesignation(request.getDesignation());
	    user.setMobileNumber(request.getMobileNumber());
	    user.setRole(AddUser.Role.valueOf(request.getRole()));
	    user.setUserType(AddUser.UserType.valueOf(request.getUserType()));

	    userRepository.save(user);

	    return RestWithStatusList.builder()
	            .status("SUCCESS")
	            .statusMsg("User updated successfully")
	            .data(java.util.Arrays.asList(AddUserMapper.toResponse(user)))
	            .build();
	}

	@Override
	public RestWithStatusList deactivateUser(Long id) {
		AddUser user = userRepository.findById(id)
	            .orElseThrow(() -> new RuntimeException("User not found"));

	    user.setStatus(AddUser.UserStatus.INACTIVE);
	    userRepository.save(user);

	    return RestWithStatusList.builder()
	            .status("SUCCESS")
	            .statusMsg("User deactivated successfully")
	            .data(java.util.Collections.emptyList())
	            .build();
	}

	@Override
	public RestWithStatusList searchUsers(String instCode, String term) {
		 List<AddUserResponse> users = userRepository.searchUsers(instCode, term)
		            .stream()
		            .map(AddUserMapper::toResponse)
		            .collect(Collectors.toList());

		    return RestWithStatusList.builder()
		            .status("SUCCESS")
		            .statusMsg("Search completed")
		            .data(new java.util.ArrayList<>(users))
		            .build();
	}

	@Override
	public RestWithStatusList getUsersByInstitution(String instCode) {
		 List<AddUserResponse> users = userRepository.findAll()
		            .stream()
		            .filter(u -> instCode.equals(u.getInstitutionCode()))
		            .map(AddUserMapper::toResponse)
		            .collect(Collectors.toList());

		    return RestWithStatusList.builder()
		            .status("SUCCESS")
		            .statusMsg("Users fetched successfully")
		            .data(new java.util.ArrayList<>(users))
		            .build();
	}
}