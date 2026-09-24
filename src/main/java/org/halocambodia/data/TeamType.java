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
@Table(name = "list_team_type",schema  = "public")
@Getter
@Setter
public class TeamType extends AbstractEntity {
	 @Id
	    @GeneratedValue(strategy = GenerationType.SEQUENCE, generator = "public.list_team_type_list_team_type_id_seq")
	    // The initial value is to account for data.sql demo data ids
	    @SequenceGenerator(name = "public.list_team_type_list_team_type_id_seq", initialValue = 1,allocationSize = 1)
	    @Column(name = "list_team_type_id")
	    private Long id; 
	 
		@Override
		public Long getId() {
		    return id;
		}
	    
	    @Column(name = "list_team_type_name", nullable = false)
	    @NotBlank(message = "Team type name name cannot be null or empty")
	    private String teamTypeName;

	    
	    @OneToMany(mappedBy = "teamType", cascade = CascadeType.ALL, fetch = FetchType.LAZY)
	    private List<Teams> teams = new ArrayList<>();

}
