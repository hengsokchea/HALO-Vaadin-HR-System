package org.halocambodia.data;

import java.math.BigDecimal;
import java.sql.Time;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

import org.halocambodia.enums.EmployeeTypeEnum;
import org.halocambodia.enums.GenderEnum;
import org.hibernate.annotations.Formula;

import jakarta.persistence.CascadeType;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
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
import jakarta.persistence.Version;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import lombok.*;

@Entity
@Table(name = "emp_master",schema  = "public")
@Getter
@Setter
public class Employee extends AbstractEntity {
	
	@Override
	public Long getId() {
	    return id;
	}
	
	 @Id
	    @GeneratedValue(strategy = GenerationType.SEQUENCE, generator = "public.emp_master_emp_id_seq")
	    // The initial value is to account for data.sql demo data ids
	    @SequenceGenerator(name = "public.emp_master_emp_id_seq", initialValue = 1,allocationSize = 1)
	    @Column(name = "emp_id")
	    private Long id; 
	    
	    @Column(name = "insurance_no", nullable = false,unique = true)
	    @NotNull(message = "Insurance cannot be null or empty")
	    private Integer insuranceNo;
	    
	    @Column(name = "name_en", nullable = false)
	    @NotNull(message = "Name(EN) is required")
	    @Size(min = 1, message = "Name(EN) cannot be empty")
	    private String nameEn;
	    
	    @Column(name = "name_kh", nullable = false)
	    @NotNull(message = "Name(KH) is required")
	    @Size(min = 1, message = "Name(KH) cannot be empty")
	    private String nameKh;
	    
	    //@Column(name = "gender")
	    //private GenderE gender;
	    @Enumerated(EnumType.STRING)
	    @Column(name = "gender", nullable = false)
	    @NotNull(message = "Gender is required")
	    private GenderEnum gender;
	    
	    @Column(name = "date_of_birth", nullable = false)
	    @NotNull(message = "Date of birth cannot be null or empty")
	    private LocalDate dob;
	    
	    @Column(name = "marital_status", nullable = false)
	    @NotNull(message = "Marital Status is required")
	    @Size(min = 1, message = "Marital Status cannot be empty")
	    private String maritalStatus;
	    
	    @Column(name = "phone_number")
	    private String phoneNumber;

	    @Column(name = "personal_email")
	    private String personalEmail;
	    
	    
	    
	    
	    @Column(name = "official_cell_no")
	    private String officialCellNo;
	    
	    @Column(name = "official_email")
	    private String officialEmail;	    
	    

	    
	    @Column(name = "current_address")
	    private String currentAddress;
	    

	    
	    @Column(name = "blood_group", nullable = false)
	    @NotNull(message = "Blood Group is required")
	    @Size(min = 1, message = "Blood Group cannot be empty")
	    private String bloodGroup;
	    
	    @Column(name = "join_date", nullable = false)
	    @NotNull(message = "Join date cannot be null or empty")
	    private LocalDate joinDate;

	    
	    @ManyToOne(fetch = FetchType.LAZY)		
	    @JoinColumn(name = "emp_position",nullable = false)	    
	    @NotNull(message = "Position cannot be null or empty. Please select only one.")
	    private Positions positions;
	    

	    
	    @ManyToOne(fetch = FetchType.LAZY)		
	    @JoinColumn(name = "insurance_type_id", nullable = false)
	    @NotNull(message = "Insurance Type cannot be null or empty. Please select only one.")
	    private InsuranceType insuranceType ;
	    
	    @Column(name = "probation_end_date", nullable = false)	
	    @NotNull(message = "Probation date cannot be null or empty")
	    private LocalDate probationEndDate;
	    
	    @ManyToOne(fetch = FetchType.LAZY)		
	    @JoinColumn(name = "probation_status_id",nullable = false)
	    @NotNull(message = "Probation Status cannot be null or empty. Please select only one.")
	    private ProbationStatus probationStatus ;
	    
	    
	    @Column(name = "current_contract_start_date")	    
	    private LocalDate currentContractStartDate;
	    
	    @Column(name = "current_contract_end_date")	    
	    private LocalDate currentContractEndDate;

	    @ManyToOne(fetch = FetchType.LAZY)		
	    @JoinColumn(name = "contract_type_id", nullable = false)
	    @NotNull(message = "Contract Type cannot be null or empty. Please select only one.")
	    private ContractType contractType ;
	    
	    @ManyToOne(fetch = FetchType.LAZY)		
	    @JoinColumn(name = "nationality_id", nullable = false)
	    @NotNull(message = "Nationality cannot be null or empty. Please select only one.")
	    private Nationality nationality;
	    
	    @ManyToOne(fetch = FetchType.LAZY)		
	    @JoinColumn(name = "ethnicity_id", nullable = false)
	    @NotNull(message = "Ethnicity cannot be null or empty. Please select only one.")
	    private Ethnicity ethnicity;
	    
