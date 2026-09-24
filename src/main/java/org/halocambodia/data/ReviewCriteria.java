package org.halocambodia.data;

import org.halocambodia.enums.ReviewValueType;

import jakarta.persistence.*;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import lombok.Getter;
import lombok.Setter;

@Entity
@Table(name = "review_criteria", schema = "public",
       uniqueConstraints = @UniqueConstraint(name = "review_criteria_criteria_code_key", columnNames = "criteria_code"))
@Getter
@Setter
public class ReviewCriteria extends AbstractEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.SEQUENCE, generator = "public.review_criteria_review_criteria_id_seq")
    @SequenceGenerator(
            name = "public.review_criteria_review_criteria_id_seq",
            sequenceName = "public.review_criteria_review_criteria_id_seq",
            allocationSize = 1
    )
    @Column(name = "criteria_id")
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "group_id", nullable = false)
    @NotNull(message = "Criteria group is required")
    private ReviewCriteriaGroup group;

    @Column(name = "criteria_code", nullable = false, unique = true)
    @NotNull(message = "Criteria code is required")
    @Size(min = 1, message = "Criteria code cannot be empty")
    private String criteriaCode;

    @Column(name = "criteria_name_en", nullable = false)
    @NotNull(message = "Criteria name (EN) is required")
    @Size(min = 1, message = "Criteria name (EN) cannot be empty")
    private String criteriaNameEn;

    @Column(name = "criteria_name_kh", nullable = false)
    @NotNull(message = "Criteria name (KH) is required")
    @Size(min = 1, message = "Criteria name (KH) cannot be empty")
    private String criteriaNameKh;

    @Column(name = "sort_order", nullable = false)
    private Integer sortOrder = 1;

    @Column(name = "is_active", nullable = false)
    private Boolean isActive = true;

    @Column(name = "min_value", nullable = false)
    private Integer minValue = 1;

    @Column(name = "max_value", nullable = false)
    private Integer maxValue = 5;

    @Enumerated(EnumType.STRING)
    @Column(name = "value_type", nullable = false)
    private ReviewValueType valueType = ReviewValueType.RATING;

    @Column(name = "criteria_desc_en")
    private String criteriaDescEn;

    @Column(name = "criteria_desc_kh")
    private String criteriaDescKh;

    @Version
    @Column(name = "version")
    private Long version;
    
	@Override
	public Long getId() {
	    return id;
	}
}
