package com.jpb.reconciliation.reconciliation.service;

import java.time.LocalDateTime;
import java.util.*;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.jpb.reconciliation.reconciliation.constants.EnableStatus;
import com.jpb.reconciliation.reconciliation.dto.InstitutionDTO;
import com.jpb.reconciliation.reconciliation.entity.*;
import com.jpb.reconciliation.reconciliation.exception.*;
import com.jpb.reconciliation.reconciliation.repository.InstitutionRepository;

import lombok.RequiredArgsConstructor;

@Service
@RequiredArgsConstructor
@Transactional
public class InstitutionService {

    private final InstitutionRepository institutionRepository;

    // =========================================================
    // CREATE
    // =========================================================

    public Institution createInstitution(
            InstitutionDTO dto) {

        validateDuplicates(dto);

        Institution institution = mapToEntity(dto);

        return institutionRepository.save(institution);
    }

    // =========================================================
    // GET ALL
    // =========================================================

    public List<Institution> getAllInstitutions() {
        return institutionRepository.findAll();
    }

    // =========================================================
    // GET BY ID
    // =========================================================

    public Institution getInstitutionById(Long id) {

        return institutionRepository.findById(id)
                .orElseThrow(() ->
                        new ResourceNotFoundException(
                                "Institution not found"));
    }

    // =========================================================
    // UPDATE
    // =========================================================

    public Institution updateInstitution(
            Long id,
            InstitutionDTO dto) {

        Institution existing =
                getInstitutionById(id);

        existing.setInstitutionNameFull(
                dto.getInstitutionNameFull());

        existing.setInstitutionNameShort(
                dto.getInstitutionNameShort());

        existing.setPrimaryEmail(
                dto.getPrimaryEmail());

        existing.setPrimaryMobile(
                dto.getPrimaryMobile());

        existing.setStatus(dto.getStatus());

        return institutionRepository.save(existing);
    }

    // =========================================================
    // UPDATE STATUS
    // =========================================================

    public Institution updateStatus(
            Long id,
            EnableStatus status) {

        Institution institution =
                getInstitutionById(id);

        institution.setStatus(status);

        return institutionRepository.save(institution);
    }

    // =========================================================
    // DELETE
    // =========================================================

    public void deleteInstitution(Long id) {

        Institution institution =
                getInstitutionById(id);

        institution.setStatus(
                EnableStatus.INACTIVE);

        institutionRepository.save(institution);
    }

    // =========================================================
    // HARD DELETE
    // =========================================================

    public void hardDeleteInstitution(Long id) {

        Institution institution =
                getInstitutionById(id);

        institutionRepository.delete(institution);
    }

    // =========================================================
    // DUPLICATE VALIDATION
    // =========================================================

    private void validateDuplicates(
            InstitutionDTO dto) {

        institutionRepository
                .findByInstitutionNameFull(
                        dto.getInstitutionNameFull())
                .ifPresent(data -> {
                    throw new DuplicateResourceException(
                            "Institution already exists");
                });

        institutionRepository
                .findByPrimaryEmail(
                        dto.getPrimaryEmail())
                .ifPresent(data -> {
                    throw new DuplicateResourceException(
                            "Primary email already exists");
                });
    }

    // =========================================================
    // MAP DTO TO ENTITY
    // =========================================================

    private Institution mapToEntity(
            InstitutionDTO dto) {

        Institution institution =
                Institution.builder()

                        .institutionCode(
                                dto.getInstitutionCode())

                        .institutionNameFull(
                                dto.getInstitutionNameFull())

                        .institutionNameShort(
                                dto.getInstitutionNameShort())

                        .bankType(dto.getBankType())

                        .bankLogoName(
                                dto.getBankLogoName())

                        .bankLogoPath(
                                dto.getBankLogoPath())

                        .regAddressLine1(
                                dto.getRegAddressLine1())

                        .regAddressLine2(
                                dto.getRegAddressLine2())

                        .regAddressLine3(
                                dto.getRegAddressLine3())

                        .regCity(dto.getRegCity())
                        .regState(dto.getRegState())
                        .regCountry(dto.getRegCountry())

                        .primaryFullName(
                                dto.getPrimaryFullName())

                        .primaryEmail(
                                dto.getPrimaryEmail())

                        .primaryMobile(
                                dto.getPrimaryMobile())

                        .selectedProducts(
                                dto.getSelectedProducts())

                        .enableMFA(
                                dto.getEnableMFA())

                        .enableHRMS(
                                dto.getEnableHRMS())

                        .enableOTP(
                                dto.getEnableOTP())

                        .status(
                                dto.getStatus() == null
                                        ? EnableStatus.ACTIVE
                                        : dto.getStatus())

                        .createdDate(
                                LocalDateTime.now())

                        .build();

        // PRODUCT VARIANTS

        if (dto.getSelectedVariants() != null) {

            List<InstitutionProductVariant> variants =
                    new ArrayList<>();

            dto.getSelectedVariants()
                    .forEach((product, variantList) -> {

                        for (String variant : variantList) {

                            InstitutionProductVariant pv =
                                    InstitutionProductVariant
                                            .builder()
                                            .productName(product)
                                            .variantName(variant)
                                            .institution(institution)
                                            .build();

                            variants.add(pv);
                        }
                    });

            institution.setSelectedVariants(
                    variants);
        }

        return institution;
    }
}