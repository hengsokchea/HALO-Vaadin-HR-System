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
import jakarta.persistence.Transient;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;
import lombok.*;

@Entity
@Table(name = "data_feed",schema  = "core_system")
@Getter
@Setter
public class DataFeeds extends AbstractEntity {
	
	
	 @Id
	    @GeneratedValue(strategy = GenerationType.SEQUENCE, generator = "core_system.data_feed_data_feed_id_seq")
	    // The initial value is to account for data.sql demo data ids
	    @SequenceGenerator(name = "core_system.data_feed_data_feed_id_seq", initialValue = 1,allocationSize = 1)
	    @Column(name = "data_feed_id")
	    private Long id;	 
	    
	    @Column(name = "data_feed_name", nullable = false, unique = true)
	    @NotBlank(message = "Data feed name cannot be null or empty")
	    @Size(min = 1, message = "Data Feed Name cannot be empty")
	    private String dataFeedName;
	    
	    @Column(name = "first_query", nullable = false)
	    @NotBlank(message = "First Query cannot be null or empty")
	    @Size(min = 1, message = "First Query cannot be empty")
	    private String firstQuery;
	    
	    @Column(name = "is_active", nullable = false)
	    @NotNull(message = "Run in the background? cannot be null")
	    private Boolean runInBackground = false;
	    
	    @Column(name = "last_batch_id")
	    private Long lastBatchID ;
	    
	    @Column(name = "schedule_start_time", nullable = false)
	    @NotNull(message = "schedule start time cannot be null or empty")
	    private LocalTime   scheduleStartTime;
	    
	    @Column(name = "schedule_end_time", nullable = false)
	    @NotNull(message = "schedule end time cannot be null or empty")
	    private LocalTime   scheduleEndTime;
	    
	    @Column(name = "frequency_value")
	    private Integer frequencyValue ;
	    
	    @Column(name = "frequency_units")
	    private String frequencyUnits ;
	    
	    @Column(name = "last_run", columnDefinition = "TIMESTAMP(0) WITH TIME ZONE")
	    private ZonedDateTime lastRun;  
	    
	    @OneToMany(mappedBy = "dataFeeds", cascade = CascadeType.ALL, fetch = FetchType.LAZY)
	    private List<DataFeedsStep> dataFeedsSteps= new ArrayList<>();
	    
	    
	    @OneToMany(mappedBy = "dataFeeds", cascade = CascadeType.ALL, fetch = FetchType.LAZY)
	    private List<DataFeedsLog> dataFeedsLogs= new ArrayList<>();
	    
	    //access function
		public ZonedDateTime getLastRun() {
			return lastRun != null  ? lastRun.withZoneSameInstant(ZoneId.of("Asia/Phnom_Penh")) : null;
		}
		
		@Override
		public Long getId() {
		    return id;
		}
		
		@Transient
		private Integer errorCount = 0;
	 
}