	    @ManyToOne(fetch = FetchType.LAZY)		
	    @JoinColumn(name = "religion_id")	    
	    private Religions religions;
	    
	    @ManyToOne(fetch = FetchType.LAZY)		
	    @JoinColumn(name = "last_educational_qualification_id")
	    private EducationType  lastEducationType;
	    
	    
	    
	    @Column(name = "last_organization")
	    private String lastOrganization;
	    
	    @Column(name = "pre_halo_work_experience")
	    private String preHaloWorkExperience;
	    
	    @Column(name = "pre_existing_medical_condition")
	    private Boolean preExistingMedicalCondition;
	    
	    
		@OneToMany(mappedBy = "employee",cascade = CascadeType.ALL,orphanRemoval = true, fetch = FetchType.LAZY)
	    private List<EmployeeNominee> employeeNominees = new ArrayList<>();
		
	    
	    @Column(name = "nominee_name")
	    private String nomineeName;
	    
	    @Column(name = "nominee_phone")
	    private String nomineePhone;
	    
	    @Column(name = "nominee_nid")
	    private String nomineeNid;
	    
	    @ManyToOne(fetch = FetchType.LAZY)		
	    @JoinColumn(name = "relationship_with_nominee_id")
	    private Relationship  relationshipWithNominee;
	    
	    @ManyToOne(fetch = FetchType.LAZY)		
	    @JoinColumn(name = "attendance_type_shift_id", nullable = false)
	    @NotNull(message = "Attendance Type cannot be null or empty. Please select only one.")
	    private Shift  attendanceType;
	    
	    

	    
	    @Column(name = "nsff_card_number")
	    private String nsffCardNumber;
	    
	    @Column(name = "nsff_expiry_date")
	    private LocalDate nsffExpiryDate;

	    
	    
	    @Column(name = "id_card_number")
	    private String idCardNumber;
	    
	    @Column(name = "id_card_expiry_date")
	    private LocalDate idCardExpiryDate;

	    
	    @Column(name = "gross_salary_usd", nullable = false, precision = 14, scale = 2)
	    @NotNull(message = "Salary cannot be null or empty")
	    private BigDecimal grossSalaryUsd = BigDecimal.ZERO;
	    
	    @ManyToOne(fetch = FetchType.LAZY)		
	    @JoinColumn(name = "locations", nullable = false)
	    @NotNull(message = "Location cannot be null or empty. Please select only one.")
	    private Branch branch;
	    
	    @ManyToOne(fetch = FetchType.LAZY)		
	    @JoinColumn(name = "emp_category", nullable = false)
	    @NotNull(message = "Category cannot be null or empty. Please select only one.")
	    private Category category;
	    
	    @ManyToOne(fetch = FetchType.LAZY)		
	    @JoinColumn(name = "present_address_gazetteer_id", nullable = false)
	    @NotNull(message = "Present Address cannot be null or empty. Please select only one.")
	    private Gazetteer presentAddress;	    
	    
	    @ManyToOne(fetch = FetchType.LAZY)		
	    @JoinColumn(name = "permanent_address_gazetteer_id", nullable = false)
	    @NotNull(message = "Permanent Address cannot be null or empty. Please select only one.")
	    private Gazetteer permanentAddress;
	    
	    @ManyToOne(fetch = FetchType.LAZY)		
	    @JoinColumn(name = "native_location_gazetteer_id", nullable = false)
	    @NotNull(message = "Native Location cannot be null or empty. Please select only one.")
	    private Gazetteer nativeLocation;

	    
	    @ManyToOne(fetch = FetchType.LAZY)		
	    @JoinColumn(name = "bank_id")
	    private  Bank bank;
	    
	    
	    @Column(name = "bank_account")
	    private String bankAccount;
	    
	    @Column(name = "bank_account_name")
	    private String bankAccountName;


	    
	    @ManyToOne(fetch = FetchType.LAZY)		
	    @JoinColumn(name = "driving_license_category")
	    private DrivingLicense drivingLicense;
	    
	    @Column(name = "driving_license_expire")
	    private LocalDate drivingLicenseExpire;
	    
	    @ManyToOne(fetch = FetchType.LAZY)		
	    @JoinColumn(name = "department_id", nullable = false)
	    @NotNull(message = "Department cannot be null or empty. Please select only one.")
	    private Department department;
	    
	    @Column(name = "vetting_submitted")
	    private Boolean referenceVerified;
	    
	    @Column(name = "vetting_date")
	    private LocalDate referenceVerifiedDate;
	    
	    @Column(name = "nssf_retire")
	    private String nssfRetire;
	    
	    @Column(name = "nssf_retire_card")
	    private String nssfRetireCard;
	    
