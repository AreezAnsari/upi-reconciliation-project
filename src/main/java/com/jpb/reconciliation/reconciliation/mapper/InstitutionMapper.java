
package com.jpb.reconciliation.reconciliation.mapper;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

import org.springframework.stereotype.Component;

import com.jpb.reconciliation.reconciliation.dto.InstitutionDTO;
import com.jpb.reconciliation.reconciliation.entity.Institution;
import com.jpb.reconciliation.reconciliation.entity.InstitutionProductVariant;

@Component
public class InstitutionMapper {

    // =========================================================
    // ENTITY -> DTO
    // =========================================================

    public InstitutionDTO toDTO(Institution institution) {

        if (institution == null) {
            return null;
        }

        return InstitutionDTO.builder()

                // BASIC
                .id(institution.getId())
                .institutionCode(institution.getInstitutionCode())
                .institutionNameFull(institution.getInstitutionNameFull())
                .institutionNameShort(institution.getInstitutionNameShort())

                // BANK
                .bankType(institution.getBankType())
                .bankLogoName(institution.getBankLogoName())
                .bankLogoPath(institution.getBankLogoPath())

                // REGISTERED ADDRESS
                .regAddressLine1(institution.getRegAddressLine1())
                .regAddressLine2(institution.getRegAddressLine2())
                .regAddressLine3(institution.getRegAddressLine3())
                .regCity(institution.getRegCity())
                .regState(institution.getRegState())
                .regCountry(institution.getRegCountry())
                .regPhoneCode(institution.getRegPhoneCode())
                .regCityCode(institution.getRegCityCode())
                .regPhone(institution.getRegPhone())

                // COMMUNICATION ADDRESS
                .sameAsRegistered(institution.getSameAsRegistered())
                .commAddressLine1(institution.getCommAddressLine1())
                .commAddressLine2(institution.getCommAddressLine2())
                .commAddressLine3(institution.getCommAddressLine3())
                .commCity(institution.getCommCity())
                .commState(institution.getCommState())
                .commCountry(institution.getCommCountry())
                .commPhoneCode(institution.getCommPhoneCode())
                .commCityCode(institution.getCommCityCode())
                .commPhone(institution.getCommPhone())

                // PRIMARY CONTACT
                .primaryFullName(institution.getPrimaryFullName())
                .primaryEmail(institution.getPrimaryEmail())
                .primaryMobileCode(institution.getPrimaryMobileCode())
                .primaryMobile(institution.getPrimaryMobile())
                .primaryAltMobileCode(institution.getPrimaryAltMobileCode())
                .primaryAltMobile(institution.getPrimaryAltMobile())

                // SECONDARY CONTACT
                .secondaryFullName(institution.getSecondaryFullName())
                .secondaryEmail(institution.getSecondaryEmail())
                .secondaryMobileCode(institution.getSecondaryMobileCode())
                .secondaryMobile(institution.getSecondaryMobile())
                .secondaryAltMobileCode(institution.getSecondaryAltMobileCode())
                .secondaryAltMobile(institution.getSecondaryAltMobile())

                // PRODUCTS
                .selectedProducts(institution.getSelectedProducts())

                // VARIANTS
                .selectedVariants(
                        institution.getSelectedVariants()
                                .stream()
                                .collect(Collectors.groupingBy(
                                        InstitutionProductVariant::getProductName,
                                        Collectors.mapping(
                                                InstitutionProductVariant::getVariantName,
                                                Collectors.toList()
                                        )
                                ))
                )

                // SECURITY
                .enableMFA(institution.getEnableMFA())
                .enableHRMS(institution.getEnableHRMS())
                .enableOTP(institution.getEnableOTP())

                // STATUS
                .status(institution.getStatus())

                .build();
    }

    // =========================================================
    // DTO -> ENTITY
    // =========================================================

    public Institution toEntity(InstitutionDTO dto) {

        if (dto == null) {
            return null;
        }

        Institution institution = Institution.builder()

                // BASIC
                .institutionCode(dto.getInstitutionCode())
                .institutionNameFull(dto.getInstitutionNameFull())
                .institutionNameShort(dto.getInstitutionNameShort())

                // BANK
                .bankType(dto.getBankType())
                .bankLogoName(dto.getBankLogoName())
                .bankLogoPath(dto.getBankLogoPath())

                // REGISTERED ADDRESS
                .regAddressLine1(dto.getRegAddressLine1())
                .regAddressLine2(dto.getRegAddressLine2())
                .regAddressLine3(dto.getRegAddressLine3())
                .regCity(dto.getRegCity())
                .regState(dto.getRegState())
                .regCountry(dto.getRegCountry())
                .regPhoneCode(dto.getRegPhoneCode())
                .regCityCode(dto.getRegCityCode())
                .regPhone(dto.getRegPhone())

                // COMMUNICATION ADDRESS
                .sameAsRegistered(dto.getSameAsRegistered())
                .commAddressLine1(dto.getCommAddressLine1())
                .commAddressLine2(dto.getCommAddressLine2())
                .commAddressLine3(dto.getCommAddressLine3())
                .commCity(dto.getCommCity())
                .commState(dto.getCommState())
                .commCountry(dto.getCommCountry())
                .commPhoneCode(dto.getCommPhoneCode())
                .commCityCode(dto.getCommCityCode())
                .commPhone(dto.getCommPhone())

                // PRIMARY CONTACT
                .primaryFullName(dto.getPrimaryFullName())
                .primaryEmail(dto.getPrimaryEmail())
                .primaryMobileCode(dto.getPrimaryMobileCode())
                .primaryMobile(dto.getPrimaryMobile())
                .primaryAltMobileCode(dto.getPrimaryAltMobileCode())
                .primaryAltMobile(dto.getPrimaryAltMobile())

                // SECONDARY CONTACT
                .secondaryFullName(dto.getSecondaryFullName())
                .secondaryEmail(dto.getSecondaryEmail())
                .secondaryMobileCode(dto.getSecondaryMobileCode())
                .secondaryMobile(dto.getSecondaryMobile())
                .secondaryAltMobileCode(dto.getSecondaryAltMobileCode())
                .secondaryAltMobile(dto.getSecondaryAltMobile())

                // PRODUCTS
                .selectedProducts(dto.getSelectedProducts())

                // SECURITY
                .enableMFA(dto.getEnableMFA())
                .enableHRMS(dto.getEnableHRMS())
                .enableOTP(dto.getEnableOTP())

                // STATUS
                .status(dto.getStatus())

                // DATE
                .createdDate(LocalDateTime.now())

                .build();

        // =========================================================
        // PRODUCT VARIANTS
        // =========================================================

        List<InstitutionProductVariant> variantList =
                new ArrayList<>();

        if (dto.getSelectedVariants() != null) {

            for (Map.Entry<String, List<String>> entry :
                    dto.getSelectedVariants().entrySet()) {

                String productName = entry.getKey();

                List<String> variants = entry.getValue();

                if (variants != null) {

                    for (String variantName : variants) {

                        InstitutionProductVariant variant =
                                new InstitutionProductVariant();

                        variant.setInstitution(institution);

                        variant.setProductName(productName);

                        // ERROR FIXED HERE
                        variant.setVariantName(variantName);

                        variantList.add(variant);
                    }
                }
            }
        }

        institution.setSelectedVariants(variantList);

        return institution;
    }

