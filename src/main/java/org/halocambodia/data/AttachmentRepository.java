package org.halocambodia.data;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.transaction.annotation.Transactional;

public interface AttachmentRepository extends JpaRepository<Attachment, Long>, JpaSpecificationExecutor<Attachment> {

    List<Attachment> findByFileDataIsNotNull();

    @Query("SELECT a FROM Attachment a WHERE a.attachmentEntityTable = :entity")
    List<Attachment> findByEntity(@Param("entity") Object entity);

    @Query("SELECT a FROM Attachment a WHERE a.attachmentEntityTable = :entity AND a.attachmentType.id = :typeId")
    List<Attachment> findByEntityAndType(@Param("entity") Object entity, @Param("typeId") Long typeId);

    // ✅ CORRECT: Use the discriminator value and entity ID through the @Any association
    @Query("SELECT a FROM Attachment a WHERE TYPE(a.attachmentEntityTable) = :entityClass AND a.attachmentEntityTable.id = :entityId")
    List<Attachment> findByEntityTableAndEntityId(@Param("entityClass") Class<?> entityClass, @Param("entityId") Long entityId);
    
    @Query("SELECT a FROM Attachment a WHERE TYPE(a.attachmentEntityTable) = :entityClass AND a.attachmentEntityTable.id = :entityId AND a.attachmentType = :attachmentType")
    List<Attachment> findByEntityTableAndEntityIdAndAttachmentType(
            @Param("entityClass") Class<?> entityClass, 
            @Param("entityId") Long entityId, 
            @Param("attachmentType") AttachmentType attachmentType);
    
    @Query("SELECT a FROM Attachment a WHERE TYPE(a.attachmentEntityTable) = :entityClass AND a.attachmentEntityTable.id = :entityId AND a.survey123Id = :survey123Id")
    List<Attachment> findByEntityTableAndEntityIdAndSurvey123Id(
            @Param("entityClass") Class<?> entityClass, 
            @Param("entityId") Long entityId, 
            @Param("survey123Id") UUID survey123Id);
    
    @Modifying
    @Transactional
    @Query("DELETE FROM Attachment a WHERE TYPE(a.attachmentEntityTable) = :entityClass AND a.attachmentEntityTable.id = :entityId")
    void deleteByEntityTableAndEntityId(@Param("entityClass") Class<?> entityClass, @Param("entityId") Long entityId);
}