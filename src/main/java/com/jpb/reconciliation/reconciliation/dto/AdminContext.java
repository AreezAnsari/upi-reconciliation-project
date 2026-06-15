package com.jpb.reconciliation.reconciliation.dto;

public class AdminContext {

    private final String username;
    private final String bankCode;
    private final String branchCode; // null for Bank Admin

    public AdminContext(String username, String bankCode, String branchCode) {
        this.username   = username;
        this.bankCode   = bankCode;
        this.branchCode = branchCode;
    }

    public String getUsername()   { return username;   }
    public String getBankCode()   { return bankCode;   }
    public String getBranchCode() { return branchCode; }

    public boolean isBranchAdmin() { return branchCode != null; }
}