    // =========================================================
    // UPDATE ENTITY
    // =========================================================

    public void updateEntityFromDTO(
            InstitutionDTO dto,
            Institution institution
    ) {

        // BASIC
        institution.setInstitutionCode(dto.getInstitutionCode());
        institution.setInstitutionNameFull(dto.getInstitutionNameFull());
        institution.setInstitutionNameShort(dto.getInstitutionNameShort());

        // BANK
        institution.setBankType(dto.getBankType());
        institution.setBankLogoName(dto.getBankLogoName());
        institution.setBankLogoPath(dto.getBankLogoPath());

        // REGISTERED ADDRESS
        institution.setRegAddressLine1(dto.getRegAddressLine1());
        institution.setRegAddressLine2(dto.getRegAddressLine2());
        institution.setRegAddressLine3(dto.getRegAddressLine3());
        institution.setRegCity(dto.getRegCity());
        institution.setRegState(dto.getRegState());
        institution.setRegCountry(dto.getRegCountry());
        institution.setRegPhoneCode(dto.getRegPhoneCode());
        institution.setRegCityCode(dto.getRegCityCode());
        institution.setRegPhone(dto.getRegPhone());

        // COMMUNICATION ADDRESS
        institution.setSameAsRegistered(dto.getSameAsRegistered());
        institution.setCommAddressLine1(dto.getCommAddressLine1());
        institution.setCommAddressLine2(dto.getCommAddressLine2());
        institution.setCommAddressLine3(dto.getCommAddressLine3());
        institution.setCommCity(dto.getCommCity());
        institution.setCommState(dto.getCommState());
        institution.setCommCountry(dto.getCommCountry());
        institution.setCommPhoneCode(dto.getCommPhoneCode());
        institution.setCommCityCode(dto.getCommCityCode());
        institution.setCommPhone(dto.getCommPhone());

        // PRIMARY CONTACT
        institution.setPrimaryFullName(dto.getPrimaryFullName());
        institution.setPrimaryEmail(dto.getPrimaryEmail());
        institution.setPrimaryMobileCode(dto.getPrimaryMobileCode());
        institution.setPrimaryMobile(dto.getPrimaryMobile());
        institution.setPrimaryAltMobileCode(dto.getPrimaryAltMobileCode());
        institution.setPrimaryAltMobile(dto.getPrimaryAltMobile());

        // SECONDARY CONTACT
        institution.setSecondaryFullName(dto.getSecondaryFullName());
        institution.setSecondaryEmail(dto.getSecondaryEmail());
        institution.setSecondaryMobileCode(dto.getSecondaryMobileCode());
        institution.setSecondaryMobile(dto.getSecondaryMobile());
        institution.setSecondaryAltMobileCode(dto.getSecondaryAltMobileCode());
        institution.setSecondaryAltMobile(dto.getSecondaryAltMobile());

        // PRODUCTS
        institution.setSelectedProducts(dto.getSelectedProducts());

        // SECURITY
        institution.setEnableMFA(dto.getEnableMFA());
        institution.setEnableHRMS(dto.getEnableHRMS());
        institution.setEnableOTP(dto.getEnableOTP());

        // STATUS
        institution.setStatus(dto.getStatus());

        // =========================================================
        // UPDATE VARIANTS
        // =========================================================

        institution.getSelectedVariants().clear();

        if (dto.getSelectedVariants() != null) {

            for (Map.Entry<String, List<String>> entry :
                    dto.getSelectedVariants().entrySet()) {

                String productName = entry.getKey();

                List<String> variants = entry.getValue();

                if (variants != null) {

                    for (String variantName : variants) {

                        InstitutionProductVariant variant =
                                new InstitutionProductVariant();

                        variant.setInstitution(institution);

                        variant.setProductName(productName);

                        variant.setVariantName(variantName);

                        institution.getSelectedVariants()
                                .add(variant);
                    }
                }
            }
        }
    }
}

