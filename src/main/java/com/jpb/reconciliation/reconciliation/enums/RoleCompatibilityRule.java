package com.jpb.reconciliation.reconciliation.enums;

import java.util.Arrays;
import java.util.Set;
import java.util.stream.Collectors;

public final class RoleCompatibilityRule {

    private RoleCompatibilityRule() {}

    public static final String[][] CONFLICT_PAIRS = {
        { StandardRole.MAKER.name(), StandardRole.CHECKER.name() }
    };

    public static final Set<String> SUPERVISOR_ELIGIBLE = Arrays.stream(new StandardRole[]{
            StandardRole.MAKER,
            StandardRole.CHECKER,
            StandardRole.IT_OPS
    }).map(StandardRole::name).collect(Collectors.toSet());

    public static String[] findConflict(Set<String> upperCaseNames) {
        for (String[] pair : CONFLICT_PAIRS) {
            if (upperCaseNames.contains(pair[0]) && upperCaseNames.contains(pair[1])) {
                return pair;
            }
        }
        return null;
    }

    public static Set<String> ineligibleForSupervisor(Set<String> upperCaseNames) {
        return upperCaseNames.stream()
                .filter(r -> !r.equals(StandardRole.SUPERVISOR.name()))
                .filter(r -> !SUPERVISOR_ELIGIBLE.contains(r))
                .collect(Collectors.toSet());
    }
}
