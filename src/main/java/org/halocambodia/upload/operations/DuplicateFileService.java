package org.halocambodia.upload.operations;

import lombok.RequiredArgsConstructor;
import org.halocambodia.upload.domain.UploadSession;
import org.halocambodia.upload.domain.UploadStatus;
import org.halocambodia.upload.repository.UploadSessionRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.Comparator;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class DuplicateFileService {

    private final UploadSessionRepository repository;

    public List<DuplicateFileGroup> findDuplicates() {
        Map<String, List<UploadSession>> grouped = repository.findByStatus(UploadStatus.COMPLETED).stream()
                .filter(session -> session.getChecksumSha256() != null && !session.getChecksumSha256().isBlank())
                .collect(Collectors.groupingBy(UploadSession::getChecksumSha256));

        return grouped.entrySet().stream()
                .filter(entry -> entry.getValue().size() > 1)
                .map(entry -> {
                    List<UploadSession> uploads = entry.getValue().stream()
                            .sorted(Comparator.comparing(UploadSession::getCreatedAt))
                            .toList();
                    long size = uploads.getFirst().getTotalSize() == null ? 0 : uploads.getFirst().getTotalSize();
                    return new DuplicateFileGroup(entry.getKey(), size,
                            size * Math.max(0, uploads.size() - 1L), uploads);
                })
                .sorted(Comparator.comparingLong(DuplicateFileGroup::reclaimableBytes).reversed())
                .toList();
    }
}
