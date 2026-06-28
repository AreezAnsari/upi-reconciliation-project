package com.jpb.reconciliation.reconciliation.mapper;

import org.springframework.stereotype.Component;

import com.jpb.reconciliation.reconciliation.dto.InstitutionDTO;
import com.jpb.reconciliation.reconciliation.entity.Institution;

@Component
public class InstitutionMapper {

    /**
     * Convert Entity → DTO
     */
    public InstitutionDTO toDTO(Institution institution) {
        if (institution == null) return null;

        return InstitutionDTO.builder()
                .institutionId(institution.getInstitutionId())
                .institutionName(institution.getInstitutionName())
                .description(institution.getDescription())
                .institutionUserId(institution.getInstitutionUserId())
                .userRole(institution.getUserRole())
                .enableStatus(institution.getEnableStatus())
                .webAddress(institution.getWebAddress())
                .dataEncryptionKey(institution.getDataEncryptionKey())
                .language(institution.getLanguage())
                .numberOfUsersAllowed(institution.getNumberOfUsersAllowed())
                .logoPath(institution.getLogoPath())
                // Address
                .addressLine1(institution.getAddressLine1())
                .addressLine2(institution.getAddressLine2())
                .addressLine3(institution.getAddressLine3())
                .city(institution.getCity())
                .state(institution.getState())
                .country(institution.getCountry())
                .zipCode(institution.getZipCode())
                // Contact
                .contactName(institution.getContactName())
                .mobileNumber(institution.getMobileNumber())
                .faxNumber(institution.getFaxNumber())
                .emailAddress(institution.getEmailAddress())
                .technicalContactName(institution.getTechnicalContactName())
                .technicalPhoneNumber(institution.getTechnicalPhoneNumber())
                .technicalEmailAddress(institution.getTechnicalEmailAddress())
                // Configuration
                .enableCaptcha(institution.getEnableCaptcha())
                .enableBlockingUnsecureIp(institution.getEnableBlockingUnsecureIp())
                .enableProfilePasswordAuthentication(institution.getEnableProfilePasswordAuthentication())
                .enableFees(institution.getEnableFees())
                .enableSecureAuthentication(institution.getEnableSecureAuthentication())
                .allowedBatchThreadCount(institution.getAllowedBatchThreadCount())
                .enableRiskManagement(institution.getEnableRiskManagement())
                .enableInternetBanking(institution.getEnableInternetBanking())
                .internetBankingPrefix(institution.getInternetBankingPrefix())
                .internetBankingUrl(institution.getInternetBankingUrl())
                .internetBankingInquiryUrl(institution.getInternetBankingInquiryUrl())
                .internetBankingConnectionTimeout(institution.getInternetBankingConnectionTimeout())
                .internetBankingReadTimeout(institution.getInternetBankingReadTimeout())
                .enableVpasAcquiringBin(institution.getEnableVpasAcquiringBin())
                .enableImpsPayment(institution.getEnableImpsPayment())
                .enableIvr3d(institution.getEnableIvr3d())
                .chooseCryptographicMethod(institution.getChooseCryptographicMethod())
                .authorizationLevel(institution.getAuthorizationLevel())
                .enableOtp(institution.getEnableOtp())
                .otpModel(institution.getOtpModel())
                .otpAllowed(institution.getOtpAllowed())
                .enableCurrencyConversion(institution.getEnableCurrencyConversion())
                .enableStandingInstruction(institution.getEnableStandingInstruction())
                .enableSdkIntegration(institution.getEnableSdkIntegration())
                .enable3dSecurePreAuthentication(institution.getEnable3dSecurePreAuthentication())
                .enableOneClickCheckout(institution.getEnableOneClickCheckout())
                .enableSingleTid(institution.getEnableSingleTid())
                .build();
    }

