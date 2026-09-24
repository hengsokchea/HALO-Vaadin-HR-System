package org.halocambodia.utility;

import com.vaadin.flow.server.VaadinRequest;
import com.vaadin.flow.server.VaadinService;

public final class ApplicationUrlUtil {

    private ApplicationUrlUtil() {
    }

    public static String getContextPath() {
        VaadinRequest request =
                VaadinService.getCurrentRequest();

        if (request == null) {
            return "";
        }

        String contextPath =
                request.getContextPath();

        return normalizeContextPath(contextPath);
    }

    public static String contextUrl(String path) {
        String contextPath =
                getContextPath();

        String normalizedPath =
                normalizePath(path);

        return contextPath + normalizedPath;
    }

    private static String normalizeContextPath(
            String contextPath) {

        if (contextPath == null
                || contextPath.isBlank()
                || "/".equals(contextPath)) {

            return "";
        }

        String normalized =
                contextPath.trim();

        if (!normalized.startsWith("/")) {
            normalized = "/" + normalized;
        }

        while (normalized.length() > 1
                && normalized.endsWith("/")) {

            normalized =
                    normalized.substring(
                            0,
                            normalized.length() - 1
                    );
        }

        return normalized;
    }

    private static String normalizePath(String path) {
        if (path == null || path.isBlank()) {
            return "";
        }

        String normalized = path.trim();

        if (!normalized.startsWith("/")) {
            normalized = "/" + normalized;
        }

        return normalized;
    }
}