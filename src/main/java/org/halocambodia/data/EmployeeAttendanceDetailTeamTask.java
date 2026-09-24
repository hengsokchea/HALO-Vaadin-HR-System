package org.halocambodia.data;

import java.util.UUID;

import jakarta.persistence.*;
import lombok.Getter;
import lombok.Setter;

@Entity
@Table(name = "emp_attendance_detail_team_task", schema = "public")
@Getter
@Setter
public class EmployeeAttendanceDetailTeamTask extends AbstractEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.SEQUENCE,
            generator = "public.emp_attendance_detail_team_ta_emp_attendance_detail_team_ta_seq")
    @SequenceGenerator(
            name = "public.emp_attendance_detail_team_ta_emp_attendance_detail_team_ta_seq",
            allocationSize = 1
    )
    @Column(name = "emp_attendance_detail_team_task_id")
    private Long id;

    
	@Override
	public Long getId() {
	    return id;
	}

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "task_id")
    private Tasks task;


    @Column(name = "survey123_id",updatable = false,nullable = false)
    private UUID survey123Id=UUID.randomUUID();

    @Version
    @Column(name = "version")
    private Long version;
    
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "emp_attendance_detail_team_id")
    private EmployeeAttendanceDetailTeam attendanceDetailTeam;
}
