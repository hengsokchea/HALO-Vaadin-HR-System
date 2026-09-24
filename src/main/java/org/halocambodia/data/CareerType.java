package org.halocambodia.data;

import java.sql.Time;
import java.time.LocalDate;
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
import jakarta.persistence.Transient;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import lombok.*;

@Entity
@Table(name = "list_career_type",schema  = "public")
@Getter
@Setter
public class CareerType extends AbstractEntity {
	
	
	 @Id
	 @GeneratedValue(strategy = GenerationType.SEQUENCE, generator = "public.list_career_type_career_type_id_seq")
	    // The initial value is to account for data.sql demo data ids
	 @SequenceGenerator(name = "public.list_career_type_career_type_id_seq", initialValue = 1,allocationSize = 1)
	 @Column(name = "career_type_id")
	 private Long id; 
	    
	 @Column(name = "career_type", nullable = false, unique = true)
	 @NotNull(message = "Career Name(EN) is required")
	 @Size(min = 1, message = "Career Name(EN) cannot be empty")
	 private String careerTypeName;
	    
	 @Column(name = "remark")
	 private String remark;
	 
	 @ManyToOne(fetch = FetchType.LAZY)	
	 @JoinColumn(name = "list_career_type_group_id",nullable = false)
	 @NotNull(message = "Career Type Group cannot be null or empty. Please select only one.")
	 private CareerTypeGroup careerTypeGroup;
	 
		@Override
		public Long getId() {
		    return id;
		}

}
