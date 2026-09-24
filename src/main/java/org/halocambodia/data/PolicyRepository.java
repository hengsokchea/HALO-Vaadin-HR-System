package org.halocambodia.data;

import java.util.List;
import java.util.Optional;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

public interface PolicyRepository        extends JpaRepository<Policy, Long>,                JpaSpecificationExecutor<Policy> {

    Optional<Policy> findByCode(String code);

    List<Policy> findByPolicyCategory(
            PolicyCategory policyCategory);

    List<Policy> findByPolicyCategoryId(
            Long categoryId);

    List<Policy> findByPublishedTrue();

    List<Policy> findByActiveTrue();

    List<Policy> findByPolicyCategoryAndPublishedTrue(
            PolicyCategory policyCategory);

    List<Policy> findByPolicyCategoryAndActiveTrue(
            PolicyCategory policyCategory);

    /*
     * Finds the highest three-digit number for one category.
     *
     * Examples:
     * HR001 -> 1
     * HR025 -> 25
     * ICT003 -> 3
     */
    @Query(
        value = """
            SELECT COALESCE(
                MAX(
                    CAST(
                        RIGHT(p.code, 3)
                        AS INTEGER
                    )
                ),
                0
            )
            FROM public.policy p
            WHERE LEFT(
                      UPPER(p.code),
                      CHAR_LENGTH(CAST(:prefix AS TEXT))
                  ) = UPPER(:prefix)
              AND CHAR_LENGTH(p.code)
                    = CHAR_LENGTH(CAST(:prefix AS TEXT)) + 3
              AND RIGHT(p.code, 3) ~ '^[0-9]{3}$'
            """,
        nativeQuery = true
    )
    Integer findMaximumCodeNumber(
            @Param("prefix") String prefix);
}