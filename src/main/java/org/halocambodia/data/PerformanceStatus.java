package org.halocambodia.data;

import jakarta.persistence.*;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import lombok.Getter;
import lombok.Setter;

import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;

@Entity
@Table(name = "performance_status", schema = "public")
@Getter
@Setter
public class PerformanceStatus extends AbstractEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.SEQUENCE, generator = "public.performance_status_performance_status_id_seq")
    @SequenceGenerator(
            name = "public.performance_status_performance_status_id_seq",
            sequenceName = "public.performance_status_performance_status_id_seq",
            allocationSize = 1
    )
    @Column(name = "performance_status_id")
    private Long id;
    
    @Column(name = "status_name", nullable = false, unique = true)
    @NotNull(message = "Status is required")
    @Size(min = 1, message = "Status cannot be empty")
    private String statusName;
    
    @Column(name = "note")
    private String note;
    

    @Version
    @Column(name = "version")
    private Long version;

	@Override
	public Long getId() {
	    return id;
	}
}