    /**
     * Convert DTO → Entity
     */
    public Institution toEntity(InstitutionDTO dto) {
        if (dto == null) return null;

        return Institution.builder()
                .institutionName(dto.getInstitutionName())
                .description(dto.getDescription())
                .institutionUserId(dto.getInstitutionUserId())
                .userRole(dto.getUserRole())
                .enableStatus(dto.getEnableStatus())
                .webAddress(dto.getWebAddress())
                .dataEncryptionKey(dto.getDataEncryptionKey())
                .language(dto.getLanguage())
                .numberOfUsersAllowed(dto.getNumberOfUsersAllowed())
                .logoPath(dto.getLogoPath())
                // Address
                .addressLine1(dto.getAddressLine1())
                .addressLine2(dto.getAddressLine2())
                .addressLine3(dto.getAddressLine3())
                .city(dto.getCity())
                .state(dto.getState())
                .country(dto.getCountry())
                .zipCode(dto.getZipCode())
                // Contact
                .contactName(dto.getContactName())
                .mobileNumber(dto.getMobileNumber())
                .faxNumber(dto.getFaxNumber())
                .emailAddress(dto.getEmailAddress())
                .technicalContactName(dto.getTechnicalContactName())
                .technicalPhoneNumber(dto.getTechnicalPhoneNumber())
                .technicalEmailAddress(dto.getTechnicalEmailAddress())
                // Configuration
                .enableCaptcha(dto.getEnableCaptcha())
                .enableBlockingUnsecureIp(dto.getEnableBlockingUnsecureIp())
                .enableProfilePasswordAuthentication(dto.getEnableProfilePasswordAuthentication())
                .enableFees(dto.getEnableFees())
                .enableSecureAuthentication(dto.getEnableSecureAuthentication())
                .allowedBatchThreadCount(dto.getAllowedBatchThreadCount())
                .enableRiskManagement(dto.getEnableRiskManagement())
                .enableInternetBanking(dto.getEnableInternetBanking())
                .internetBankingPrefix(dto.getInternetBankingPrefix())
                .internetBankingUrl(dto.getInternetBankingUrl())
                .internetBankingInquiryUrl(dto.getInternetBankingInquiryUrl())
                .internetBankingConnectionTimeout(dto.getInternetBankingConnectionTimeout())
                .internetBankingReadTimeout(dto.getInternetBankingReadTimeout())
                .enableVpasAcquiringBin(dto.getEnableVpasAcquiringBin())
                .enableImpsPayment(dto.getEnableImpsPayment())
                .enableIvr3d(dto.getEnableIvr3d())
                .chooseCryptographicMethod(dto.getChooseCryptographicMethod())
                .authorizationLevel(dto.getAuthorizationLevel())
                .enableOtp(dto.getEnableOtp())
                .otpModel(dto.getOtpModel())
                .otpAllowed(dto.getOtpAllowed())
                .enableCurrencyConversion(dto.getEnableCurrencyConversion())
                .enableStandingInstruction(dto.getEnableStandingInstruction())
                .enableSdkIntegration(dto.getEnableSdkIntegration())
                .enable3dSecurePreAuthentication(dto.getEnable3dSecurePreAuthentication())
                .enableOneClickCheckout(dto.getEnableOneClickCheckout())
                .enableSingleTid(dto.getEnableSingleTid())
                .build();
    }

    /**
     * Update existing entity from DTO (for PUT/PATCH)
     */
    public void updateEntityFromDTO(InstitutionDTO dto, Institution institution) {
        institution.setInstitutionName(dto.getInstitutionName());
        institution.setDescription(dto.getDescription());
        institution.setUserRole(dto.getUserRole());
        institution.setEnableStatus(dto.getEnableStatus());
        institution.setWebAddress(dto.getWebAddress());
        institution.setLanguage(dto.getLanguage());
        institution.setNumberOfUsersAllowed(dto.getNumberOfUsersAllowed());
        institution.setLogoPath(dto.getLogoPath());
        // Address
        institution.setAddressLine1(dto.getAddressLine1());
        institution.setAddressLine2(dto.getAddressLine2());
        institution.setAddressLine3(dto.getAddressLine3());
        institution.setCity(dto.getCity());
        institution.setState(dto.getState());
        institution.setCountry(dto.getCountry());
        institution.setZipCode(dto.getZipCode());
        // Contact
        institution.setContactName(dto.getContactName());
        institution.setMobileNumber(dto.getMobileNumber());
        institution.setFaxNumber(dto.getFaxNumber());
        institution.setEmailAddress(dto.getEmailAddress());
        institution.setTechnicalContactName(dto.getTechnicalContactName());
        institution.setTechnicalPhoneNumber(dto.getTechnicalPhoneNumber());
        institution.setTechnicalEmailAddress(dto.getTechnicalEmailAddress());
        // Configuration
        institution.setEnableCaptcha(dto.getEnableCaptcha());
        institution.setEnableBlockingUnsecureIp(dto.getEnableBlockingUnsecureIp());
        institution.setEnableProfilePasswordAuthentication(dto.getEnableProfilePasswordAuthentication());
        institution.setEnableFees(dto.getEnableFees());
        institution.setEnableSecureAuthentication(dto.getEnableSecureAuthentication());
        institution.setAllowedBatchThreadCount(dto.getAllowedBatchThreadCount());
        institution.setEnableRiskManagement(dto.getEnableRiskManagement());
        institution.setEnableInternetBanking(dto.getEnableInternetBanking());
        institution.setInternetBankingPrefix(dto.getInternetBankingPrefix());
        institution.setInternetBankingUrl(dto.getInternetBankingUrl());
        institution.setInternetBankingInquiryUrl(dto.getInternetBankingInquiryUrl());
        institution.setInternetBankingConnectionTimeout(dto.getInternetBankingConnectionTimeout());
        institution.setInternetBankingReadTimeout(dto.getInternetBankingReadTimeout());
        institution.setEnableVpasAcquiringBin(dto.getEnableVpasAcquiringBin());
        institution.setEnableImpsPayment(dto.getEnableImpsPayment());
        institution.setEnableIvr3d(dto.getEnableIvr3d());
        institution.setChooseCryptographicMethod(dto.getChooseCryptographicMethod());
        institution.setAuthorizationLevel(dto.getAuthorizationLevel());
        institution.setEnableOtp(dto.getEnableOtp());
        institution.setOtpModel(dto.getOtpModel());
        institution.setOtpAllowed(dto.getOtpAllowed());
        institution.setEnableCurrencyConversion(dto.getEnableCurrencyConversion());
        institution.setEnableStandingInstruction(dto.getEnableStandingInstruction());
        institution.setEnableSdkIntegration(dto.getEnableSdkIntegration());
        institution.setEnable3dSecurePreAuthentication(dto.getEnable3dSecurePreAuthentication());
        institution.setEnableOneClickCheckout(dto.getEnableOneClickCheckout());
        institution.setEnableSingleTid(dto.getEnableSingleTid());
    }
}