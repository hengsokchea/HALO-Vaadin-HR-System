package org.halocambodia.data;

import java.sql.Time;
import java.time.LocalDate;
import java.time.LocalTime;
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
@Table(name = "data_feed_step",schema  = "core_system")
@Getter
@Setter
public class DataFeedsStep extends AbstractEntity {
	 @Id
	    @GeneratedValue(strategy = GenerationType.SEQUENCE, generator = "core_system.data_feed_step_data_feed_step_id_seq")
	    // The initial value is to account for data.sql demo data ids
	    @SequenceGenerator(name = "core_system.data_feed_step_data_feed_step_id_seq", initialValue = 1,allocationSize = 1)
	    @Column(name = "data_feed_step_id")
	    private Long id;	 
	 
	    @ManyToOne(fetch = FetchType.LAZY)		
	    @JoinColumn(name = "data_feed_id",nullable = false)
	    @NotNull(message = "DataFeeds cannot be null or empty. Please select only one.")
	    private DataFeeds dataFeeds;
	 
	    @Column(name = "step_number")
	    private Integer stepNumber ;
	    
	    @Column(name = "step_description", nullable = false)
	    @NotBlank(message = "Step Description cannot be null or empty")
	    private String stepDescription;
	    
	    @Column(name = "step_query", nullable = false)
	    @NotBlank(message = "Step Query cannot be null or empty")
	    private String stepQuery;
	    
	    
	    @Column(name = "is_active", nullable = false)
	    @NotNull(message = "Is Active cannot be null")
	    private Boolean isActive = false;

	    
		@Override
		public Long getId() {
		    return id;
		}
}
