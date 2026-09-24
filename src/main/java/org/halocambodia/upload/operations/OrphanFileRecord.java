package org.halocambodia.upload.operations;

import java.nio.file.Path;
import java.util.UUID;

public record OrphanFileRecord(
        OrphanType type,
        UUID uploadUuid,
        String databaseFileName,
        Path filesystemPath,
        long size,
        String message) {

    public enum OrphanType {
        DATABASE_RECORD_WITHOUT_FILE,
        FILE_WITHOUT_DATABASE_RECORD
    }
}
