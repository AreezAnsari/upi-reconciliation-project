package com.jpb.reconciliation.reconciliation.service;

import com.jpb.reconciliation.reconciliation.enums.StandardRole;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;

import javax.persistence.EntityManager;
import javax.persistence.PersistenceContext;

@Service
public class RecRoleCodeGeneratorService {

    private static final Logger log = LoggerFactory.getLogger(RecRoleCodeGeneratorService.class);

    @PersistenceContext
    private EntityManager em;

    private static final String STANDARD_SEQ = "ROLE_CODE_SEQ";
    private static final String CUSTOM_SEQ   = "ROLE_CODE_CUSTOM_SEQ";

    /**
     * PEEK = actually reserves the code via NEXTVAL.
     * Frontend sends this back as reservedRoleCode.
     * Service reuses it directly — no second NEXTVAL call.
     */
    @Transactional(propagation = Propagation.REQUIRES_NEW)
    public String peekNextCode(String roleName) {
        String seqName = resolveSequence(roleName);
        long next = fetchNextVal(seqName);
        log.info("RESERVED code for roleName={} → {} (seq={})", roleName, next, seqName);
        return String.valueOf(next);
    }

    /**
     * Generate a FRESH code — used as fallback when:
     * - No reservedRoleCode was sent (edit mode, direct API call)
     * - Reserved code went stale (already taken by another role)
     * - Race condition retry after DataIntegrityViolationException
     */
    @Transactional(propagation = Propagation.REQUIRES_NEW)
    public String generateNextCode(String roleMasterName) {
        String seqName = resolveSequence(roleMasterName);
        long next = fetchNextVal(seqName);
        log.info("FRESH code for roleMasterName={} → {} (seq={})", roleMasterName, next, seqName);
        return String.valueOf(next);
    }

    // ── private helpers ───────────────────────────────────────────────────────

    /**
     * Extract primary token from composite names like "MAKER + CHECKER"
     * and decide which sequence to use.
     */
    private String resolveSequence(String roleName) {
        // For composite names like "MAKER + CHECKER", use first token
        String primaryToken = roleName.trim().toUpperCase().split("\\+")[0].trim();
        boolean isCustom = !StandardRole.fromRoleName(primaryToken).isStandard();
        return isCustom ? CUSTOM_SEQ : STANDARD_SEQ;
    }

    private long fetchNextVal(String sequenceName) {
        Object result = em
                .createNativeQuery("SELECT " + sequenceName + ".NEXTVAL FROM DUAL")
                .getSingleResult();
        return ((Number) result).longValue();
    }
}