package org.halocambodia.upload.security;

import java.util.List;
import java.util.Locale;
import java.util.Optional;

import org.halocambodia.upload.exception.UploadException;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import lombok.RequiredArgsConstructor;

@Component
@RequiredArgsConstructor
public class MimeAndExtensionValidator {

    private final UploadFileTypeRepository repository;

    /**
     * Validates only extension and MIME type.
     */
    @Transactional(readOnly = true)
    public UploadFileType validate(
            String fileName,
            String mimeType) {

        return validate(fileName, mimeType, null);
    }

    /**
     * Validates extension, MIME type, and optional file size.
     */
    @Transactional(readOnly = true)
    public UploadFileType validate(
            String fileName,
            String mimeType,
            Long fileSize) {

        String extension = extensionOf(fileName);
        String normalizedMime = normalizeMimeType(mimeType);

        if (extension.isBlank()) {
            throw new UploadException(
                    HttpStatus.UNSUPPORTED_MEDIA_TYPE,
                    "The file has no valid extension. | "
                            + "ឯកសារមិនមានផ្នែកបន្ថែមត្រឹមត្រូវទេ។"
            );
        }

        UploadFileType rule = findRule(
                extension,
                normalizedMime
        );

        if (!rule.isEnabled()) {
            throw new UploadException(
                    HttpStatus.UNSUPPORTED_MEDIA_TYPE,
                    rule.getDisplayName()
                            + " files are disabled. | "
                            + "ឯកសារប្រភេទ "
                            + rule.getDisplayName()
                            + " ត្រូវបានបិទ។"
            );
        }

        validateMaximumFileSize(
                rule,
                fileSize
        );

        return rule;
    }

    private UploadFileType findRule(
            String extension,
            String normalizedMime) {

        Optional<UploadFileType> exactRule =
                repository
                        .findFirstByExtensionIgnoreCaseAndMimeTypeIgnoreCase(
                                extension,
                                normalizedMime
                        );

        if (exactRule.isPresent()) {
            return exactRule.get();
        }

        /*
         * Some browsers use application/octet-stream for otherwise valid
         * files. Only accept it when there is exactly one enabled database
         * rule for the extension.
         */
        if ("application/octet-stream".equals(normalizedMime)) {
            List<UploadFileType> extensionRules =
                    repository
                            .findByExtensionIgnoreCaseOrderBySortOrderAsc(
                                    extension
                            );

            List<UploadFileType> enabledRules =
                    extensionRules.stream()
                            .filter(UploadFileType::isEnabled)
                            .toList();

            if (enabledRules.size() == 1) {
                return enabledRules.get(0);
            }

            if (enabledRules.size() > 1) {
                throw new UploadException(
                        HttpStatus.UNSUPPORTED_MEDIA_TYPE,
                        "The browser could not identify the file type clearly. "
                                + "Please use a file with a recognised MIME type. | "
                                + "កម្មវិធីរុករកមិនអាចកំណត់ប្រភេទឯកសារបានច្បាស់ទេ។"
                );
            }
        }

        /*
         * If a row exists for the extension but with another MIME type,
         * report a MIME mismatch rather than saying the extension is unknown.
         */
        List<UploadFileType> extensionRules =
                repository
                        .findByExtensionIgnoreCaseOrderBySortOrderAsc(
                                extension
                        );

        if (!extensionRules.isEmpty()) {
            throw new UploadException(
                    HttpStatus.UNSUPPORTED_MEDIA_TYPE,
                    "The MIME type does not match the file extension: ."
                            + extension
                            + " / "
                            + normalizedMime
                            + " | ប្រភេទ MIME មិនត្រូវគ្នានឹងផ្នែកបន្ថែមឯកសារទេ។"
            );
        }

        throw new UploadException(
                HttpStatus.UNSUPPORTED_MEDIA_TYPE,
                "This file type is not configured or allowed: ."
                        + extension
                        + " | មិនបានកំណត់ ឬមិនអនុញ្ញាតឯកសារប្រភេទនេះទេ។"
        );
    }

    private void validateMaximumFileSize(
            UploadFileType rule,
            Long fileSize) {

        if (fileSize == null || fileSize <= 0L) {
            return;
        }

        Long maximumSize = rule.getMaxFileSize();

        if (maximumSize == null || maximumSize <= 0L) {
            return;
        }

        if (fileSize > maximumSize) {
            throw new UploadException(
                    HttpStatus.PAYLOAD_TOO_LARGE,
                    rule.getDisplayName()
                            + " exceeds the maximum allowed size of "
                            + formatFileSize(maximumSize)
                            + ". | ទំហំឯកសារលើសពីកម្រិតអនុញ្ញាត "
                            + formatFileSize(maximumSize)
                            + "។"
            );
        }
    }

    private String normalizeMimeType(String mimeType) {
        if (mimeType == null || mimeType.isBlank()) {
            return "application/octet-stream";
        }

        return mimeType
                .trim()
                .toLowerCase(Locale.ROOT);
    }

    private String extensionOf(String fileName) {
        if (fileName == null || fileName.isBlank()) {
            return "";
        }

        String normalizedName =
                fileName.replace('\\', '/');

        String baseName =
                normalizedName.substring(
                        normalizedName.lastIndexOf('/') + 1
                );

        int dotIndex = baseName.lastIndexOf('.');

        if (dotIndex < 0
                || dotIndex == baseName.length() - 1) {
            return "";
        }

        return baseName
                .substring(dotIndex + 1)
                .trim()
                .toLowerCase(Locale.ROOT)
                .replaceAll("[^a-z0-9]", "");
    }

    private String formatFileSize(long bytes) {
        if (bytes < 1024L) {
            return bytes + " B";
        }

        if (bytes < 1024L * 1024L) {
            return String.format(
                    Locale.ROOT,
                    "%.2f KB",
                    bytes / 1024.0
            );
        }

        if (bytes < 1024L * 1024L * 1024L) {
            return String.format(
                    Locale.ROOT,
                    "%.2f MB",
                    bytes / (1024.0 * 1024.0)
            );
        }

        return String.format(
                Locale.ROOT,
                "%.2f GB",
                bytes / (1024.0 * 1024.0 * 1024.0)
        );
    }
}