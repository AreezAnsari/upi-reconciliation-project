package com.jpb.reconciliation.reconciliation.service;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.stereotype.Service;

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

    @Override
    public String register(KalUserDto dto) {

        if (repo.findByUsername(dto.getUsername()).isPresent()) {
            throw new KalUserCustomException("Username already exists");
        }

        KalCreateUser emp = new KalCreateUser();
        emp.setUsername(dto.getUsername());
        emp.setEmail(dto.getEmail());
        emp.setPhone(dto.getPhone());
        emp.setPassword(encoder.encode(dto.getPassword()));

        repo.save(emp);

        return "Employee Registered Successfully";
    }

    @Override
    public String login(String username, String password) {

        KalCreateUser emp = repo.findByUsername(username)
                .orElseThrow(() -> new KalUserCustomException("User not found"));

        if (!encoder.matches(password, emp.getPassword())) {
            throw new KalUserCustomException("Invalid Password");
        }

        return "Login Successful";
    }
}