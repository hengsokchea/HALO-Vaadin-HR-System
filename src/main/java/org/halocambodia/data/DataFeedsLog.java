package org.halocambodia.data;


import org.hibernate.annotations.JdbcTypeCode;
import org.hibernate.annotations.Type;
import org.hibernate.type.SqlTypes;

import java.sql.Time;
import java.time.LocalDate;
import java.time.LocalTime;
import java.time.ZoneId;
import java.time.ZonedDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

import jakarta.persistence.Basic;
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
@Table(name = "data_feed_log",schema  = "core_system")
@Getter
@Setter
public class DataFeedsLog  {
	
	
	 @Id
	    @GeneratedValue(strategy = GenerationType.SEQUENCE, generator = "core_system.data_feed_log_data_feed_log_id_seq")
	    // The initial value is to account for data.sql demo data ids
	    @SequenceGenerator(name = "core_system.data_feed_log_data_feed_log_id_seq", initialValue = 1,allocationSize = 1)
	    @Column(name = "data_feed_log_id")
	    private Long id;	 
	    
	    @ManyToOne(fetch = FetchType.LAZY)		
	    @JoinColumn(name = "data_feed_id",nullable = false)
	    @NotNull(message = "DataFeeds cannot be null or empty. Please select only one.")
	    private DataFeeds dataFeeds;
	    
	    @Column(name = "batch_id")
	    private Long batchID ;
	    
	   // @Type(value = JsonBinaryType.class)
	   // @Column(columnDefinition = "jsonb")
	   // private String data;
	    
	    @Basic(fetch = FetchType.LAZY)
	    @Column(name = "data", columnDefinition = "jsonb")
	    @JdbcTypeCode(SqlTypes.JSON)
	    private String data;

	    
	    @Column(name = "dry_run", nullable = false)
	    @NotNull(message = "Whether this was a dry run or not")
	    private Boolean dryRun = false;
	    
	    @Column(name = "has_errors", nullable = false)
	    private Boolean hasErrors = false;
	    
	    @Column(name = "run_start", columnDefinition = "TIMESTAMP(0) WITH TIME ZONE")
	    private ZonedDateTime runStart;  
	    
	    @Column(name = "run_end", columnDefinition = "TIMESTAMP(0) WITH TIME ZONE")
	    private ZonedDateTime runEnd;  
	    
	    @Column(name = "parent_primary_key")
	    private UUID  parentPrimaryKey;
	    
		public ZonedDateTime getRunStart() {
			return runStart != null  ? runStart.withZoneSameInstant(ZoneId.of("Asia/Phnom_Penh")) : null;
		}
		public ZonedDateTime getRunEnd() {
			return runEnd != null  ? runEnd.withZoneSameInstant(ZoneId.of("Asia/Phnom_Penh")) : null;
		}

	 
}
