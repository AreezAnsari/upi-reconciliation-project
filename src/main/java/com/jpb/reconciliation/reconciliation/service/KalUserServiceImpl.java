package com.jpb.reconciliation.reconciliation.service;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.stereotype.Service;

import com.jpb.reconciliation.reconciliation.dto.KalApiResponseDto;
import com.jpb.reconciliation.reconciliation.dto.KalUserDto;
import com.jpb.reconciliation.reconciliation.entity.KalCreateUser;
import com.jpb.reconciliation.reconciliation.exception.KalUserCustomException;
import com.jpb.reconciliation.reconciliation.repository.KalUserRepo;

@Service
public class KalUserServiceImpl implements KalUserService {

    @Autowired
    private KalUserRepo repo;

    @Autowired
    private BCryptPasswordEncoder encoder;

    // REGISTER
    @Override
    public KalApiResponseDto register(KalUserDto dto) {

        if (repo.findByUsername(dto.getUsername()).isPresent()) {

            throw new KalUserCustomException(
                    "Username already exists"
            );
        }

        if (repo.findByEmail(dto.getEmail()).isPresent()) {

            throw new KalUserCustomException(
                    "Email already exists"
            );
        }

        KalCreateUser user = new KalCreateUser();

        user.setUsername(dto.getUsername());

        user.setEmail(dto.getEmail());

        user.setPhone(dto.getPhone());

        user.setPassword(
                encoder.encode(dto.getPassword())
        );

        repo.save(user);

        return new KalApiResponseDto(
                true,
                "Employee Registered Successfully",
                null
        );
    }

    // LOGIN
    @Override
    public KalApiResponseDto login(String username, String password) {

        System.out.println("LOGIN USERNAME: " + username);
        System.out.println("LOGIN PASSWORD: " + password);

        KalCreateUser user = repo.findByUsername(username)
                .orElseThrow(() -> new KalUserCustomException("User not found"));

        if (!encoder.matches(password, user.getPassword())) {
            throw new KalUserCustomException("Invalid Password");
        }

        String token = "kalinfotech-token-" + user.getUsername();

        return new KalApiResponseDto(true, "Login Successful", token);
    }  }
