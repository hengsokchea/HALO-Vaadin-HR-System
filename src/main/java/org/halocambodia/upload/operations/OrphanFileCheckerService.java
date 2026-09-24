package org.halocambodia.upload.operations;

import lombok.RequiredArgsConstructor;
import org.halocambodia.upload.config.UploadProperties;
import org.halocambodia.upload.domain.UploadSession;
import org.halocambodia.upload.domain.UploadStatus;
import org.halocambodia.upload.repository.UploadSessionRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.stream.Stream;

@Service
@RequiredArgsConstructor
public class OrphanFileCheckerService {

    private final UploadSessionRepository repository;
    private final UploadProperties properties;

    @Transactional(readOnly = true)
    public List<OrphanFileRecord> scan() {
        Path root = properties.getRootDirectory().resolve("files").toAbsolutePath().normalize();
        List<UploadSession> completed = repository.findByStatus(UploadStatus.COMPLETED);
        Map<Path, UploadSession> expected = new HashMap<>();
        List<OrphanFileRecord> result = new ArrayList<>();

        for (UploadSession session : completed) {
            if (session.getFinalPath() == null || session.getFinalPath().isBlank()) {
                result.add(new OrphanFileRecord(
                        OrphanFileRecord.OrphanType.DATABASE_RECORD_WITHOUT_FILE,
                        session.getUploadUuid(), session.getOriginalFileName(), null, 0,
                        "Completed database record has no final path"));
                continue;
            }

            Path path = Path.of(session.getFinalPath()).toAbsolutePath().normalize();
            expected.put(path, session);
            if (!path.startsWith(root) || !Files.isRegularFile(path)) {
                result.add(new OrphanFileRecord(
                        OrphanFileRecord.OrphanType.DATABASE_RECORD_WITHOUT_FILE,
                        session.getUploadUuid(), session.getOriginalFileName(), path, 0,
                        !path.startsWith(root) ? "Final path is outside upload root" : "Physical file is missing"));
            }
        }

        if (!Files.exists(root)) {
            return result;
        }

        try (Stream<Path> paths = Files.walk(root)) {
            paths.filter(Files::isRegularFile)
                    .map(path -> path.toAbsolutePath().normalize())
                    .filter(path -> !expected.containsKey(path))
                    .forEach(path -> result.add(new OrphanFileRecord(
                            OrphanFileRecord.OrphanType.FILE_WITHOUT_DATABASE_RECORD,
                            uuidFromFileName(path.getFileName().toString()), null, path, safeSize(path),
                            "Physical file has no completed upload database record")));
        } catch (IOException exception) {
            throw new IllegalStateException("Cannot scan completed upload directory", exception);
        }

        return result;
    }

    public boolean deletePhysicalOrphan(OrphanFileRecord orphan) {
        if (orphan.type() != OrphanFileRecord.OrphanType.FILE_WITHOUT_DATABASE_RECORD
                || orphan.filesystemPath() == null) {
            return false;
        }
        Path root = properties.getRootDirectory().resolve("files").toAbsolutePath().normalize();
        Path target = orphan.filesystemPath().toAbsolutePath().normalize();
        if (!target.startsWith(root)) {
            throw new IllegalArgumentException("Refusing to delete a path outside the upload root");
        }
        try {
            return Files.deleteIfExists(target);
        } catch (IOException exception) {
            throw new IllegalStateException("Cannot delete orphan file " + target, exception);
        }
    }

    private long safeSize(Path path) {
        try {
            return Files.size(path);
        } catch (IOException ignored) {
            return 0L;
        }
    }

    private UUID uuidFromFileName(String name) {
        int dot = name.indexOf('.');
        String candidate = dot < 0 ? name : name.substring(0, dot);
        try {
            return UUID.fromString(candidate);
        } catch (IllegalArgumentException ignored) {
            return null;
        }
    }
}
