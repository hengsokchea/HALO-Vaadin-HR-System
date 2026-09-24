package org.halocambodia.security;

import com.vaadin.flow.component.UI;
import com.vaadin.flow.router.Route;
import com.vaadin.flow.server.VaadinServletRequest;
import com.vaadin.flow.server.VaadinServletResponse;
import com.vaadin.flow.server.VaadinSession;

import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;

import org.halocambodia.data.AccessPageType;
import org.halocambodia.data.User;
import org.halocambodia.data.UserRepository;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.web.authentication.logout.SecurityContextLogoutHandler;
import org.springframework.stereotype.Component;

import java.util.List;
import java.util.Optional;

@Component
public class AuthenticatedUser {

    private final UserRepository userRepository;

    public AuthenticatedUser(UserRepository userRepository) {
        this.userRepository = userRepository;
    }

    public Optional<User> get() {
        Authentication auth = SecurityContextHolder.getContext().getAuthentication();
        if (auth == null || !(auth.getPrincipal() instanceof CustomUserPrincipal principal)) {
            return Optional.empty();
        }
        return userRepository.findById(principal.getUserId());
    }

    public void logout() {
        VaadinSession vaadinSession = VaadinSession.getCurrent();
        if (vaadinSession != null) {
            vaadinSession.setAttribute("webauthn-login-challenge",null);
            vaadinSession.setAttribute("webauthn-register-challenge",null);
        }
        
        UI ui = UI.getCurrent();

        // Get native servlet request/response
        HttpServletRequest request = VaadinServletRequest.getCurrent().getHttpServletRequest();
        HttpServletResponse response = VaadinServletResponse.getCurrent().getHttpServletResponse();

        // Tell Spring Security to log out
        new SecurityContextLogoutHandler().logout(request, response, null);

        // Close Vaadin session
        if (vaadinSession != null) {
            vaadinSession.close();
        }

        // Redirect to login page
        ui.getPage().setLocation("login");
    }

    public boolean hasRole(String role) {
        return get().map(user -> user.getRoles().stream()
                .anyMatch(r -> r.getName().equals(role)))
                .orElse(false);
    }

    public boolean hasPermissionRoute(String routeValue, AccessPageType accessPageType) {
        if (routeValue == null || routeValue.isBlank()) {
            return false;
        }
        User currentUser = get().orElseThrow(() -> new IllegalArgumentException("User not logged in"));
        return !userRepository.fetchUserPermissions(
                currentUser.getId(), routeValue.trim(), accessPageType.name()).isEmpty();
    }

    public boolean hasPage(Class<?> viewClass, AccessPageType accessPageType) {
        if (!viewClass.isAnnotationPresent(Route.class)) {
            return false;
        }
        String routeKey = viewClass.getAnnotation(Route.class).value().split("/")[0];
        User currentUser = get().orElseThrow(() -> new IllegalArgumentException("User not logged in"));

        List<Object[]> results = this.userRepository.fetchUserPermissions(
                currentUser.getId(), routeKey, accessPageType.name()
        );
        return !results.isEmpty();
    }
}
