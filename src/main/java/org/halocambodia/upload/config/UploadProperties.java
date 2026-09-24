package org.halocambodia.upload.config;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.time.Duration;
import java.util.HashSet;
import java.util.Set;

import org.halocambodia.upload.constants.UploadConstants;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.stereotype.Component;

import jakarta.annotation.PostConstruct;
import jakarta.servlet.ServletContext;
import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
@Component
@ConfigurationProperties(prefix = "halo.upload")
public class UploadProperties {

    private static final Logger log =LoggerFactory.getLogger(UploadProperties.class);

    private static final String DATA_FOLDER = "Data";
    private static final String UPLOAD_FOLDER = "uploads";

    private final ServletContext servletContext;

    /**
     * Optional configured upload directory.
     *
     * When this property is not configured, the application automatically
     * resolves the directory from the servlet context:
     *
     * hr.war:
     * ${catalina.base}/webapps/Data/hr/uploads
     *
     * inventory.war:
     * ${catalina.base}/webapps/Data/inventory/uploads
     */
    private Path rootDirectory;

    private long defaultChunkSize =
            UploadConstants.DEFAULT_CHUNK_SIZE_BYTES;

    private long maxChunkSize =
            UploadConstants.MAX_CHUNK_SIZE_BYTES;

    private long maxFileSize =
            UploadConstants.DEFAULT_MAX_FILE_SIZE_BYTES;

    private Duration sessionTtl =
            UploadConstants.DEFAULT_SESSION_TTL;

    private Duration cleanupInterval = Duration.ofHours(1);

    private boolean checksumRequired = false;

    private int maxActiveSessionsPerUser = 10;

    private long maxReservedBytesPerUser =
            50L * 1024L * 1024L * 1024L;

    private long minimumFreeDiskBytes =
            5L * 1024L * 1024L * 1024L;

    private Duration auditRetention =
            Duration.ofDays(180);

    private Duration completedFileRetention =
            Duration.ofDays(365);

    private Duration deletedFileGracePeriod =
            Duration.ofDays(30);

    private boolean automaticCompletedFileRetentionEnabled = false;

    private Set<String> allowedMimeTypes =
            new HashSet<>(
                    UploadConstants.DEFAULT_ALLOWED_MIME_TYPES
            );

    public UploadProperties(ServletContext servletContext) {
        this.servletContext = servletContext;
    }

    @PostConstruct
    public void initialize() {
        /*
         * If halo.upload.root-directory is supplied through properties,
         * environment variables or command-line parameters, keep it.
         *
         * Otherwise, calculate the path from the servlet context.
         */
        if (rootDirectory == null) {
            rootDirectory = resolveDefaultRootDirectory();
        }

        rootDirectory =
                rootDirectory
                        .toAbsolutePath()
                        .normalize();

        createUploadDirectory();

        log.info(
                "HALO upload root directory: {}",
                rootDirectory
        );

        log.info(
                "HALO application context path: {}",
                servletContext.getContextPath()
        );

        log.info(
                "HALO resolved application name: {}",
                resolveApplicationName()
        );
    }

    /**
     * Resolves:
     *
     * ${catalina.base}/webapps/Data/{applicationName}/uploads
     */
    private Path resolveDefaultRootDirectory() {
        Path tomcatBase = resolveTomcatBase();

        return tomcatBase
                .resolve("webapps")
                .resolve(DATA_FOLDER)
                .resolve(resolveApplicationName())
              .resolve(UPLOAD_FOLDER)
                .normalize();
    }

    /**
     * Tomcat provides catalina.base automatically.
     *
     * During development with Spring Boot, catalina.base may not exist.
     * In that case, user.dir is used as a safe development fallback.
     */
    private Path resolveTomcatBase() {
        String catalinaBase =
                System.getProperty("catalina.base");

        if (catalinaBase != null
                && !catalinaBase.isBlank()) {

            return Path.of(catalinaBase.trim())
                    .toAbsolutePath()
                    .normalize();
        }

        String workingDirectory =
                System.getProperty("user.dir", ".");

        log.warn(
                "System property catalina.base is unavailable. "
                        + "Using development working directory: {}",
                workingDirectory
        );

        return Path.of(workingDirectory)
                .toAbsolutePath()
                .normalize();
    }

    /**
     * Examples:
     *
     * /hr        -> hr
     * /inventory -> inventory
     * /finance   -> finance
     * empty      -> ROOT
     *
     * The context path normally comes from the deployed WAR filename.
     */
    private String resolveApplicationName() {
        String contextPath =
                servletContext.getContextPath();

        if (contextPath == null
                || contextPath.isBlank()
                || "/".equals(contextPath)) {

            return "ROOT";
        }

        String applicationName =
                contextPath.trim();

        while (applicationName.startsWith("/")) {
            applicationName =
                    applicationName.substring(1);
        }

        while (applicationName.endsWith("/")) {
            applicationName =
                    applicationName.substring(
                            0,
                            applicationName.length() - 1
                    );
        }

        /*
         * Nested context paths such as /halo/hr become halo_hr.
         */
        applicationName =
                applicationName.replace('/', '_');

        /*
         * Prevent unsupported filesystem characters and path traversal.
         */
        applicationName =
                applicationName.replaceAll(
                        "[^A-Za-z0-9._-]",
                        "_"
                );

        if (applicationName.isBlank()
                || ".".equals(applicationName)
                || "..".equals(applicationName)) {

            return "ROOT";
        }

        return applicationName;
    }

    private void createUploadDirectory() {
        try {
            Files.createDirectories(rootDirectory);

            if (!Files.isDirectory(rootDirectory)) {
                throw new IllegalStateException(
                        "Upload root is not a directory: "
                                + rootDirectory
                );
            }

            if (!Files.isWritable(rootDirectory)) {
                throw new IllegalStateException(
                        "Upload root is not writable: "
                                + rootDirectory
                );
            }

        } catch (IOException exception) {
            throw new IllegalStateException(
                    "Unable to create upload root directory: "
                            + rootDirectory,
                    exception
            );
        }
    }
}