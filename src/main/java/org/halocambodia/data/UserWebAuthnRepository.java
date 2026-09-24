package org.halocambodia.data;

import java.util.List;
import java.util.Optional;

import org.springframework.data.jpa.repository.JpaRepository;

public interface UserWebAuthnRepository extends JpaRepository<UserWebAuthn, Long> {

    Optional<UserWebAuthn> findByCredentialId(String credentialId);    

    List<UserWebAuthn> findByUserId(Long userId);
    boolean existsByUser(User user);
    
    List<UserWebAuthn> findByUser(User user); 
    
    Optional<UserWebAuthn>    findByCredentialIdAndActiveTrue( String credentialId );
    Optional<UserWebAuthn> findByRawIdAndActiveTrue(String rawId);
    
    List<UserWebAuthn>    findByUserAndActiveTrue(User user );
    
}