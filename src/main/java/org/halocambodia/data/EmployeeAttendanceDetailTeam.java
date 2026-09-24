package org.halocambodia.data;

import java.util.ArrayList;
import java.util.List;
import java.util.Set;
import java.util.UUID;

import jakarta.persistence.*;
import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.NotNull;
import lombok.Getter;
import lombok.Setter;

@Entity
@Table(name = "emp_attendance_detail_team", schema = "public")
@Getter
@Setter
public class EmployeeAttendanceDetailTeam extends AbstractEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.SEQUENCE,
            generator = "public.emp_attendance_detail_team_emp_attendance_detail_team_id_seq")
    @SequenceGenerator(
            name = "public.emp_attendance_detail_team_emp_attendance_detail_team_id_seq",
            allocationSize = 1
    )
    @Column(name = "emp_attendance_detail_team_id")
    private Long id;
    
	@Override
	public Long getId() {
	    return id;
	}

    // team_id -> list_team
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "team_id", nullable = false)
    @NotNull(message = "Team is required")
    private Teams team;

    // emp_attendance_detail_id -> emp_attendance_detail
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "emp_attendance_detail_id", nullable = false)
    @NotNull(message = "Attendance detail is required")
    private EmployeeAttendanceDetail attendanceDetail;

    @Column(name = "survey123_id",updatable = false,nullable = false)
    private UUID survey123Id=UUID.randomUUID();

    @Version
    @Column(name = "version")
    private Long version;


    
   /*@NotEmpty(message = "At least one minefield must be selected")
    @ManyToMany(fetch = FetchType.LAZY)
    @JoinTable(
        name = "emp_attendance_detail_team_task",schema  = "public",
        joinColumns = @JoinColumn(name = "emp_attendance_detail_team_id"),
        inverseJoinColumns = @JoinColumn(name = "task_id")
    )
    private Set<Tasks> tasks;
    */
    
    @OneToMany(mappedBy = "attendanceDetailTeam",cascade = CascadeType.ALL,fetch = FetchType.LAZY,orphanRemoval = true)
    	private List<EmployeeAttendanceDetailTeamTask> teamTasks = new ArrayList<>();

    
}
