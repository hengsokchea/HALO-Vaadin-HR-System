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
import jakarta.persistence.Version;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import lombok.*;

@Entity
@Table(name = "list_disciplinary_action",schema  = "public")
@Getter
@Setter
public class Disciplinary extends AbstractEntity {
	 @Id
	    @GeneratedValue(strategy = GenerationType.SEQUENCE, generator = "public.list_disciplinary_action_disciplinary_action_id_seq")
	    // The initial value is to account for data.sql demo data ids
	    @SequenceGenerator(name = "public.list_disciplinary_action_disciplinary_action_id_seq", initialValue = 1,allocationSize = 1)
	    @Column(name = "disciplinary_action_id")
	    private Long id; 
	    
	    @Column(name = "disciplinary_action", nullable = false, unique = true)
	    @NotNull(message = "Disciplinary Name(EN) is required")
	    @Size(min = 1, message = "Disciplinary Name(EN) cannot be empty")
	    private String disciplinaryEn;
	    
	    @Column(name = "disciplinary_action_kh")
	    private String disciplinaryKh;


	    @Version
	    @Column(name = "version")
	    private Long version;
	    
		@Override
		public Long getId() {
		    return id;
		}
}
