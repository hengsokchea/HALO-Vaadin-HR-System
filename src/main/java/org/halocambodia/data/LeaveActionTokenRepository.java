package org.halocambodia.data;

import java.util.Optional;

import org.halocambodia.data.LeaveActionToken;
import org.springframework.data.jpa.repository.JpaRepository;

public interface LeaveActionTokenRepository extends JpaRepository<LeaveActionToken, Long> {

	Optional<LeaveActionToken>	findByTokenHash(String tokenHash);
}