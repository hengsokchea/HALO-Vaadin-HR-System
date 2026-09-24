package org.halocambodia.upload.security;

import java.util.List;
import java.util.Optional;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;

public interface UploadFileTypeRepository
        extends JpaRepository<UploadFileType, Long>,
                JpaSpecificationExecutor<UploadFileType> {

    Optional<UploadFileType>
            findFirstByExtensionIgnoreCaseAndMimeTypeIgnoreCase(
                    String extension,
                    String mimeType
            );

    Optional<UploadFileType>
            findFirstByExtensionIgnoreCaseAndMimeTypeIgnoreCaseAndEnabledTrue(
                    String extension,
                    String mimeType
            );

    List<UploadFileType>
            findByExtensionIgnoreCaseOrderBySortOrderAsc(
                    String extension
            );

    List<UploadFileType>
            findByEnabledTrueOrderBySortOrderAsc();

    boolean existsByExtensionIgnoreCaseAndMimeTypeIgnoreCase(
            String extension,
            String mimeType
    );
}