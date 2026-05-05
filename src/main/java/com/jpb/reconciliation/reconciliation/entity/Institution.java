package com.jpb.reconciliation.reconciliation.entity;

import javax.persistence.Column;
import javax.persistence.Entity;
import javax.persistence.EnumType;
import javax.persistence.Enumerated;
import javax.persistence.GeneratedValue;
import javax.persistence.GenerationType;
import javax.persistence.Id;
import javax.persistence.Table;

import com.jpb.reconciliation.reconciliation.constants.EnableStatus;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Entity
@Table(name = "institution")
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class Institution {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "institution_id")
    private Long institutionId;

    @Column(name = "institution_name", nullable = false, unique = true, length = 100)
    private String institutionName;

    @Column(name = "description", length = 255)
    private String description;

    @Column(name = "institution_user_id", nullable = false, unique = true, length = 50)
    private String institutionUserId;

    @Column(name = "user_role", length = 50)
    private String userRole;

    @Enumerated(EnumType.STRING)
    @Column(name = "enable_status", nullable = false, length = 20)
    private EnableStatus enableStatus;

    @Column(name = "web_address", nullable = false, length = 255)
    private String webAddress;

    @Column(name = "data_encryption_key", length = 100)
    private String dataEncryptionKey;

    @Column(name = "language", nullable = false, length = 20)
    private String language;

    @Column(name = "number_of_users_allowed", nullable = false)
    private Integer numberOfUsersAllowed;

    @Column(name = "logo_path", length = 500)
    private String logoPath;

    // ─── Address ──────────────────────────────────────────────────────────────

    @Column(name = "address_line1", nullable = false, length = 255)
    private String addressLine1;

    @Column(name = "address_line2", length = 255)
    private String addressLine2;

    @Column(name = "address_line3", length = 255)
    private String addressLine3;

    @Column(name = "city", nullable = false, length = 100)
    private String city;

    @Column(name = "state", nullable = false, length = 100)
    private String state;

    @Column(name = "country", nullable = false, length = 100)
    private String country;

    @Column(name = "zip_code", nullable = false, length = 20)
    private String zipCode;

    // ─── Contact ──────────────────────────────────────────────────────────────

    @Column(name = "contact_name", nullable = false, length = 100)
    private String contactName;

    @Column(name = "mobile_number", nullable = false, length = 20)
    private String mobileNumber;

    @Column(name = "fax_number", length = 20)
    private String faxNumber;

    @Column(name = "email_address", nullable = false, length = 150)
    private String emailAddress;

    @Column(name = "technical_contact_name", length = 100)
    private String technicalContactName;

    @Column(name = "technical_phone_number", length = 20)
    private String technicalPhoneNumber;

    @Column(name = "technical_email_address", length = 150)
    private String technicalEmailAddress;

    // ─── Configuration ────────────────────────────────────────────────────────

    @Column(name = "enable_captcha")
    private Boolean enableCaptcha;

    @Column(name = "enable_blocking_unsecure_ip")
    private Boolean enableBlockingUnsecureIp;

    @Column(name = "enable_profile_password_authentication")
    private Boolean enableProfilePasswordAuthentication;

    @Column(name = "enable_fees")
    private Boolean enableFees;

    @Column(name = "enable_secure_authentication")
    private Boolean enableSecureAuthentication;

    @Column(name = "allowed_batch_thread_count")
    private Integer allowedBatchThreadCount;

    @Column(name = "enable_risk_management")
    private Boolean enableRiskManagement;

    @Column(name = "enable_internet_banking")
    private Boolean enableInternetBanking;

    @Column(name = "internet_banking_prefix", length = 50)
    private String internetBankingPrefix;

    @Column(name = "internet_banking_url", length = 255)
    private String internetBankingUrl;

    @Column(name = "internet_banking_inquiry_url", length = 255)
    private String internetBankingInquiryUrl;

    @Column(name = "internet_banking_connection_timeout")
    private Integer internetBankingConnectionTimeout;

    @Column(name = "internet_banking_read_timeout")
    private Integer internetBankingReadTimeout;

    @Column(name = "enable_vpas_acquiring_bin")
    private Boolean enableVpasAcquiringBin;

    @Column(name = "enable_imps_payment")
    private Boolean enableImpsPayment;

    @Column(name = "enable_ivr_3d")
    private Boolean enableIvr3d;

    @Column(name = "choose_cryptographic_method")
    private Boolean chooseCryptographicMethod;

    @Column(name = "authorization_level", length = 100)
    private String authorizationLevel;

    @Column(name = "enable_otp")
    private Boolean enableOtp;

    @Column(name = "otp_model", length = 50)
    private String otpModel;

    @Column(name = "otp_allowed", length = 50)
    private String otpAllowed;

    @Column(name = "enable_currency_conversion")
    private Boolean enableCurrencyConversion;

    @Column(name = "enable_standing_instruction")
    private Boolean enableStandingInstruction;

    @Column(name = "enable_sdk_integration")
    private Boolean enableSdkIntegration;

    @Column(name = "enable_3d_secure_pre_authentication")
    private Boolean enable3dSecurePreAuthentication;

    @Column(name = "enable_one_click_checkout")
    private Boolean enableOneClickCheckout;

    @Column(name = "enable_single_tid")
    private Boolean enableSingleTid;

}