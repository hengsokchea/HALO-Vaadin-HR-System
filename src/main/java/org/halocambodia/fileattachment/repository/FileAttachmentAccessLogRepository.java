package org.halocambodia.fileattachment.repository;

import java.util.List;

import org.halocambodia.fileattachment.data.FileAttachmentAccessLog;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface FileAttachmentAccessLogRepository       extends JpaRepository<FileAttachmentAccessLog, Long> {

    List<FileAttachmentAccessLog> findTop500ByOrderByAccessedAtDesc();

    List<FileAttachmentAccessLog> findByFileAttachmentIdOrderByAccessedAtDesc( Long fileAttachmentId  );

    List<FileAttachmentAccessLog> findByOwnerTypeAndOwnerIdOrderByAccessedAtDesc(String ownerType, Long ownerId );
}