package org.halocambodia.data;

import jakarta.persistence.*;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import lombok.Getter;
import lombok.Setter;

import java.util.ArrayList;
import java.util.List;

@Entity
@Table(name = "review_criteria_group", schema = "public",uniqueConstraints = @UniqueConstraint(name = "review_criteria_group_group_code_key", columnNames = "group_code"))
@Getter
@Setter
public class ReviewCriteriaGroup extends AbstractEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.SEQUENCE, generator = "public.review_criteria_group_review_criteria_group_id_seq")
    @SequenceGenerator(
            name = "public.review_criteria_group_review_criteria_group_id_seq",
            sequenceName = "public.review_criteria_group_review_criteria_group_id_seq",
            allocationSize = 1
    )
    @Column(name = "group_id")
    private Long id;
    
	@Override
	public Long getId() {
	    return id;
	}

    @Column(name = "group_code", nullable = false, unique = true)
    @NotNull(message = "Group code is required")
    @Size(min = 1, message = "Group code cannot be empty")
    private String groupCode;

    @Column(name = "group_name_en", nullable = false)
    @NotNull(message = "Group name (EN) is required")
    @Size(min = 1, message = "Group name (EN) cannot be empty")
    private String groupNameEn;

    @Column(name = "group_name_kh", nullable = false)
    @NotNull(message = "Group name (KH) is required")
    @Size(min = 1, message = "Group name (KH) cannot be empty")
    private String groupNameKh;
    
    @Column(name = "group_desc_en")
    private String groupDescEn;

    @Column(name = "group_desc_kh")
    private String groupDescKh;
    

    @Column(name = "sort_order", nullable = false)
    private Integer sortOrder = 1;

    @Column(name = "is_active", nullable = false)
    private Boolean isActive = true;

    @Version
    @Column(name = "version")
    private Long version;

    @OneToMany(mappedBy = "group", fetch = FetchType.LAZY, cascade = CascadeType.ALL, orphanRemoval = true)
    @OrderBy("sortOrder ASC NULLS LAST")
    private List<ReviewCriteria> criteria = new ArrayList<>();
}
