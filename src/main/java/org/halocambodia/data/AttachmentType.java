package org.halocambodia.data;

import java.sql.Time;
import java.time.LocalDate;
import java.time.LocalTime;
import java.time.ZoneId;
import java.time.ZonedDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

import jakarta.persistence.CascadeType;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.OneToMany;
import jakarta.persistence.SequenceGenerator;
import jakarta.persistence.Table;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.*;

@Entity
@Table(name = "attachment_type",schema  = "public")
@Getter
@Setter
public class AttachmentType extends AbstractEntity {
	
	 @Id
	    @GeneratedValue(strategy = GenerationType.SEQUENCE, generator = "public.attachment_type_attachment_type_id_seq")
	    // The initial value is to account for data.sql demo data ids
	    @SequenceGenerator(name = "public.attachment_type_attachment_type_id_seq", initialValue = 1,allocationSize = 1)
	    @Column(name = "attachment_type_id")
	    private Long id;	 
	    
	    @Column(name = "attachment_type_name", nullable = false, unique = true)
	    @NotBlank(message = "Aattachment type name cannot be null or empty")
	    private String attachmentTypeName;
	    
	    @Column(name = "sort_order")
	    private Integer sortOrder ;
	    
	    @Column(name = "obsolete_date")
	    private LocalDate obsoleteDate;
	
		@Override
		public Long getId() {
		    return id;
		}
	 
}
