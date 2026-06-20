package com.jpb.reconciliation.reconciliation.service;

import com.jpb.reconciliation.reconciliation.enums.StandardRole;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;

import javax.persistence.EntityManager;
import javax.persistence.PersistenceContext;

/**
 * RecRoleCodeGeneratorService
 * ────────────────────────────
 * Single responsibility: generate the next unique 4-digit role code from the
 * Oracle DB sequence.
 *
 * Two sequences are used:
 *   ROLE_CODE_SEQ        → standard roles (MAKER, CHECKER, etc.)  1008–8999
 *   ROLE_CODE_CUSTOM_SEQ → OTHER / custom-named roles              9001–9999
 *
 * Propagation.REQUIRES_NEW is intentional:
 *   Once a sequence value is consumed it is never re-issued, even if the
 *   outer transaction rolls back. This avoids duplicate-key errors on retry.
 *
 * NOTE: composite role names like "MAKER + CHECKER" are handled by extracting
 * the first (primary) token to decide which sequence to use.
 */
@Service
public class RecRoleCodeGeneratorService {

    private static final Logger log = LoggerFactory.getLogger(RecRoleCodeGeneratorService.class);

    @PersistenceContext
    private EntityManager em;

    private static final String STANDARD_SEQ = "ROLE_CODE_SEQ";
    private static final String CUSTOM_SEQ   = "ROLE_CODE_CUSTOM_SEQ";

    /**
     * Generate the next unique 4-digit role code.
     *
     * Accepts any role name string — plain ("MAKER"), composite ("MAKER + CHECKER"),
     * or custom ("MY_ROLE"). The primary token is used to decide the sequence.
     *
     * @param roleMasterName  The role name or combined name (e.g. "MAKER + CHECKER")
     * @return Zero-padded 4-digit string, e.g. "1008", "9001"
     */
    @Transactional(propagation = Propagation.REQUIRES_NEW)
    public String generateNextCode(String roleMasterName) {
        StandardRole category = StandardRole.fromRoleName(roleMasterName); // handles composite names
        String sequenceName   = category.isStandard() ? STANDARD_SEQ : CUSTOM_SEQ;

        long   nextVal = fetchNextVal(sequenceName);
        String code    = String.format("%04d", nextVal);

        log.info("Generated role code [{}] from sequence [{}] for role master [{}]",
                 code, sequenceName, roleMasterName);
        return code;
    }

    /**
     * Overload for when the StandardRole category is already resolved.
     */
    @Transactional(propagation = Propagation.REQUIRES_NEW)
    public String generateNextCode(StandardRole category) {
        String sequenceName = category.isStandard() ? STANDARD_SEQ : CUSTOM_SEQ;
        long   nextVal      = fetchNextVal(sequenceName);
        String code         = String.format("%04d", nextVal);

        log.info("Generated role code [{}] from sequence [{}] for category [{}]",
                 code, sequenceName, category.name());
        return code;
    }

    /**
     * Peek the NEXT expected code WITHOUT consuming the sequence.
     *
     * Uses Oracle USER_SEQUENCES.LAST_NUMBER which is the value the sequence
     * will issue on the very next NEXTVAL call — no side effects, no increment.
     *
     * ⚠️  ESTIMATE ONLY — not a reservation.
     *     If another transaction calls NEXTVAL between peek and actual
     *     generateNextCode(), the final saved code will be higher.
     *     Always surface this as "~XXXX" or "Estimated" in the UI.
     *
     * @param roleMasterName  Role name or composite name (e.g. "MAKER + CHECKER")
     * @return Zero-padded 4-digit estimate string, e.g. "~1008"
     */
    public String peekNextCode(String roleMasterName) {
        StandardRole category   = StandardRole.fromRoleName(roleMasterName);
        String       seqName    = category.isStandard() ? STANDARD_SEQ : CUSTOM_SEQ;

        try {
            Object result = em.createNativeQuery(
                    "SELECT LAST_NUMBER FROM USER_SEQUENCES WHERE SEQUENCE_NAME = :seq")
                    .setParameter("seq", seqName)
                    .getSingleResult();

            long nextExpected = ((Number) result).longValue();
            String code = String.format("%04d", nextExpected);

            log.debug("Peeked sequence [{}] → next expected code [{}] for role [{}]",
                      seqName, code, roleMasterName);
            return code;

        } catch (Exception e) {
            log.warn("Could not peek sequence [{}]: {}", seqName, e.getMessage());
            return "----";  // frontend treats "----" as loading failed
        }
    }

    // ── private helpers ───────────────────────────────────────────────────────

    private long fetchNextVal(String sequenceName) {
        // Oracle syntax; for PostgreSQL use: "SELECT nextval('" + sequenceName + "')"
        Object result = em
                .createNativeQuery("SELECT " + sequenceName + ".NEXTVAL FROM DUAL")
                .getSingleResult();
        return ((Number) result).longValue();
    }
}