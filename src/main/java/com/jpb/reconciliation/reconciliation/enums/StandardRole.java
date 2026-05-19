package com.jpb.reconciliation.reconciliation.enums;

public enum StandardRole {

    MAKER(1001),
    CHECKER(1002),
    WORKER(1003),
    AUDITOR(1004),
    IT_OPS(1005),
    SUPERVISOR(1006),
    RCC_CXO(1007),
    OTHER(9000);

    private final int code;

    StandardRole(int code) {
        this.code = code;
    }

    public int getCode() {
        return code;
    }

    public static Integer getCodeByRoleName(String roleName) {

        for (StandardRole role : values()) {

            if (role.name().equalsIgnoreCase(roleName)) {
                return role.getCode();
            }
        }

        return null;
    }
}