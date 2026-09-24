package org.halocambodia.data;

import jakarta.persistence.*;
import jakarta.validation.constraints.AssertTrue;
import jakarta.validation.constraints.NotNull;
import lombok.Getter;
import lombok.Setter;

@Entity
@Table(name = "emp_performance_review_rating", schema = "public")
@Getter
@Setter
public class EmployeePerformanceReviewRating extends AbstractEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.SEQUENCE, generator = "public.emp_performance_review_rating_performance_review_rating_id_seq")
    @SequenceGenerator(
            name = "public.emp_performance_review_rating_performance_review_rating_id_seq",
            sequenceName = "public.emp_performance_review_rating_performance_review_rating_id_seq",
            allocationSize = 1
    )
    @Column(name = "performance_review_rating_id")
    private Long id;
    
	@Override
	public Long getId() {
	    return id;
	}

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "emp_performance_review_id", nullable = false)
    @NotNull(message = "Review is required")
    private EmployeePerformanceReview review;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "criteria_id", nullable = false)
    @NotNull(message = "Criteria is required")
    private ReviewCriteria criteria;

    @Column(name = "rating_value")
    private Integer ratingValue;

    @Column(name = "bool_value")
    private Boolean boolValue;

    @Column(name = "text_value")
    private String textValue;

    @Version
    @Column(name = "version")
    private Long version;

}
