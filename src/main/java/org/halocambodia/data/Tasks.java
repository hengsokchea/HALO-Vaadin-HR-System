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
@Table(name = "goims_tasks", schema = "public")
@Getter
@Setter
public class Tasks extends AbstractEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.SEQUENCE,generator = "public.goims_tasks_id_seq")
    @SequenceGenerator(name = "public.goims_tasks_id_seq", allocationSize = 1 )
    @Column(name = "goims_task_id")
    private Long id;
    
	@Override
	public Long getId() {
	    return id;
	}
    
    @Column(name = "task_id")
    private Long taskId;
    
    @Column(name = "task_code")
    private String taskCode;
    
    @Column(name = "task_type_id")
    private Long taskTypeId;
    
    @Column(name = "task_status_id")
    private Long taskStatusId;
    
    @Column(name = "task_priority_id")
    private Long taskPriorityId;
    
    @Column(name = "task_name")
    private String taskName;
    
    @Column(name = "gazetteer_id")
    private Long gazetteerId;
    
    @Column(name = "identified_date")
    private LocalDate identifiedDate;
    
    @Column(name = "start_date")
    private LocalDate startDate;
    
    @Column(name = "end_date")
    private LocalDate endDate;
    

}
