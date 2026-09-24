package org.halocambodia.data;

import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

import org.hibernate.annotations.UuidGenerator;

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
import jakarta.persistence.OrderBy;
import jakarta.persistence.SequenceGenerator;
import jakarta.persistence.Table;
import jakarta.persistence.Version;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import lombok.Getter;
import lombok.Setter;

@Entity
@Table(name = "policy", schema = "public")
@Getter
@Setter
public class Policy extends AbstractEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.SEQUENCE, generator = "policy_policy_id_generator")
    @SequenceGenerator(
            name = "policy_policy_id_generator",
            sequenceName = "public.policy_policy_id_seq",
            allocationSize = 1
    )
    @Column(name = "policy_id")
    private Long id;

    @NotBlank(message = "Code is required")
    @Size(max = 100, message = "Code must not exceed 100 characters")
    @Column(name = "code", nullable = false, unique = true, length = 100)
    private String code;

    @NotBlank(message = "English title is required")
    @Size(max = 500, message = "English title must not exceed 500 characters")
    @Column(name = "title_en", nullable = false, length = 500)
    private String titleEn;

    @NotBlank(message = "Khmer title is required")
    @Size(max = 500, message = "Khmer title must not exceed 500 characters")
    @Column(name = "title_kh", nullable = false, length = 500)
    private String titleKh;

    @Column(name = "summary", columnDefinition = "text")
    private String summary;

    @Column(name = "content", columnDefinition = "text")
    private String content;

    @NotNull(message = "Effective date is required")
    @Column(name = "effective_date", nullable = false)
    private LocalDate effectiveDate;

    @Column(name = "expiry_date")
    private LocalDate expiryDate;

    @Column(name = "published", nullable = false)
    private boolean published;

    @Column(name = "required_acknowledge", nullable = false)
    private boolean requiredAcknowledge;

    @Column(name = "active", nullable = false)
    private boolean active;
    
    @UuidGenerator
    @Column(name = "public_token", nullable = false, unique = true, updatable = false, columnDefinition = "uuid")
    private UUID publicToken;

    @NotNull(message = "Policy category is required")
    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "policy_category_id", nullable = false)
    private PolicyCategory policyCategory;



    @OneToMany(
            mappedBy = "policy",
            fetch = FetchType.LAZY,
            cascade = CascadeType.ALL
    )
    private List<PolicyAcknowledge> acknowledgements = new ArrayList<>();

    @Version
    @Column(name = "version", nullable = false)
    private Long version = 0L;

    @Override
    public Long getId() {
        return id;
    }


}
