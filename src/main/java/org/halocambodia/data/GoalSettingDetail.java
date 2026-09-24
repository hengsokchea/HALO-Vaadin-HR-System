package org.halocambodia.data;

import java.time.LocalDate;
import java.util.UUID;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.SequenceGenerator;
import jakarta.persistence.Table;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.*;

@Entity
@Table(name = "emp_goal_setting_detail",schema  = "public")
@Getter
@Setter
public class GoalSettingDetail extends AbstractEntity {
	 @Id
	    @GeneratedValue(strategy = GenerationType.SEQUENCE, generator = "public.emp_goal_setting_detail_emp_goal_setting_detail_id_seq")
	    // The initial value is to account for data.sql demo data ids
	    @SequenceGenerator(name = "public.emp_goal_setting_detail_emp_goal_setting_detail_id_seq", initialValue = 1,allocationSize = 1)
	    @Column(name = "emp_goal_setting_detail_id")
	    private Long id;
	 
	    @ManyToOne(fetch = FetchType.LAZY)		
	    @JoinColumn(name = "emp_goal_setting_id",nullable = false)
	    @NotNull(message = "Goal Setting cannot be null or empty. Please select only one.")
	    private GoalSetting goalSetting;
	    
	 
	    @Column(name = "work_goal", nullable = false)
	    @NotBlank(message = "Work goal cannot be null or empty")
	    private String workGoal;
	    
	    @Column(name = "work_goal_date", nullable = false)
	    @NotNull(message = "Work Goal Date cannot be null or empty")
	    private LocalDate workGoalDate;
	


	    @Column(name = "survey123_id")
	    private UUID  survey123ID;
	    
		@Override
		public Long getId() {
		    return id;
		}
	
}
