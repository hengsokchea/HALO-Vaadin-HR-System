package org.halocambodia.data;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.stereotype.Repository;
import java.util.List;

@Repository
public interface MigrateAttachmentRepository extends JpaRepository<MigrateAttachment, Long> {
    
    @Query("SELECT ma FROM MigrateAttachment ma WHERE ma.status IS NULL OR ma.status != 'MIGRATED'")
    List<MigrateAttachment> findPendingMigrations();
    
    List<MigrateAttachment> findByStatus(String status);
    
    List<MigrateAttachment> findByEmpId(Integer empId);
    
    long countByStatus(String status);
    
    @Query("SELECT ma FROM MigrateAttachment ma WHERE ma.status = 'FILE_NOT_FOUND' OR ma.status LIKE 'ERROR:%'")
    List<MigrateAttachment> findFailedMigrations();
}