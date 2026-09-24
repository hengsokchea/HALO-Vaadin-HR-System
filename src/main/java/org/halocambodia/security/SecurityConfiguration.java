package org.halocambodia.security;

import com.vaadin.flow.spring.security.VaadinSecurityConfigurer;
import org.halocambodia.views.login.LoginView;
import org.springframework.boot.security.autoconfigure.web.servlet.PathRequest;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.DisabledException;
import org.springframework.security.config.annotation.authentication.configuration.AuthenticationConfiguration;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.web.SecurityFilterChain;

@Configuration
@EnableWebSecurity
public class SecurityConfiguration {

    @Bean
    public PasswordEncoder passwordEncoder() {
        return new BCryptPasswordEncoder();
    }

    @Bean
    public AuthenticationManager authenticationManager(
            AuthenticationConfiguration authenticationConfiguration) throws Exception {
        return authenticationConfiguration.getAuthenticationManager();
    }

    @Bean
    public SecurityFilterChain securityFilterChain(HttpSecurity http) throws Exception {

        http.authorizeHttpRequests(auth -> auth
                // Spring Boot standard static resources
                .requestMatchers(PathRequest.toStaticResources().atCommonLocations()).permitAll()

                // Public Vaadin and application frontend resources
                .requestMatchers(
                        "/VAADIN/**",
                        "/frontend/**",
                        "/line-awesome/**",
                        "/images/**",
                        "/icons/**",
                        "/assets/**",
                        "/favicon.ico",
                        "/manifest.webmanifest",
                        "/sw.js",
                        "/offline.html",
                        "/robots.txt"
                ).permitAll()

                // Upload administration endpoints must be declared first
                .requestMatchers("/api/uploads/admin/**")
                .hasAnyRole("ADMIN", "UPLOAD_ADMIN")

                // Normal upload API requires an authenticated login
                .requestMatchers("/api/uploads/**")
                .authenticated()

                // Legacy file API should not be public
                .requestMatchers("/api/files/**")
                .authenticated()
                
                .requestMatchers(
                        "/actuator/health",
                        "/actuator/info"
                ).permitAll()

                .requestMatchers(
                        "/actuator/haloUpload",
                        "/actuator/metrics/**",
                        "/actuator/prometheus"
                ).permitAll()
                
                .requestMatchers(
                	    "/policy/read/**"
                	).permitAll()
        );

        // Required for PDF/document preview in an iframe.
        // Consider sameOrigin() instead of disable() when all previews are same-origin.
        http.headers(headers ->
                headers.frameOptions(frameOptions -> frameOptions.sameOrigin())
        );

        /*
         * The upload JavaScript sends same-origin authenticated requests.
         * Ignore CSRF only for these REST endpoints so chunk POST/DELETE requests
         * are not rejected with HTTP 403.
         *
         * Authentication and role checks above still remain active.
         */
        http.csrf(csrf -> csrf
                .ignoringRequestMatchers(
                        "/api/uploads/**",
                        "/api/files/**"
                )
        );

        // Vaadin security must be applied after custom request matchers.
        http.with(VaadinSecurityConfigurer.vaadin(),
                vaadin -> vaadin.loginView(LoginView.class));

       /* http.formLogin(form -> form
                .failureHandler((request, response, exception) -> {
                    if (exception instanceof DisabledException) {
                        response.sendRedirect("/login?disabled");
                    } else {
                        response.sendRedirect("/login?error");
                    }
                })
        );
        */
        http.formLogin(form -> form
        	    .failureHandler((request, response, exception) -> {
        	        String contextPath = request.getContextPath();

        	        if (exception instanceof DisabledException) {
        	            response.sendRedirect(contextPath + "/login?disabled");
        	        } else {
        	            response.sendRedirect(contextPath + "/login?error");
        	        }
        	    })
        	);

        return http.build();
    }
}
