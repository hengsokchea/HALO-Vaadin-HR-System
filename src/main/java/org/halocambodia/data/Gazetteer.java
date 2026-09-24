package org.halocambodia.data;

import jakarta.persistence.*;
import jakarta.validation.constraints.*;
import lombok.Getter;
import lombok.Setter;

import java.util.LinkedHashSet;
import java.util.Set;

@Entity
@Table(
    name = "gazetteer",
    schema = "public",
    uniqueConstraints = @UniqueConstraint(
        name = "gazetteer_gazetteer_code_key",
        columnNames = "gazetteer_code"
    )
)
@Getter
@Setter
public class Gazetteer extends AbstractEntity {
	


    @Id
    @GeneratedValue(strategy = GenerationType.SEQUENCE, generator = "gazetteer_seq")
    @SequenceGenerator(
        name = "gazetteer_seq",
        sequenceName = "gazetteer_gazetteer_id_seq",
        allocationSize = 1
    )
    @Column(name = "gazetteer_id")
    private Long id;
    
	@Override
	public Long getId() {
	    return id;
	}

    @NotNull(message = "Level is required")
    @Min(value = 1, message = "Level must be >= 1")
    @Max(value = 4, message = "Level must be <= 4") // adjust
    @Column(name = "level", nullable = false)
    private Integer level;

    @NotBlank(message = "Gazetteer code is required")
    @Column(name = "gazetteer_code", nullable = false)
    private String code;

    @NotBlank(message = "Name (EN) is required")
    @Column(name = "gazetteer_en", nullable = false)
    private String nameEn;

    @NotBlank(message = "Name (KH) is required")
    @Column(name = "gazetteer_kh", nullable = false)
    private String nameKh;
    
    @Version
    @Column(name = "version")
    private Long version;
    
    @Column(name = "note")
    private String note;


    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "parent_gazetteer_id")
    private Gazetteer parent;

    @OneToMany(mappedBy = "parent", fetch = FetchType.LAZY,
               cascade = {CascadeType.PERSIST, CascadeType.MERGE})
    @OrderBy("level ASC, nameEn ASC")
    private Set<Gazetteer> children = new LinkedHashSet<>();

    // Remove this if AbstractEntity already has @Version
    // @Version
    // @Column(name = "version")
    // private Long version;

    public void addChild(Gazetteer child) {
        children.add(child);
        child.setParent(this);
    }

    public void removeChild(Gazetteer child) {
        children.remove(child);
        child.setParent(null);
    }
}




