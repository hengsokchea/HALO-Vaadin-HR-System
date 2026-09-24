package org.halocambodia.security;

import org.halocambodia.data.User;
import org.halocambodia.data.UserRepository;
import org.springframework.security.authentication.AuthenticationProvider;
import org.springframework.security.authentication.BadCredentialsException;
import org.springframework.security.authentication.DisabledException;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.AuthenticationException;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Component;

import java.util.List;

@Component
public class CustomAuthenticationProvider implements AuthenticationProvider {

    private final UserRepository userRepository;
    private final PasswordEncoder passwordEncoder;
    private final UserDetailsServiceImpl userDetailsService;

    public CustomAuthenticationProvider(UserRepository userRepository,
                                        PasswordEncoder passwordEncoder,
                                        UserDetailsServiceImpl userDetailsService) {
        this.userRepository = userRepository;
        this.passwordEncoder = passwordEncoder;
        this.userDetailsService = userDetailsService;
    }

/*    @Override
    public Authentication authenticate(Authentication authentication) throws AuthenticationException {
        String username = authentication.getName();
        String rawPassword = authentication.getCredentials().toString();

        List<User> users = userRepository.findAllByUsername(username);
        if (users.isEmpty()) {
            throw new BadCredentialsException("Invalid username or password");
        }

        for (User user : users) {
            
            if (passwordEncoder.matches(rawPassword, user.getHashedPassword())) {
                CustomUserPrincipal principal =
                        new CustomUserPrincipal(user, userDetailsService.getAuthorities(user.getRoles()));

                return new UsernamePasswordAuthenticationToken(principal, rawPassword, principal.getAuthorities());
            }
        }

        throw new BadCredentialsException("Invalid username or password");
    }
    */
    
    @Override
    public Authentication authenticate(Authentication authentication) throws AuthenticationException {
        final String username = authentication.getName() == null ? "" : authentication.getName().trim();
        final String rawPassword = authentication.getCredentials() == null ? "" : authentication.getCredentials().toString();

        // Case-insensitive lookup; create this repo method if you don't have it yet.
        List<User> candidates = userRepository.findAllByUsernameIgnoreCase(username);
        if (candidates == null || candidates.isEmpty()) {
            throw new BadCredentialsException("Invalid username or password");
        }

        boolean sawDisabledMatchingUser = false;

        for (User u : candidates) {
            String hash = u.getHashedPassword(); // ensure this is the right field

            boolean matches = false;
            if (hash != null && !hash.isBlank()) {
                // Primary: encoded match (bcrypt/pbkdf2/etc.)
                try {
                    matches = passwordEncoder.matches(rawPassword, hash);
                } catch (IllegalArgumentException ex) {
                    // Happens if encoder can't parse hash (e.g., wrong prefix) – treat as no match below.
                    matches = false;
                }

                // TEMPORARY fallback: allow plaintext if your DB still has old values (remove once migrated)
                if (!matches && !hash.startsWith("$")) { // crude but effective plaintext hint
                    matches = rawPassword.equals(hash);
                }
            }

            if (matches) {
                if (Boolean.TRUE.equals(u.getCanLogin())) {
                    CustomUserPrincipal principal =
                        new CustomUserPrincipal(u, userDetailsService.getAuthorities(u.getRoles()));
                    // do NOT store raw password in the token; use null
                    return new UsernamePasswordAuthenticationToken(principal, null, principal.getAuthorities());
                } else {
                    sawDisabledMatchingUser = true;
                }
            }
        }

        if (sawDisabledMatchingUser) {
            // You’ll need a failure handler to show a friendly message; see below.
            throw new org.springframework.security.authentication.DisabledException("Account disabled");
        }

        throw new BadCredentialsException("Invalid username or password");
    }


    @Override
    public boolean supports(Class<?> authentication) {
        return UsernamePasswordAuthenticationToken.class.isAssignableFrom(authentication);
    }
}
