package org.halocambodia.data;

import java.util.List;
import java.util.Optional;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;
import org.springframework.data.jpa.repository.Query;

public interface AttachmentTypeRepository  extends JpaRepository<AttachmentType, Long>, JpaSpecificationExecutor<AttachmentType> {

    List<AttachmentType> findAllByObsoleteDateIsNullOrderBySortOrder();
    
    @Query("SELECT at FROM AttachmentType at WHERE at.obsoleteDate IS NULL ORDER BY at.sortOrder ASC NULLS LAST")
    List<AttachmentType> findActiveAttachmentTypes();
    
    Optional<AttachmentType> findByAttachmentTypeName(String name);
}
