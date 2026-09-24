package org.halocambodia.data;

import java.math.BigDecimal;
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
@Table(name = "emp_training",schema  = "public")
@Getter
@Setter
public class EmployeeTraining extends AbstractEntity {
	@Override
	public Long getId() {
	    return id;
	}
	 @Id
	    @GeneratedValue(strategy = GenerationType.SEQUENCE, generator = "public.emp_training_training_id_seq")
	    // The initial value is to account for data.sql demo data ids
	    @SequenceGenerator(name = "public.emp_training_training_id_seq", initialValue = 1,allocationSize = 1)
	    @Column(name = "training_id")
	    private Long id; 
	 
	    @ManyToOne(fetch = FetchType.LAZY)		
	    @JoinColumn(name = "emp_id", nullable = false)
	    @NotNull(message = "Employee cannot be null or empty. Please select only one.")
	    private Employee employee;
	    
	    @ManyToOne(fetch = FetchType.LAZY)		
	    @JoinColumn(name = "training_type_id", nullable = false)
	    @NotNull(message = "Training Course cannot be null or empty. Please select only one.")
	    private TrainingCourse trainingType;
	    
	    @ManyToOne(fetch = FetchType.LAZY)		
	    @JoinColumn(name = "training_center_id", nullable = false)
	    @NotNull(message = "Training Center cannot be null or empty. Please select only one.")
	    private TrainingProvider trainingCenter;
	    
	    @Column(name = "start_date")
	    private LocalDate startDate;
	    
	    @Column(name = "end_date", nullable = false)
	    @NotNull(message = "End Date cannot be null or empty")
	    private LocalDate endDate;
	    
	    @Column(name = "max_score", precision = 14, scale = 2)
	    private BigDecimal maxScore ;
	    
	    @Column(name = "score", precision = 14, scale = 2)
	    private BigDecimal score ;
	    
	    @Column(name = "result")
	    private String result;
	    
	    @Column(name = "trainer_name")
	    private String trainer;
	    
	    @Column(name = "remark")
	    private String remark;
	    
	    @Version
	    @Column(name = "version")
	    private Long version;
	    
	    @Column(name = "halo_support_amount", nullable = false, precision = 14, scale = 2)
	    @NotNull(message = "HALO Support Amount(USD) cannot be null or empty")
	    private BigDecimal haloSupportAmount = BigDecimal.ZERO;

}
