package com.jpb.reconciliation.reconciliation.mapper;

import java.util.Arrays;

import com.jpb.reconciliation.reconciliation.dto.MainBankDTO;
import com.jpb.reconciliation.reconciliation.entity.MainBank;

public class MainBankMapper {

    // DTO → Entity (for create)
    public static MainBank mapToEntity(MainBankDTO dto, MainBank entity) {

        entity.setBankNameFull(dto.getBankNameFull() != null
                ? dto.getBankNameFull().trim() : null);
        entity.setBankNameShort(dto.getBankNameShort());

        // List<String> → comma-separated String
        if (dto.getBankType() != null) {
            entity.setBankType(String.join(",", dto.getBankType()));
        }

        // Registered address
        entity.setRegAddressLine1(dto.getRegAddressLine1());
        entity.setRegAddressLine2(dto.getRegAddressLine2());
        entity.setRegAddressLine3(dto.getRegAddressLine3());
        entity.setRegCity(dto.getRegCity());
        entity.setRegState(dto.getRegState());
        entity.setRegCountry(dto.getRegCountry());
        entity.setRegPhoneCode(dto.getRegPhoneCode());
        entity.setRegCityCode(dto.getRegCityCode());
        entity.setRegPhone(dto.getRegPhone());

        // Communication address
        if (dto.getSameAsRegistered() != null) {
            entity.setSameAsRegistered(dto.getSameAsRegistered() ? "Y" : "N");

            // Auto-copy reg address to comm if sameAsRegistered = true
            if (dto.getSameAsRegistered()) {
                entity.setCommAddressLine1(dto.getRegAddressLine1());
                entity.setCommAddressLine2(dto.getRegAddressLine2());
                entity.setCommAddressLine3(dto.getRegAddressLine3());
                entity.setCommCity(dto.getRegCity());
                entity.setCommState(dto.getRegState());
                entity.setCommCountry(dto.getRegCountry());
                entity.setCommPhoneCode(dto.getRegPhoneCode());
                entity.setCommCityCode(dto.getRegCityCode());
                entity.setCommPhone(dto.getRegPhone());
            } else {
                entity.setCommAddressLine1(dto.getCommAddressLine1());
                entity.setCommAddressLine2(dto.getCommAddressLine2());
                entity.setCommAddressLine3(dto.getCommAddressLine3());
                entity.setCommCity(dto.getCommCity());
                entity.setCommState(dto.getCommState());
                entity.setCommCountry(dto.getCommCountry());
                entity.setCommPhoneCode(dto.getCommPhoneCode());
                entity.setCommCityCode(dto.getCommCityCode());
                entity.setCommPhone(dto.getCommPhone());
            }
        }

        // Primary contact
        entity.setPrimaryFullName(dto.getPrimaryFullName());
        entity.setPrimaryEmail(dto.getPrimaryEmail());
        entity.setPrimaryMobileCode(dto.getPrimaryMobileCode());
        entity.setPrimaryMobile(dto.getPrimaryMobile());
        entity.setPrimaryAltMobileCode(dto.getPrimaryAltMobileCode());
        entity.setPrimaryAltMobile(dto.getPrimaryAltMobile());

        // Secondary contact
        entity.setSecondaryFullName(dto.getSecondaryFullName());
        entity.setSecondaryEmail(dto.getSecondaryEmail());
        entity.setSecondaryMobileCode(dto.getSecondaryMobileCode());
        entity.setSecondaryMobile(dto.getSecondaryMobile());
        entity.setSecondaryAltMobileCode(dto.getSecondaryAltMobileCode());
        entity.setSecondaryAltMobile(dto.getSecondaryAltMobile());

        // Products — List<String> → comma-separated String
        if (dto.getSelectedProducts() != null) {
            entity.setSelectedProducts(String.join(",", dto.getSelectedProducts()));
        }

        // Security
        if (dto.getEnableMfa() != null)  entity.setEnableMfa(dto.getEnableMfa() ? "Y" : "N");
        if (dto.getEnableHrms() != null) entity.setEnableHrms(dto.getEnableHrms() ? "Y" : "N");
        if (dto.getEnableOtp() != null)  entity.setEnableOtp(dto.getEnableOtp() ? "Y" : "N");

        return entity;
    }

