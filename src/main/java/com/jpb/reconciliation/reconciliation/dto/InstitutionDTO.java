package com.jpb.reconciliation.reconciliation.dto;

import javax.validation.constraints.Email;
import javax.validation.constraints.Min;
import javax.validation.constraints.NotBlank;
import javax.validation.constraints.NotNull;
import javax.validation.constraints.Pattern;

import com.jpb.reconciliation.reconciliation.constants.EnableStatus;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class InstitutionDTO {

    private Long institutionId;

    @NotBlank(message = "Institution name is required")
    private String institutionName;

    private String description;

    @NotBlank(message = "Institution user ID is required")
    private String institutionUserId;

    private String userRole;

    @NotNull(message = "Enable status is required")
    private EnableStatus enableStatus;

    @NotBlank(message = "Web address is required")
    private String webAddress;

    private String dataEncryptionKey;

    @NotBlank(message = "Language is required")
    private String language;

    @NotNull(message = "Number of users allowed is required")
    @Min(value = 1, message = "At least 1 user must be allowed")
    private Integer numberOfUsersAllowed;

    private String logoPath;

    @NotBlank(message = "Address line 1 is required")
    private String addressLine1;

    private String addressLine2;
    private String addressLine3;

    @NotBlank(message = "City is required")
    private String city;

    @NotBlank(message = "State is required")
    private String state;

    @NotBlank(message = "Country is required")
    private String country;

    @NotBlank(message = "Zip code is required")
    private String zipCode;

    @NotBlank(message = "Contact name is required")
    private String contactName;

    @NotBlank(message = "Mobile number is required")
    @Pattern(regexp = "^[0-9]{10,15}$", message = "Invalid mobile number")
    private String mobileNumber;

    private String faxNumber;

    @NotBlank(message = "Email address is required")
    @Email(message = "Invalid email address")
    private String emailAddress;

    private String technicalContactName;
    private String technicalPhoneNumber;
    private String technicalEmailAddress;

    private Boolean enableCaptcha;
    private Boolean enableBlockingUnsecureIp;
    private Boolean enableProfilePasswordAuthentication;
    private Boolean enableFees;
    private Boolean enableSecureAuthentication;

    @Min(value = 1, message = "Batch thread count must be at least 1")
    private Integer allowedBatchThreadCount;

    private Boolean enableRiskManagement;
    private Boolean enableInternetBanking;
    private String internetBankingPrefix;
    private String internetBankingUrl;
    private String internetBankingInquiryUrl;
    private Integer internetBankingConnectionTimeout;
    private Integer internetBankingReadTimeout;
    private Boolean enableVpasAcquiringBin;
    private Boolean enableImpsPayment;
    private Boolean enableIvr3d;
    private Boolean chooseCryptographicMethod;
    private String authorizationLevel;
    private Boolean enableOtp;
    private String otpModel;
    private String otpAllowed;
    private Boolean enableCurrencyConversion;
    private Boolean enableStandingInstruction;
    private Boolean enableSdkIntegration;
    private Boolean enable3dSecurePreAuthentication;
    private Boolean enableOneClickCheckout;
    private Boolean enableSingleTid;
}