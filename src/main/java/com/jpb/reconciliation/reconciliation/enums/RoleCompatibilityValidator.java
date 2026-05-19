package com.jpb.reconciliation.reconciliation.enums;

import com.jpb.reconciliation.reconciliation.enums.RoleCompatibilityRule;
import com.jpb.reconciliation.reconciliation.enums.StandardRole;
import org.springframework.stereotype.Component;

import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;

/**
 * Validates role-combination rules before any DB writes.
 * Throws IllegalArgumentException (→ 400) on violation.
 */
@Component
public class RoleCompatibilityValidator {

    public void validate(List<String> roleNames) {

        if (roleNames == null || roleNames.isEmpty()) {
            throw new IllegalArgumentException(
                    "roleNames must contain at least one role.");
        }

        Set<String> upper = roleNames.stream()
                .map(String::toUpperCase)
                .collect(Collectors.toSet());

        checkConflictPairs(upper);
        checkSupervisorEligibility(upper);
    }

    // ── Rule 1 ────────────────────────────────────────────────────────────────

    private void checkConflictPairs(Set<String> upper) {
        String[] conflict = RoleCompatibilityRule.findConflict(upper);
        if (conflict != null) {
            throw new IllegalArgumentException(
                    "'" + conflict[0] + "' and '" + conflict[1] + "' cannot be assigned "
                    + "to the same person. Separation of duty: a MAKER initiates "
                    + "transactions and a CHECKER approves them — they must be "
                    + "different individuals.");
        }
    }

    // ── Rule 2 ────────────────────────────────────────────────────────────────

    private void checkSupervisorEligibility(Set<String> upper) {
        if (!upper.contains(StandardRole.SUPERVISOR.name())) {
            return; // SUPERVISOR not selected — nothing to check
        }

        Set<String> ineligible = RoleCompatibilityRule.ineligibleForSupervisor(upper);
        if (!ineligible.isEmpty()) {
            String eligible = RoleCompatibilityRule.SUPERVISOR_ELIGIBLE.stream()
                    .sorted()
                    .collect(Collectors.joining(", "));

            throw new IllegalArgumentException(
                    "SUPERVISOR can only be combined with: " + eligible + ". "
                    + "Ineligible role(s) in your request: "
                    + ineligible.stream().sorted().collect(Collectors.joining(", ")) + ".");
        }
    }
}