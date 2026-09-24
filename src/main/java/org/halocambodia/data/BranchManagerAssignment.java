package org.halocambodia.data;

import jakarta.persistence.*;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotNull;
import lombok.Getter;
import lombok.Setter;

import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;

@Entity
@Table(name = "branch_manager_assignment", schema = "public")
@Getter
@Setter
public class BranchManagerAssignment extends AbstractEntity {
	
	

    @Id
    @GeneratedValue(strategy = GenerationType.SEQUENCE, generator = "public.branch_manager_assignment_branch_manager_assignment_id_seq")
    @SequenceGenerator(
            name = "public.branch_manager_assignment_branch_manager_assignment_id_seq",
            sequenceName = "public.branch_manager_assignment_branch_manager_assignment_id_seq",
            allocationSize = 1
    )
    @Column(name = "branch_manager_assignment_id")
    private Long id;
    
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "location_id", nullable = false)
    @NotNull(message = "Location is required")
    private Branch branch;

    
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "emp_id", nullable = false)
    @NotNull(message = "Employee is required")
    private Employee employee;
    
    @Column(name = "sort_order")
    private Integer sortOrder;

    @Version
    @Column(name = "version")
    private Long version;
    
	@Override
	public Long getId() {
	    return id;
	}


}
