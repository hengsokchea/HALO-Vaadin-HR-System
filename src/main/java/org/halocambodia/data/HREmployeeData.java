package org.halocambodia.data;

import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;

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
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.Getter;
import lombok.Setter;

@Entity
@Table(name = "cam_view_emp_master_basic_data",schema  = "public")
@Getter
@Setter
public class HREmployeeData{
	    @Id
	    @Column(name = "emp_id")
	    private Long id;
	    
	    @Column(name = "insurance_no_var")	   
	    private String insurance;
	    	    
	    @Column(name = "name_en")	   
	    private String nameEn;
	    
	    @Column(name = "name_kh")	   
	    private String nameKh;
	    
	    @Column(name = "gender")	   
	    private String gender;
	    
	    @Column(name = "marital_status")	   
	    private String maritalStatus;
	    
	    @Column(name = "emp_position")	   
	    private String position;
	    
	    @Column(name = "branch_code")	   
	    private String location;
	    
	    @Column(name = "department_name")	   
	    private String department;
	    
	    @Column(name = "employment_group")	   
	    private String status;
	    
	    @Column(name = "emp_category")	   
	    private String empCategory;
	    
	    @Column(name = "employee_type")	   
	    private String employeeType;

	    @OneToMany(mappedBy = "employee", cascade = CascadeType.ALL, fetch = FetchType.LAZY)
	    private List<HREmployeeWithSupervisor> employeeWithSupervisors= new ArrayList<>();
	   
	    
	    
	    
}
