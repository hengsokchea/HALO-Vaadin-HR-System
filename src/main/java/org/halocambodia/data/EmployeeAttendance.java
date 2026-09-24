package org.halocambodia.data;

import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

import jakarta.persistence.*;
import jakarta.validation.constraints.NotNull;
import lombok.Getter;
import lombok.Setter;

@Entity
@Table(name = "emp_attendance", schema = "public")
@Getter
@Setter
public class EmployeeAttendance extends AbstractEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.SEQUENCE,
            generator = "public.emp_attendance_emp_attendance_id_seq")
    @SequenceGenerator(
            name = "public.emp_attendance_emp_attendance_id_seq",
            allocationSize = 1
    )
    @Column(name = "emp_attendance_id")
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "reported_by_emp_id", nullable = false)
    @NotNull(message = "Reported by employee is required")
    private Employee reportedByEmployee;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "reported_by_position_id")
    private Positions reportedByPosition;

    @Column(name = "reported_date", nullable = false)
    @NotNull(message = "Reported date is required")
    private LocalDate reportedDate;
    
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "report_location_id", nullable = false)
    @NotNull(message = "Report Location is required")
    private Branch reportLocation;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "qc_by_emp_id")
    private Employee qcByEmployee;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "qc_by_position_id")
    private Positions qcByPosition;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "qc_location_id")
    private Branch qcLocation;

    @Column(name = "qc_date")
    private LocalDate qcDate;

    @Column(name = "survey123_id")
    private UUID survey123Id;

    @Version
    @Column(name = "version")
    private Long version;
    
	@Override
	public Long getId() {
	    return id;
	}

    @OneToMany(mappedBy = "attendance",cascade = CascadeType.ALL,orphanRemoval = true)
    private List<EmployeeAttendanceDetail>attendanceDetail= new ArrayList<>();
}
