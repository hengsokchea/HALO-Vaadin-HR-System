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
import lombok.*;

@Entity
@Table(name = "list_career_type_group",schema  = "public")
@Getter
@Setter
public class CareerTypeGroup extends AbstractEntity {
	@Override
	public Long getId() {
	    return id;
	}
	 @Id
	 @GeneratedValue(strategy = GenerationType.SEQUENCE, generator = "public.list_career_type_group_list_career_type_group_id_seq")
	    // The initial value is to account for data.sql demo data ids
	 @SequenceGenerator(name = "public.list_career_type_group_list_career_type_group_id_seq", initialValue = 1,allocationSize = 1)
	 @Column(name = "list_career_type_group_id")
	 private Long id; 
	    
	 @Column(name = "career_type_group_name", nullable = false, unique = true)
	 @NotBlank(message = "Career type group  cannot be null or empty")
	 private String careerTypeGroupName;
	    
	 @Column(name = "note")
	 private String remark;
	 
	 @OneToMany(mappedBy = "careerTypeGroup", cascade = CascadeType.ALL, fetch = FetchType.LAZY)
	 private List<CareerType> careerTypes = new ArrayList<>();

}
