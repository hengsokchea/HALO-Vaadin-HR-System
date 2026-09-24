package org.halocambodia.data;

import jakarta.persistence.*;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.Getter;
import lombok.Setter;

@Entity
@Table(name = "migrate_attachment", schema = "public")
@Getter
@Setter
public class MigrateAttachment {
    
    @Id
    @GeneratedValue(strategy = GenerationType.SEQUENCE, generator = "migrate_attachment_migrate_attachment_id_seq")
    @SequenceGenerator(
        name = "migrate_attachment_migrate_attachment_id_seq",
        sequenceName = "migrate_attachment_migrate_attachment_id_seq",
        schema = "public",
        allocationSize = 1
    )
    @Column(name = "migrate_attachment_id")
    private Long id;
    
    @Column(name = "emp_id", nullable = false)
    @NotNull(message = "Employee ID cannot be null")
    private Integer empId;
    
    @Column(name = "file_name", nullable = false)
    @NotBlank(message = "File name cannot be null or empty")
    private String fileName;
    
    @Column(name = "status")
    private String status;
    
    @Column(name = "note")
    private String note;
    
    @Column(name = "attachment_type_id", nullable = false)
    @NotNull(message = "Attachment type ID cannot be null")
    private Integer attachmentTypeId;
    
    // Optional: Add relationships if needed for queries
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "emp_id", referencedColumnName = "emp_id", 
                insertable = false, updatable = false)
    private Employee employee;
    
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "attachment_type_id", referencedColumnName = "attachment_type_id",
                insertable = false, updatable = false)
    private AttachmentType attachmentType;
}