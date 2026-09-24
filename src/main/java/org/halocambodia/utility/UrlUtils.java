package org.halocambodia.utility;

import com.vaadin.flow.server.VaadinServletRequest;
import jakarta.servlet.http.HttpServletRequest;
import org.springframework.web.util.UriComponentsBuilder;

public class UrlUtils {
    
    private UrlUtils() {
        // Private constructor to prevent instantiation
    }
    
    /**
     * Gets the base URL of the application including context path
     * Example: https://web.halocambodia.local/hr
     */
    public static String getBaseUrl() {
        HttpServletRequest request = VaadinServletRequest.getCurrent();
        if (request == null) {
            throw new IllegalStateException("No current request available");
        }
        
        return request.getRequestURL().toString()
            .replace(request.getRequestURI(), request.getContextPath());
    }
    
    /**
     * Gets the base URL using UriComponentsBuilder (more robust)
     */
    public static String getBaseUrlUsingBuilder() {
        HttpServletRequest request = VaadinServletRequest.getCurrent();
        if (request == null) {
            throw new IllegalStateException("No current request available");
        }
        
        // Use fromUriString() instead of fromHttpUrl()
        return UriComponentsBuilder.fromUriString(request.getRequestURL().toString())
            .replacePath(request.getContextPath())
            .build()
            .toUriString();
    }
    
    /**
     * Builds a full URL with path
     * Example: https://web.halocambodia.local/hr/leave-action/TOKEN
     */
    public static String buildUrl(String path) {
        String baseUrl = getBaseUrl();
        // Ensure path starts with /
        if (!path.startsWith("/")) {
            path = "/" + path;
        }
        return baseUrl + path;
    }
    
    /**
     * Builds a URL with path and query parameters
     * Example: https://web.halocambodia.local/hr/leave/action?token=ABC123
     */
    public static String buildUrlWithParams(String path, String... params) {
        StringBuilder url = new StringBuilder(buildUrl(path));
        if (params.length > 0) {
            url.append("?");
            for (int i = 0; i < params.length; i += 2) {
                if (i > 0) url.append("&");
                url.append(params[i]).append("=").append(params[i + 1]);
            }
        }
        return url.toString();
    }
}