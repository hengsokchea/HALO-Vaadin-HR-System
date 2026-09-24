package org.halocambodia.data;

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
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.*;

@Entity
@Table(name = "po_grade",schema  = "public")
@Getter
@Setter
public class PoGrade extends AbstractEntity {
	
	@Override
	public Long getId() {
	    return id;
	}
	
	@Id
    @GeneratedValue(strategy = GenerationType.SEQUENCE, generator = "public.po_grad_po_grad_id_seq")
    // The initial value is to account for data.sql demo data ids
    @SequenceGenerator(name = "public.po_grad_po_grad_id_seq", initialValue = 1,allocationSize = 1)
    @Column(name = "po_grade_id")
    private Long id;

	@Column(name = "po_grade", nullable = false, unique = true)
	@NotBlank(message = "PO Grade cannot be null or empty")
	private String poGrade;
	    

}
