package com.jpb.reconciliation.reconciliation.repository;

import com.jpb.reconciliation.reconciliation.entity.AddUser;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface AddUserRepository extends JpaRepository<AddUser, Long> {

    // ── Existing checks ───────────────────────────────────────────────────────

    boolean existsByUsername(String username);

    boolean existsByEmail(String email);

    boolean existsByFullName(String fullName);

    // ── NEW: Lookup by institutionCode + username (for auth flow) ─────────────

    /**
     * Find user by institutionCode AND username.
     * Used in: verifyCredentials, setPassword, login steps.
     */
    Optional<AddUser> findByInstitutionCodeAndUsername(String institutionCode, String username);

    /**
     * Find user by email only (for forgot-password flow).
     * findFirst used to avoid NonUniqueResultException.
     */
    Optional<AddUser> findFirstByEmail(String email);

    /**
     * Find user by email ordered by id (safe for duplicate-prevention).
     */
    Optional<AddUser> findFirstByEmailOrderByIdAsc(String email);

    /**
     * Find user by username only (fallback when no institutionCode provided).
     */
    Optional<AddUser> findFirstByUsername(String username);

    // ── Existing institution-scoped fetch ─────────────────────────────────────

    List<AddUser> findAllByInstitutionCode(String institutionCode);

    // ── Existing search ───────────────────────────────────────────────────────

    @Query("SELECT u FROM AddUser u WHERE u.institutionCode = :instCode " +
           "AND (LOWER(u.username) LIKE LOWER(CONCAT('%', :term, '%')) " +
           "OR LOWER(u.email) LIKE LOWER(CONCAT('%', :term, '%')) " +
           "OR LOWER(u.fullName) LIKE LOWER(CONCAT('%', :term, '%')))")
    List<AddUser> searchUsers(@Param("instCode") String instCode,
                               @Param("term") String term);
}