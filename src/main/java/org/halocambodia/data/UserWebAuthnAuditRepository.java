package org.halocambodia.data;

import org.springframework.data.jpa.repository.JpaRepository;

public interface UserWebAuthnAuditRepository extends JpaRepository<UserWebAuthnAudit, Long> {
}
