package org.halocambodia.fileattachment.data;

import java.util.Locale;

/**
 * Broad technical file category derived from MIME type/file extension.
 * This is separate from the existing database AttachmentType entity, which
 * represents the business document type selected by the user.
 */
public enum FileAttachmentType {
    DOCUMENT,
    IMAGE,
    VIDEO,
    AUDIO,
    ARCHIVE,
    DISK_IMAGE,
    OTHER;

    public static FileAttachmentType detect(String mimeType, String fileName) {
        String mime = mimeType == null ? "" : mimeType.toLowerCase(Locale.ROOT);
        String name = fileName == null ? "" : fileName.toLowerCase(Locale.ROOT);

        if (mime.startsWith("image/")) return IMAGE;
        if (mime.startsWith("video/")) return VIDEO;
        if (mime.startsWith("audio/")) return AUDIO;
        if (name.endsWith(".zip") || name.endsWith(".rar") || name.endsWith(".7z")
                || mime.contains("zip") || mime.contains("compressed")) return ARCHIVE;
        if (name.endsWith(".iso")) return DISK_IMAGE;
        if (mime.equals("application/pdf") || mime.startsWith("text/")
                || name.endsWith(".doc") || name.endsWith(".docx")
                || name.endsWith(".xls") || name.endsWith(".xlsx")
                || name.endsWith(".ppt") || name.endsWith(".pptx")
                || name.endsWith(".csv") || name.endsWith(".txt")) return DOCUMENT;
        return OTHER;
    }
}
