package org.halocambodia.data;

import java.sql.Blob;
import java.sql.Time;
import java.time.LocalDate;
import java.time.LocalTime;
import java.time.ZoneId;
import java.time.ZonedDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

import org.hibernate.annotations.Type;



import jakarta.persistence.Basic;
import jakarta.persistence.CascadeType;
import jakarta.persistence.Column;
import jakarta.persistence.DiscriminatorType;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.Lob;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.OneToMany;
import jakarta.persistence.SequenceGenerator;
import jakarta.persistence.Table;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;

import org.hibernate.annotations.Any;
import org.hibernate.annotations.AnyDiscriminator;
import org.hibernate.annotations.AnyDiscriminatorValue;
import org.hibernate.annotations.AnyDiscriminatorValues;
import org.hibernate.annotations.AnyKeyJavaClass;
import org.hibernate.annotations.JdbcType;
import org.hibernate.annotations.JdbcTypeCode;
import org.hibernate.type.SqlTypes;
import org.hibernate.type.descriptor.jdbc.BinaryJdbcType;

import lombok.*;

@Entity
@Table(name = "attachment",schema  = "public")
@Getter
@Setter
public class Attachment extends AbstractEntity {
	
	
	 @Id
	    @GeneratedValue(strategy = GenerationType.SEQUENCE, generator = "public.attachment_attachment_id_seq")
	    // The initial value is to account for data.sql demo data ids
	    @SequenceGenerator(name = "public.attachment_attachment_id_seq", initialValue = 1,allocationSize = 1)
	    @Column(name = "attachment_id")
	    private Long id;	 
	    
	    @ManyToOne(fetch = FetchType.LAZY)		
	    @JoinColumn(name = "attachment_type_id",nullable = false)
	    @NotNull(message = "Attachment type cannot be null or empty. Please select only one.")
	    private AttachmentType attachmentType;
	    
	    @Column(name = "file_name", nullable = false)
	    @NotBlank(message = "file_name cannot be null or empty")
	    private String fileName;
	    
	    @Column(name = "content_type", nullable = false)
	    @NotBlank(message = "content_type cannot be null or empty")
	    private String contentType;
	    
	    @Column(name = "data_size")
	    private Long  dataSize;
	    
	    @Column(name = "survey123_id")
	    private UUID  survey123Id;
	    
	    @Column(name = "remark")
	    private String  remark;
	    
	    
		@Override
		public Long getId() {
		    return id;
		}
	    
	    @Column(name = "external_data", columnDefinition = "jsonb")
	    @JdbcTypeCode(SqlTypes.JSON)
	    private String externalData;

	    
	    
	    @Lob
	    @Basic(fetch = FetchType.LAZY)
	    @Column(name = "file_data")
	    @JdbcType(BinaryJdbcType.class)
	    private byte[] fileData;
	    
	    @Any
	    @AnyDiscriminator(DiscriminatorType.STRING) // Discriminator column is of type STRING
	    @AnyKeyJavaClass(Long.class) // Foreign key type (asset_inventory_id) is Long
	    @AnyDiscriminatorValues({
	        @AnyDiscriminatorValue(discriminator = "emp_goal_setting", entity = GoalSetting.class),
	        @AnyDiscriminatorValue(discriminator = "emp_leave", entity = EmployeeLeave.class),
	        @AnyDiscriminatorValue(discriminator = "emp_master", entity = Employee.class)
	    })
	    @JoinColumn(name = "entity_id", nullable = false) // Foreign key column for the entity ID
	    @Column(name = "entity_table") 
	    private Object attachmentEntityTable;
 
}
