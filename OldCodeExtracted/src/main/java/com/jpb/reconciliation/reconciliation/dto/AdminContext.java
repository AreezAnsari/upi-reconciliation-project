package com.jpb.reconciliation.reconciliation.dto;

public class AdminContext {

    private final String username;
    private final String bankCode;
    private final String branchCode;   // null for Bank Admin
    private final Long   creatorUserId; // non-null only when creator is a USER (not admin)

    public AdminContext(String username, String bankCode, String branchCode) {
        this(username, bankCode, branchCode, null);
    }

    public AdminContext(String username, String bankCode, String branchCode, Long creatorUserId) {
        this.username       = username;
        this.bankCode       = bankCode;
        this.branchCode     = branchCode;
        this.creatorUserId  = creatorUserId;
    }

    public String getUsername()      { return username;      }
    public String getBankCode()      { return bankCode;      }
    public String getBranchCode()    { return branchCode;    }
    public Long   getCreatorUserId() { return creatorUserId; }

    public boolean isBranchAdmin()  { return branchCode != null && creatorUserId == null; }
    public boolean isUserCreator()  { return creatorUserId != null; }
}