    // Entity → DTO (for API responses)
    public static MainBankDTO mapToDTO(MainBank entity) {
        MainBankDTO dto = new MainBankDTO();

        dto.setBankId(entity.getBankId());
        dto.setBankCode(entity.getBankCode());
        dto.setStatus(entity.getStatus());
        dto.setCreatedAt(entity.getCreatedAt());
        dto.setCreatedBy(entity.getCreatedBy());
        dto.setLogoPath(entity.getLogoPath());
        dto.setInactivatedAt(entity.getInactivatedAt());
        dto.setBlockScheduledAt(entity.getBlockScheduledAt());
        dto.setUpdatedAt(entity.getUpdatedAt());
        dto.setUpdatedBy(entity.getUpdatedBy());
        dto.setBlockScheduledBy(entity.getBlockScheduledBy());
        dto.setPreBlockStatus(entity.getPreBlockStatus());
        dto.setBankAdminId(entity.getBankAdminId());

        dto.setBankNameFull(entity.getBankNameFull());
        dto.setBankNameShort(entity.getBankNameShort());

        // comma-separated → List<String>
        if (entity.getBankType() != null && !entity.getBankType().isEmpty()) {
            dto.setBankType(Arrays.asList(entity.getBankType().split(",")));
        }

        // Registered address
        dto.setRegAddressLine1(entity.getRegAddressLine1());
        dto.setRegAddressLine2(entity.getRegAddressLine2());
        dto.setRegAddressLine3(entity.getRegAddressLine3());
        dto.setRegCity(entity.getRegCity());
        dto.setRegState(entity.getRegState());
        dto.setRegCountry(entity.getRegCountry());
        dto.setRegPhoneCode(entity.getRegPhoneCode());
        dto.setRegCityCode(entity.getRegCityCode());
        dto.setRegPhone(entity.getRegPhone());

        // Communication address
        dto.setSameAsRegistered("Y".equals(entity.getSameAsRegistered()));
        dto.setCommAddressLine1(entity.getCommAddressLine1());
        dto.setCommAddressLine2(entity.getCommAddressLine2());
        dto.setCommAddressLine3(entity.getCommAddressLine3());
        dto.setCommCity(entity.getCommCity());
        dto.setCommState(entity.getCommState());
        dto.setCommCountry(entity.getCommCountry());
        dto.setCommPhoneCode(entity.getCommPhoneCode());
        dto.setCommCityCode(entity.getCommCityCode());
        dto.setCommPhone(entity.getCommPhone());

        // Primary contact
        dto.setPrimaryFullName(entity.getPrimaryFullName());
        dto.setPrimaryEmail(entity.getPrimaryEmail());
        dto.setPrimaryMobileCode(entity.getPrimaryMobileCode());
        dto.setPrimaryMobile(entity.getPrimaryMobile());
        dto.setPrimaryAltMobileCode(entity.getPrimaryAltMobileCode());
        dto.setPrimaryAltMobile(entity.getPrimaryAltMobile());

        // Secondary contact
        dto.setSecondaryFullName(entity.getSecondaryFullName());
        dto.setSecondaryEmail(entity.getSecondaryEmail());
        dto.setSecondaryMobileCode(entity.getSecondaryMobileCode());
        dto.setSecondaryMobile(entity.getSecondaryMobile());
        dto.setSecondaryAltMobileCode(entity.getSecondaryAltMobileCode());
        dto.setSecondaryAltMobile(entity.getSecondaryAltMobile());

        // Products — comma-separated → List<String>
        if (entity.getSelectedProducts() != null && !entity.getSelectedProducts().isEmpty()) {
            dto.setSelectedProducts(Arrays.asList(entity.getSelectedProducts().split(",")));
        }

        // Security
        dto.setEnableMfa("Y".equals(entity.getEnableMfa()));
        dto.setEnableHrms("Y".equals(entity.getEnableHrms()));
        dto.setEnableOtp("Y".equals(entity.getEnableOtp()));

        // Super-user credentials — returned on create so success page can display them
        dto.setBankId(entity.getBankId());
        dto.setDefaultPassword(entity.getDefaultPassword());

        return dto;
    }
}