	    @ManyToOne(fetch = FetchType.LAZY)		
	    @JoinColumn(name = "po_grade", nullable = false)
	    @NotNull(message = "PO Grade cannot be null or empty. Please select only one.")
	    private PoGrade poGrade;
	    
	    @Column(name = "police_clearance")
	    private Boolean policePlearance;
	    
	    @ManyToOne(fetch = FetchType.LAZY)		
	    @JoinColumn(name = "contract_id")
	    private Contracts contracts ;
	    
	    @Column(name = "code")
	    private Integer code;
	    
	    
	    @Enumerated(EnumType.STRING)
	    @Column(name = "employee_type", nullable = false)
	    @NotNull(message = "Employee type is required")
	    private EmployeeTypeEnum employeeType=EmployeeTypeEnum.National;
	    
	    
	    @ManyToOne(fetch = FetchType.LAZY)		
	    @JoinColumn(name = "emp_status_career_type_id")
	    private CareerType careerType;

	    @Column(name = "last_career_type_date")
	    private LocalDate lastCareerTypeDate;
	    
	    @ManyToOne(fetch = FetchType.LAZY)		
	    @JoinColumn(name = "team_id")
	    private Teams teams;
	    
	    @Column(name = "note")
	    private String note;
	    
	    @Column(name = "tenure_in_years", nullable = false, precision = 14, scale = 2)
	    @NotNull(message = "Tenure (in Years) cannot be null or empty")
	    private BigDecimal tenureInYears = BigDecimal.ZERO;
	    
	    
	    @Column(name = "has_disability")
	    private Boolean hasDisability;
	    
	    
		@Version
		@Column(name = "version")
		private Long version;
		
	    
		@OneToMany(mappedBy = "employee",cascade = CascadeType.ALL,orphanRemoval = true, fetch = FetchType.LAZY)
		@jakarta.persistence.OrderBy("sortOrder ASC NULLS LAST")
	    private List<EmployeeSupervision> employeeSupervisions = new ArrayList<>();

		@OneToMany(mappedBy = "employee",cascade = CascadeType.ALL,orphanRemoval = true, fetch = FetchType.LAZY)		
	    private List<EmployeeDisability> employeeDisabilities = new ArrayList<>();
		
		
		@OneToMany(mappedBy = "employee",cascade = CascadeType.ALL,orphanRemoval = true, fetch = FetchType.LAZY)		
	    private List<EmployeeEmergencyContact> employeeEmergencyContacts = new ArrayList<>();
		
		@OneToMany(mappedBy = "employee",cascade = CascadeType.ALL,orphanRemoval = true, fetch = FetchType.LAZY)		
	    private List<EmployeeRelativeWorking> employeeRelativeWorkings = new ArrayList<>();
		
		@OneToMany(mappedBy = "employee",cascade = CascadeType.ALL,orphanRemoval = true, fetch = FetchType.LAZY)		
	    private List<EmployeeLanguage> employeeLanguages = new ArrayList<>();

		@OneToMany(mappedBy = "employee",cascade = CascadeType.ALL,orphanRemoval = true, fetch = FetchType.LAZY)		
	    private List<EmployeeDependencyFamily> dependencyFamilies = new ArrayList<>();
		
		@OneToMany(mappedBy = "employee",cascade = CascadeType.ALL,orphanRemoval = true, fetch = FetchType.LAZY)		
	    private List<EmployeeVaccination> employeeVaccinations = new ArrayList<>();
		
		@OneToMany(mappedBy = "employee",cascade = CascadeType.ALL,orphanRemoval = true, fetch = FetchType.LAZY)		
	    private List<EmployeeEducation> employeeEducations = new ArrayList<>();
		
		@OneToMany(mappedBy = "employee",cascade = CascadeType.ALL,orphanRemoval = true, fetch = FetchType.LAZY)		
	    private List<EmployeeDisciplinary> employeeDisciplinaries = new ArrayList<>();
		
		
		@OneToMany(mappedBy = "employee",cascade = CascadeType.ALL,orphanRemoval = true, fetch = FetchType.LAZY)		
	    private List<EmployeeLeave> employeeLeaves = new ArrayList<>();
		
		
		@OneToMany(mappedBy = "employee",cascade = CascadeType.ALL,orphanRemoval = true, fetch = FetchType.LAZY)
		@jakarta.persistence.OrderBy("fromDate DESC")
	    private List<EmployeeCareerHistory> employeeCareerHistories = new ArrayList<>();
		
		@OneToMany(mappedBy = "employee",cascade = CascadeType.ALL,orphanRemoval = true, fetch = FetchType.LAZY)
		@jakarta.persistence.OrderBy("endDate DESC")
	    private List<EmployeeTraining> employeeTrainings = new ArrayList<>();
		
		
	    @Transient
	    private List<Attachment> attachments = new ArrayList<>();

}
