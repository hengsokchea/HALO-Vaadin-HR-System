package org.halocambodia.data;

import java.time.LocalDate;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

import org.halocambodia.data.*;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

public interface PolicyReadRepository extends JpaRepository<Policy, Long> {

    Optional<Policy> findByPublicToken(UUID publicToken);

    @Query("""
        select p
          from Policy p
          join fetch p.policyCategory c
         where p.publicToken = :token
           and p.active = true
           and p.published = true
           and p.effectiveDate <= :today
           and (p.expiryDate is null or p.expiryDate >= :today)
    """)
    Optional<Policy> findAvailableByPublicToken(
            @Param("token") UUID token,
            @Param("today") LocalDate today);

    @Query("""
        select p
          from Policy p
          join fetch p.policyCategory c
         where p.active = true
           and p.published = true
           and p.effectiveDate <= :today
           and (p.expiryDate is null or p.expiryDate >= :today)
         order by p.effectiveDate desc, p.code asc
    """)
    List<Policy> findAllAvailable(@Param("today") LocalDate today);
}
