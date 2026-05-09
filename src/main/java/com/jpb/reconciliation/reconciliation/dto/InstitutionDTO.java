package com.jpb.reconciliation.reconciliation.dto;

import java.util.List;
import java.util.Map;

import javax.validation.constraints.Email;
import javax.validation.constraints.NotBlank;
import javax.validation.constraints.Pattern;

import com.jpb.reconciliation.reconciliation.constants.EnableStatus;

import lombok.*;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class InstitutionDTO {

    private Long id;

    private String institutionCode;

    // =========================================================
    // BASIC DETAILS
    // =========================================================

    @NotBlank(message = "Institution Name is required")
    private String institutionNameFull;

    private String institutionNameShort;

    private List<String> bankType;

    private String bankLogoName;
    private String bankLogoPath;

    // =========================================================
    // REGISTERED ADDRESS
    // =========================================================

    private String regAddressLine1;
    private String regAddressLine2;
    private String regAddressLine3;

    private String regCity;
    private String regState;
    private String regCountry;

    private String regPhoneCode;
    private String regCityCode;
    private String regPhone;

    // =========================================================
    // COMMUNICATION ADDRESS
    // =========================================================

    private Boolean sameAsRegistered;

    private String commAddressLine1;
    private String commAddressLine2;
    private String commAddressLine3;

    private String commCity;
    private String commState;
    private String commCountry;

    private String commPhoneCode;
    private String commCityCode;
    private String commPhone;

    // =========================================================
    // PRIMARY CONTACT
    // =========================================================

    private String primaryFullName;

    @Email(message = "Invalid primary email")
    private String primaryEmail;

    private String primaryMobileCode;

    @Pattern(
            regexp = "^[6-9][0-9]{9}$",
            message = "Invalid Mobile Number"
    )
    private String primaryMobile;

    private String primaryAltMobileCode;
    private String primaryAltMobile;

    // =========================================================
    // SECONDARY CONTACT
    // =========================================================

    private String secondaryFullName;
    private String secondaryEmail;

    private String secondaryMobileCode;
    private String secondaryMobile;

    private String secondaryAltMobileCode;
    private String secondaryAltMobile;

    // =========================================================
    // PRODUCTS
    // =========================================================

    private List<String> selectedProducts;

    private Map<String, List<String>> selectedVariants;

    // =========================================================
    // SECURITY
    // =========================================================

    private Boolean enableMFA;
    private Boolean enableHRMS;
    private Boolean enableOTP;

    // =========================================================
    // STATUS
    // =========================================================

    private EnableStatus status;
}