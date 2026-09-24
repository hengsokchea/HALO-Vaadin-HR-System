package org.halocambodia.data;

import java.time.LocalTime;
import java.time.ZonedDateTime;
import java.util.List;

import org.hibernate.annotations.JdbcTypeCode;
import org.hibernate.annotations.Type;
import org.hibernate.type.SqlTypes;



import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.SequenceGenerator;
import jakarta.persistence.Table;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.Getter;
import lombok.Setter;

@Entity
@Table(name = "data_explorer_user",schema  = "core_system")
@Getter
@Setter
public class DataExplorerUser extends AbstractEntity{
	
	
	 @Id
	    @GeneratedValue(strategy = GenerationType.SEQUENCE, generator = "core_system.data_explorer_user_data_explorer_user_id_seq")
	    // The initial value is to account for data.sql demo data ids
	    @SequenceGenerator(name = "core_system.data_explorer_user_data_explorer_user_id_seq", initialValue = 1,allocationSize = 1)
	    @Column(name = "data_explorer_user_id")
	    private Long id;
	 
	    @ManyToOne(fetch = FetchType.LAZY)		
	    @JoinColumn(name = "data_explorer_id",nullable = false)
	    @NotNull(message = "Data explorer cannot be null or empty. Please select only one.")
	    private DataExplorer dataExplorer;
	    
	    @ManyToOne(fetch = FetchType.LAZY)		
	    @JoinColumn(name = "user_id",nullable = false)
	    @NotNull(message = "User cannot be null or empty. Please select only one.")
	    private User user;
	    
	    
	
	    
	    @Column(name = "sort_order")	   
	    private Integer sortOrder;


		@Override
		public Long getId() {
		    return id;
		}
	    
	    @Column(name = "config", columnDefinition = "jsonb")
	    @JdbcTypeCode(SqlTypes.JSON)
	    private String config;

}
