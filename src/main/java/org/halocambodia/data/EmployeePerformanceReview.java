package org.halocambodia.data;

import jakarta.persistence.*;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotNull;
import lombok.Getter;
import lombok.Setter;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

@Entity
@Table(name = "emp_performance_review", schema = "public")
@Getter
@Setter
public class EmployeePerformanceReview extends AbstractEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.SEQUENCE, generator = "public.emp_performance_review_emp_performance_review_id_seq")
    @SequenceGenerator(
            name = "public.emp_performance_review_emp_performance_review_id_seq",
            sequenceName = "public.emp_performance_review_emp_performance_review_id_seq",
            allocationSize = 1
    )
    @Column(name = "emp_performance_review_id")
    private Long id;

	@Override
	public Long getId() {
	    return id;
	}
	
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "emp_id", nullable = false)
    @NotNull(message = "Employee is required")
    private Employee employee;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "emp_position_id", nullable = false)
    @NotNull(message = "Position is required")
    private Positions position;
    
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "location_id", nullable = false)
    @NotNull(message = "Location is required")
    private Branch branch;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "supervisor_emp_id", nullable = false)
    @NotNull(message = "Supervisor is required")
    private Employee supervisor;

    @Column(name = "review_date", nullable = false)
    @NotNull(message = "Review date is required")
    private LocalDate reviewDate;
        
    @Column(name = "overall_rating", precision = 5, scale = 2)
    private BigDecimal  overallRating;
    

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "manager_status_id", nullable = false)
    @NotNull(message = "Line Manager Status is required")
    private PerformanceStatus supervisorStatus;
    
    @Column(name = "manager_review_date")
    private LocalDate supervisorReviewDate;

    
    @Column(name = "manager_comments")
    private String managerComments;
    
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "final_review_emp_id", nullable = false)
    @NotNull(message = "Final Review is required")
    private Employee finalReviewBy;
    
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "final_review_status_id", nullable = false)
    @NotNull(message = "Final Review is required")
    private PerformanceStatus finalStatus ;
    
    @Column(name = "final_review_date")
    private LocalDate finalReviewDate;
    
    @Column(name = "final_review_comments")
    private String finalReviewComments;

    @Version
    @Column(name = "version")
    private Long version;

    @OneToMany(mappedBy = "review", fetch = FetchType.LAZY, cascade = CascadeType.ALL, orphanRemoval = true)
    @OrderBy("id ASC")
    private List<EmployeePerformanceReviewRating> ratings = new ArrayList<>();
}
