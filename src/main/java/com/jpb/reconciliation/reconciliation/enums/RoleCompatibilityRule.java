package com.jpb.reconciliation.reconciliation.enums;

import java.util.Arrays;
import java.util.Set;
import java.util.stream.Collectors;

/**
 * Single source of truth for all role-combination business rules.
 *
 * To add a new rule in future: edit only this file — nothing else changes.
 */
public final class RoleCompatibilityRule {

    private RoleCompatibilityRule() {}

    // ── Rule 1: Conflict pairs — these roles can NEVER be on the same person ──
    // MAKER initiates a transaction; CHECKER approves it — must be different people.
    public static final String[][] CONFLICT_PAIRS = {
        { StandardRole.MAKER.name(), StandardRole.CHECKER.name() }
        // add more pairs here as needed:
        // { StandardRole.AUDITOR.name(), StandardRole.WORKER.name() }
    };

    // ── Rule 2: Which roles are allowed to ALSO hold SUPERVISOR ───────────────
    // MAKER, CHECKER, IT_OPS can additionally carry the SUPERVISOR role.
    // All others (AUDITOR, WORKER, RCC_CXO …) cannot.
    public static final Set<String> SUPERVISOR_ELIGIBLE = Arrays.stream(new StandardRole[]{
            StandardRole.MAKER,
            StandardRole.CHECKER,
            StandardRole.IT_OPS
    }).map(StandardRole::name).collect(Collectors.toSet());

    // ── Helpers (used by RoleCompatibilityValidator) ──────────────────────────

    /** Returns the first conflicting pair found, or null if none. */
    public static String[] findConflict(Set<String> upperCaseNames) {
        for (String[] pair : CONFLICT_PAIRS) {
            if (upperCaseNames.contains(pair[0]) && upperCaseNames.contains(pair[1])) {
                return pair;
            }
        }
        return null;
    }

    /**
     * Returns roles that are NOT eligible to pair with SUPERVISOR.
     * Empty set means the combination is valid.
     */
    public static Set<String> ineligibleForSupervisor(Set<String> upperCaseNames) {
        return upperCaseNames.stream()
                .filter(r -> !r.equals(StandardRole.SUPERVISOR.name()))
                .filter(r -> !SUPERVISOR_ELIGIBLE.contains(r))
                .collect(Collectors.toSet());
    }
    
    private static Set<String> toUpper(Set<String> in) {
        return in.stream().map(String::toUpperCase).collect(Collectors.toSet());
    }
}