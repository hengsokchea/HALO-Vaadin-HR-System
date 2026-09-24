package org.halocambodia.views.employee;

import java.nio.file.Path;
import java.time.LocalDate;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Comparator;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Objects;
import java.util.Optional;
import java.util.Set;
import java.util.stream.Collectors;

import org.springframework.data.domain.Sort;
import org.springframework.data.jpa.domain.Specification;


import com.vaadin.flow.component.AttachEvent;
import com.vaadin.flow.component.Component;
import com.vaadin.flow.component.UI;
import com.vaadin.flow.component.button.Button;
import com.vaadin.flow.component.button.ButtonVariant;
import com.vaadin.flow.component.checkbox.Checkbox;
import com.vaadin.flow.component.dependency.Uses;
import com.vaadin.flow.component.details.Details;
import com.vaadin.flow.component.dialog.Dialog;
import com.vaadin.flow.component.formlayout.FormLayout;
import com.vaadin.flow.component.grid.ColumnTextAlign;
import com.vaadin.flow.component.grid.Grid;
import com.vaadin.flow.component.grid.GridVariant;
import com.vaadin.flow.component.grid.HeaderRow;
import com.vaadin.flow.component.html.Div;
import com.vaadin.flow.component.html.Span;
import com.vaadin.flow.component.icon.Icon;
import com.vaadin.flow.component.icon.VaadinIcon;
import com.vaadin.flow.component.notification.Notification;
import com.vaadin.flow.component.notification.NotificationVariant;
import com.vaadin.flow.component.orderedlayout.HorizontalLayout;
import com.vaadin.flow.component.orderedlayout.VerticalLayout;
import com.vaadin.flow.component.tabs.Tab;
import com.vaadin.flow.component.tabs.TabSheet;
import com.vaadin.flow.component.tabs.TabVariant;
import com.vaadin.flow.component.textfield.BigDecimalField;
import com.vaadin.flow.component.textfield.IntegerField;
import com.vaadin.flow.component.textfield.NumberField;
import com.vaadin.flow.component.textfield.TextArea;
import com.vaadin.flow.component.textfield.TextField;
import com.vaadin.flow.data.binder.Binder;
import com.vaadin.flow.data.provider.CallbackDataProvider;
import com.vaadin.flow.data.renderer.ComponentRenderer;
import com.vaadin.flow.data.renderer.LitRenderer;
import com.vaadin.flow.data.renderer.Renderer;
import com.vaadin.flow.component.combobox.ComboBox;
import com.vaadin.flow.component.combobox.ComboBoxVariant;
import com.vaadin.flow.component.combobox.MultiSelectComboBox;
import com.vaadin.flow.component.combobox.MultiSelectComboBox.AutoExpandMode;
import com.vaadin.flow.component.combobox.MultiSelectComboBoxVariant;
import com.vaadin.flow.component.confirmdialog.ConfirmDialog;
import com.vaadin.flow.component.contextmenu.MenuItem;
import com.vaadin.flow.component.datepicker.DatePicker;
import com.vaadin.flow.router.PageTitle;
import com.vaadin.flow.router.Route;

import jakarta.annotation.security.PermitAll;
import jakarta.persistence.criteria.Join;
import jakarta.persistence.criteria.JoinType;
import jakarta.persistence.criteria.Predicate;

import org.apache.poi.ss.formula.functions.T;
import org.halocambodia.component.AdvancedSearchPanel;
import org.halocambodia.component.AttachmentComponent;
import org.halocambodia.component.DateRangePicker;
import org.halocambodia.component.GazetteerField;
import org.halocambodia.component.GazetteerField.LayoutMode;
import org.halocambodia.component.GazetteerMultiSearchField;
import org.halocambodia.component.PreviewReportDiv;
import org.halocambodia.data.Attachment;
import org.halocambodia.data.AttachmentRepository;
import org.halocambodia.data.AttachmentTypeRepository;
import org.halocambodia.data.Bank;
import org.halocambodia.data.BankRepository;
import org.halocambodia.data.BloodGroup;
import org.halocambodia.data.BloodGroupRepository;
import org.halocambodia.data.Branch;
import org.halocambodia.data.BranchRepository;
import org.halocambodia.data.CareerType;
import org.halocambodia.data.CareerTypeGroup;
import org.halocambodia.data.CareerTypeGroupRepository;
import org.halocambodia.data.CareerTypeRepository;
import org.halocambodia.data.Category;
import org.halocambodia.data.CategoryRepository;
import org.halocambodia.data.ContractType;
import org.halocambodia.data.ContractTypeRepository;
import org.halocambodia.data.Contracts;
import org.halocambodia.data.ContractsRepository;
import org.halocambodia.data.DateTimeUtilFormart;
import org.halocambodia.data.Department;
import org.halocambodia.data.DepartmentRepository;
import org.halocambodia.data.DisabilityTypeOption;
import org.halocambodia.data.DisabilityTypeOptionRepository;
import org.halocambodia.data.DisabilityTypeRepository;
import org.halocambodia.data.Disciplinary;
import org.halocambodia.data.DisciplinaryRepository;
import org.halocambodia.data.Donors;
import org.halocambodia.data.DonorsRepository;
import org.halocambodia.data.DrivingLicense;
import org.halocambodia.data.DrivingLicenseRepository;
import org.halocambodia.data.EducationCenter;
import org.halocambodia.data.EducationCenterRepository;
import org.halocambodia.data.EducationType;
import org.halocambodia.data.EducationTypeRepository;
import org.halocambodia.data.Employee;
import org.halocambodia.data.EmployeeCareerHistory;
import org.halocambodia.data.EmployeeDependencyFamily;
import org.halocambodia.data.EmployeeDisability;
import org.halocambodia.data.EmployeeDisciplinary;
import org.halocambodia.data.EmployeeEducation;
import org.halocambodia.data.EmployeeEmergencyContact;
import org.halocambodia.data.EmployeeLanguage;
import org.halocambodia.data.EmployeeLeave;
import org.halocambodia.data.EmployeeLeaveDetail;
import org.halocambodia.data.EmployeeNominee;
import org.halocambodia.data.EmployeePerformanceReview;
import org.halocambodia.data.EmployeeRelativeWorking;
import org.halocambodia.data.EmployeeSupervision;
import org.halocambodia.data.EmployeeTraining;
import org.halocambodia.data.EmployeeTrainingRepository;
import org.halocambodia.data.EmployeeVaccination;
import org.halocambodia.data.Ethnicity;
import org.halocambodia.data.EthnicityRepository;
import org.halocambodia.data.FileUploadUtility;
import org.halocambodia.data.Gazetteer;
import org.halocambodia.data.InsuranceType;
import org.halocambodia.data.InsuranceTypeRepository;
import org.halocambodia.data.Language;
import org.halocambodia.data.LanguageRepository;
import org.halocambodia.data.LanguageSkill;
import org.halocambodia.data.LanguageSkillRepository;
import org.halocambodia.data.MaritalStatus;
import org.halocambodia.data.MaritalStatusRepository;
import org.halocambodia.data.Nationality;
import org.halocambodia.data.NationalityRepository;
import org.halocambodia.data.PoGrade;
import org.halocambodia.data.PoGradeRepository;
import org.halocambodia.data.PositionRepository;
import org.halocambodia.data.Positions;
import org.halocambodia.data.ProbationStatus;
import org.halocambodia.data.ProbationStatusRepository;
import org.halocambodia.data.Relationship;
import org.halocambodia.data.RelationshipRepository;
import org.halocambodia.data.Religions;
import org.halocambodia.data.ReligionsRepository;
import org.halocambodia.data.Shift;
import org.halocambodia.data.ShiftRepository;
import org.halocambodia.data.Teams;
import org.halocambodia.data.TeamsRepository;
import org.halocambodia.data.TrainingProvider;
import org.halocambodia.data.TrainingCenterRepository;
import org.halocambodia.data.TrainingCourse;
import org.halocambodia.data.TrainingCourseRepository;
import org.halocambodia.data.User;
import org.halocambodia.data.Vaccination;
import org.halocambodia.data.VaccinationRepository;
import org.halocambodia.data.VaccinationType;
import org.halocambodia.data.VaccinationTypeRepository;
import org.halocambodia.enums.EducationStatusEnum;
import org.halocambodia.enums.EmployeeTypeEnum;
import org.halocambodia.enums.GenderEnum;
import org.halocambodia.security.AuthenticatedUser;
import org.halocambodia.services.EmployeeInternationActiveService;
import org.halocambodia.services.EmployeeService;
import org.halocambodia.services.GazetteerService;
import org.halocambodia.services.UserService;
import org.halocambodia.views.*;


@Route(value = "international-staff", layout = MainLayout.class)
@PageTitle("International Staff")
@PermitAll
@Uses(Icon.class)
public class InternationalStaffView extends PageDialogLayout<Employee, EmployeeInternationActiveService> {
	
	private final Button btnClonePresentToPermanent =new Button("Clone Present → Permanent | ចម្លង បច្ចុប្បន្ន → អចិន្ត្រៃយ៍", VaadinIcon.COPY.create());
	private final Button btnClonePresentToNative = new Button("Clone Present → Native | ចម្លង បច្ចុប្បន្ន → ទីកន្លែងកំណើត", VaadinIcon.COPY.create());
	private final Button btnClonePermanentToNative =new Button("Clone Permanent → Native | ចម្លង អចិន្ត្រៃយ៍ → ទីកន្លែងកំណើត", VaadinIcon.COPY.create());
	boolean allowSeeSalary=false;

	private final IntegerField insuranceNo = new IntegerField("Insurance | លេខធានារ៉ាប់រង");
    private final TextField nameEn = new TextField("Name(En) | ឈ្មោះ (អង់គ្លេស)");
    private final TextField nameKh = new TextField("Name(Kh) | ឈ្មោះ (ខ្មែរ)");
    private final ComboBox<GenderEnum> gender = new ComboBox<>("Gender | ភេទ",GenderEnum.values());
    private final DatePicker dob = new DatePicker("Date of birth | ថ្ងៃខែឆ្នាំកំណើត");
    private final ComboBox<String> maritalStatus = new ComboBox<>("Marital Status | ស្ថានភាពអាពាហ៍ពិពាហ៍");
    private final TextField phoneNumber = new TextField("Personal Cell No | លេខទូរស័ព្ទផ្ទាល់ខ្លួន");
    private final ComboBox<String> bloodGroup = new ComboBox<>("Blood Group | ក្រុមឈាម");
    private final DatePicker joinDate = new DatePicker("Date of Join (DOJ) | ថ្ងៃចូលធ្វើការ");
    private final ComboBox<Positions> positions = new ComboBox<>("Designation | មុខតំណែង");
    private final ComboBox<Department> department = new ComboBox<>("Department | ផ្នែក");
    private final ComboBox<Category> category = new ComboBox<>("Catagory​ | ប្រភេទ");
    private final ComboBox<Teams> teams = new ComboBox<>("Team | ក្រុម"); 
    
    private final TextField personalEmail = new TextField("Personal Email | អ៊ីមែលផ្ទាល់ខ្លួន");
    private final TextField officialCellNo = new TextField("Official Cell No | លេខទូរស័ព្ទការងារ");
    private final TextField officialEmail = new TextField("Official Email | អ៊ីមែលការងារ");
    
    private final ComboBox<InsuranceType> insuranceType = new ComboBox<>("Insurance Type | ប្រភេទធានារ៉ាប់រង");
    private final ComboBox<Nationality> nationality = new ComboBox<>("Nationality | សញ្ជាតិ");
    private final ComboBox<Ethnicity> ethnicity = new ComboBox<>("Ethnicity | ជនជាតិ");
    private final ComboBox<Religions> religions = new ComboBox<>("Religion | សាសនា");
    
    private final DatePicker probationEndDate = new DatePicker("Probation End Date | ថ្ងៃបញ្ចប់សាកល្បងការងារ");
    private final ComboBox<ProbationStatus> probationStatus = new ComboBox<>("Probation Status | ស្ថានភាពសាកល្បងការងារ");
    
    private final DatePicker currentContractStartDate = new DatePicker("Current Contract Start Date | ថ្ងៃចាប់ផ្តើមកិច្ចសន្យាបច្ចុប្បន្ន");
    private final DatePicker currentContractEndDate = new DatePicker("Current Contract End Date | ថ្ងៃបញ្ចប់កិច្ចសន្យាបច្ចុប្បន្ន");
    private final ComboBox<ContractType> contractType = new ComboBox<>("Contract Type | ប្រភេទកិច្ចសន្យា");
    private final NumberField grossSalaryUsd=new NumberField("Gross Salary - USD | ប្រាក់ខែសរុប (ដុល្លារអាមេរិក)");
    private final TextField grossSalaryMasked = new TextField("Gross Salary - USD | ប្រាក់ខែសរុប (ដុល្លារអាមេរិក)");

    
    
    private final ComboBox<Bank> bank = new ComboBox<>("Name of Bank | ឈ្មោះធនាគារ");
    private final TextField bankAccount = new TextField("Bank Account Number | លេខគណនីធនាគារ");
    private final TextField bankAccountName = new TextField("Bank Account Name | ឈ្មោះម្ចាស់គណនី");
    
    private final IntegerField code=new IntegerField("Code | កូដ");
    
    private final TextField lastOrganization = new TextField("Last Organization | កន្លែងធ្វើការចុងក្រោយ");
    private final TextField preHaloWorkExperience = new TextField("Pre HALO-Work Experience​ | បទពិសោធន៍ការងារមុនចូល HALO");
    private final Checkbox preExistingMedicalCondition = new Checkbox("Pre-Existing Medical Condition | ជំងឺប្រចាំកាយ");
    
    
    private final ComboBox<Branch> branch = new ComboBox<>("Location | តំបន់");
    private final TextField idCardNumber = new TextField("NID (National Identification Number)​ | លេខអត្តសញ្ញាណប័ណ្ណ");
    private final DatePicker idCardExpiryDate = new DatePicker("NID Expiry Date | ថ្ងៃផុតកំណត់អត្តសញ្ញាណប័ណ្ណ");
    private final TextField nsffCardNumber = new TextField("National Social Security Fund (NSSF) No | លេខបណ្ណ ប.ស.ស (NSSF)");
    private final DatePicker nsffExpiryDate = new DatePicker("National Social Security Fund (NSSF) Expiry Date | ថ្ងៃផុតកំណត់បណ្ណ ប.ស.ស (NSSF)");
    private final TextField nssfRetire = new TextField("Nssf Retire No | លេខចូលនិវត្តន៍ ប.ស.ស");
    private final ComboBox<DrivingLicense> drivingLicense = new ComboBox<>("Driving License | ប័ណ្ណបើកបរ");
    private final DatePicker drivingLicenseExpire = new DatePicker("Driving License Expire | ថ្ងៃផុតកំណត់ប័ណ្ណបើកបរ");
    private final ComboBox<PoGrade> poGrade = new ComboBox<>("PO Grade | កម្រិតថ្នាក់តំណែង");
    
    private final Checkbox referenceVerified=new Checkbox("Reference Verified | បានផ្ទៀងផ្ទាត់ឯកសារយោង");
    private final DatePicker referenceVerifiedDate = new DatePicker("Reference Verified Date | ថ្ងៃផ្ទៀងផ្ទាត់ឯកសារយោង");
    
    private final Checkbox policePlearance=new Checkbox("Police Clearance | លិខិតបញ្ជាក់ពីសមត្ថកិច្ច");

    
    private final ComboBox<Shift> attendanceType = new ComboBox("Attendance Type | ប្រភេទវត្តមាន");
    
    
    private final TextArea note=new TextArea("Remarks | កំណត់សម្គាល់");
    
    private  GazetteerField presentAddress;
    private  GazetteerField permanentAddress;
    private  GazetteerField nativeLocation;
    
    private final CareerTypeGroupRepository careerTypeGroupRepository;
    
    // for Supervisor
    private final Grid<EmployeeSupervision> supervisionGrid = new Grid<>(EmployeeSupervision.class, false);
    private final List<EmployeeSupervision> supervisionBuffer = new ArrayList<>();
    
    
 // --- Disability (PWD) ---
    private final Grid<EmployeeDisability> disabilityGrid = new Grid<>(EmployeeDisability.class, false);
    private final List<EmployeeDisability> disabilityBuffer = new ArrayList<>();
    
    
 // --- Emergency Contact ---
    private final Grid<EmployeeEmergencyContact> emergencyGrid = new Grid<>(EmployeeEmergencyContact.class, false);
    private final List<EmployeeEmergencyContact> emergencyBuffer = new ArrayList<>();
    
 // --- Relative Working ---
    private final Grid<EmployeeRelativeWorking> relativeWorkingGrid = new Grid<>(EmployeeRelativeWorking.class, false);
    private final List<EmployeeRelativeWorking> relativeWorkingBuffer = new ArrayList<>();
    
 // --- Language ---
    private final Grid<EmployeeLanguage> languageGrid = new Grid<>(EmployeeLanguage.class, false);
    private final List<EmployeeLanguage> languageBuffer = new ArrayList<>();

 // --- Dependency Family ---
    private final Grid<EmployeeDependencyFamily> dependencyFamilyGrid = new Grid<>(EmployeeDependencyFamily.class, false);
    private final List<EmployeeDependencyFamily> dependencyFamilyBuffer = new ArrayList<>();

    
 // --- Vaccination ---
    private final Grid<EmployeeVaccination> vaccinationGrid = new Grid<>(EmployeeVaccination.class, false);
    private final List<EmployeeVaccination> vaccinationBuffer = new ArrayList<>();

    // caches (avoid DB call per row)   
    private List<Vaccination> vaccinationsCache = new ArrayList<>();
   
    private final LanguageRepository languageRepository;
    private final LanguageSkillRepository languageSkillRepository;
    
 // --- Education ---
    private final Grid<EmployeeEducation> educationGrid = new Grid<>(EmployeeEducation.class, false);
    private final List<EmployeeEducation> educationBuffer = new ArrayList<>();

    // cache (optional)
    private List<EducationType> educationTypesCache = new ArrayList<>();
    private List<EducationCenter> educationCentersCache = new ArrayList<>();
    
 // --- Disciplinary ---
    private final Grid<EmployeeDisciplinary> disciplinaryGrid = new Grid<>(EmployeeDisciplinary.class, false);
    private final List<EmployeeDisciplinary> disciplinaryBuffer = new ArrayList<>();

    // cache (optional, like vaccination/education)
    private List<Disciplinary> disciplinaryActionsCache = new ArrayList<>();
    
 // ========================= 1) NationalStaffActiveView: add fields (like Dependency Family) =========================

 // --- Career History ---
	 private final Grid<EmployeeCareerHistory> careerHistoryGrid = new Grid<>(EmployeeCareerHistory.class, false);
	 private final List<EmployeeCareerHistory> careerHistoryBuffer = new ArrayList<>();
	 
	 // --- Career History ---
	 private final Grid<EmployeeTraining> trainingGrid = new Grid<>(EmployeeTraining.class, false);
	 private final List<EmployeeTraining> trainingBuffer = new ArrayList<>();
	
	// Add with other cache fields:
	 private List<TrainingCourse> trainingTypesCache = new ArrayList<>();
	 private List<TrainingProvider> trainingCentersCache = new ArrayList<>();
	 
	// --- Nominee ---
	 private final Grid<EmployeeNominee> nomineeGrid = new Grid<>(EmployeeNominee.class, false);
	 private final List<EmployeeNominee> nomineeBuffer = new ArrayList<>();



    private final DisabilityTypeOptionRepository disabilityTypeOptionRepository;
    private final DisabilityTypeRepository disabilityTypeRepository;

    private final MaritalStatusRepository maritalStatusRepository; 
    private final BloodGroupRepository bloodGroupRepository;
    private final PositionRepository positionRepository;
    private final DepartmentRepository departmentRepository;
    private final CategoryRepository categoryRepository;
    private final InsuranceTypeRepository insuranceTypeRepository;
    private final ProbationStatusRepository probationStatusRepository;
    private final ContractTypeRepository contractTypeRepository;
    private final BankRepository bankRepository;
    
    private final TeamsRepository teamsRepository;
    private final BranchRepository branchRepository;
    private final PoGradeRepository poGradeRepository;
    private final DrivingLicenseRepository drivingLicenseRepository;
    
    private final NationalityRepository nationalityRepository;
    private final EthnicityRepository ethnicityRepository;
    private final ReligionsRepository religionsRepository;
    private final RelationshipRepository  relationshipRepository;
    private final ShiftRepository shiftRepository;
    
	private final GazetteerService gazetteerService;
	private final Optional<User> currentUserLogin;
	
    private final VaccinationRepository vaccinationRepository;
    
    private final EducationTypeRepository educationTypeRepository;
    private final EducationCenterRepository educationCenterRepository;
    
    private final DisciplinaryRepository disciplinaryRepository;
    private final CareerTypeRepository careerTypeRepository;
    
    private final DonorsRepository donorsRepository;
    private final ContractsRepository contractsRepository;

    private final TrainingCourseRepository trainingTypeRepository;
    private final TrainingCenterRepository trainingCenterRepository;
    
    private AttachmentComponent attachmentComponent;
    private final AttachmentRepository attachmentRepository;
    private final AttachmentTypeRepository attachmentTypeRepository;
    private final FileUploadUtility fileUploadUtility;
    private Path uploadDir;
    
    private Path baseAttachmentsDir;
    private static final String attachmentsSubDirectory="attachments";

    public InternationalStaffView(EmployeeInternationActiveService service, UserService userService, AuthenticatedUser authenticatedUser,GazetteerService gazetteerService,MaritalStatusRepository maritalStatusRepository,BloodGroupRepository bloodGroupRepository,PositionRepository positionRepository,DepartmentRepository departmentRepository,CategoryRepository categoryRepository,TeamsRepository teamsRepository,BranchRepository branchRepository,PoGradeRepository poGradeRepository,DrivingLicenseRepository drivingLicenseRepository,DisabilityTypeOptionRepository disabilityTypeOptionRepository,DisabilityTypeRepository disabilityTypeRepository,InsuranceTypeRepository insuranceTypeRepository,ProbationStatusRepository probationStatusRepository,ContractTypeRepository contractTypeRepository,NationalityRepository nationalityRepository,EthnicityRepository ethnicityRepository,BankRepository bankRepository,RelationshipRepository  relationshipRepository,ShiftRepository shiftRepository,    LanguageRepository languageRepository,LanguageSkillRepository languageSkillRepository,VaccinationRepository vaccinationRepository,EducationTypeRepository educationTypeRepository,EducationCenterRepository educationCenterRepository,DisciplinaryRepository disciplinaryRepository,CareerTypeRepository careerTypeRepository,DonorsRepository donorsRepository,ContractsRepository contractsRepository,TrainingCourseRepository trainingTypeRepository,TrainingCenterRepository trainingCenterRepository, AttachmentRepository attachmentRepository, AttachmentTypeRepository attachmentTypeRepository, FileUploadUtility fileUploadUtility,ReligionsRepository religionsRepository,CareerTypeGroupRepository careerTypeGroupRepository) {
        super(Employee.class, service, userService, authenticatedUser);
        this.currentUserLogin = authenticatedUser.get();
        this.gazetteerService=gazetteerService;
        this.maritalStatusRepository=maritalStatusRepository;
        this.bloodGroupRepository=bloodGroupRepository;
        this.positionRepository=positionRepository;
        this.departmentRepository=departmentRepository;
        this.categoryRepository=categoryRepository;
        this.insuranceTypeRepository=insuranceTypeRepository;
        this.probationStatusRepository=probationStatusRepository;
        this.contractTypeRepository=contractTypeRepository;
        
        this.teamsRepository=teamsRepository;
        this.branchRepository=branchRepository;
        this.poGradeRepository=poGradeRepository;
        this.drivingLicenseRepository=drivingLicenseRepository;
        
        this.disabilityTypeRepository=disabilityTypeRepository;
        this.disabilityTypeOptionRepository=disabilityTypeOptionRepository;
        this.nationalityRepository=nationalityRepository;
        this.ethnicityRepository=ethnicityRepository;
        this.religionsRepository=religionsRepository;
        this.bankRepository=bankRepository;
        this.relationshipRepository=relationshipRepository;
        this.shiftRepository=shiftRepository;
        
        this.languageRepository = languageRepository;
        this.languageSkillRepository = languageSkillRepository;
        
        this.vaccinationRepository = vaccinationRepository;
        
        this.educationTypeRepository = educationTypeRepository;
        this.educationCenterRepository = educationCenterRepository;
        this.disciplinaryRepository=disciplinaryRepository;
        this.careerTypeRepository=careerTypeRepository;
        
        this.donorsRepository=donorsRepository;
        this.contractsRepository=contractsRepository;

        this.trainingTypeRepository=trainingTypeRepository;
        this.trainingCenterRepository=trainingCenterRepository;
        
        this.attachmentRepository = attachmentRepository;
        this.attachmentTypeRepository = attachmentTypeRepository;
        this.fileUploadUtility = fileUploadUtility;
        this.uploadDir =null;// fileUploadUtility.initializeUploadDirectory("hr", "");
        
        this.careerTypeGroupRepository = careerTypeGroupRepository;
    }


    @Override
    protected void onViewAttachOnce(AttachEvent attachEvent) throws Exception {
    	
    	baseAttachmentsDir=fileUploadUtility.initializeUploadDirectory("hr", attachmentsSubDirectory);
        MenuItem miPrint=leftMenu.addAction(VaadinIcon.USER_CARD,null, this::printIDCard,"Print ID Card | បោះពុម្ពប័ណ្ណអត្តសញ្ញាណ");
        MenuItem miEmployeeInfo=leftMenu.addAction(VaadinIcon.NOTEBOOK,null, this::preveiwEmployeeInfo,"View Employee Information | មើលព័ត៌មានបុគ្គលិក");
    	this.enableToggleColumn = true;
    	this.toggleColumnFrozen=false;
        allowSeeSalary = canSeeSalary();
        
        if (uploadDir == null) {
            uploadDir = fileUploadUtility.initializeUploadDirectory("hr", "");
        }
        
    	this.presentAddress = new GazetteerField(gazetteerService);
    	this.presentAddress.setLabel("Present Address | អាសយដ្ឋានបច្ចុប្បន្ន");
    	this.presentAddress.setSizeFull();

    	this.permanentAddress = new GazetteerField(gazetteerService);
    	this.permanentAddress.setLabel("Permanent Address | អាសយដ្ឋានអចិន្ត្រៃយ៍");
    	this.permanentAddress.setSizeFull();

    	this.nativeLocation = new GazetteerField(gazetteerService);
    	this.nativeLocation.setLabel("Native Location | ទីកន្លែងកំណើត");
    	this.nativeLocation.setSizeFull();
    	
    	grossSalaryMasked.setReadOnly(true);
    	grossSalaryMasked.setWidthFull();
    	grossSalaryMasked.setValue("XXXXX");

    	grossSalaryUsd.setWidthFull();


        
        configureGrid();
        

        
        configureEditorLayout();
        binderField();
        
        preLoadingData();
        
        configureNomineeGrid();
        
        HeaderRow headerRow = grid.prependHeaderRow();
        HeaderRow.HeaderCell nameGroup = headerRow.join(grid.getColumnByKey("nameEn"), grid.getColumnByKey("nameKh"));
        Div titleName = new Div();
        titleName.setText("Name​ | ឈ្មោះបុគ្គលិក");
        titleName.setWidthFull();
        titleName.getStyle()
                .set("display", "flex")
                .set("justify-content", "center")
                .set("align-items", "center");
        nameGroup.setComponent(titleName);

        
        HeaderRow.HeaderCell presentAddressGroup= headerRow.join(grid.getColumnByKey("presentAddress.parent.parent.parent.nameEn"), grid.getColumnByKey("presentAddress.parent.parent.nameEn"),grid.getColumnByKey("presentAddress.parent.nameEn"),grid.getColumnByKey("presentAddress.nameEn"),grid.getColumnByKey("presentAddress.code"));
        Div titleCurrentAddress = new Div();
        titleCurrentAddress.setText("Present Address | អាសយដ្ឋានបច្ចុប្បន្ន");
        titleCurrentAddress.setWidthFull();
        titleCurrentAddress.getStyle()
                .set("display", "flex")
                .set("justify-content", "center")
                .set("align-items", "center");
        presentAddressGroup.setComponent(titleCurrentAddress);
        
        HeaderRow.HeaderCell permanentAddressGroup= headerRow.join(grid.getColumnByKey("permanentAddress.parent.parent.parent.nameEn"), grid.getColumnByKey("permanentAddress.parent.parent.nameEn"),grid.getColumnByKey("permanentAddress.parent.nameEn"),grid.getColumnByKey("permanentAddress.nameEn"),grid.getColumnByKey("permanentAddress.code"));
        Div titlePermanentAddress = new Div();
        titlePermanentAddress.setText("Permanent Address | អាសយដ្ឋានអចិន្ត្រៃយ៍");
        titlePermanentAddress.setWidthFull();
        titlePermanentAddress.getStyle()
                .set("display", "flex")
                .set("justify-content", "center")
                .set("align-items", "center");
        permanentAddressGroup.setComponent(titlePermanentAddress);
        
        HeaderRow.HeaderCell nativeLocationGroup= headerRow.join(grid.getColumnByKey("nativeLocation.parent.parent.parent.nameEn"), grid.getColumnByKey("nativeLocation.parent.parent.nameEn"),grid.getColumnByKey("nativeLocation.parent.nameEn"),grid.getColumnByKey("nativeLocation.nameEn"),grid.getColumnByKey("nativeLocation.code"));
        Div titleNativeLocation = new Div();
        titleNativeLocation.setText("Native Location | ទីកន្លែងកំណើត");
        titleNativeLocation.setWidthFull();
        titleNativeLocation.getStyle()
                .set("display", "flex")
                .set("justify-content", "center")
                .set("align-items", "center");
        nativeLocationGroup.setComponent(titleNativeLocation);

    }
    private void configureNomineeGrid() {
        nomineeGrid.setWidthFull();
        nomineeGrid.setAllRowsVisible(true);
        nomineeGrid.addThemeVariants(
            GridVariant.LUMO_COLUMN_BORDERS,
            GridVariant.LUMO_ROW_STRIPES,
            GridVariant.LUMO_WRAP_CELL_CONTENT
        );

        // Priority column with number field
        nomineeGrid.addComponentColumn(nominee -> {
            IntegerField priorityField = new IntegerField();
            priorityField.setWidthFull();
            priorityField.setMin(1);
            priorityField.setStepButtonsVisible(true);
            priorityField.setValue(nominee.getPriority());
            priorityField.addValueChangeListener(ev -> nominee.setPriority(ev.getValue()));
            return priorityField;
        }).setHeader("Priority | អាទិភាព")
          .setResizable(true)
          .setAutoWidth(true);

        // Nominee Name
        nomineeGrid.addComponentColumn(nominee -> {
            TextField nameField = new TextField();
            nameField.setWidthFull();
            nameField.setValue(nominee.getNomineeName() == null ? "" : nominee.getNomineeName());
            nameField.addValueChangeListener(ev -> nominee.setNomineeName(ev.getValue()));
            return nameField;
        }).setHeader("Nominee Name | ឈ្មោះអ្នកទទួលផល")
          .setResizable(true)
          .setAutoWidth(true);

        // Nominee Phone
        nomineeGrid.addComponentColumn(nominee -> {
            TextField phoneField = new TextField();
            phoneField.setWidthFull();
            phoneField.setValue(nominee.getNomineePhone() == null ? "" : nominee.getNomineePhone());
            phoneField.addValueChangeListener(ev -> nominee.setNomineePhone(ev.getValue()));
            return phoneField;
        }).setHeader("Phone | លេខទូរស័ព្ទ")
          .setResizable(true)
          .setAutoWidth(true);

        // Nominee NID
        nomineeGrid.addComponentColumn(nominee -> {
            TextField nidField = new TextField();
            nidField.setWidthFull();
            nidField.setValue(nominee.getNomineeNid() == null ? "" : nominee.getNomineeNid());
            nidField.addValueChangeListener(ev -> nominee.setNomineeNid(ev.getValue()));
            return nidField;
        }).setHeader("NID | អត្តសញ្ញាណប័ណ្ណ")
          .setResizable(true)
          .setAutoWidth(true);

        // Relationship
        nomineeGrid.addComponentColumn(nominee -> {
            ComboBox<Relationship> relationshipCombo = new ComboBox<>();
            relationshipCombo.setItems(this.relationshipRepository.findAll(Sort.by("relationshipEn")));
            relationshipCombo.setItemLabelGenerator(r -> formatEnKh(r.getRelationshipEn(), r.getRelationshipKh()));
            relationshipCombo.setWidthFull();
            relationshipCombo.setClearButtonVisible(true);
            relationshipCombo.setValue(nominee.getRelationshipWithNominee());
            relationshipCombo.addValueChangeListener(ev -> nominee.setRelationshipWithNominee(ev.getValue()));
            return relationshipCombo;
        }).setHeader("Relationship | ទំនាក់ទំនង")
          .setResizable(true)
          .setAutoWidth(true);

        // Gazetteer (Address) - New column
        nomineeGrid.addComponentColumn(nominee -> {
            GazetteerField gazetteerField = new GazetteerField(gazetteerService, LayoutMode.HORIZONTAL);
            gazetteerField.setWidthFull();
            gazetteerField.setValue(nominee.getGazetteer());
            gazetteerField.addValueChangeListener(ev -> nominee.setGazetteer(ev.getValue()));
            return gazetteerField;
        }).setHeader("Address | អាសយដ្ឋាន")
          .setResizable(true)
          .setAutoWidth(true)
          .setFlexGrow(1);

        // Delete action
        nomineeGrid.addComponentColumn(nominee -> {
            Button deleteBtn = new Button(VaadinIcon.TRASH.create());
            deleteBtn.addThemeVariants(ButtonVariant.LUMO_TERTIARY, ButtonVariant.LUMO_ICON, ButtonVariant.LUMO_ERROR);
            deleteBtn.addClickListener(click -> {
                ConfirmDialog dialog = new ConfirmDialog();
                dialog.setHeader("Confirm Delete");
                dialog.setText("Are you sure you want to delete this nominee? | តើអ្នកប្រាកដថាចង់លុបអ្នកទទួលផលនេះទេ?");
                dialog.setCancelable(true);
                dialog.setCancelText("Cancel | បោះបង់");
                dialog.setConfirmText("Delete | លុប");
                dialog.setConfirmButtonTheme("error primary");
                dialog.addConfirmListener(ev -> {
                    nomineeBuffer.remove(nominee);
                    nomineeGrid.getDataProvider().refreshAll();
                });
                dialog.open();
            });
            return deleteBtn;
        }).setHeader("Action | សកម្មភាព")
          .setFrozenToEnd(true)
          .setAutoWidth(true)
          .setFlexGrow(0);

        nomineeGrid.setItems(nomineeBuffer);
    }
    
    private Integer nextNomineePriority() {
        return nomineeBuffer.stream()
                .map(EmployeeNominee::getPriority)
                .filter(Objects::nonNull)
                .max(Integer::compareTo)
                .map(p -> p + 1)
                .orElse(1);
    }
    
    private Component buildNomineeTab() {
        Button btnAdd = new Button("Add Nominee | បន្ថែមអ្នកទទួលផល", VaadinIcon.PLUS.create());
        btnAdd.addThemeVariants(ButtonVariant.LUMO_PRIMARY);
        btnAdd.addClickListener(e -> {
            EmployeeNominee nominee = new EmployeeNominee();
            nominee.setPriority(nextNomineePriority());
            nomineeBuffer.add(nominee);
            nomineeGrid.getDataProvider().refreshAll();
        });

        // Optional: Add help text
        Div helpText = new Div();
        helpText.add(new Span("Note: Nominee Name, Relationship, and Address are required fields. | កំណត់សម្គាល់៖ ឈ្មោះអ្នកទទួលផល, ទំនាក់ទំនង និង អាសយដ្ឋាន ត្រូវបានទាមទារ។"));
        helpText.getStyle()
            .set("font-size", "var(--lumo-font-size-xs)")
            .set("color", "var(--lumo-secondary-text-color)")
            .set("padding", "var(--lumo-space-s) 0")
            .set("margin", "0");

        VerticalLayout layout = new VerticalLayout(helpText, btnAdd, nomineeGrid);
        layout.setPadding(false);
        layout.setSpacing(false);
        layout.setMargin(false);
        layout.setWidthFull();
        return layout;
    }
    
    private void preveiwEmployeeInfo() {
    	if(!this.allowSeeSalary) {
            Notification.show("You don't have permission to do this operation",6000, Notification.Position.MIDDLE).addThemeVariants(NotificationVariant.LUMO_WARNING);
            return;
    	}
        if (grid.getSelectedItems().isEmpty()) {
            Notification.show("Please select grid before print",
                    5000, Notification.Position.MIDDLE);
            return;
        }

        // Collect all selected IDs
        java.util.Set<Long> ids = grid.getSelectedItems().stream()
                .map(Employee::getInsuranceNo)
                .map(Integer::longValue)   
                .filter(java.util.Objects::nonNull)
                .collect(java.util.stream.Collectors.toSet());

        if (ids.isEmpty()) {
            Notification.show("Selected rows have no IDs.",5000, Notification.Position.MIDDLE).addThemeVariants(NotificationVariant.LUMO_ERROR);
            return;
        }

        generateEmployeeForm(ids); 
    	
    }
    
    private void generateEmployeeForm(Collection<Long> Ids) {
        if (Ids == null || Ids.isEmpty()) {
            Notification.show("Employee cannot be null or empty.",  9000, Notification.Position.TOP_CENTER).addThemeVariants(NotificationVariant.LUMO_ERROR);
            return;
        }

        HashMap<String, Object> parameters = new HashMap<>();
        parameters.put("p_insurance_no", Ids); // ✅ Collection<Long>
        parameters.put("SUBREPORT_DIR", "report_embed");
        parameters.put("ATTACHMENT_DIR", baseAttachmentsDir.toString());
        parameters.put("currentUserLogin", this.currentUserLogin.get().getName());

        PreviewReport previewReport =  new PreviewReport("report_embed/staff_information.jasper", parameters);
        previewReport.open();
    }
    
    
    private void printIDCard() {
        if (grid.getSelectedItems().isEmpty()) {
            Notification.show("Please select grid before print",
                    5000, Notification.Position.TOP_CENTER);
            return;
        }

        // Collect all selected IDs
        java.util.Set<Long> ids = grid.getSelectedItems().stream()
                .map(Employee::getInsuranceNo)
                .map(Integer::longValue)   
                .filter(java.util.Objects::nonNull)
                .collect(java.util.stream.Collectors.toSet());

        if (ids.isEmpty()) {
            Notification.show("Selected rows have no IDs.",5000, Notification.Position.TOP_CENTER).addThemeVariants(NotificationVariant.LUMO_ERROR);
            return;
        }

        generateIDCardReportForm(ids); 
    	
    }
    
    private void generateIDCardReportForm(Collection<Long> Ids) {
        if (Ids == null || Ids.isEmpty()) {
            Notification.show("Employee cannot be null or empty.",  9000, Notification.Position.TOP_CENTER).addThemeVariants(NotificationVariant.LUMO_ERROR);
            return;
        }

        HashMap<String, Object> parameters = new HashMap<>();
        parameters.put("p_insurance_no", Ids); // ✅ Collection<Long>
        parameters.put("SUBREPORT_DIR", "report_embed");
        parameters.put("ATTACHMENT_DIR", baseAttachmentsDir.toString());
        parameters.put("currentUserLogin", this.currentUserLogin.get().getName());

        PreviewReport previewReport =  new PreviewReport("report_embed/national_id_card.jasper", parameters);
        previewReport.open();
    }
    
    private void preLoadingData() {  
    	
    	injectStickySearchCss();
    	
        this.gender.setItemLabelGenerator(e->e.getLabel());
        this.maritalStatus.setItems(maritalStatusRepository.findAll().stream().map(MaritalStatus::getMaritalStatusName).toList());
        this.bloodGroup.setItems(bloodGroupRepository.findAll().stream().map(BloodGroup::getBloodGroup).toList());
        this.positions.setItems(this.positionRepository.findAll(org.springframework.data.domain.Sort.by("position")));
        this.positions.setItemLabelGenerator(e -> formatEnKh(e.getPosition(), e.getPositionKh()));
        
        this.department.setItems(this.departmentRepository.findAll(org.springframework.data.domain.Sort.by("name")));
        this.department.setItemLabelGenerator(e -> formatEnKh(e.getName(), e.getNameKH()));
        
        this.category.setItems(this.categoryRepository.findAll(org.springframework.data.domain.Sort.by("category")));
        this.category.setItemLabelGenerator(Category::getCategory);
        
        this.insuranceType.setItems(this.insuranceTypeRepository.findAll());
        this.insuranceType.setItemLabelGenerator(InsuranceType::getInsuranceTypeName);
        
        this.probationStatus.setItems(this.probationStatusRepository.findAll());
        this.probationStatus.setItemLabelGenerator(ProbationStatus::getProbationStatusName);
        
        
        this.contractType.setItems(this.contractTypeRepository.findAll());
        this.contractType.setItemLabelGenerator(ContractType::getContractTypeName);
        
        this.teams.setItems(this.teamsRepository.findAll(org.springframework.data.domain.Sort.by("teamCode")));
        this.teams.setItemLabelGenerator(Teams::getTeamCode);
        
        this.branch.setItems(this.branchRepository.findAll(org.springframework.data.domain.Sort.by("branchShortName")));
        this.branch.setItemLabelGenerator(Branch::getBranchShortName);
        
        this.poGrade.setItems(this.poGradeRepository.findAll(org.springframework.data.domain.Sort.by("poGrade")));
        this.poGrade.setItemLabelGenerator(PoGrade::getPoGrade);
        
        this.drivingLicense.setItems(this.drivingLicenseRepository.findAll(org.springframework.data.domain.Sort.by("drivingLicenseName")));
        this.drivingLicense.setItemLabelGenerator(DrivingLicense::getDrivingLicenseName);
        
        this.nationality.setItems(this.nationalityRepository.findAll(org.springframework.data.domain.Sort.by("nationalityEn")));
        this.nationality.setItemLabelGenerator(e->e.getNationalityEn() +" | " + e.getNationalityKh());

        this.ethnicity.setItems(this.ethnicityRepository.findAll(org.springframework.data.domain.Sort.by("ethnicityEn")));
        this.ethnicity.setItemLabelGenerator(e->e.getEthnicityEn() +" | " + e.getEthnicityKh());
        
        
        this.bank.setItems(this.bankRepository.findAll(org.springframework.data.domain.Sort.by("bankName")));
        this.bank.setItemLabelGenerator(Bank::getBankName);
        
        //this.relationshipWithNominee.setItems(this.relationshipRepository.findAll(org.springframework.data.domain.Sort.by("relationshipEn")));
        //this.relationshipWithNominee.setItemLabelGenerator(Relationship::getRelationshipEn);
        
        this.attendanceType.setItems(this.shiftRepository.findAll());
        this.attendanceType.setItemLabelGenerator(Shift::getShiftName);
        

        this.vaccinationsCache = vaccinationRepository.findAll(org.springframework.data.domain.Sort.by("vaccineName"));
        
        this.educationTypesCache = educationTypeRepository.findAll(org.springframework.data.domain.Sort.by("educationTypeEN"));
        this.educationCentersCache = educationCenterRepository.findAll(org.springframework.data.domain.Sort.by("educationCenterEN"));
        
        this.disciplinaryActionsCache = disciplinaryRepository.findAll(org.springframework.data.domain.Sort.by("disciplinaryEn"));
        
        this.religions.setItems(this.religionsRepository.findAll(org.springframework.data.domain.Sort.by("religionEn")));
        this.religions.setItemLabelGenerator(e -> formatEnKh(e.getReligionEn(), e.getReligionKh()));


     // In preLoadingData() method, add:
        this.trainingTypesCache = trainingTypeRepository.findAll();
        this.trainingCentersCache = trainingCenterRepository.findAll();

        if (enableToggleColumn) {
            grid.setItemDetailsRenderer(this.buildGridTabRenderer());
        }
        
    }



    @Override
    protected void populateForm(Employee entity) throws Exception {
        this.entity = entity;

        boolean isNew = (this.entity.getId() == null);

        
        if (isNew) {
            this.entity.setEmployeeType(EmployeeTypeEnum.National);
            grossSalaryUsd.setVisible(true);
            grossSalaryUsd.setReadOnly(false);
            grossSalaryMasked.setVisible(false);
            
            // SOLUTION 1: Set new entity mode on attachment component
            if (attachmentComponent != null) {
                attachmentComponent.setNewEntityMode(true);
                attachmentComponent.clear();
            }
        }else {
            grossSalaryUsd.setVisible(allowSeeSalary);
            grossSalaryMasked.setVisible(!allowSeeSalary);
            grossSalaryUsd.setReadOnly(true);
            
            // SOLUTION 1: Set existing entity mode on attachment component
            if (attachmentComponent != null) {
                attachmentComponent.setNewEntityMode(false);
                attachmentComponent.setEntity(entity);
                attachmentComponent.refresh();
            }

        }

     // Add this with your other buffer initializations
        nomineeBuffer.clear();
        if (this.entity.getEmployeeNominees() != null) {
            nomineeBuffer.addAll(this.entity.getEmployeeNominees());
        }
        nomineeGrid.getDataProvider().refreshAll();

        
        // Supervisor buffer
        supervisionBuffer.clear();
        if (this.entity.getEmployeeSupervisions() != null) {
            supervisionBuffer.addAll(this.entity.getEmployeeSupervisions());
        }
        supervisionGrid.getDataProvider().refreshAll();
        
        // --- Disability buffer ---
        disabilityBuffer.clear();
        if (this.entity.getEmployeeDisabilities() != null) {   // ✅ make sure Employee has this getter
            disabilityBuffer.addAll(this.entity.getEmployeeDisabilities());
        }
        disabilityGrid.getDataProvider().refreshAll();
        
        // ✅ Emergency Contact buffer
        emergencyBuffer.clear();
        if (this.entity.getEmployeeEmergencyContacts() != null) {
            emergencyBuffer.addAll(this.entity.getEmployeeEmergencyContacts());
        }
        emergencyGrid.getDataProvider().refreshAll();

        
     // ✅ Relative Working buffer
        relativeWorkingBuffer.clear();
        if (this.entity.getEmployeeRelativeWorkings() != null) {
            relativeWorkingBuffer.addAll(this.entity.getEmployeeRelativeWorkings());
        }
        relativeWorkingGrid.getDataProvider().refreshAll();

     // ✅ Language buffer
        languageBuffer.clear();
        if (this.entity.getEmployeeLanguages() != null) {
            languageBuffer.addAll(this.entity.getEmployeeLanguages());
        }
        languageGrid.getDataProvider().refreshAll();
        
     // ✅ Dependency Family buffer
        dependencyFamilyBuffer.clear();
        if (this.entity.getDependencyFamilies() != null) {
            dependencyFamilyBuffer.addAll(this.entity.getDependencyFamilies());
        }
        dependencyFamilyGrid.getDataProvider().refreshAll();
        
     // ✅ Vaccination buffer
        vaccinationBuffer.clear();
        if (this.entity.getEmployeeVaccinations() != null) {
            vaccinationBuffer.addAll(this.entity.getEmployeeVaccinations());
        }
        vaccinationGrid.getDataProvider().refreshAll();

     // ✅ Education buffer
        educationBuffer.clear();
        if (this.entity.getEmployeeEducations() != null) {
            educationBuffer.addAll(this.entity.getEmployeeEducations());
        }
        educationGrid.getDataProvider().refreshAll();

     // ✅ Disciplinary buffer
        disciplinaryBuffer.clear();
        if (this.entity.getEmployeeDisciplinaries() != null) {
            disciplinaryBuffer.addAll(this.entity.getEmployeeDisciplinaries());
        }
        disciplinaryGrid.getDataProvider().refreshAll();

     // ========================= 2) populateForm(): load buffer =========================
        careerHistoryBuffer.clear();
        if (this.entity.getEmployeeCareerHistories() != null) {
            careerHistoryBuffer.addAll(this.entity.getEmployeeCareerHistories());
        }
        careerHistoryGrid.getDataProvider().refreshAll();
        

        trainingBuffer.clear();
        if (this.entity.getEmployeeTrainings() != null) {
            trainingBuffer.addAll(this.entity.getEmployeeTrainings());
        }
        trainingGrid.getDataProvider().refreshAll();

        
        if (attachmentComponent != null) {
            attachmentComponent.setEntity(entity);
            attachmentComponent.refresh();
        }
        
        binder.readBean(this.entity);
        editorLayout.open();
    }


    @Override
    protected void focusFirstField() {
        this.nameEn.focus();
    }

    @Override
    protected Employee createNewEntity() {
    	Employee r = new Employee();
    	r.setNationality(this.nationalityRepository.findByIsDefaultTrue().orElse(null));
    	r.setEthnicity(this.ethnicityRepository.findByIsDefaultTrue().orElse(null));
    	r.setEmployeeType(EmployeeTypeEnum.International);
        return r;
    }

    @Override
    protected void configureEditorLayout() throws Exception {

        // --- small helper to build consistent FormLayouts ---
        java.util.function.Function<Component[], FormLayout> form = (components) -> {
            FormLayout fl = new FormLayout(components);
            fl.setWidthFull();

            fl.setResponsiveSteps(
                    new FormLayout.ResponsiveStep("0", 1),
                    new FormLayout.ResponsiveStep("600px", 2),
                    new FormLayout.ResponsiveStep("1000px", 3),
                    new FormLayout.ResponsiveStep("1300px", 4)
            );

            for (Component c : components) {
                if (c instanceof com.vaadin.flow.component.HasSize hs) {
                    hs.setWidthFull();
                }
            }
            return fl;
        };
        
        configureAddressCloneButtons();
        HorizontalLayout clonePresentRow = new HorizontalLayout(
                btnClonePresentToPermanent,
                btnClonePresentToNative
        );
        clonePresentRow.setWidthFull();
        clonePresentRow.setPadding(false);
        clonePresentRow.setSpacing(true);
        clonePresentRow.setDefaultVerticalComponentAlignment(Alignment.CENTER);
        
        HorizontalLayout clonePermanentRow = new HorizontalLayout(btnClonePermanentToNative);
        clonePermanentRow.setWidthFull();
        clonePermanentRow.setPadding(false);
        clonePermanentRow.setSpacing(true);
        clonePermanentRow.setDefaultVerticalComponentAlignment(Alignment.CENTER);

        // ---------- Tab 1: Personal ----------
        FormLayout personalForm = form.apply(new Component[] {
                insuranceNo, nameEn, nameKh,
                gender, dob, maritalStatus,
                presentAddress,clonePresentRow, permanentAddress,clonePermanentRow, nativeLocation,
                phoneNumber, personalEmail,
                bloodGroup,
                nationality, ethnicity,religions,
                lastOrganization, preHaloWorkExperience, preExistingMedicalCondition,
                note
        });
        personalForm.setColspan(note, 4);
        personalForm.setColspan(clonePresentRow, 4);  
        personalForm.setColspan(clonePermanentRow, 4);  
        personalForm.setColspan(presentAddress, 4);
        personalForm.setColspan(permanentAddress, 4);
        personalForm.setColspan(nativeLocation, 4);

        // ---------- Tab 2: Employment ----------
        FormLayout employmentForm = form.apply(new Component[] {
                joinDate, positions, poGrade, department,
                category, insuranceType, teams,
                officialCellNo, officialEmail,
                probationEndDate, probationStatus,
                currentContractStartDate, currentContractEndDate, contractType,
                branch,
                attendanceType
               
        });

        // ---------- Tab 3: Payroll & Bank ----------
        FormLayout payrollForm = form.apply(new Component[] {
               
                grossSalaryUsd,grossSalaryMasked,
                bank, bankAccount, bankAccountName,
                code
        });

        // ---------- Tab 4: ID & Compliance (Documents moved here) ----------
        FormLayout complianceForm = form.apply(new Component[] {
                idCardNumber, idCardExpiryDate,
                nsffCardNumber, nsffExpiryDate,
                nssfRetire,
                drivingLicense, drivingLicenseExpire,
                referenceVerified, referenceVerifiedDate,
                policePlearance
        });

        // ---------- Tab 5: Nominee ----------
      //  FormLayout nomineeForm = form.apply(new Component[] {
      //          nomineeName, nomineePhone,
      //          nomineeNid, relationshipWithNominee
      //  });

        Component nomineeForm = buildNomineeTab();
        // ---------- Other Tabs ----------
        Component supervisorTab = buildSupervisorTab();
        Component disabilityTab = buildDisabilityTab();
        Component emergencyTab = buildEmergencyContactTab();
        Component relativeWorkingTab = buildRelativeWorkingTab();
        Component languageTab = buildLanguageTab();        
        Component dependencyFamilyTab = buildDependencyFamilyTab();
        Component vaccinationTab = buildVaccinationTab();
        Component educationTab = buildEducationTab();
        Component disciplinaryTab = buildDisciplinaryTab();
        Component careerHistoryTab = buildCareerHistoryTab();
        Component trainingTab = buildTrainingTab();
      

        // ---------- Build TabSheet ----------
        TabSheet tabs = new TabSheet();
        tabs.setWidthFull();
       // tabs.addClassName("sticky-tabs");
        Tab tPersonal      = new Tab(VaadinIcon.USER.create(), buildTabEnKh("Personal", "ព័ត៌មានផ្ទាល់ខ្លួន"));
       // Tab tEmployment    = new Tab(VaadinIcon.BRIEFCASE.create(), buildTabEnKh("Employment", "ការងារ"));
        Tab tPayroll       = new Tab(VaadinIcon.CREDIT_CARD.create(), buildTabEnKh("Payroll & Bank", "ប្រាក់ខែ និងធនាគារ"));
        Tab tCompliance    = new Tab(VaadinIcon.FILE_TEXT.create(), buildTabEnKh("ID", "អត្តសញ្ញាណ"));
        Tab tNominee       = new Tab(VaadinIcon.MONEY.create(), buildTabEnKh("Nominee", "អ្នកទទួលផល"));
        //Tab tSupervisor    = new Tab(VaadinIcon.USER_CHECK.create(), buildTabEnKh("Supervisor", "អ្នកគ្រប់គ្រង"));
        Tab tDisability    = new Tab(VaadinIcon.ACCESSIBILITY.create(), buildTabEnKh("Disability", "ពិការភាព"));
        Tab tEmergency     = new Tab(VaadinIcon.PHONE.create(), buildTabEnKh("Emergency Contact", "ទំនាក់ទំនងបន្ទាន់"));
        Tab tDependency    = new Tab(VaadinIcon.FAMILY.create(), buildTabEnKh("Dependency", "សមាជិកអាស្រ័យ"));
        Tab tRelative      = new Tab(VaadinIcon.USERS.create(), buildTabEnKh("Declaration", "សាច់ញាតិ"));
        //Tab tLanguage      = new Tab(VaadinIcon.GLOBE.create(), buildTabEnKh("Language", "ភាសា"));
        Tab tVaccination   = new Tab(VaadinIcon.HEART.create(), buildTabEnKh("Vaccination", "ការចាក់វ៉ាក់សាំង"));
        Tab tEducation     = new Tab(VaadinIcon.ACADEMY_CAP.create(), buildTabEnKh("Education", "ការអប់រំ"));
        Tab tDisciplinary  = new Tab(VaadinIcon.GAVEL.create(), buildTabEnKh("Disciplinary", "ពិន័យ"));
        Tab tCareerHistory = new Tab(VaadinIcon.TRENDING_UP.create(), buildTabEnKh("Employment History", "ប្រវត្តិការងារ"));
        Tab tTraining = new Tab(VaadinIcon.BOOK.create(), buildTabEnKh("Training", "ការបណ្តុះបណ្តាល"));
        Tab tAttachment = new Tab(VaadinIcon.PAPERCLIP.create(), buildTabEnKh("Attachments", "ឯកសារភ្ជាប់"));
        
       
        



        for (Tab t : new Tab[]{ tPersonal, tPayroll, tCompliance, tNominee,  tDisability, tEmergency, tRelative ,tDependency,tVaccination,tEducation,tDisciplinary,tCareerHistory,tTraining,tAttachment}) {
            t.addThemeVariants(com.vaadin.flow.component.tabs.TabVariant.LUMO_ICON_ON_TOP);
        }
        
        Details detailsPersonalForm = new Details("Basic Information | ព័ត៌មានមូលដ្ឋាន", personalForm);
        detailsPersonalForm.setWidthFull();
        detailsPersonalForm.setOpened(true);
        
        Details detailsEmploymentForm = new Details("Employment | ការងារ", employmentForm);
        detailsEmploymentForm.setWidthFull();
        detailsEmploymentForm.setOpened(true);
        
        Details detailsSupervisorTab = new Details("Supervisor | អ្នកគ្រប់គ្រង", supervisorTab);
        detailsSupervisorTab.setWidthFull();
        detailsSupervisorTab.setOpened(true);
        
        Details detailsLanguageForm = new Details("Language | ភាសា", languageTab);
        detailsLanguageForm.setWidthFull();
        detailsLanguageForm.setOpened(true);
        
        
        
        
        VerticalLayout personalTabContent =new VerticalLayout(detailsPersonalForm,detailsEmploymentForm,detailsSupervisorTab,detailsLanguageForm);
        personalTabContent.setSizeFull();
        personalTabContent.setMargin(false);
        personalTabContent.setPadding(false);
        
        tabs.add(tPersonal, buildTabWithStickySearch(null,personalTabContent));
        
        
       // tabs.add(tEmployment,  buildTabWithStickySearch(null,employmentForm));
        tabs.add(tPayroll,  buildTabWithStickySearch(null,payrollForm));
        tabs.add(tCompliance,  buildTabWithStickySearch(null,complianceForm));
        tabs.add(tNominee,  buildTabWithStickySearch(null,nomineeForm));
        
       // tabs.add(tSupervisor,  buildTabWithStickySearch(null,supervisorTab));
        tabs.add(tEmergency,  buildTabWithStickySearch(null,emergencyTab));
        tabs.add(tDependency,  buildTabWithStickySearch(null,dependencyFamilyTab));
        tabs.add(tRelative,  buildTabWithStickySearch(null,relativeWorkingTab));
       // tabs.add(tLanguage,  buildTabWithStickySearch(null,languageTab));
        
        tabs.add(tEducation,  buildTabWithStickySearch(null,educationTab));
        tabs.add(tCareerHistory,  buildTabWithStickySearch(null,careerHistoryTab));
        tabs.add(tTraining,  buildTabWithStickySearch(null,trainingTab));
        tabs.add(tDisciplinary,  buildTabWithStickySearch(null,disciplinaryTab));
        tabs.add(tDisability,  buildTabWithStickySearch(null,disabilityTab));
        tabs.add(tVaccination,  buildTabWithStickySearch(null,vaccinationTab));
        
        //tabs.add(tAttachment, buildAttachmentTab());
        
        tabs.add(tAttachment, buildTabWithStickySearch(null, buildAttachmentTab()));
        


        // wrap like your current style
        Div wrapper = new Div(tabs);
        wrapper.getStyle()
                .set("padding", "0 1rem")
                .set("box-sizing", "border-box");

        editorLayout.add(wrapper);

        // footer
        configureEditorFooter();
    }
    private Component buildTrainingTab() {
        Button btnAdd = new Button("Add training", VaadinIcon.PLUS.create());
        btnAdd.addThemeVariants(ButtonVariant.LUMO_PRIMARY);
        btnAdd.addClickListener(e -> openTrainingDialog(null)); // null = new

        trainingGrid.removeAllColumns();
        trainingGrid.setWidthFull();
        trainingGrid.setAllRowsVisible(true);
        trainingGrid.addThemeVariants(
            GridVariant.LUMO_COLUMN_BORDERS, 
            GridVariant.LUMO_ROW_STRIPES, 
            GridVariant.LUMO_WRAP_CELL_CONTENT
        );

        trainingGrid.addColumn(t -> t.getTrainingType() == null ? "" : t.getTrainingType().getTrainingCourseName()).setHeader("Training Type").setAutoWidth(true).setResizable(true);

        trainingGrid.addColumn(t -> t.getTrainingCenter() == null ? "" :t.getTrainingCenter().getTrainingProviderName()).setHeader("Training Provider").setAutoWidth(true).setResizable(true);

        trainingGrid.addColumn(t -> t.getStartDate() == null ? "" : 
            DateTimeUtilFormart.DATE_FORMATTER.format(t.getStartDate()))
            .setHeader("Start Date").setAutoWidth(true).setResizable(true);

        trainingGrid.addColumn(t -> t.getEndDate() == null ? "" : 
            DateTimeUtilFormart.DATE_FORMATTER.format(t.getEndDate()))
            .setHeader("End Date").setAutoWidth(true).setResizable(true);

        trainingGrid.addColumn(t -> t.getMaxScore() == null ? "" : t.getMaxScore().toString())
        .setHeader("Max Score").setAutoWidth(true).setResizable(true);
        
        trainingGrid.addColumn(t -> t.getScore() == null ? "" : t.getScore().toString())
            .setHeader("Score").setAutoWidth(true).setResizable(true);

        trainingGrid.addColumn(t -> t.getResult() == null ? "" : t.getResult())
            .setHeader("Result").setAutoWidth(true).setResizable(true);
        
        trainingGrid.addColumn(t -> t.getHaloSupportAmount() == null ? "" : t.getHaloSupportAmount())
        .setHeader("Cost (USD) | ថវិកាជំនួយ").setAutoWidth(true).setResizable(true);

        trainingGrid.addColumn(t -> t.getTrainer() == null ? "" : t.getTrainer())
            .setHeader("Trainer").setAutoWidth(true).setResizable(true);

        trainingGrid.addColumn(t -> t.getRemark() == null ? "" : t.getRemark())
            .setHeader("Remark").setAutoWidth(true).setResizable(true).setFlexGrow(1);

        trainingGrid.addComponentColumn(t -> {
            Button edit = new Button(VaadinIcon.EDIT.create());
            edit.addThemeVariants(ButtonVariant.LUMO_TERTIARY, ButtonVariant.LUMO_ICON);
            edit.addClickListener(e -> openTrainingDialog(t));

            Button del = new Button(VaadinIcon.TRASH.create());
            del.addThemeVariants(ButtonVariant.LUMO_TERTIARY, ButtonVariant.LUMO_ICON, ButtonVariant.LUMO_ERROR);
            del.addClickListener(e -> {
                ConfirmDialog dlg = new ConfirmDialog();
                dlg.setHeader("Confirm delete");
                dlg.setText("Are you sure you want to delete this training record?");
                dlg.setCancelable(true);
                dlg.setCancelText("Cancel");
                dlg.setConfirmText("Delete");
                dlg.setConfirmButtonTheme("error primary");
                dlg.addConfirmListener(ev -> {
                    trainingBuffer.remove(t);
                    trainingGrid.getDataProvider().refreshAll();
                });
                dlg.open();
            });

            HorizontalLayout actions = new HorizontalLayout(edit, del);
            actions.setPadding(false);
            actions.setSpacing(true);
            actions.setMargin(false);
            actions.setJustifyContentMode(JustifyContentMode.CENTER);
            actions.setDefaultVerticalComponentAlignment(Alignment.CENTER);
            return actions;
        })
        .setHeader("Action")
        .setFrozenToEnd(true)
        .setAutoWidth(true)
        .setFlexGrow(0)
        .setTextAlign(ColumnTextAlign.CENTER);

        trainingGrid.setItems(trainingBuffer);

        VerticalLayout layout = new VerticalLayout(btnAdd, trainingGrid);
        layout.setPadding(false);
        layout.setSpacing(false);
        layout.setMargin(false);
        layout.setWidthFull();
        return layout;
    }
    private void openTrainingDialog(EmployeeTraining editing) {
        boolean isNew = (editing == null);
        EmployeeTraining bean = isNew ? new EmployeeTraining() : editing;

        Dialog dialog = new Dialog();
        dialog.setHeaderTitle(isNew ? "Add Training" : "Edit Training");
        dialog.setWidth("1000px");

        // Training Type
        ComboBox<TrainingCourse> trainingType = new ComboBox<>("Training Course | វគ្គបណ្តុះបណ្តាល");
        trainingType.setItems(trainingTypeRepository.findByIsActiveTrueOrderByTrainingCourseNameAsc());
        trainingType.setItemLabelGenerator(TrainingCourse::getTrainingCourseName);
        trainingType.setClearButtonVisible(true);

        // Training Center
        ComboBox<TrainingProvider> trainingCenter = new ComboBox<>("Training Provider | អ្នកផ្តល់ការបណ្តុះបណ្តាល");
        trainingCenter.setItems(trainingCenterRepository.findAll());
        trainingCenter.setItemLabelGenerator(TrainingProvider::getTrainingProviderName);
        trainingCenter.setClearButtonVisible(true);

        // Dates
        DatePicker startDate = new DatePicker("Start Date | កាលបរិច្ឆេទចាប់ផ្តើម");
        DatePicker endDate = new DatePicker("End Date | កាលបរិច្ឆេទបញ្ចប់");

        // Score
        BigDecimalField maxScore = new BigDecimalField("Max Score | ពិន្ទុខ្ពស់បំផុត");
        maxScore.setClearButtonVisible(true);
        
        // Score
        BigDecimalField score = new BigDecimalField("Score | ពិន្ទុ");
        score.setClearButtonVisible(true);


        // Result
        ComboBox<String> result = new ComboBox<>("Result | លទ្ធផល");
        result.setItems("Pass", "Failed"); // Only Pass or Failed
        result.setClearButtonVisible(true);
        result.setWidthFull();

        // Score
        BigDecimalField haloSupportAmount = new BigDecimalField("Cost (USD) | ថវិកាជំនួយ");
        haloSupportAmount.setClearButtonVisible(true);
        
        // Trainer
        TextField trainer = new TextField("Trainer | គ្រូបណ្តុះបណ្តាល");
        trainer.setWidthFull();

        // Remark
        TextArea remark = new TextArea("Remark | កំណត់សម្គាល់");
        remark.setWidthFull();

        // Binder
        com.vaadin.flow.data.binder.BeanValidationBinder<EmployeeTraining> b =  new com.vaadin.flow.data.binder.BeanValidationBinder<>(EmployeeTraining.class);

        b.bind(trainingType, "trainingType");
        b.bind(trainingCenter, "trainingCenter");
        b.bind(startDate, "startDate");
        b.bind(endDate, "endDate");
        b.bind(maxScore, "maxScore");
        b.bind(score, "score");
        b.bind(result, "result");
        b.bind(haloSupportAmount, "haloSupportAmount");
        
        b.bind(trainer, "trainer");
        b.bind(remark, "remark");

        b.readBean(bean);

        FormLayout form = new FormLayout(
            trainingType, trainingCenter,
            startDate, endDate,
            maxScore,
            score, result,
            haloSupportAmount,
            trainer, remark
        );
        form.setWidthFull();
        form.setResponsiveSteps(
            new FormLayout.ResponsiveStep("0", 1),
            new FormLayout.ResponsiveStep("600px", 2),
            new FormLayout.ResponsiveStep("1000px", 3)
        );
        form.setColspan(remark, 3);
        Button btnSave = new Button("Save", e -> {
            try {
                // Validate and save
                if (!b.writeBeanIfValid(bean)) return;

                // Additional validations
                if (bean.getEndDate() != null && bean.getStartDate() != null
                        && bean.getEndDate().isBefore(bean.getStartDate())) {
                    showErrorMessage("End Date cannot be before Start Date. | ថ្ងៃបញ្ចប់មិនអាចមុនថ្ងៃចាប់ផ្តើមបានទេ។");
                    return;
                }

                // Required fields check
                if (bean.getTrainingType() == null || bean.getTrainingCenter() == null 
                        || bean.getEndDate() == null) {
                    showErrorMessage("Please complete: Training Course, Training Provider, and End Date. | សូមបំពេញ៖ វគ្គបណ្តុះបណ្តាល, អ្នកផ្តល់ការបណ្តុះបណ្តាល និង ថ្ងៃបញ្ចប់។");
                    return;
                }

                if (isNew) trainingBuffer.add(bean);

                trainingGrid.getDataProvider().refreshAll();
                dialog.close();
            } catch (Exception ex) {
                showErrorMessage(ex.getMessage());
            }
        });
        btnSave.addThemeVariants(ButtonVariant.LUMO_PRIMARY);

        Button btnCancel = new Button("Cancel", e -> dialog.close());

        dialog.getFooter().add(btnCancel, btnSave);
        dialog.add(form);
        dialog.open();
    }   
    private void configureAddressCloneButtons() {
        btnClonePresentToPermanent.addThemeVariants(ButtonVariant.LUMO_PRIMARY);
        btnClonePresentToNative.addThemeVariants(ButtonVariant.LUMO_PRIMARY);

        // ✅ style for new button (can be PRIMARY or CONTRAST)
        btnClonePermanentToNative.addThemeVariants(ButtonVariant.LUMO_PRIMARY);

        btnClonePresentToPermanent.addClickListener(e -> {
            Gazetteer v = presentAddress.getValue();
            if (v == null) {
                showErrorMessage("Please select Present Address first.");
                return;
            }
            permanentAddress.setValue(v);
        });

        btnClonePresentToNative.addClickListener(e -> {
            Gazetteer v = presentAddress.getValue();
            if (v == null) {
                showErrorMessage("Please select Present Address first.");
                return;
            }
            nativeLocation.setValue(v);
        });

        // ✅ Permanent -> Native
        btnClonePermanentToNative.addClickListener(e -> {
            Gazetteer v = permanentAddress.getValue();
            if (v == null) {
                showErrorMessage("Please select Permanent Address first.");
                return;
            }
            nativeLocation.setValue(v);
        });

        // optional enable/disable
        Runnable syncEnabled = () -> {
            boolean hasPresent = presentAddress.getValue() != null;
            boolean hasPermanent = permanentAddress.getValue() != null;

            btnClonePresentToPermanent.setEnabled(hasPresent);
            btnClonePresentToNative.setEnabled(hasPresent);
            btnClonePermanentToNative.setEnabled(hasPermanent);
        };

        presentAddress.addValueChangeListener(ev -> syncEnabled.run());
        permanentAddress.addValueChangeListener(ev -> syncEnabled.run());
        syncEnabled.run();
    }


    private Component buildTabEnKh(String tabLabelEN,String tabLabelKh) {
        Div personalLabel = new Div(
                new com.vaadin.flow.component.html.Span(tabLabelEN),
                new com.vaadin.flow.component.html.Span(tabLabelKh)
        );
        personalLabel.getStyle().set("display", "flex").set("flex-direction", "column").set("line-height", "1.1");
        return personalLabel;
    }
    private Component buildDisciplinaryTab() {

        Button btnAdd = new Button("Add disciplinary", VaadinIcon.PLUS.create());
        btnAdd.addThemeVariants(ButtonVariant.LUMO_PRIMARY);
        btnAdd.addClickListener(e -> openDisciplinaryDialog(null)); // null = new

        disciplinaryGrid.removeAllColumns();
        disciplinaryGrid.setWidthFull();
        disciplinaryGrid.setAllRowsVisible(true);
        disciplinaryGrid.addThemeVariants( GridVariant.LUMO_COLUMN_BORDERS, GridVariant.LUMO_ROW_STRIPES, GridVariant.LUMO_WRAP_CELL_CONTENT  );
        disciplinaryGrid.addColumn(d -> d.getPositions() == null ? "" : formatEnKh(d.getPositions().getPosition(), d.getPositions().getPositionKh())).setHeader("Position").setAutoWidth(true).setResizable(true);
        disciplinaryGrid.addColumn(d -> d.getStartDate() == null ? "" : DateTimeUtilFormart.DATE_FORMATTER.format(d.getStartDate())).setHeader("Start Date").setAutoWidth(true).setResizable(true);
        disciplinaryGrid.addColumn(d -> d.getExpireDate() == null ? "" : DateTimeUtilFormart.DATE_FORMATTER.format(d.getExpireDate())).setHeader("Expire Date").setAutoWidth(true).setResizable(true);
        disciplinaryGrid.addColumn(d -> d.getDisciplinary() == null ? "" : formatEnKh(d.getDisciplinary().getDisciplinaryEn(), d.getDisciplinary().getDisciplinaryKh())).setHeader("Disciplinary Action").setAutoWidth(true).setResizable(true);
        disciplinaryGrid.addColumn(d -> d.getDescription() == null ? "" : d.getDescription()).setHeader("Description").setAutoWidth(true).setResizable(true).setFlexGrow(1);
        disciplinaryGrid.addColumn(d -> d.getTeams() == null ? "" : d.getTeams().getTeamCode()).setHeader("Team").setAutoWidth(true).setResizable(true);
        disciplinaryGrid.addColumn(d -> d.getBranch() == null ? "" : d.getBranch().getBranchShortName()).setHeader("Location").setAutoWidth(true).setResizable(true);

        disciplinaryGrid.addColumn(d -> d.getReportedBy() == null ? "" : formatEnKh(d.getReportedBy().getNameEn(), d.getReportedBy().getNameKh())+ "-" + d.getReportedBy().getInsuranceNo()).setHeader("Reported By").setAutoWidth(true).setResizable(true);  
        disciplinaryGrid.addColumn(d -> d.getFineUsd() == null ? "" : d.getFineUsd().toString()).setHeader("Fine (USD)").setAutoWidth(true).setResizable(true); 
        
        disciplinaryGrid.addColumn(d -> d.getExpireDate() == null ? "" : d.getExpireDate().isAfter(LocalDate.now()) ? "Active" : "Inactive").setHeader("Status").setAutoWidth(true).setResizable(true);


       
        disciplinaryGrid.addComponentColumn(d -> {
            Button edit = new Button(VaadinIcon.EDIT.create());
            edit.addThemeVariants(ButtonVariant.LUMO_TERTIARY, ButtonVariant.LUMO_ICON);
            edit.addClickListener(e -> openDisciplinaryDialog(d));

            Button del = new Button(VaadinIcon.TRASH.create());
            del.addThemeVariants(ButtonVariant.LUMO_TERTIARY, ButtonVariant.LUMO_ICON, ButtonVariant.LUMO_ERROR);

            del.addClickListener(e -> {
                ConfirmDialog dlg = new ConfirmDialog();
                dlg.setHeader("Confirm delete");
                dlg.setText("Are you sure you want to delete this disciplinary record?");
                dlg.setCancelable(true);
                dlg.setCancelText("Cancel");
                dlg.setConfirmText("Delete");
                dlg.setConfirmButtonTheme("error primary");
                dlg.addConfirmListener(ev -> {
                    disciplinaryBuffer.remove(d);
                    disciplinaryGrid.getDataProvider().refreshAll();
                });
                dlg.open();
            });

            HorizontalLayout actions = new HorizontalLayout(edit, del);
            actions.setPadding(false);
            actions.setSpacing(true);
            actions.setMargin(false);
            actions.setDefaultVerticalComponentAlignment(Alignment.CENTER);
            actions.setJustifyContentMode(JustifyContentMode.CENTER);
            return actions;
        })
        .setHeader("Action")
        .setFrozenToEnd(true)
        .setAutoWidth(true)
        .setFlexGrow(0);

        disciplinaryGrid.setItems(disciplinaryBuffer);

        VerticalLayout layout = new VerticalLayout(btnAdd, disciplinaryGrid);
        layout.setPadding(false);
        layout.setSpacing(false);
        layout.setMargin(false);
        layout.setWidthFull();
        return layout;
    }
    private void openDisciplinaryDialog(EmployeeDisciplinary editing) {

        boolean isNew = (editing == null);
        EmployeeDisciplinary bean = isNew ? new EmployeeDisciplinary() : editing;

        Dialog dialog = new Dialog();
        dialog.setHeaderTitle(isNew ? "Add Disciplinary" : "Edit Disciplinary");
        dialog.setWidth("1000px");
        
        ComboBox<Positions> position = new ComboBox<>("Position");
        position.setItems(this.positionRepository.findAll(org.springframework.data.domain.Sort.by("position")));
        position.setItemLabelGenerator(p -> formatEnKh(p.getPosition(), p.getPositionKh()));
        position.setClearButtonVisible(true);


        DatePicker startDate = new DatePicker("Start Date");
        DatePicker expireDate = new DatePicker("Expire Date");

        ComboBox<Disciplinary> disciplinary = new ComboBox<>("Disciplinary Action");
        disciplinary.setItems(disciplinaryActionsCache);
        disciplinary.setItemLabelGenerator(d -> formatEnKh(d.getDisciplinaryEn(), d.getDisciplinaryKh()));
        disciplinary.setClearButtonVisible(true);

        TextArea description = new TextArea("Description");
        description.setWidthFull();

        ComboBox<Teams> teams = new ComboBox<>("Team");
        teams.setItems(this.teamsRepository.findAll(org.springframework.data.domain.Sort.by("teamCode")));
        teams.setItemLabelGenerator(Teams::getTeamCode);
        teams.setClearButtonVisible(true);

        ComboBox<Branch> branch = new ComboBox<>("Location");
        branch.setItems(this.branchRepository.findAll(org.springframework.data.domain.Sort.by("branchShortName")));
        branch.setItemLabelGenerator(Branch::getBranchShortName);
        branch.setClearButtonVisible(true);

        // Employee pickers (Reported By / Warned-Fined By)
        ComboBox<Employee> reportedBy = buildLazyComboBox(
                e -> formatEnKh(e.getNameEn(), e.getNameKh()) + "-" + e.getInsuranceNo().toString(),
                (filter, pageable) -> service.searchEmployeesByNameEnKhInsurance(filter, pageable),
                filter -> service.countEmployeesByNameEnKhInsurance(filter)
        );
        reportedBy.setLabel("Reported By");



       // warnedFinedBy.setLabel("Warned Fined By");


        // Fine
        BigDecimalField fineUsd = new BigDecimalField("Fine (USD)");

        fineUsd.setClearButtonVisible(true);

   
        

        if (this.entity != null) {
            if (bean.getPositions() == null) {
                bean.setPositions(this.entity.getPositions());
            }
            if (bean.getTeams() == null) {
                bean.setTeams(this.entity.getTeams());
            }
            if (bean.getBranch() == null) {
                bean.setBranch(this.entity.getBranch());
            }
        }
        



        // auto-calc duration
        Runnable recalc = () -> {
            if (startDate.getValue() == null || expireDate.getValue() == null) return;
            if (expireDate.getValue().isBefore(startDate.getValue())) return;

            //long totalMonths = ChronoUnit.MONTHS.between(startDate.getValue().withDayOfMonth(1), expireDate.getValue().withDayOfMonth(1));
            //int y = (int) (totalMonths / 12);
            //int m = (int) (totalMonths % 12);

            //years.setValue(y);
            //month.setValue(m);
        };
        startDate.addValueChangeListener(e -> recalc.run());
        expireDate.addValueChangeListener(e -> recalc.run());

        // Binder
        com.vaadin.flow.data.binder.BeanValidationBinder<EmployeeDisciplinary> b = new com.vaadin.flow.data.binder.BeanValidationBinder<>(EmployeeDisciplinary.class);

        b.bind(position, "positions");
        b.bind(startDate, "startDate");
        b.bind(expireDate, "expireDate");
        b.bind(disciplinary, "disciplinary");
        b.bind(description, "description");
        b.bind(teams, "teams");
        b.bind(branch, "branch");
        b.bind(reportedBy, "reportedBy");
       // b.bind(warnedFinedBy, "warnedFinedBy");

        // Convert NumberField (Double) <-> BigDecimal
        b.bind(fineUsd,"fineUsd");


        b.readBean(bean);

        FormLayout form = new FormLayout(
        		position,
        		disciplinary,
        		teams,
        		fineUsd,
                startDate, expireDate, 
                description,

                reportedBy,
                branch
                
        );
        form.setWidthFull();
        form.setResponsiveSteps(
                new FormLayout.ResponsiveStep("0", 1),
                new FormLayout.ResponsiveStep("600px", 2),
                new FormLayout.ResponsiveStep("1000px", 3)
        );
        form.setColspan(description, 3);
        
        position.setValue(bean.getPositions());
        teams.setValue(bean.getTeams());
        branch.setValue(bean.getBranch());


        Button btnSave = new Button("Save", e -> {
            try {
                if (!b.writeBeanIfValid(bean)) return;

                // extra checks
                if (bean.getExpireDate() != null && bean.getStartDate() != null
                        && bean.getExpireDate().isBefore(bean.getStartDate())) {
                    showErrorMessage("Expire Date cannot be before Start Date.");
                    return;
                }

                if (isNew) disciplinaryBuffer.add(bean);

                disciplinaryGrid.getDataProvider().refreshAll();
                dialog.close();
            } catch (Exception ex) {
                showErrorMessage(ex.getMessage());
            }
        });
        btnSave.addThemeVariants(ButtonVariant.LUMO_PRIMARY);

        Button btnCancel = new Button("Cancel", e -> dialog.close());

        dialog.getFooter().add(btnCancel, btnSave);
        dialog.add(form);
        dialog.open();
    }

    private Component buildVaccinationTab() {

        Button btnAdd = new Button("Add vaccination", VaadinIcon.PLUS.create());
        btnAdd.addThemeVariants(ButtonVariant.LUMO_PRIMARY);

        btnAdd.addClickListener(e -> {
            EmployeeVaccination ev = new EmployeeVaccination();
            vaccinationBuffer.add(ev);
            vaccinationGrid.getDataProvider().refreshAll();
        });

        vaccinationGrid.removeAllColumns(); // IMPORTANT if method can be called more than once
        vaccinationGrid.setWidthFull();
        vaccinationGrid.setAllRowsVisible(true);
        vaccinationGrid.addThemeVariants(
                GridVariant.LUMO_COLUMN_BORDERS,
                GridVariant.LUMO_ROW_STRIPES,
                GridVariant.LUMO_WRAP_CELL_CONTENT
        );

        // Vaccine Name
        vaccinationGrid.addComponentColumn(ev -> {
            ComboBox<Vaccination> cb = new ComboBox<>();
            cb.setItems(vaccinationsCache);
            cb.setItemLabelGenerator(Vaccination::getVaccineName);
            cb.setPlaceholder("Select vaccine...");
            cb.setClearButtonVisible(true);
            cb.setWidthFull();

            cb.setValue(ev.getVaccination());
            cb.addValueChangeListener(ch -> ev.setVaccination(ch.getValue()));
            return cb;
        }).setHeader("Vaccine Name").setResizable(true).setAutoWidth(true);

        // Date Injection
        vaccinationGrid.addComponentColumn(ev -> {
            DatePicker dp = new DatePicker();
            dp.setWidthFull();
            dp.setValue(ev.getDateInjection());
            dp.addValueChangeListener(ch -> ev.setDateInjection(ch.getValue()));
            return dp;
        }).setHeader("Date Injection").setResizable(true).setAutoWidth(true);

        // Delete (confirm)
        vaccinationGrid.addComponentColumn(ev -> {
            Button del = new Button(VaadinIcon.TRASH.create());
            del.addThemeVariants(ButtonVariant.LUMO_TERTIARY, ButtonVariant.LUMO_ICON, ButtonVariant.LUMO_ERROR);

            del.addClickListener(click -> {
                ConfirmDialog dlg = new ConfirmDialog();
                dlg.setHeader("Confirm delete");
                dlg.setText("Are you sure you want to delete this vaccination record?");
                dlg.setCancelable(true);
                dlg.setCancelText("Cancel");
                dlg.setConfirmText("Delete");
                dlg.setConfirmButtonTheme("error primary");
                dlg.addConfirmListener(ok -> {
                    vaccinationBuffer.remove(ev);
                    vaccinationGrid.getDataProvider().refreshAll();
                });
                dlg.open();
            });

            return del;
        }).setHeader("Action").setFrozenToEnd(true).setAutoWidth(true).setFlexGrow(0);

        vaccinationGrid.setItems(vaccinationBuffer);

        VerticalLayout layout = new VerticalLayout(btnAdd, vaccinationGrid);
        layout.setPadding(false);
        layout.setSpacing(false);
        layout.setMargin(false);
        layout.setWidthFull();
        return layout;
    }


    private Component buildDependencyFamilyTab() {

        Button btnAdd = new Button("Add dependency", VaadinIcon.PLUS.create());
        btnAdd.addThemeVariants(ButtonVariant.LUMO_PRIMARY);
        btnAdd.addClickListener(e -> openDependencyFamilyDialog(null)); // null = new

        dependencyFamilyGrid.setWidthFull();
        dependencyFamilyGrid.setAllRowsVisible(true);
        dependencyFamilyGrid.addThemeVariants(
                GridVariant.LUMO_COLUMN_BORDERS,
                GridVariant.LUMO_ROW_STRIPES,
                GridVariant.LUMO_WRAP_CELL_CONTENT
        );

        dependencyFamilyGrid.addColumn(df -> df.getDependencyName() == null ? "" : df.getDependencyName())
                .setHeader("Name").setAutoWidth(true).setResizable(true);

        dependencyFamilyGrid.addColumn(df -> df.getGender() == null ? "" : df.getGender().getLabel())
                .setHeader("Gender").setAutoWidth(true).setResizable(true);

        dependencyFamilyGrid.addColumn(df -> df.getDob() == null ? "" : DateTimeUtilFormart.DATE_FORMATTER.format(df.getDob()))
                .setHeader("DOB").setAutoWidth(true).setResizable(true);

        dependencyFamilyGrid.addColumn(df -> df.getRelationship() == null ? "" : formatEnKh(df.getRelationship().getRelationshipEn(), df.getRelationship().getRelationshipKh()))
                .setHeader("Relationship").setAutoWidth(true).setResizable(true);

        dependencyFamilyGrid.addColumn(df -> df.getPhone() == null ? "" : df.getPhone())
                .setHeader("Phone").setAutoWidth(true).setResizable(true);

        dependencyFamilyGrid.addColumn(df -> df.getNid() == null ? "" : df.getNid())
                .setHeader("NID").setAutoWidth(true).setResizable(true);

        dependencyFamilyGrid.addColumn(df -> df.getOccupation() == null ? "" : df.getOccupation())
                .setHeader("Occupation").setAutoWidth(true).setResizable(true);

        dependencyFamilyGrid.addColumn(df -> Boolean.TRUE.equals(df.getIncludeTax()) ? "Yes" : "No")
                .setHeader("Include Tax").setAutoWidth(true).setResizable(true);

        dependencyFamilyGrid.addComponentColumn(df -> {
            Button edit = new Button(VaadinIcon.EDIT.create());
            edit.addThemeVariants(ButtonVariant.LUMO_TERTIARY, ButtonVariant.LUMO_ICON);
            edit.addClickListener(e -> openDependencyFamilyDialog(df));

            Button del = new Button(VaadinIcon.TRASH.create());
            del.addThemeVariants(ButtonVariant.LUMO_TERTIARY, ButtonVariant.LUMO_ICON, ButtonVariant.LUMO_ERROR);
            del.addClickListener(e -> {
                ConfirmDialog dlg = new ConfirmDialog();
                dlg.setHeader("Confirm delete");
                dlg.setText("Are you sure you want to delete this dependency?");
                dlg.setCancelable(true);
                dlg.setCancelText("Cancel");
                dlg.setConfirmText("Delete");
                dlg.setConfirmButtonTheme("error primary");

                dlg.addConfirmListener(ev -> {
                    dependencyFamilyBuffer.remove(df);
                    dependencyFamilyGrid.getDataProvider().refreshAll();
                });

                dlg.open();
            });

            HorizontalLayout actions = new HorizontalLayout(edit, del);
            actions.setPadding(false);
            actions.setSpacing(true);
            actions.setMargin(false);
            actions.setJustifyContentMode(JustifyContentMode.CENTER);
            actions.setDefaultVerticalComponentAlignment(Alignment.CENTER);

            return actions;
        })
        .setHeader("Action")
        .setFrozenToEnd(true)
        .setAutoWidth(true)
        .setFlexGrow(0);


        dependencyFamilyGrid.setItems(dependencyFamilyBuffer);

        VerticalLayout layout = new VerticalLayout(btnAdd, dependencyFamilyGrid);
        layout.setPadding(false);
        layout.setSpacing(false);
        layout.setMargin(false);
        layout.setWidthFull();
        return layout;
    }
    private void openDependencyFamilyDialog(EmployeeDependencyFamily editing) {

        boolean isNew = (editing == null);
        EmployeeDependencyFamily bean = isNew ? new EmployeeDependencyFamily() : editing;

        Dialog dialog = new Dialog();
        dialog.setHeaderTitle(isNew ? "Add Dependency" : "Edit Dependency");
        dialog.setWidth("900px");

        // Fields
        TextField name = new TextField("Name");
        ComboBox<GenderEnum> gender = new ComboBox<>("Gender", GenderEnum.values());
        gender.setItemLabelGenerator(GenderEnum::getLabel);

        DatePicker dob = new DatePicker("Date of birth");
        TextField nid = new TextField("NID");
        TextField phone = new TextField("Phone");
        TextField occupation = new TextField("Occupation");
        Checkbox includeTax = new Checkbox("Include Tax");

        ComboBox<Relationship> relationship = new ComboBox<>("Relationship");
        relationship.setItems(this.relationshipRepository.findAll(org.springframework.data.domain.Sort.by("relationshipEn")));
        relationship.setItemLabelGenerator(r -> formatEnKh(r.getRelationshipEn(), r.getRelationshipKh()));
        relationship.setClearButtonVisible(true);

        // Binder
        com.vaadin.flow.data.binder.BeanValidationBinder<EmployeeDependencyFamily> b =
                new com.vaadin.flow.data.binder.BeanValidationBinder<>(EmployeeDependencyFamily.class);

        b.bind(name, "dependencyName");
        b.bind(gender, "gender");
        b.bind(dob, "dob");
        b.bind(nid, "nid");
        b.bind(phone, "phone");
        b.bind(occupation, "occupation");
        b.bind(includeTax, "includeTax");
        b.bind(relationship, "relationship");

        b.readBean(bean);

        FormLayout form = new FormLayout(name, gender, dob, relationship, nid, phone, occupation, includeTax);
        form.setWidthFull();
        form.setResponsiveSteps(
                new FormLayout.ResponsiveStep("0", 1),
                new FormLayout.ResponsiveStep("600px", 2),
                new FormLayout.ResponsiveStep("1000px", 3)
        );
        form.setColspan(name, 3);

        Button btnSave = new Button("Save", e -> {
            try {
                if (!b.writeBeanIfValid(bean)) return;

                // optional: prevent empty save (but your entity has required fields anyway)
                // if (bean.getDependencyName() == null || bean.getDependencyName().trim().isEmpty()) return;

                if (isNew) dependencyFamilyBuffer.add(bean);

                dependencyFamilyGrid.getDataProvider().refreshAll();
                dialog.close();
            } catch (Exception ex) {
                showErrorMessage(ex.getMessage());
            }
        });
        btnSave.addThemeVariants(ButtonVariant.LUMO_PRIMARY);

        Button btnCancel = new Button("Cancel", e -> dialog.close());

        dialog.getFooter().add(btnCancel, btnSave);
        dialog.add(form);
        dialog.open();
    }

    private Component buildLanguageTab() {

        Button btnAdd = new Button("Add language", VaadinIcon.PLUS.create());
        btnAdd.addThemeVariants(ButtonVariant.LUMO_PRIMARY);

        btnAdd.addClickListener(e -> {
            EmployeeLanguage el = new EmployeeLanguage();
            languageBuffer.add(el);
            languageGrid.getDataProvider().refreshAll();
        });

        languageGrid.setWidthFull();
        languageGrid.setAllRowsVisible(true);
        languageGrid.addThemeVariants(
                GridVariant.LUMO_COLUMN_BORDERS,
                GridVariant.LUMO_ROW_STRIPES,
                GridVariant.LUMO_WRAP_CELL_CONTENT
        );

        // Language
        languageGrid.addComponentColumn(el -> {
            ComboBox<Language> cb = buildLanguageComboBox();
            cb.setWidthFull();
            cb.setValue(el.getLanguage());
            cb.addValueChangeListener(ev -> el.setLanguage(ev.getValue()));
            return cb;
        }).setHeader("Language").setResizable(true).setAutoWidth(true);

        // Reading
        languageGrid.addComponentColumn(el -> {
            ComboBox<LanguageSkill> cb = buildLanguageSkillComboBox();
            cb.setWidthFull();
            cb.setValue(el.getLanguageReading());
            cb.addValueChangeListener(ev -> el.setLanguageReading(ev.getValue()));
            return cb;
        }).setHeader("Reading").setResizable(true).setAutoWidth(true);

        // Speaking
        languageGrid.addComponentColumn(el -> {
            ComboBox<LanguageSkill> cb = buildLanguageSkillComboBox();
            cb.setWidthFull();
            cb.setValue(el.getLanguageSpeaking());
            cb.addValueChangeListener(ev -> el.setLanguageSpeaking(ev.getValue()));
            return cb;
        }).setHeader("Speaking").setResizable(true).setAutoWidth(true);

        // Listening
        languageGrid.addComponentColumn(el -> {
            ComboBox<LanguageSkill> cb = buildLanguageSkillComboBox();
            cb.setWidthFull();
            cb.setValue(el.getLanguageListening());
            cb.addValueChangeListener(ev -> el.setLanguageListening(ev.getValue()));
            return cb;
        }).setHeader("Listening").setResizable(true).setAutoWidth(true);

        // Writing
        languageGrid.addComponentColumn(el -> {
            ComboBox<LanguageSkill> cb = buildLanguageSkillComboBox();
            cb.setWidthFull();
            cb.setValue(el.getLanguageWriting());
            cb.addValueChangeListener(ev -> el.setLanguageWriting(ev.getValue()));
            return cb;
        }).setHeader("Writing").setResizable(true).setAutoWidth(true);

        // Delete
        languageGrid.addComponentColumn(el -> {
            Button del = new Button(VaadinIcon.TRASH.create());
            del.addThemeVariants(ButtonVariant.LUMO_TERTIARY, ButtonVariant.LUMO_ICON, ButtonVariant.LUMO_ERROR);
            del.addClickListener(click -> {
                languageBuffer.remove(el);
                languageGrid.getDataProvider().refreshAll();
            });
            return del;
        }).setHeader("Action").setFrozenToEnd(true).setAutoWidth(true).setFlexGrow(0);

        languageGrid.setItems(languageBuffer);

        VerticalLayout layout = new VerticalLayout(btnAdd, languageGrid);
        layout.setPadding(false);
        layout.setSpacing(false);
        layout.setMargin(false);
        layout.setWidthFull();
        return layout;
    }

    private ComboBox<Language> buildLanguageComboBox() {
        ComboBox<Language> cb = new ComboBox<>();
        cb.setItems(languageRepository.findAll(org.springframework.data.domain.Sort.by("languageName")));
        cb.setItemLabelGenerator(Language::getLanguageName);
        cb.setPlaceholder("Select language...");
        cb.setClearButtonVisible(true);
        return cb;
    }

    private ComboBox<LanguageSkill> buildLanguageSkillComboBox() {
        ComboBox<LanguageSkill> cb = new ComboBox<>();
        cb.setItems(languageSkillRepository.findAll(org.springframework.data.domain.Sort.by("languageName")));
        cb.setItemLabelGenerator(LanguageSkill::getLanguageName);
        cb.setPlaceholder("Select skill...");
        cb.setClearButtonVisible(true);
        return cb;
    }

    private Component buildRelativeWorkingTab() {

        Button btnAdd = new Button("Add relative working", VaadinIcon.PLUS.create());
        btnAdd.addThemeVariants(ButtonVariant.LUMO_PRIMARY);

        btnAdd.addClickListener(e -> {
            EmployeeRelativeWorking rw = new EmployeeRelativeWorking();
            relativeWorkingBuffer.add(rw);
            relativeWorkingGrid.getDataProvider().refreshAll();
        });

        relativeWorkingGrid.setWidthFull();
        relativeWorkingGrid.setAllRowsVisible(true);
        relativeWorkingGrid.addThemeVariants(
                GridVariant.LUMO_COLUMN_BORDERS,
                GridVariant.LUMO_ROW_STRIPES,
                GridVariant.LUMO_WRAP_CELL_CONTENT
        );

        // Relative Employee (lazy search like supervisor)
        relativeWorkingGrid.addComponentColumn(rw -> {
            ComboBox<Employee> cb = buildRelativeEmployeeComboBox();
            cb.setWidthFull();
            cb.setValue(rw.getEmployeeRelative());
            cb.addValueChangeListener(ev -> rw.setEmployeeRelative(ev.getValue()));
            return cb;
        }).setHeader("Relative Employee").setResizable(true).setAutoWidth(true);

        // Department
        relativeWorkingGrid.addComponentColumn(rw -> {
            ComboBox<Department> cb = new ComboBox<>();
            cb.setItems(this.departmentRepository.findAll(org.springframework.data.domain.Sort.by("name")));
            cb.setItemLabelGenerator(d -> formatEnKh(d.getName(), d.getNameKH()));
            cb.setWidthFull();
            cb.setClearButtonVisible(true);

            cb.setValue(rw.getDepartment());
            cb.addValueChangeListener(ev -> rw.setDepartment(ev.getValue()));
            return cb;
        }).setHeader("Department").setResizable(true).setAutoWidth(true);

        // Relationship
        relativeWorkingGrid.addComponentColumn(rw -> {
            ComboBox<Relationship> cb = new ComboBox<>();
            cb.setItems(this.relationshipRepository.findAll(org.springframework.data.domain.Sort.by("relationshipEn")));
            cb.setItemLabelGenerator(Relationship::getRelationshipEn);
            cb.setWidthFull();
            cb.setClearButtonVisible(true);

            cb.setValue(rw.getRelationship());
            cb.addValueChangeListener(ev -> rw.setRelationship(ev.getValue()));
            return cb;
        }).setHeader("Relationship").setResizable(true).setAutoWidth(true);

        // Delete
        relativeWorkingGrid.addComponentColumn(rw -> {
            Button del = new Button(VaadinIcon.TRASH.create());
            del.addThemeVariants(ButtonVariant.LUMO_TERTIARY, ButtonVariant.LUMO_ICON, ButtonVariant.LUMO_ERROR);
            del.addClickListener(click -> {
                relativeWorkingBuffer.remove(rw);
                relativeWorkingGrid.getDataProvider().refreshAll();
            });
            return del;
        }).setHeader("Action").setFrozenToEnd(true).setAutoWidth(true).setFlexGrow(0);

        relativeWorkingGrid.setItems(relativeWorkingBuffer);

        VerticalLayout layout = new VerticalLayout(btnAdd, relativeWorkingGrid);
        layout.setPadding(false);
        layout.setSpacing(false);
        layout.setMargin(false);
        layout.setWidthFull();
        return layout;
    }
    private ComboBox<Employee> buildRelativeEmployeeComboBox() {
        ComboBox<Employee> cb = buildLazyComboBox(
                e -> formatEnKh(e.getNameEn(), e.getNameKh()),
                (filter, pageable) -> service.searchEmployeesByNameEnKhInsurance(filter, pageable),
                filter -> service.countEmployeesByNameEnKhInsurance(filter)
        );
        cb.setPlaceholder("Select relative employee...");
        cb.setClearButtonVisible(true);
        return cb;
    }

    private Component buildEmergencyContactTab() {

        Button btnAdd = new Button("Add emergency contact", VaadinIcon.PLUS.create());
        btnAdd.addThemeVariants(ButtonVariant.LUMO_PRIMARY);

        btnAdd.addClickListener(e -> {
            EmployeeEmergencyContact ec = new EmployeeEmergencyContact();
            emergencyBuffer.add(ec);
            emergencyGrid.getDataProvider().refreshAll();
        });

        emergencyGrid.setWidthFull();
        emergencyGrid.setAllRowsVisible(true);
        emergencyGrid.addThemeVariants(GridVariant.LUMO_COLUMN_BORDERS,GridVariant.LUMO_ROW_STRIPES,GridVariant.LUMO_WRAP_CELL_CONTENT);

        // Contact Name
        emergencyGrid.addComponentColumn(ec -> {
            TextField tf = new TextField();
            tf.setWidthFull();
            tf.setValue(ec.getEmergencyContactName() == null ? "" : ec.getEmergencyContactName());
            tf.addValueChangeListener(ev -> ec.setEmergencyContactName(ev.getValue()));
            return tf;
        }).setHeader("Contact Name").setResizable(true).setFrozen(true).setAutoWidth(true);

        // Phone
        emergencyGrid.addComponentColumn(ec -> {
            TextField tf = new TextField();
            tf.setWidthFull();
            tf.setValue(ec.getEmergencyContactPhone() == null ? "" : ec.getEmergencyContactPhone());
            tf.addValueChangeListener(ev -> ec.setEmergencyContactPhone(ev.getValue()));
            return tf;
        }).setHeader("Phone").setResizable(true).setAutoWidth(true);

        // Relationship
        emergencyGrid.addComponentColumn(ec -> {
            ComboBox<Relationship> cb = new ComboBox<>();
            cb.setItems(this.relationshipRepository.findAll(org.springframework.data.domain.Sort.by("relationshipEn")));
            cb.setItemLabelGenerator(Relationship::getRelationshipEn);
            cb.setWidthFull();
            cb.setClearButtonVisible(true);

            cb.setValue(ec.getRelationship());
            cb.addValueChangeListener(ev -> ec.setRelationship(ev.getValue()));
            return cb;
        }).setHeader("Relationship").setResizable(true).setAutoWidth(true);

        // Address (GazetteerField)
        emergencyGrid.addComponentColumn(ec -> {
            GazetteerField gf = new GazetteerField(gazetteerService,LayoutMode.HORIZONTAL);
            gf.setWidthFull();
            gf.setValue(ec.getGazetteer());
            gf.addValueChangeListener(ev -> ec.setGazetteer(ev.getValue()));
            return gf;
        }).setHeader("Address").setResizable(true).setAutoWidth(true).setFlexGrow(0);

        // Delete
        emergencyGrid.addComponentColumn(ec -> {
            Button del = new Button(VaadinIcon.TRASH.create());
            del.addThemeVariants(ButtonVariant.LUMO_TERTIARY, ButtonVariant.LUMO_ICON, ButtonVariant.LUMO_ERROR);
            del.addClickListener(click -> {
                emergencyBuffer.remove(ec);
                emergencyGrid.getDataProvider().refreshAll();
            });
            return del;
        }).setHeader("Action").setFrozenToEnd(true).setAutoWidth(true);

        emergencyGrid.setItems(emergencyBuffer);

        VerticalLayout layout = new VerticalLayout(btnAdd, emergencyGrid);
        layout.setPadding(false);
        layout.setSpacing(false);
        layout.setMargin(false);
        layout.setWidthFull();
        return layout;
    }

    private Component buildSupervisorTab() {

        // Toolbar
        Button btnAdd = new Button("Add supervisor", VaadinIcon.PLUS.create());
        btnAdd.addThemeVariants(ButtonVariant.LUMO_PRIMARY);

        btnAdd.addClickListener(e -> {
            EmployeeSupervision es = new EmployeeSupervision();
            es.setSortOrder(nextSupervisorSortOrder());
            supervisionBuffer.add(es);
            supervisionGrid.getDataProvider().refreshAll();
        });

        // Grid
        supervisionGrid.setWidthFull();
        supervisionGrid.setAllRowsVisible(true);

        supervisionGrid.addComponentColumn(es -> {
            IntegerField f = new IntegerField();
            f.setWidthFull();
            f.setStepButtonsVisible(true);
            f.setMin(1);

            f.setValue(es.getSortOrder());
            f.addValueChangeListener(ev -> es.setSortOrder(ev.getValue()));
            return f;
        })
        .setHeader("Order")
        .setWidth("160px")        // choose your width
        .setFlexGrow(0)           // keep fixed
        .setAutoWidth(false)      // IMPORTANT: stop auto shrinking
        .setResizable(true).setTextAlign(ColumnTextAlign.CENTER);


        supervisionGrid.addComponentColumn(es -> {
            ComboBox<Employee> cb =buildSupervisorComboBox();
            		// buildSupervisorComboBox();
            
            cb.setWidthFull();
            cb.setValue(es.getSupervisor());
            cb.addValueChangeListener(ev -> es.setSupervisor(ev.getValue()));
            return cb;
        }).setHeader("Supervisor").setFlexGrow(1).setResizable(true);

        supervisionGrid.addComponentColumn(es -> {
            Button del = new Button(VaadinIcon.TRASH.create());
            del.addThemeVariants(ButtonVariant.LUMO_TERTIARY, ButtonVariant.LUMO_ICON, ButtonVariant.LUMO_ERROR);
            del.addClickListener(click -> {
                supervisionBuffer.remove(es);
                supervisionGrid.getDataProvider().refreshAll();
            });
            return del;
        }).setHeader("Action").setFlexGrow(0).setAutoWidth(true);

        supervisionGrid.setItems(supervisionBuffer);

        VerticalLayout layout = new VerticalLayout(btnAdd, supervisionGrid);
        layout.setPadding(false);
        layout.setSpacing(false);
        layout.setMargin(false);
        layout.setWidthFull();
        return layout;
    }
    private ComboBox<Employee> buildSupervisorComboBox() {
        ComboBox<Employee> cb = buildLazyComboBox(
                e -> formatEnKh(e.getNameEn(), e.getNameKh()),          // label
                (filter, pageable) -> service.searchEmployeesByNameEnKhInsurance(filter, pageable),
                filter -> service.countEmployeesByNameEnKhInsurance(filter)                
        );

        cb.setPlaceholder("Select supervisor...");
        cb.setClearButtonVisible(true);
        return cb;
    }

    private Integer nextSupervisorSortOrder() {
        return supervisionBuffer.stream()
                .map(EmployeeSupervision::getSortOrder)
                .filter(Objects::nonNull)
                .max(Integer::compareTo)
                .map(v -> v + 1)
                .orElse(1);
    }


    @Override
    protected void binderField() {
        binder.bindInstanceFields(this);

    }

    @Override
    protected List<ColumnDef<Employee>> getColumnDefs() {
        return List.of(

                col("insuranceNo", this.insuranceNo.getLabel(), e->e.getInsuranceNo(), e -> e.getInsuranceNo() != null ? e.getInsuranceNo().toString() : ""),
                col("nameEn", this.nameEn.getLabel(), e->e.getNameEn(), e -> e.getNameEn() != null ? e.getNameEn() : ""),
                col("nameKh", this.nameKh.getLabel(), e->e.getNameKh(), e -> e.getNameKh() != null ? e.getNameKh() : ""),
                col("positions", this.positions.getLabel(), e->e.getPositions(), e -> e.getPositions() != null ? e.getPositions().getPosition() : ""),
                col("department", this.department.getLabel(), e->e.getDepartment(), e -> e.getDepartment() != null ? e.getDepartment().getName() : ""),
                col("joinDate", this.joinDate.getLabel(), e->e.getJoinDate(), e -> e.getJoinDate() != null ?DateTimeUtilFormart.DATE_FORMATTER.format( e.getJoinDate()) : ""),
                col("poGrade", this.poGrade.getLabel(), e->e.getPoGrade(), e -> e.getPoGrade() != null ? e.getPoGrade().getPoGrade() : ""),                
                col("gender", this.gender.getLabel(), e->e.getGender(), e -> e.getGender() != null ? e.getGender().getLabel() : ""),
                col("branch", this.branch.getLabel(), e->e.getBranch(), e -> e.getBranch() != null ? e.getBranch().getBranchShortName() : ""),
                col("officialCellNo", this.officialCellNo.getLabel(), e->e.getOfficialCellNo(), e -> e.getOfficialCellNo() != null ? e.getOfficialCellNo() : ""),
                col("officialEmail", this.officialEmail.getLabel(), e->e.getOfficialEmail(), e -> e.getOfficialEmail() != null ? e.getOfficialEmail() : ""),
                col("personalEmail", this.personalEmail.getLabel(), e->e.getPersonalEmail(), e -> e.getPersonalEmail() != null ? e.getPersonalEmail() : ""),
                col("lineManagers", "Line Manager | អ្នកគ្រប់គ្រងផ្ទាល់", 
                	    e -> e.getEmployeeSupervisions(),
                	    e -> {
                	        if (e.getEmployeeSupervisions() == null || e.getEmployeeSupervisions().isEmpty()) {
                	            return "";
                	        }                	       
                	        return e.getEmployeeSupervisions().stream()
                	            .filter(es -> es.getSupervisor() != null)
                	            .min(Comparator.comparing(EmployeeSupervision::getSortOrder, 
                	                Comparator.nullsLast(Comparator.naturalOrder())))
                	            .map(es -> {
                	                Employee sup = es.getSupervisor();
                	                return String.format("%s - %d", 
                	                    sup.getNameEn() != null ? sup.getNameEn() : "", 
                	                    sup.getInsuranceNo() != null ? sup.getInsuranceNo() : 0);
                	            })
                	            .orElse("");
                	    }
                	),
                
                col("insuranceType", this.insuranceType.getLabel(), e->e.getInsuranceType(), e -> e.getInsuranceType() != null ? e.getInsuranceType().getInsuranceTypeName() : ""),
                col("careerType", "Employment Status | ស្ថានភាពបុគ្គលិក", e->e.getCareerType(), e -> e.getCareerType() != null ? e.getCareerType().getCareerTypeGroup().getCareerTypeGroupName() : ""),
                col("lastCareerTypeDate", "Leaving Date | កាលបរិច្ឆេទចាកចេញ",
                        e -> e.getLastCareerTypeDate(),
                        e -> e.getLastCareerTypeDate() != null ? DateTimeUtilFormart.DATE_FORMATTER.format(e.getLastCareerTypeDate()) : ""),
                

                col("teams", this.teams.getLabel(), e->e.getTeams(), e -> e.getTeams() != null ? e.getTeams().getTeamCode() : ""),
                col("category", this.category.getLabel(), e->e.getCategory(), e -> e.getCategory() != null ? e.getCategory().getCategory() : ""),
                col("contractType", this.contractType.getLabel(), e->e.getContractType(), e -> e.getContractType() != null ? e.getContractType().getContractTypeName() : ""),
                
                // ---------- Present Address ----------
                col("presentAddress.parent.parent.parent.nameEn", "Present Province | ខេត្ត (បច្ចុប្បន្ន)", e -> e.getPresentAddress(), e -> { Gazetteer p = toLevel(e.getPresentAddress(), 1); return p != null ? p.getNameEn() : ""; }),
                col("presentAddress.parent.parent.nameEn", "Present District | ស្រុក (បច្ចុប្បន្ន)", e -> e.getPresentAddress(), e -> { Gazetteer d = toLevel(e.getPresentAddress(), 2); return d != null ? d.getNameEn() : ""; }),
                col("presentAddress.parent.nameEn", "Present Commune | ឃុំ (បច្ចុប្បន្ន)", e -> e.getPresentAddress(), e -> { Gazetteer c = toLevel(e.getPresentAddress(), 3); return c != null ? c.getNameEn() : ""; }),
                col("presentAddress.nameEn", "Present Village | ភូមិ (បច្ចុប្បន្ន)", e -> e.getPresentAddress(), e -> { Gazetteer v = toLevel(e.getPresentAddress(), 4); return v != null ? v.getNameEn() : ""; }),
                col("presentAddress.code", "Present Village Code | លេខកូដភូមិ (បច្ចុប្បន្ន)", e -> e.getPresentAddress(), e -> { Gazetteer v = toLevel(e.getPresentAddress(), 4); return v != null ? v.getCode() : ""; }),
                
                
                
                
                col("dob", this.dob.getLabel(), e->e.getDob(), e -> e.getDob() != null ?DateTimeUtilFormart.DATE_FORMATTER.format( e.getDob()) : ""),
                col("maritalStatus", this.maritalStatus.getLabel(), e->e.getMaritalStatus(), e -> e.getMaritalStatus() != null ? e.getMaritalStatus() : ""),
                
                col("phoneNumber", this.phoneNumber.getLabel(), e->e.getPhoneNumber(), e -> e.getPhoneNumber() != null ? e.getPhoneNumber() : ""),
               

                
                col("bloodGroup", this.bloodGroup.getLabel(), e->e.getBloodGroup(), e -> e.getBloodGroup() != null ? e.getBloodGroup() : ""),
                
                col("nationality", this.nationality.getLabel(), e->e.getNationality(), e -> e.getNationality() != null ? e.getNationality().getNationalityEn() : ""),
                col("ethnicity", this.ethnicity.getLabel(), e->e.getEthnicity(), e -> e.getEthnicity() != null ? e.getEthnicity().getEthnicityEn() : ""),
                col("religions", "Religion | សាសនា", e -> e.getReligions(), e -> e.getReligions() != null ? formatEnKh(e.getReligions().getReligionEn(), e.getReligions().getReligionKh()) : ""),
                



                // ---------- Permanent Address ----------
                col("permanentAddress.parent.parent.parent.nameEn", "Permanent Province | ខេត្ត (អចិន្ត្រៃយ៍)", e -> e.getPermanentAddress(), e -> { Gazetteer p = toLevel(e.getPermanentAddress(), 1); return p != null ? p.getNameEn() : ""; }),
                col("permanentAddress.parent.parent.nameEn", "Permanent District | ស្រុក (អចិន្ត្រៃយ៍)", e -> e.getPermanentAddress(), e -> { Gazetteer d = toLevel(e.getPermanentAddress(), 2); return d != null ? d.getNameEn() : ""; }),
                col("permanentAddress.parent.nameEn", "Permanent Commune | ឃុំ (អចិន្ត្រៃយ៍)", e -> e.getPermanentAddress(), e -> { Gazetteer c = toLevel(e.getPermanentAddress(), 3); return c != null ? c.getNameEn() : ""; }),
                col("permanentAddress.nameEn", "Permanent Village | ភូមិ (អចិន្ត្រៃយ៍)", e -> e.getPermanentAddress(), e -> { Gazetteer v = toLevel(e.getPermanentAddress(), 4); return v != null ? v.getNameEn() : ""; }),
                col("permanentAddress.code", "Permanent Village Code | លេខកូដភូមិ (អចិន្ត្រៃយ៍)", e -> e.getPermanentAddress(),e -> { Gazetteer v = toLevel(e.getPermanentAddress(), 4); return v != null ? v.getCode() : ""; }),


                // ---------- Native Location ----------
                col("nativeLocation.parent.parent.parent.nameEn", "Native Province | ខេត្ត (ទីកន្លែងកំណើត)", e -> e.getNativeLocation(), e -> { Gazetteer p = toLevel(e.getNativeLocation(), 1); return p != null ? p.getNameEn() : ""; }),
                col("nativeLocation.parent.parent.nameEn", "Native District | ស្រុក (ទីកន្លែងកំណើត)", e -> e.getNativeLocation(), e -> { Gazetteer d = toLevel(e.getNativeLocation(), 2); return d != null ? d.getNameEn() : ""; }),
                col("nativeLocation.parent.nameEn", "Native Commune | ឃុំ (ទីកន្លែងកំណើត)", e -> e.getNativeLocation(), e -> { Gazetteer c = toLevel(e.getNativeLocation(), 3); return c != null ? c.getNameEn() : ""; }),
                col("nativeLocation.nameEn", "Native Village | ភូមិ (ទីកន្លែងកំណើត)", e -> e.getNativeLocation(), e -> { Gazetteer v = toLevel(e.getNativeLocation(), 4); return v != null ? v.getNameEn() : ""; }),
                col("nativeLocation.code", "Native Village Code | លេខកូដភូមិ (ទីកន្លែងកំណើត)",e -> e.getNativeLocation(), e -> { Gazetteer v = toLevel(e.getNativeLocation(), 4); return v != null ? v.getCode() : ""; }),

                
                
                
                col("tenureInYears", "Tenure (in Years)", e->e.getTenureInYears(), e -> e.getTenureInYears() != null ? e.getTenureInYears().toString() : ""),
                col("hasDisability","Differently Abled/ Person with Disability", e->e.getHasDisability(), e -> e.getHasDisability()!=null && e.getHasDisability()==true?"Yes": "No"),


                

                
                col("probationEndDate", this.probationEndDate.getLabel(), e->e.getProbationEndDate(), e -> e.getProbationEndDate() != null ?DateTimeUtilFormart.DATE_FORMATTER.format( e.getProbationEndDate()) : ""),
                col("probationStatus", this.probationStatus.getLabel(), e->e.getProbationStatus(), e -> e.getProbationStatus() != null ? e.getProbationStatus().getProbationStatusName() : ""),
                
                
                col("currentContractStartDate", this.currentContractStartDate.getLabel(), e->e.getCurrentContractStartDate(), e -> e.getCurrentContractStartDate() != null ?DateTimeUtilFormart.DATE_FORMATTER.format( e.getCurrentContractStartDate()) : ""),
                col("currentContractEndDate", this.currentContractEndDate.getLabel(), e->e.getCurrentContractEndDate(), e -> e.getCurrentContractEndDate() != null ?DateTimeUtilFormart.DATE_FORMATTER.format( e.getCurrentContractEndDate()) : ""),
                
                
                col("grossSalaryUsd", this.grossSalaryUsd.getLabel(), e->e.getGrossSalaryUsd(), e -> e.getGrossSalaryUsd() != null ? (this.allowSeeSalary? e.getGrossSalaryUsd().toString():"XXXXX") : ""),
                
                col("bank", this.bank.getLabel(), e->e.getBank(), e -> e.getBank() != null ? e.getBank().getBankName() : ""),
                col("bankAccount", this.bankAccount.getLabel(), e->e.getBankAccount(), e -> e.getBankAccount() != null ? e.getBankAccount(): ""),
                col("bankAccountName", this.bankAccountName.getLabel(), e->e.getBankAccountName(), e -> e.getBankAccountName() != null ? e.getBankAccountName() : ""),
                
                col("contracts.donor", "Donor | ម្ចាស់ជំនួយ", e->e.getContracts(), e -> e.getContracts() != null ? e.getContracts().getDonor().getDonorShortName() : ""),
                col("contracts", "Contract | លេខកុងត្រា", e->e.getContracts(), e -> e.getContracts() != null ? e.getContracts().getContractCode() : ""),
                col("code", this.code.getLabel(), e->e.getCode(), e -> e.getCode() != null ? e.getCode().toString() : ""),
                
                
                col("lastEducationType", "Last Educational Qualification | កម្រិតវប្បធម៌ចុងក្រោយ", e->e.getLastEducationType(), e -> e.getLastEducationType() != null ? e.getLastEducationType().getEducationTypeEN() : ""),
                col("lastOrganization", lastOrganization.getLabel(), e->e.getLastOrganization(), e -> e.getLastOrganization() != null ? e.getLastOrganization() : ""),
                col("preHaloWorkExperience", preHaloWorkExperience.getLabel(), e->e.getPreHaloWorkExperience(), e -> e.getPreHaloWorkExperience() != null ? e.getPreHaloWorkExperience() : ""),
                col("preExistingMedicalCondition", this.preExistingMedicalCondition.getLabel(), e->e.getPreExistingMedicalCondition(), e -> e.getPreExistingMedicalCondition() != null ? (e.getPreExistingMedicalCondition()==true ? "Yes":"No"): ""),
                
                col("idCardNumber", this.idCardNumber.getLabel(), e->e.getIdCardNumber(), e -> e.getIdCardNumber() != null ? e.getIdCardNumber() : ""),
                col("idCardExpiryDate", this.idCardExpiryDate.getLabel(), e->e.getIdCardExpiryDate(), e -> e.getIdCardExpiryDate() != null ?DateTimeUtilFormart.DATE_FORMATTER.format( e.getIdCardExpiryDate()) : ""),
                col("nsffCardNumber", this.nsffCardNumber.getLabel(), e->e.getNsffCardNumber(), e -> e.getNsffCardNumber() != null ? e.getNsffCardNumber() : ""),
                col("nsffExpiryDate", this.nsffExpiryDate.getLabel(), e->e.getNsffExpiryDate(), e -> e.getNsffExpiryDate() != null ?DateTimeUtilFormart.DATE_FORMATTER.format( e.getNsffExpiryDate()) : ""),
                col("nssfRetire", this.nssfRetire.getLabel(), e->e.getNssfRetire(), e -> e.getNssfRetire() != null ? e.getNssfRetire() : ""),
                col("drivingLicense", this.drivingLicense.getLabel(), e->e.getDrivingLicense(), e -> e.getDrivingLicense() != null ? e.getDrivingLicense().getDrivingLicenseName() : ""),
                col("drivingLicenseExpire", this.drivingLicenseExpire.getLabel(), e->e.getDrivingLicenseExpire(), e -> e.getDrivingLicenseExpire() != null ?DateTimeUtilFormart.DATE_FORMATTER.format( e.getDrivingLicenseExpire()) : ""),
                
                
                col("referenceVerified", this.referenceVerified.getLabel(), e->e.getReferenceVerified(), e -> e.getReferenceVerified() != null ? (e.getReferenceVerified()==true ? "Yes":"No"): ""),
                col("referenceVerifiedDate", this.referenceVerifiedDate.getLabel(), e->e.getReferenceVerifiedDate(), e -> e.getReferenceVerifiedDate() != null ?DateTimeUtilFormart.DATE_FORMATTER.format( e.getReferenceVerifiedDate()) : ""),
                
                col("policePlearance", this.policePlearance.getLabel(), e->e.getPolicePlearance(), e -> e.getPolicePlearance() != null ? (e.getPolicePlearance()==true ? "Yes":"No"): ""),
                
           
                col("attendanceType", this.attendanceType.getLabel(), e->e.getAttendanceType(), e -> e.getAttendanceType() != null ? e.getAttendanceType().getShiftName() : ""),
                
                
                
                col("userCreated.name", "Created By", e -> e.getUserCreated() != null ? e.getUserCreated().getName() : "",  e -> e.getUserCreated() != null ? e.getUserCreated().getName() : ""),
                col("createdAt", "Created At", e -> e.getCreatedAt(), e -> e.getCreatedAt() != null ? DateTimeUtilFormart.DATE_TIME_FORMATTER.format(e.getCreatedAt()) : ""),
                col("userUpdated.name", "Updated By", e -> e.getUserUpdated() != null ? e.getUserUpdated().getName() : "", e -> e.getUserUpdated() != null ? e.getUserUpdated().getName() : ""),
                col("updatedAt", "Updated At", e -> e.getUpdatedAt() , e -> e.getUpdatedAt() != null ? DateTimeUtilFormart.DATE_TIME_FORMATTER.format(e.getUpdatedAt()) : "")
        );
    }
    private Gazetteer toLevel(Gazetteer g, int targetLevel) {
        Gazetteer cur = g;
        while (cur != null && cur.getLevel() != null && cur.getLevel() > targetLevel) {
            cur = cur.getParent();
        }
        return (cur != null && cur.getLevel() != null && cur.getLevel() == targetLevel) ? cur : null;
    }

    private static String staffLabel(Employee e) {
        if (e == null) return "";
        String ins = (e.getInsuranceNo() == null) ? "" : e.getInsuranceNo().toString();
        String name = formatEnKh(e.getNameEn(), e.getNameKh());
        if (ins.isBlank()) return name;
        if (name.isBlank()) return ins;
        return name  + " - " + ins;
    }

    
    @Override
    protected Specification<Employee> buildCombinedSpecification() {
        return (root, query, cb) -> {
            try {
                List<Predicate> predicates = new ArrayList<>();
                List<String> sqlFilter = new ArrayList<>();

               // query.distinct(true);
                Join<Employee, Nationality> nationalityJoin = root.join("nationality", JoinType.LEFT);
                Join<Employee, Ethnicity> ethnicityJoin = root.join("ethnicity", JoinType.LEFT);
                Join<Employee, Religions> religionsJoin = root.join("religions", JoinType.LEFT);
                Join<Employee, Positions> positionsJoin = root.join("positions", JoinType.LEFT);
                
                Join<Employee, Department> departmentJoin = root.join("department", JoinType.LEFT);
                Join<Employee, Category> categoryJoin = root.join("category", JoinType.LEFT);
                Join<Employee, InsuranceType> insuranceTypeJoin = root.join("insuranceType", JoinType.LEFT);
                Join<Employee, Teams> teamsJoin = root.join("teams", JoinType.LEFT);
                Join<Employee, Branch> branchJoin = root.join("branch", JoinType.LEFT);
                Join<Employee, ProbationStatus> probationStatusJoin = root.join("probationStatus", JoinType.LEFT);
                Join<Employee, ContractType> contractTypeJoin = root.join("contractType", JoinType.LEFT);
                Join<Employee, Bank> bankJoin = root.join("bank", JoinType.LEFT);
                Join<Employee, DrivingLicense> drivingLicenseJoin = root.join("drivingLicense", JoinType.LEFT);
                Join<Employee, PoGrade> poGradeJoin = root.join("poGrade", JoinType.LEFT);
                Join<Employee, Relationship> relationshipWithNomineeJoin = root.join("relationshipWithNominee", JoinType.LEFT);
                Join<Employee, Shift> attendanceTypeJoin = root.join("attendanceType", JoinType.LEFT);
                

                
                Join<Employee, Contracts> contractsJoin = root.join("contracts", JoinType.LEFT);
                Join<Contracts, Donors> donorJoin = contractsJoin.join("donor", JoinType.LEFT);
                
                Join<Employee, Gazetteer> presentVillageJoin = root.join("presentAddress", JoinType.LEFT);
                Join<Gazetteer, Gazetteer> presentCommuneJoin = presentVillageJoin.join("parent", JoinType.LEFT);
                Join<Gazetteer, Gazetteer> presentDistrictJoin = presentCommuneJoin.join("parent", JoinType.LEFT);
                Join<Gazetteer, Gazetteer> presentProvinceJoin = presentDistrictJoin.join("parent", JoinType.LEFT);

                Join<Employee, Gazetteer> permanentVillageJoin = root.join("permanentAddress", JoinType.LEFT);
                Join<Gazetteer, Gazetteer> permanentCommuneJoin = permanentVillageJoin.join("parent", JoinType.LEFT);
                Join<Gazetteer, Gazetteer> permanentDistrictJoin = permanentCommuneJoin.join("parent", JoinType.LEFT);
                Join<Gazetteer, Gazetteer> permanentProvinceJoin = permanentDistrictJoin.join("parent", JoinType.LEFT);

                Join<Employee, Gazetteer> nativeVillageJoin = root.join("nativeLocation", JoinType.LEFT);
                Join<Gazetteer, Gazetteer> nativeCommuneJoin = nativeVillageJoin.join("parent", JoinType.LEFT);
                Join<Gazetteer, Gazetteer> nativeDistrictJoin = nativeCommuneJoin.join("parent", JoinType.LEFT);
                Join<Gazetteer, Gazetteer> nativeProvinceJoin = nativeDistrictJoin.join("parent", JoinType.LEFT);
                

                Join<Employee, User> userCreated = root.join("userCreated", JoinType.LEFT);
                Join<Employee, User> userUpdated = root.join("userUpdated", JoinType.LEFT);
                
                Join<Employee, CareerType> careerTypeJoin = root.join("careerType", JoinType.LEFT);
                Join<Employee, CareerTypeGroup> careerTypeGroupJoin = careerTypeJoin.join("careerTypeGroup", JoinType.LEFT);

                //
                predicates.add(cb.equal(root.get("employeeType"), EmployeeTypeEnum.International));
                

                // Quick search
                String quick = quickSearchField.getValue();
                if (quick != null && !quick.isEmpty()) {
                    String like = "%" + quick.toLowerCase().trim() + "%";
                    predicates.add(cb.or(
                            buildLikePredicate(cb, root.get("id"), like),
                            
                            buildLikePredicate(cb, root.get("insuranceNo"), like),
                            buildLikePredicate(cb, root.get("nameEn"), like),
                            buildLikePredicate(cb, root.get("nameKh"), like),
                            buildLikePredicate(cb, root.get("gender"), like),
                            buildLikePredicate(cb, root.get("dob"), like),
                            buildLikePredicate(cb, root.get("maritalStatus"), like),
                            buildLikePredicate(cb, root.get("phoneNumber"), like),
                            buildLikePredicate(cb, root.get("personalEmail"), like),
                            buildLikePredicate(cb, root.get("officialCellNo"), like),
                            buildLikePredicate(cb, root.get("officialEmail"), like),
                            buildLikePredicate(cb, root.get("bloodGroup"), like),
                            buildLikePredicate(cb, root.get("hasDisability"), like),
                            
                            buildLikePredicate(cb, nationalityJoin.get("nationalityEn"), like),
                            buildLikePredicate(cb, ethnicityJoin.get("ethnicityEn"), like),
                            buildLikePredicate(cb, religionsJoin.get("religionEn"), like),
                            buildLikePredicate(cb, religionsJoin.get("religionKh"), like),
                            
                            buildLikePredicate(cb, root.get("joinDate"), like),
                            buildLikePredicate(cb, positionsJoin.get("position"), like),
                            
                            buildLikePredicate(cb, departmentJoin.get("name"), like),
                            buildLikePredicate(cb, categoryJoin.get("category"), like),
                            buildLikePredicate(cb, insuranceTypeJoin.get("insuranceTypeName"), like),
                            buildLikePredicate(cb, teamsJoin.get("teamCode"), like),
                            buildLikePredicate(cb, branchJoin.get("branchShortName"), like),
                            buildLikePredicate(cb, probationStatusJoin.get("probationStatusName"), like),
                            buildLikePredicate(cb, contractTypeJoin.get("contractTypeName"), like),
                            buildLikePredicate(cb, bankJoin.get("bankName"), like),
                            buildLikePredicate(cb, drivingLicenseJoin.get("drivingLicenseName"), like),
                            buildLikePredicate(cb, poGradeJoin.get("poGrade"), like),
                            buildLikePredicate(cb, relationshipWithNomineeJoin.get("relationshipEn"), like),
                            buildLikePredicate(cb, attendanceTypeJoin.get("shiftName"), like),
                            
                            buildLikePredicate(cb, root.get("probationEndDate"), like),
                            buildLikePredicate(cb, root.get("currentContractStartDate"), like),
                            buildLikePredicate(cb, root.get("currentContractEndDate"), like),
                            buildLikePredicate(cb, root.get("bankAccount"), like),
                            buildLikePredicate(cb, root.get("bankAccountName"), like),
                            buildLikePredicate(cb, root.get("code"), like),
                            buildLikePredicate(cb, root.get("lastOrganization"), like),
                            buildLikePredicate(cb, root.get("preHaloWorkExperience"), like),
                            buildLikePredicate(cb, root.get("preExistingMedicalCondition"), like),
                            buildLikePredicate(cb, root.get("idCardNumber"), like),
                            buildLikePredicate(cb, root.get("idCardExpiryDate"), like),
                            buildLikePredicate(cb, root.get("nsffCardNumber"), like),
                            buildLikePredicate(cb, root.get("nsffExpiryDate"), like),
                            buildLikePredicate(cb, root.get("nssfRetire"), like),
                            buildLikePredicate(cb, root.get("drivingLicenseExpire"), like),
                            buildLikePredicate(cb, root.get("referenceVerified"), like),
                            buildLikePredicate(cb, root.get("referenceVerifiedDate"), like),
                            buildLikePredicate(cb, root.get("policePlearance"), like),
                            buildLikePredicate(cb, root.get("nomineeName"), like),
                            buildLikePredicate(cb, root.get("nomineePhone"), like),
                            buildLikePredicate(cb, root.get("nomineeNid"), like),
                            
                            
                            buildLikePredicate(cb, presentProvinceJoin.get("nameEn"), like),
                            buildLikePredicate(cb, presentDistrictJoin.get("nameEn"), like),
                            buildLikePredicate(cb, presentCommuneJoin.get("nameEn"), like),
                            buildLikePredicate(cb, presentVillageJoin.get("nameEn"), like),
                            buildLikePredicate(cb, presentVillageJoin.get("code"), like),
                            
                            buildLikePredicate(cb, permanentProvinceJoin.get("nameEn"), like),
                            buildLikePredicate(cb, permanentDistrictJoin.get("nameEn"), like),
                            buildLikePredicate(cb, permanentCommuneJoin.get("nameEn"), like),
                            buildLikePredicate(cb, permanentVillageJoin.get("nameEn"), like),
                            buildLikePredicate(cb, permanentVillageJoin.get("code"), like),
                            
                            buildLikePredicate(cb, nativeProvinceJoin.get("nameEn"), like),
                            buildLikePredicate(cb, nativeDistrictJoin.get("nameEn"), like),
                            buildLikePredicate(cb, nativeCommuneJoin.get("nameEn"), like),
                            buildLikePredicate(cb, nativeVillageJoin.get("nameEn"), like),
                            buildLikePredicate(cb, nativeVillageJoin.get("code"), like),

                            buildLikePredicate(cb, userCreated.get("name"), like),
                            buildLikePredicate(cb, userUpdated.get("name"), like),
                            buildLikePredicate(cb, root.get("createdAt"), like),
                            buildLikePredicate(cb, root.get("updatedAt"), like)
                    ));
                }

                // Advanced filter
                if (advPanel != null) {
                	
                	@SuppressWarnings("unchecked")
                	MultiSelectComboBox<Employee> advanceFilterStaffMs =(MultiSelectComboBox<Employee>) advPanel.getField("staff", MultiSelectComboBox.class);
                	advanceFilterBuildInPredicate(
                	        cb,
                	        root,
                	        advanceFilterStaffMs == null ? null : advanceFilterStaffMs.getValue(),
                	        InternationalStaffView::staffLabel,
                	        "Staff | បុគ្គលិក",
                	        predicates,
                	        sqlFilter
                	);

                	
                	
                	@SuppressWarnings("unchecked")
					ComboBox<GenderEnum> advanceFilterGender = advPanel.getField("gender", ComboBox.class);
                	if (advanceFilterGender != null && advanceFilterGender.getValue() != null) {
                	    predicates.add(cb.equal(root.get("gender"), advanceFilterGender.getValue()));
                	    sqlFilter.add(this.gender.getLabel() + " = " + advanceFilterGender.getValue().getLabel());
                	}

                           
          

                    DateRangePicker advanceFilterDob = advPanel.getField("dob", DateRangePicker.class);
                    advanceFilterBuildBetweenPredicate(cb,cb.function("DATE", LocalDate.class, root.get("dob")),advanceFilterDob == null ? null : advanceFilterDob.getFrom(),advanceFilterDob == null ? null : advanceFilterDob.getTo(), this.dob.getLabel(), predicates, sqlFilter);


                    
                    @SuppressWarnings("unchecked")
                    MultiSelectComboBox<String> advanceFilterMaritalMs =(MultiSelectComboBox<String>) advPanel.getField("maritalStatus", MultiSelectComboBox.class);
                    advanceFilterBuildInPredicate(
                            cb,
                            root.get("maritalStatus"),
                            advanceFilterMaritalMs == null ? null : advanceFilterMaritalMs.getValue(),
                            v -> v.toString(),
                            this.maritalStatus.getLabel(),
                            predicates,
                            sqlFilter
                    );

                    
                    
                 // Phone Number (contains)
                    TextField advanceFilterPhoneNumber = advPanel.getField("phoneNumber", TextField.class);
                    advanceFilterBuildLikePredicate(
                            cb,
                            root.get("phoneNumber"),
                            advanceFilterPhoneNumber == null ? null : advanceFilterPhoneNumber.getValue(),
                            this.phoneNumber.getLabel(),
                            predicates,
                            sqlFilter
                    );

                    // Personal Email (contains)
                    TextField advanceFilterPersonalEmail = advPanel.getField("personalEmail", TextField.class);
                    advanceFilterBuildLikePredicate(
                            cb,
                            root.get("personalEmail"),
                            advanceFilterPersonalEmail == null ? null : advanceFilterPersonalEmail.getValue(),
                            this.personalEmail.getLabel(),
                            predicates,
                            sqlFilter
                    );
                    
                 // Official Cell No (contains)
                    TextField advanceFilterOfficialCellNo = advPanel.getField("officialCellNo", TextField.class);
                    advanceFilterBuildLikePredicate(
                            cb,
                            root.get("officialCellNo"),
                            advanceFilterOfficialCellNo == null ? null : advanceFilterOfficialCellNo.getValue(),
                            this.officialCellNo.getLabel(),
                            predicates,
                            sqlFilter
                    );

                    // Official Email (contains)
                    TextField advanceFilterOfficialEmail = advPanel.getField("officialEmail", TextField.class);
                    advanceFilterBuildLikePredicate(
                            cb,
                            root.get("officialEmail"),
                            advanceFilterOfficialEmail == null ? null : advanceFilterOfficialEmail.getValue(),
                            this.officialEmail.getLabel(),
                            predicates,
                            sqlFilter
                    );

                    
                 // Blood Group IN (...)
                    @SuppressWarnings("unchecked")
                    MultiSelectComboBox<String> advanceFilterBloodGroupMs =(MultiSelectComboBox<String>) advPanel.getField("bloodGroup", MultiSelectComboBox.class);
                    advanceFilterBuildInPredicate(
                            cb,
                            root.get("bloodGroup"),
                            advanceFilterBloodGroupMs == null ? null : advanceFilterBloodGroupMs.getValue(),
                            s -> s,
                            this.bloodGroup.getLabel(),
                            predicates,
                            sqlFilter
                    );



                    // Nationality IN (...)
                    @SuppressWarnings("unchecked")
                    MultiSelectComboBox<Nationality> advanceFilterNationalityMs = (MultiSelectComboBox<Nationality>) advPanel.getField("nationality", MultiSelectComboBox.class);
                    advanceFilterBuildInPredicate(
                            cb,
                            root.get("nationality"),
                            advanceFilterNationalityMs == null ? null : advanceFilterNationalityMs.getValue(),
                            n -> formatEnKh(n.getNationalityEn(), n.getNationalityKh()),
                            this.nationality.getLabel(),
                            predicates,
                            sqlFilter
                    );



                    // Ethnicity IN (...)
                    @SuppressWarnings("unchecked")
                    MultiSelectComboBox<Ethnicity> advanceFilterEthnicityMs =
                            (MultiSelectComboBox<Ethnicity>) advPanel.getField("ethnicity", MultiSelectComboBox.class);

                    advanceFilterBuildInPredicate(
                            cb,
                            root.get("ethnicity"),
                            advanceFilterEthnicityMs == null ? null : advanceFilterEthnicityMs.getValue(),
                            e -> formatEnKh(e.getEthnicityEn(), e.getEthnicityKh()),
                            this.ethnicity.getLabel(),
                            predicates,
                            sqlFilter
                    );
                    
                    @SuppressWarnings("unchecked")
                    MultiSelectComboBox<Religions> advanceFilterReligions =
                            (MultiSelectComboBox<Religions>) advPanel.getField("religions", MultiSelectComboBox.class);

                    advanceFilterBuildInPredicate(
                            cb,
                            root.get("religions"),
                            advanceFilterReligions == null ? null : advanceFilterReligions.getValue(),
                            e -> formatEnKh(e.getReligionEn(), e.getReligionKh()),
                            this.religions.getLabel(),
                            predicates,
                            sqlFilter
                    );



                    @SuppressWarnings("unchecked")
                    MultiSelectComboBox<Positions> advanceFilterPositionsMs =(MultiSelectComboBox<Positions>) advPanel.getField("positions", MultiSelectComboBox.class);
                    advanceFilterBuildInPredicate(
                            cb,
                            root.get("positions"),
                            advanceFilterPositionsMs == null ? null : advanceFilterPositionsMs.getValue(),
                            p -> formatEnKh(p.getPosition(), p.getPositionKh()),
                            this.positions.getLabel(),
                            predicates,
                            sqlFilter
                    );

                    

                    // Department IN (...)
                    @SuppressWarnings("unchecked")
                    MultiSelectComboBox<Department> advanceFilterDepartmentMs =(MultiSelectComboBox<Department>) advPanel.getField("department", MultiSelectComboBox.class);
                    advanceFilterBuildInPredicate(
                            cb,
                            root.get("department"),
                            advanceFilterDepartmentMs == null ? null : advanceFilterDepartmentMs.getValue(),
                            d -> formatEnKh(d.getName(), d.getNameKH()),
                            this.department.getLabel(),
                            predicates,
                            sqlFilter
                    );


                    // Category IN (...)
                    @SuppressWarnings("unchecked")
                    MultiSelectComboBox<Category> advanceFilterCategoryMs =(MultiSelectComboBox<Category>) advPanel.getField("category", MultiSelectComboBox.class);
                    advanceFilterBuildInPredicate(
                            cb,
                            root.get("category"),
                            advanceFilterCategoryMs == null ? null : advanceFilterCategoryMs.getValue(),
                            Category::getCategory,
                            this.category.getLabel(),
                            predicates,
                            sqlFilter
                    );


                    // Teams IN (...)
                    @SuppressWarnings("unchecked")
                    MultiSelectComboBox<Teams>  advanceFilterTeamsMs =(MultiSelectComboBox<Teams>) advPanel.getField("teams", MultiSelectComboBox.class);

                    advanceFilterBuildInPredicate(
                            cb,
                            root.get("teams"),
                            advanceFilterTeamsMs == null ? null : advanceFilterTeamsMs.getValue(),
                            Teams::getTeamCode,
                            this.teams.getLabel(),
                            predicates,
                            sqlFilter
                    );



                    // Branch IN (...)
                    @SuppressWarnings("unchecked")
                    MultiSelectComboBox<Branch> advanceFilterBranchMs = (MultiSelectComboBox<Branch>) advPanel.getField("branch", MultiSelectComboBox.class);

                    advanceFilterBuildInPredicate(
                            cb,
                            root.get("branch"),
                            advanceFilterBranchMs == null ? null : advanceFilterBranchMs.getValue(),
                            Branch::getBranchShortName,
                            this.branch.getLabel(),
                            predicates,
                            sqlFilter
                    );


                    @SuppressWarnings("unchecked")
                    MultiSelectComboBox<Donors> advanceFilterDonorMs =
                            (MultiSelectComboBox<Donors>) advPanel.getField("donor", MultiSelectComboBox.class);

                    advanceFilterBuildInPredicate(
                            cb,
                            donorJoin,  // Join<Contracts, Donors>
                            advanceFilterDonorMs == null ? null : advanceFilterDonorMs.getValue(),
                            Donors::getDonorShortName,
                            "Donor | ម្ចាស់ជំនួយ",
                            predicates,
                            sqlFilter
                    );

                    
                    @SuppressWarnings("unchecked")
                    MultiSelectComboBox<Contracts> advanceFilterContractsMs = (MultiSelectComboBox<Contracts>) advPanel.getField("contracts", MultiSelectComboBox.class);

                    advanceFilterBuildInPredicate(
                            cb,
                            contractsJoin, 
                            advanceFilterContractsMs == null ? null : advanceFilterContractsMs.getValue(),
                            Contracts::getContractCode,
                            "Contract | លេខកុងត្រា",
                            predicates,
                            sqlFilter
                    );
                    
                    
                    
                    

                    @SuppressWarnings("unchecked")
                    MultiSelectComboBox<DrivingLicense> advanceFilterDrivingLicenseMs =(MultiSelectComboBox<DrivingLicense>) advPanel.getField("drivingLicense", MultiSelectComboBox.class);

                    advanceFilterBuildInPredicate(
                            cb,
                            root.get("drivingLicense"), // <-- change to your exact field name in Employee
                            advanceFilterDrivingLicenseMs == null ? null : advanceFilterDrivingLicenseMs.getValue(),
                            		DrivingLicense::getDrivingLicenseName, // <-- adjust getters
                            this.drivingLicense.getLabel(),
                            predicates,
                            sqlFilter
                    );

                    @SuppressWarnings("unchecked")
                    MultiSelectComboBox<PoGrade> advanceFilterPoGradeMs = (MultiSelectComboBox<PoGrade>) advPanel.getField("poGrade", MultiSelectComboBox.class);

                    advanceFilterBuildInPredicate(
                            cb,
                            root.get("poGrade"), // <-- exact field in Employee
                            advanceFilterPoGradeMs == null ? null : advanceFilterPoGradeMs.getValue(),
                            		PoGrade::getPoGrade, // adjust getters
                            this.poGrade.getLabel(),
                            predicates,
                            sqlFilter
                    );
                    

                 // hasDisability (ComboBox Yes/No)
                    @SuppressWarnings("unchecked")
                    ComboBox<Boolean> advHasDisability = advPanel.getField("hasDisability", ComboBox.class);
                    if (advHasDisability != null && advHasDisability.getValue() != null) {
                        predicates.add(cb.equal(root.get("hasDisability"), advHasDisability.getValue()));
                        sqlFilter.add("Differently Abled/ Person with Disability = " + (advHasDisability.getValue() ? "Yes" : "No"));
                    }

                    // insuranceType
                    @SuppressWarnings("unchecked")
                    ComboBox<InsuranceType> advInsuranceType = advPanel.getField("insuranceType", ComboBox.class);
                    if (advInsuranceType != null && advInsuranceType.getValue() != null) {
                        predicates.add(cb.equal(root.get("insuranceType"), advInsuranceType.getValue()));
                        sqlFilter.add(this.insuranceType.getLabel() + " = " + advInsuranceType.getValue().getInsuranceTypeName());
                    }

                    // probationStatus
                    @SuppressWarnings("unchecked")
                    ComboBox<ProbationStatus> advProbationStatus = advPanel.getField("probationStatus", ComboBox.class);
                    if (advProbationStatus != null && advProbationStatus.getValue() != null) {
                        predicates.add(cb.equal(root.get("probationStatus"), advProbationStatus.getValue()));
                        sqlFilter.add(this.probationStatus.getLabel() + " = " + advProbationStatus.getValue().getProbationStatusName());
                    }

                    // contractType
                    @SuppressWarnings("unchecked")
                    ComboBox<ContractType> advContractType = advPanel.getField("contractType", ComboBox.class);
                    if (advContractType != null && advContractType.getValue() != null) {
                        predicates.add(cb.equal(root.get("contractType"), advContractType.getValue()));
                        sqlFilter.add(this.contractType.getLabel() + " = " + advContractType.getValue().getContractTypeName());
                    }

                    // referenceVerified (ComboBox Yes/No)
                    @SuppressWarnings("unchecked")
                    ComboBox<Boolean> advReferenceVerified = advPanel.getField("referenceVerified", ComboBox.class);
                    if (advReferenceVerified != null && advReferenceVerified.getValue() != null) {
                        predicates.add(cb.equal(root.get("referenceVerified"), advReferenceVerified.getValue()));
                        sqlFilter.add(this.referenceVerified.getLabel() + " = " + (advReferenceVerified.getValue() ? "Yes" : "No"));
                    }

                    // policePlearance (ComboBox Yes/No)
                    @SuppressWarnings("unchecked")
                    ComboBox<Boolean> advPolicePlearance = advPanel.getField("policePlearance", ComboBox.class);
                    if (advPolicePlearance != null && advPolicePlearance.getValue() != null) {
                        predicates.add(cb.equal(root.get("policePlearance"), advPolicePlearance.getValue()));
                        sqlFilter.add(this.policePlearance.getLabel() + " = " + (advPolicePlearance.getValue() ? "Yes" : "No"));
                    }

                    // attendanceType (Shift)
                    @SuppressWarnings("unchecked")
                    ComboBox<Shift> advAttendanceType = advPanel.getField("attendanceType", ComboBox.class);
                    if (advAttendanceType != null && advAttendanceType.getValue() != null) {
                        predicates.add(cb.equal(root.get("attendanceType"), advAttendanceType.getValue()));
                        sqlFilter.add(this.attendanceType.getLabel() + " = " + advAttendanceType.getValue().getShiftName());
                    }

                    DateRangePicker advanceFilterJoinDate = advPanel.getField("joinDate", DateRangePicker.class);
                    advanceFilterBuildBetweenPredicate(
                            cb,
                            cb.function("DATE", LocalDate.class, root.get("joinDate")),
                            advanceFilterJoinDate == null ? null : advanceFilterJoinDate.getFrom(),
                            		advanceFilterJoinDate == null ? null : advanceFilterJoinDate.getTo(),
                            this.joinDate.getLabel(),
                            predicates,
                            sqlFilter
                    );

                    DateRangePicker advanceFilterProbationEndDate = advPanel.getField("probationEndDate", DateRangePicker.class);
                    advanceFilterBuildBetweenPredicate(
                            cb,
                            cb.function("DATE", LocalDate.class, root.get("probationEndDate")),
                            advanceFilterProbationEndDate == null ? null : advanceFilterProbationEndDate.getFrom(),
                            advanceFilterProbationEndDate == null ? null : advanceFilterProbationEndDate.getTo(),
                            this.probationEndDate.getLabel(),
                            predicates,
                            sqlFilter
                    );

                    DateRangePicker advanceFilterCurrentContractStartDateR = advPanel.getField("currentContractStartDate", DateRangePicker.class);
                    advanceFilterBuildBetweenPredicate(
                            cb,
                            cb.function("DATE", LocalDate.class, root.get("currentContractStartDate")),
                            advanceFilterCurrentContractStartDateR == null ? null : advanceFilterCurrentContractStartDateR.getFrom(),
                            advanceFilterCurrentContractStartDateR == null ? null : advanceFilterCurrentContractStartDateR.getTo(),
                            this.currentContractStartDate.getLabel(),
                            predicates,
                            sqlFilter
                    );

                    DateRangePicker advanceFilterCurrentContractEndDateR = advPanel.getField("currentContractEndDate", DateRangePicker.class);
                    advanceFilterBuildBetweenPredicate(
                            cb,
                            cb.function("DATE", LocalDate.class, root.get("currentContractEndDate")),
                            advanceFilterCurrentContractEndDateR == null ? null : advanceFilterCurrentContractEndDateR.getFrom(),
                            advanceFilterCurrentContractEndDateR == null ? null : advanceFilterCurrentContractEndDateR.getTo(),
                            this.currentContractEndDate.getLabel(),
                            predicates,
                            sqlFilter
                    );

                    DateRangePicker advanceFilterIdCardExpiryDateR = advPanel.getField("idCardExpiryDate", DateRangePicker.class);
                    advanceFilterBuildBetweenPredicate(
                            cb,
                            cb.function("DATE", LocalDate.class, root.get("idCardExpiryDate")),
                            advanceFilterIdCardExpiryDateR == null ? null : advanceFilterIdCardExpiryDateR.getFrom(),
                            advanceFilterIdCardExpiryDateR == null ? null : advanceFilterIdCardExpiryDateR.getTo(),
                            this.idCardExpiryDate.getLabel(),
                            predicates,
                            sqlFilter
                    );

                    DateRangePicker advanceFilterNsffExpiryDateR = advPanel.getField("nsffExpiryDate", DateRangePicker.class);
                    advanceFilterBuildBetweenPredicate(
                            cb,
                            cb.function("DATE", LocalDate.class, root.get("nsffExpiryDate")),
                            advanceFilterNsffExpiryDateR == null ? null : advanceFilterNsffExpiryDateR.getFrom(),
                            advanceFilterNsffExpiryDateR == null ? null : advanceFilterNsffExpiryDateR.getTo(),
                            this.nsffExpiryDate.getLabel(),
                            predicates,
                            sqlFilter
                    );

                    DateRangePicker advanceFilterDrivingLicenseExpireR = advPanel.getField("drivingLicenseExpire", DateRangePicker.class);
                    advanceFilterBuildBetweenPredicate(
                            cb,
                            cb.function("DATE", LocalDate.class, root.get("drivingLicenseExpire")),
                            advanceFilterDrivingLicenseExpireR == null ? null : advanceFilterDrivingLicenseExpireR.getFrom(),
                            advanceFilterDrivingLicenseExpireR == null ? null : advanceFilterDrivingLicenseExpireR.getTo(),
                            this.drivingLicenseExpire.getLabel(),
                            predicates,
                            sqlFilter
                    );

                    DateRangePicker advanceFilterReferenceVerifiedDateR = advPanel.getField("referenceVerifiedDate", DateRangePicker.class);
                    advanceFilterBuildBetweenPredicate(
                            cb,
                            cb.function("DATE", LocalDate.class, root.get("referenceVerifiedDate")),
                            advanceFilterReferenceVerifiedDateR == null ? null : advanceFilterReferenceVerifiedDateR.getFrom(),
                            advanceFilterReferenceVerifiedDateR == null ? null : advanceFilterReferenceVerifiedDateR.getTo(),
                            this.referenceVerifiedDate.getLabel(),
                            predicates,
                            sqlFilter
                    );



                    TextField advanceFilterLastOrg = advPanel.getField("lastOrganization", TextField.class);
                    advanceFilterBuildLikePredicate(cb, root.get("lastOrganization"),advanceFilterLastOrg == null ? null : advanceFilterLastOrg.getValue(), this.lastOrganization.getLabel(), predicates, sqlFilter);

                    TextField advanceFilterPreHalo = advPanel.getField("preHaloWorkExperience", TextField.class);
                    advanceFilterBuildLikePredicate(cb, root.get("preHaloWorkExperience"),advanceFilterPreHalo == null ? null : advanceFilterPreHalo.getValue(), this.preHaloWorkExperience.getLabel(), predicates, sqlFilter);



                    TextField advanceFilterNsffCard = advPanel.getField("nsffCardNumber", TextField.class);
                    advanceFilterBuildLikePredicate(cb, root.get("nsffCardNumber"),advanceFilterNsffCard == null ? null : advanceFilterNsffCard.getValue(), this.nsffCardNumber.getLabel(), predicates, sqlFilter);

                    TextField aadvanceFilterIdCard = advPanel.getField("idCardNumber", TextField.class);
                    advanceFilterBuildLikePredicate(cb, root.get("idCardNumber"),aadvanceFilterIdCard == null ? null : aadvanceFilterIdCard.getValue(),this.idCardNumber.getLabel(), predicates, sqlFilter);

                    TextField advanceFilterBankAccName = advPanel.getField("bankAccountName", TextField.class);
                    advanceFilterBuildLikePredicate(cb, root.get("bankAccountName"),advanceFilterBankAccName == null ? null : advanceFilterBankAccName.getValue(), this.bankAccountName.getLabel(), predicates, sqlFilter);

                    TextField advanceFilterNote = advPanel.getField("note", TextField.class);
                    advanceFilterBuildLikePredicate(cb, root.get("note"),advanceFilterNote == null ? null : advanceFilterNote.getValue(), this.note.getLabel(), predicates, sqlFilter);

                    GazetteerMultiSearchField advanceFilterPresentAddress = advPanel.getField("presentAddress", GazetteerMultiSearchField.class);
                    advanceFilterBuildGazetteerGroupedPredicate(
                            cb, root, "presentAddress",
                            advanceFilterPresentAddress,
                            "Present Address | អាសយដ្ឋានបច្ចុប្បន្ន",
                            predicates, sqlFilter
                    );

                    GazetteerMultiSearchField advanceFilterPermanentAddress = advPanel.getField("permanentAddress", GazetteerMultiSearchField.class);

                    advanceFilterBuildGazetteerGroupedPredicate(
                            cb, root, "permanentAddress",
                            advanceFilterPermanentAddress,
                            "Permanent Address | អាសយដ្ឋានអចិន្ត្រៃយ៍",
                            predicates, sqlFilter
                    );

                    GazetteerMultiSearchField advanceFilterNativeLocation = advPanel.getField("nativeLocation", GazetteerMultiSearchField.class);

                    advanceFilterBuildGazetteerGroupedPredicate(
                            cb, root, "nativeLocation",
                            advanceFilterNativeLocation,
                            "Native Location | ទីកន្លែងកំណើត",
                            predicates, sqlFilter
                    );

                    IntegerField advanceFilterCode = advPanel.getField("code", IntegerField.class);
                    if (advanceFilterCode != null && advanceFilterCode.getValue() != null) {
                        Long codeVal = advanceFilterCode.getValue().longValue();
                        predicates.add(cb.equal(root.get("code"), codeVal));
                        sqlFilter.add("Code = " + codeVal);
                    }

                    
                    // with defaul filter
                    @SuppressWarnings("unchecked")
                    MultiSelectComboBox<CareerTypeGroup> advanceFilterCareerTypeGroup = (MultiSelectComboBox<CareerTypeGroup>) advPanel.getField("careerTypeGroup", MultiSelectComboBox.class);

                    Collection<CareerTypeGroup> selectedValues = advanceFilterCareerTypeGroup != null ?  advanceFilterCareerTypeGroup.getValue() : null;

                    // If nothing selected, default to "Active"
                    if (selectedValues == null || selectedValues.isEmpty()) {
                        Optional<CareerTypeGroup> activeGroup = careerTypeGroupRepository.findByCareerTypeGroupName("Active");
                        if (activeGroup.isPresent()) {
                            selectedValues = Set.of(activeGroup.get());
                        }
                    }

                    // Now call the helper method with the collection
                    advanceFilterBuildInPredicate(
                        cb,
                        careerTypeGroupJoin,
                        selectedValues,  // This is now always a Collection (or null if nothing found)
                        CareerTypeGroup::getCareerTypeGroupName,
                        "Employment Status | ស្ថានភាពបុគ្គលិក",
                        predicates,
                        sqlFilter
                    );
 

                    
                    
                    @SuppressWarnings("unchecked")
                    MultiSelectComboBox<User> createdByMs =(MultiSelectComboBox<User>) advPanel.getField("userCreated", MultiSelectComboBox.class);
                    advanceFilterBuildInPredicate(cb, userCreated,createdByMs == null ? null : createdByMs.getValue(), User::getName, "CreatedBy", predicates, sqlFilter);

                    @SuppressWarnings("unchecked")
                    MultiSelectComboBox<User> updatedByMs = (MultiSelectComboBox<User>) advPanel.getField("userUpdated", MultiSelectComboBox.class);
                    advanceFilterBuildInPredicate(cb, userUpdated, updatedByMs == null ? null : updatedByMs.getValue(), User::getName, "Updated By", predicates, sqlFilter);

                    DateRangePicker createdRange = advPanel.getField("createdAt", DateRangePicker.class);
                    advanceFilterBuildBetweenPredicate(cb,cb.function("DATE", LocalDate.class, root.get("createdAt")),createdRange == null ? null : createdRange.getFrom(),createdRange == null ? null : createdRange.getTo(), "CreatedAt", predicates, sqlFilter);

                    DateRangePicker updatedRange = advPanel.getField("updatedAt", DateRangePicker.class);
                    advanceFilterBuildBetweenPredicate(cb,cb.function("DATE", LocalDate.class, root.get("updatedAt")),updatedRange == null ? null : updatedRange.getFrom(), updatedRange == null ? null : updatedRange.getTo(),"UpdatedAt", predicates, sqlFilter);
                }else {
                	
                    Optional<CareerTypeGroup> activeGroup = careerTypeGroupRepository.findByCareerTypeGroupName("Active");
                    if (activeGroup.isPresent()) {
                        Collection<CareerTypeGroup> defaultValues = Set.of(activeGroup.get());
                        predicates.add(careerTypeGroupJoin.get("careerTypeGroupName").in(
                            defaultValues.stream()
                                .map(CareerTypeGroup::getCareerTypeGroupName)
                                .collect(Collectors.toList())
                        ));
                        sqlFilter.add("Employment Status = Active");
                    }
                    
                }
                

                

                showSqlFilterTokens(sqlFilter);
                return cb.and(predicates.toArray(new Predicate[0]));
            } catch (Exception ex) {
                showErrorMessage("Error in filter: " + ex.getMessage());
                ex.printStackTrace();
                return cb.conjunction();
            }
        };
    }

    @Override
    protected Component buildAdvancedSearchLayout() {
        advPanel = new AdvancedSearchPanel(List.of(

                
                new AdvancedSearchPanel.FilterDef(
                	    "staff", // ✅ unique key (NOT "insuranceNo" again)
                	    "Staff Name | ឈ្មោះបុគ្គលិក",
                	    () -> buildLazyMultiSelect(
                	        e -> String.format("%s - %s",formatEnKh(e.getNameEn(), e.getNameKh()), e.getInsuranceNo() == null ? "" : e.getInsuranceNo() ),
                	        (filter, pageable) -> service.searchEmployeesByNameEnKhInsurance(filter, pageable),
                	        filter -> service.countEmployeesByNameEnKhInsurance(filter)
                	    ),
                	    c -> ((MultiSelectComboBox<?>) c).clear()
                	),
                new AdvancedSearchPanel.FilterDef(
                        "presentAddress",
                        "Present Address | អាសយដ្ឋានបច្ចុប្បន្ន",
                        () -> {
                            GazetteerMultiSearchField gf = new GazetteerMultiSearchField(gazetteerService);
                            gf.setWidthFull();
                            return gf;
                        },
                        c -> ((GazetteerMultiSearchField) c).clearAll()
                ),

                new AdvancedSearchPanel.FilterDef(
                        "permanentAddress",
                        "Permanent Address | អាសយដ្ឋានអចិន្ត្រៃយ៍",
                        () -> {
                            GazetteerMultiSearchField gf = new GazetteerMultiSearchField(gazetteerService);
                            gf.setWidthFull();
                            return gf;
                        },
                        c -> ((GazetteerMultiSearchField) c).clearAll()
                ),
                new AdvancedSearchPanel.FilterDef(
                        "nativeLocation",
                        "Native Location | ទីកន្លែងកំណើត",
                        () -> {
                            GazetteerMultiSearchField gf = new GazetteerMultiSearchField(gazetteerService);
                            gf.setWidthFull();
                            return gf;
                        },
                        c -> ((GazetteerMultiSearchField) c).clearAll()
                ),


                new AdvancedSearchPanel.FilterDef(
                        "gender",
                        this.gender.getLabel(),
                        () -> {
                            ComboBox<GenderEnum> cbo = new ComboBox<>();
                            cbo.setItems(GenderEnum.values());
                            cbo.setItemLabelGenerator(GenderEnum::getLabel);
                            cbo.setClearButtonVisible(true);                           
                            cbo.setWidthFull();
                            return cbo;
                        },
                        c -> ((ComboBox<?>) c).clear()
                ),

                new AdvancedSearchPanel.FilterDef(
                        "dob",
                        this.dob.getLabel(),
                        () -> {
                            DateRangePicker dr = new DateRangePicker();
                            dr.setWidthFull();
                            return dr;
                        },
                        c -> ((DateRangePicker) c).clear()
                ),                
                
                new AdvancedSearchPanel.FilterDef(
                        "maritalStatus",
                        this.maritalStatus.getLabel(),
                        () -> {
                        	return buildMultiSelect(s -> s, maritalStatusRepository.findAll().stream()
                        	                .map(MaritalStatus::getMaritalStatusName)
                        	                .filter(Objects::nonNull)
                        	                .toList()
                        	);

                        },
                        c -> ((MultiSelectComboBox<?>) c).clear()
                ),
                
                new AdvancedSearchPanel.FilterDef(
                        "phoneNumber",
                        this.phoneNumber.getLabel(),
                        () -> {
                            TextField tf = new TextField();
                            tf.setPlaceholder("Contains...");
                            tf.setWidthFull();
                            return tf;
                        },
                        c -> ((TextField) c).clear()
                ),
                
                new AdvancedSearchPanel.FilterDef(
                        "personalEmail",
                        this.personalEmail.getLabel(),
                        () -> {
                            TextField tf = new TextField();
                            tf.setPlaceholder("Contains...");
                            tf.setWidthFull();
                            return tf;
                        },
                        c -> ((TextField) c).clear()
                ),
                
                
                new AdvancedSearchPanel.FilterDef(
                        "officialCellNo",
                        this.officialCellNo.getLabel(),
                        () -> {
                            TextField tf = new TextField();
                            tf.setPlaceholder("Contains...");
                            tf.setWidthFull();
                            return tf;
                        },
                        c -> ((TextField) c).clear()
                ),
                
                new AdvancedSearchPanel.FilterDef(
                        "officialEmail",
                        this.officialEmail.getLabel(),
                        () -> {
                            TextField tf = new TextField();
                            tf.setPlaceholder("Contains...");
                            tf.setWidthFull();
                            return tf;
                        },
                        c -> ((TextField) c).clear()
                ),
                
                
             // Blood Group (Multi)
                new AdvancedSearchPanel.FilterDef(
                        "bloodGroup",
                        this.bloodGroup.getLabel(),
                        () -> {
                        	return buildMultiSelect(
                        	        s -> s,
                        	        bloodGroupRepository.findAll().stream()
                        	                .map(BloodGroup::getBloodGroup)
                        	                .filter(Objects::nonNull)
                        	                .toList()
                        	);

                        },
                        c -> ((MultiSelectComboBox<?>) c).clear()
                ),

                // Nationality (Multi)
                new AdvancedSearchPanel.FilterDef(
                        "nationality",
                        this.nationality.getLabel(),
                        () -> {
                        	return buildMultiSelect(
                        	        n -> formatEnKh(n.getNationalityEn(), n.getNationalityKh()),
                        	        nationalityRepository.findAll(Sort.by("nationalityEn"))
                        	);

                        },
                        c -> ((MultiSelectComboBox<?>) c).clear()
                ),

                // Ethnicity (Multi)
                new AdvancedSearchPanel.FilterDef(
                        "ethnicity",
                        this.ethnicity.getLabel(),
                        () -> {
                        	return buildMultiSelect(
                        	        e -> formatEnKh(e.getEthnicityEn(), e.getEthnicityKh()),
                        	        ethnicityRepository.findAll(Sort.by("ethnicityEn"))
                        	);

                        },
                        c -> ((MultiSelectComboBox<?>) c).clear()
                ),
                
                new AdvancedSearchPanel.FilterDef(
                	    "religions",
                	    this.religions.getLabel(),
                	    () -> buildMultiSelect(
                	        r -> formatEnKh(r.getReligionEn(), r.getReligionKh()),
                	        religionsRepository.findAll(Sort.by("religionEn"))
                	    ),
                	    c -> ((MultiSelectComboBox<?>) c).clear()
                	),
                
             // Positions (Multi)
                new AdvancedSearchPanel.FilterDef(
                        "positions",
                        this.positions.getLabel(),
                        () -> {
                        	return buildMultiSelect(
                        	        p -> formatEnKh(p.getPosition(), p.getPositionKh()),
                        	        positionRepository.findAll(Sort.by("position"))
                        	);

                        },
                        c -> ((MultiSelectComboBox<?>) c).clear()
                ),

                // Department (Multi)
                new AdvancedSearchPanel.FilterDef(
                        "department",
                        this.department.getLabel(),
                        () -> {
                        	return buildMultiSelect(
                        	        d -> formatEnKh(d.getName(), d.getNameKH()),
                        	        departmentRepository.findAll(Sort.by("name"))
                        	);

                        },
                        c -> ((MultiSelectComboBox<?>) c).clear()
                ),

                // Category (Multi)
                new AdvancedSearchPanel.FilterDef(
                        "category",
                        this.category.getLabel(),
                        () -> {
                        	return buildMultiSelect(
                        	        Category::getCategory,
                        	        categoryRepository.findAll(Sort.by("category"))
                        	);
                        },
                        c -> ((MultiSelectComboBox<?>) c).clear()
                ),

                // Teams (Multi)
                new AdvancedSearchPanel.FilterDef(
                        "teams",
                        this.teams.getLabel(),
                        () -> {
                        	return buildMultiSelect(
                        	        Teams::getTeamCode,
                        	        teamsRepository.findAll(Sort.by("teamCode"))
                        	);

                        },
                        c -> ((MultiSelectComboBox<?>) c).clear()
                ),

                // Branch (Multi)
                new AdvancedSearchPanel.FilterDef(
                        "branch",
                        this.branch.getLabel(),
                        () -> {
                        	return buildMultiSelect(
                        	        Branch::getBranchShortName,
                        	        branchRepository.findAll(Sort.by("branchShortName"))
                        	);

                        },
                        c -> ((MultiSelectComboBox<?>) c).clear()
                ),

                // Donor (Multi)  -> uses e.getContracts().getDonor()
                new AdvancedSearchPanel.FilterDef(
                        "donor",
                        "Donor | ម្ចាស់ជំនួយ",
                        () -> {
                        	return buildMultiSelect(
                        	        Donors::getDonorShortName,
                        	        donorsRepository.findAll(Sort.by("donorShortName"))
                        	);

                        },
                        c -> ((MultiSelectComboBox<?>) c).clear()
                ),

                // Contract (Multi)
                new AdvancedSearchPanel.FilterDef(
                        "contracts",
                        "Contract | លេខកុងត្រា",
                        () -> {
                        	return buildMultiSelect(
                        	        Contracts::getContractCode,
                        	        contractsRepository.findAll(Sort.by("contractCode"))
                        	);

                        },
                        c -> ((MultiSelectComboBox<?>) c).clear()
                ),
                new AdvancedSearchPanel.FilterDef(
                        "drivingLicense",
                        this.drivingLicense.getLabel(),
                        () -> buildMultiSelect(
                                d -> d.getDrivingLicenseName(),
                                this.drivingLicenseRepository.findAll() 
                        ),
                        c -> ((MultiSelectComboBox<?>) c).clear()
                ),

                new AdvancedSearchPanel.FilterDef(
                        "poGrade",
                        this.poGrade.getLabel(),
                        () -> buildMultiSelect(
                                PoGrade::getPoGrade,
                                poGradeRepository.findAll()
                        ),
                        c -> ((MultiSelectComboBox<?>) c).clear()
                ),

                

                new AdvancedSearchPanel.FilterDef(
                        "hasDisability",
                        "Differently Abled/ Person with Disability",
                        () -> buildBooleanAsYesNoComboBox("Select..."),
                        c -> ((ComboBox<?>) c).clear()
                ),

                new AdvancedSearchPanel.FilterDef(
                        "insuranceType",
                        this.insuranceType.getLabel(),
                        () -> {
                            ComboBox<InsuranceType> cb = new ComboBox<>();
                            cb.setItems(insuranceTypeRepository.findAll(Sort.by("insuranceTypeName")));
                            cb.setItemLabelGenerator(InsuranceType::getInsuranceTypeName);
                            cb.setClearButtonVisible(true);
                            cb.setWidthFull();
                            return cb;
                        },
                        c -> ((ComboBox<?>) c).clear()
                ),

                new AdvancedSearchPanel.FilterDef(
                        "probationStatus",
                        this.probationStatus.getLabel(),
                        () -> {
                            ComboBox<ProbationStatus> cb = new ComboBox<>();
                            cb.setItems(probationStatusRepository.findAll(Sort.by("probationStatusName")));
                            cb.setItemLabelGenerator(ProbationStatus::getProbationStatusName);
                            cb.setClearButtonVisible(true);
                            cb.setWidthFull();
                            return cb;
                        },
                        c -> ((ComboBox<?>) c).clear()
                ),

                new AdvancedSearchPanel.FilterDef(
                        "contractType",
                        this.contractType.getLabel(),
                        () -> {
                            ComboBox<ContractType> cb = new ComboBox<>();
                            cb.setItems(contractTypeRepository.findAll(Sort.by("contractTypeName")));
                            cb.setItemLabelGenerator(ContractType::getContractTypeName);
                            cb.setClearButtonVisible(true);
                            cb.setWidthFull();
                            return cb;
                        },
                        c -> ((ComboBox<?>) c).clear()
                ),

                new AdvancedSearchPanel.FilterDef(
                        "referenceVerified",
                        this.referenceVerified.getLabel(),
                        () -> buildBooleanAsYesNoComboBox("Select..."),
                        c -> ((ComboBox<?>) c).clear()
                ),

                new AdvancedSearchPanel.FilterDef(
                        "policePlearance",
                        this.policePlearance.getLabel(),
                        () -> buildBooleanAsYesNoComboBox("Select..."),
                        c -> ((ComboBox<?>) c).clear()
                ),

                new AdvancedSearchPanel.FilterDef(
                        "attendanceType",
                        this.attendanceType.getLabel(),
                        () -> {
                            ComboBox<Shift> cb = new ComboBox<>();
                            cb.setItems(shiftRepository.findAll(Sort.by("shiftName")));
                            cb.setItemLabelGenerator(Shift::getShiftName);
                            cb.setClearButtonVisible(true);
                            cb.setWidthFull();
                            return cb;
                        },
                        c -> ((ComboBox<?>) c).clear()
                ),

                new AdvancedSearchPanel.FilterDef(
                        "joinDate",
                        this.joinDate.getLabel(),
                        () -> { DateRangePicker dr = new DateRangePicker(); dr.setWidthFull(); return dr; },
                        c -> ((DateRangePicker) c).clear()
                ),
                new AdvancedSearchPanel.FilterDef(
                        "probationEndDate",
                        this.probationEndDate.getLabel(),
                        () -> { DateRangePicker dr = new DateRangePicker(); dr.setWidthFull(); return dr; },
                        c -> ((DateRangePicker) c).clear()
                ),
                new AdvancedSearchPanel.FilterDef(
                        "currentContractStartDate",
                        this.currentContractStartDate.getLabel(),
                        () -> { DateRangePicker dr = new DateRangePicker(); dr.setWidthFull(); return dr; },
                        c -> ((DateRangePicker) c).clear()
                ),
                new AdvancedSearchPanel.FilterDef(
                        "currentContractEndDate",
                        this.currentContractEndDate.getLabel(),
                        () -> { DateRangePicker dr = new DateRangePicker(); dr.setWidthFull(); return dr; },
                        c -> ((DateRangePicker) c).clear()
                ),
                new AdvancedSearchPanel.FilterDef(
                        "idCardExpiryDate",
                        this.idCardExpiryDate.getLabel(),
                        () -> { DateRangePicker dr = new DateRangePicker(); dr.setWidthFull(); return dr; },
                        c -> ((DateRangePicker) c).clear()
                ),
                new AdvancedSearchPanel.FilterDef(
                        "nsffExpiryDate",
                        this.nsffExpiryDate.getLabel(),
                        () -> { DateRangePicker dr = new DateRangePicker(); dr.setWidthFull(); return dr; },
                        c -> ((DateRangePicker) c).clear()
                ),
                new AdvancedSearchPanel.FilterDef(
                        "drivingLicenseExpire",
                        this.drivingLicenseExpire.getLabel(),
                        () -> { DateRangePicker dr = new DateRangePicker(); dr.setWidthFull(); return dr; },
                        c -> ((DateRangePicker) c).clear()
                ),
                new AdvancedSearchPanel.FilterDef(
                        "referenceVerifiedDate",
                        this.referenceVerifiedDate.getLabel(),
                        () -> { DateRangePicker dr = new DateRangePicker(); dr.setWidthFull(); return dr; },
                        c -> ((DateRangePicker) c).clear()
                ),

                new AdvancedSearchPanel.FilterDef(
                        "lastOrganization",
                        this.lastOrganization.getLabel(),
                        () -> { TextField tf = new TextField(); tf.setPlaceholder("Contains..."); tf.setWidthFull(); return tf; },
                        c -> ((TextField) c).clear()
                ),
                new AdvancedSearchPanel.FilterDef(
                        "preHaloWorkExperience",
                        this.preHaloWorkExperience.getLabel(),
                        () -> { TextField tf = new TextField(); tf.setPlaceholder("Contains..."); tf.setWidthFull(); return tf; },
                        c -> ((TextField) c).clear()
                ),

                new AdvancedSearchPanel.FilterDef(
                        "nsffCardNumber",
                        this.nsffCardNumber.getLabel(),
                        () -> { TextField tf = new TextField(); tf.setPlaceholder("Contains..."); tf.setWidthFull(); return tf; },
                        c -> ((TextField) c).clear()
                ),
                new AdvancedSearchPanel.FilterDef(
                        "idCardNumber",
                        this.idCardNumber.getLabel(),
                        () -> { TextField tf = new TextField(); tf.setPlaceholder("Contains..."); tf.setWidthFull(); return tf; },
                        c -> ((TextField) c).clear()
                ),
                new AdvancedSearchPanel.FilterDef(
                        "bankAccountName",
                        this.bankAccountName.getLabel(),
                        () -> { TextField tf = new TextField(); tf.setPlaceholder("Contains..."); tf.setWidthFull(); return tf; },
                        c -> ((TextField) c).clear()
                ),
                new AdvancedSearchPanel.FilterDef(
                        "note",
                        this.note.getLabel(),
                        () -> { TextField tf = new TextField(); tf.setPlaceholder("Contains..."); tf.setWidthFull(); return tf; },
                        c -> ((TextField) c).clear()
                ),

                new AdvancedSearchPanel.FilterDef(
                        "code",
                        this.code.getLabel(),
                        () -> {
                            IntegerField tf = new IntegerField();
                            tf.setPlaceholder("Equal");
                            tf.setWidthFull();
                            return tf;
                        },
                        c -> ((IntegerField) c).clear()
                ),
                
                new AdvancedSearchPanel.FilterDef(
                	    "careerTypeGroup",
                	    "Employment Status | ស្ថានភាពបុគ្គលិក",
                	    () -> buildMultiSelect(
                	        CareerTypeGroup::getCareerTypeGroupName,
                	        careerTypeGroupRepository.findAll(Sort.by("careerTypeGroupName"))
                	    ),
                	    c -> ((MultiSelectComboBox<?>) c).clear()
                	),
                
                new AdvancedSearchPanel.FilterDef(
                        "userCreated",
                        "Created By",
                        () -> buildLazyMultiSelect(
                                User::getName,
                                (filter, pageable) -> userService.searchUsersByName(filter, pageable),
                                filter -> userService.countUsersByName(filter),
                                MultiSelectComboBoxVariant.LUMO_SMALL
                        ),
                        c -> ((MultiSelectComboBox<?>) c).clear()
                ),
                new AdvancedSearchPanel.FilterDef(
                        "createdAt",
                        "Created At",
                        () -> {
                            DateRangePicker dr = new DateRangePicker();
                            dr.setWidthFull();
                            return dr;
                        },
                        c -> ((DateRangePicker) c).clear()
                ),
                new AdvancedSearchPanel.FilterDef(
                        "userUpdated",
                        "Updated By",
                        () -> buildLazyMultiSelect(
                                User::getName,
                                (filter, pageable) -> userService.searchUsersByName(filter, pageable),
                                filter -> userService.countUsersByName(filter),
                                MultiSelectComboBoxVariant.LUMO_SMALL
                        ),
                        c -> ((MultiSelectComboBox<?>) c).clear()
                ),
                new AdvancedSearchPanel.FilterDef(
                        "updatedAt",
                        "Updated At",
                        () -> {
                            DateRangePicker dr = new DateRangePicker();
                            dr.setWidthFull();
                            return dr;
                        },
                        c -> ((DateRangePicker) c).clear()
                )
        ));

        advPanel.addFilter("name");
        return advPanel;
    }

    @Override
    protected void resetAdvancedSearchFields() {
        if (advPanel != null) advPanel.clearAll();
    }
    
    @Override
    protected void beforeSave(Employee entity, boolean isNew) throws Exception {

        
        syncSupervisionToEntity(entity);
        syncDisabilityToEntity(entity);
        syncEmergencyContactsToEntity(entity);        
        syncRelativeWorkingToEntity(entity);
        syncLanguagesToEntity(entity);
        syncDependencyFamiliesToEntity(entity);
        syncVaccinationsToEntity(entity);
        syncEducationsToEntity(entity);
        syncDisciplinariesToEntity(entity);
        syncCareerHistoriesToEntity(entity);
        syncTrainingsToEntity(entity);
        syncNomineesToEntity(entity);



    }
    
    @Override
    protected void afterSave(Employee savedEntity, boolean isNew) {
        // SOLUTION 1: Save pending attachments for new entities
        if (isNew && attachmentComponent != null) {
            try {
                List<Attachment> pendingAttachments = attachmentComponent.getPendingAttachments();
                
                if (!pendingAttachments.isEmpty()) {
                    for (Attachment attachment : pendingAttachments) {
                        attachment.setAttachmentEntityTable(savedEntity);
                        currentUserLogin.ifPresent(u -> {
                            if (attachment.getId() == null) {
                                attachment.setUserCreated(u);
                            }
                            attachment.setUserUpdated(u);
                        });
                        attachmentRepository.save(attachment);
                        attachmentComponent.getAttachments().add(attachment);
                    }
                    
                    attachmentComponent.clearPendingAttachments();
                    attachmentComponent.refresh();
                    
                    Notification.show(pendingAttachments.size() + " attachment(s) saved",
                        2000, Notification.Position.TOP_CENTER)
                        .addThemeVariants(NotificationVariant.LUMO_SUCCESS);
                }
            } catch (Exception e) {
                Notification.show("Error saving attachments: " + e.getMessage(),
                    3000, Notification.Position.TOP_CENTER)
                    .addThemeVariants(NotificationVariant.LUMO_ERROR);
                e.printStackTrace();
            }
        }
        
        // Reset to existing entity mode and refresh
        if (attachmentComponent != null) {
            attachmentComponent.setNewEntityMode(false);
            attachmentComponent.setEntity(savedEntity);
            attachmentComponent.refresh();
        }
    }
    
    private void syncTrainingsToEntity(Employee target) {
        if (target.getEmployeeTrainings() == null) {
            target.setEmployeeTrainings(new ArrayList<>());
        } else {
            target.getEmployeeTrainings().clear();
        }

        for (EmployeeTraining t : trainingBuffer) {
            // Skip empty rows
            boolean emptyRow =
                t.getTrainingType() == null &&
                t.getTrainingCenter() == null &&
                t.getStartDate() == null &&
                t.getEndDate() == null &&
                t.getScore() == null &&
                (t.getResult() == null || t.getResult().trim().isEmpty()) &&
                (t.getTrainer() == null || t.getTrainer().trim().isEmpty()) &&
                (t.getRemark() == null || t.getRemark().trim().isEmpty());

            if (emptyRow) continue;

            // Enforce required fields
            if (t.getTrainingType() == null || t.getTrainingCenter() == null || t.getEndDate() == null) {
                throw new IllegalStateException("Please complete Training Type, Training Center, and End Date for each training record.");
            }

            // Validate date order
            if (t.getEndDate() != null && t.getStartDate() != null && t.getEndDate().isBefore(t.getStartDate())) {
                throw new IllegalStateException("Training: End Date cannot be before Start Date.");
            }

            t.setEmployee(target);

            if (t.getId() == null) t.setUserCreated(this.currentUserLogin.get());
            t.setUserUpdated(this.currentUserLogin.get());

            target.getEmployeeTrainings().add(t);
        }
    }

    private void syncNomineesToEntity(Employee target) {
        if (target.getEmployeeNominees() == null) {
            target.setEmployeeNominees(new ArrayList<>());
        } else {
            target.getEmployeeNominees().clear();
        }

        for (EmployeeNominee nominee : nomineeBuffer) {
            // Skip empty rows
            boolean emptyRow = (nominee.getNomineeName() == null || nominee.getNomineeName().trim().isEmpty())
                    && nominee.getNomineePhone() == null
                    && nominee.getNomineeNid() == null
                    && nominee.getRelationshipWithNominee() == null
                    && nominee.getGazetteer() == null;  // Added gazetteer check

            if (emptyRow) continue;

            // Validate required fields (nomineeName is required from entity)
            if (nominee.getNomineeName() == null || nominee.getNomineeName().trim().isEmpty()) {
                throw new IllegalStateException("Please enter Nominee Name | សូមបញ្ចូលឈ្មោះអ្នកទទួលផល");
            }

            // Validate relationship is selected (nullable = false in entity)
            if (nominee.getRelationshipWithNominee() == null) {
                throw new IllegalStateException("Please select Relationship for nominee | សូមជ្រើសរើសទំនាក់ទំនងសម្រាប់អ្នកទទួលផល");
            }

            // Validate gazetteer is selected (nullable = false in entity)
            if (nominee.getGazetteer() == null) {
                throw new IllegalStateException("Please select Address for nominee | សូមជ្រើសរើសអាសយដ្ឋានសម្រាប់អ្នកទទួលផល");
            }

            if (nominee.getPriority() == null) {
                nominee.setPriority(nextNomineePriority());
            }

            nominee.setEmployee(target);

            if (nominee.getId() == null) {
                nominee.setUserCreated(this.currentUserLogin.get());
            }
            nominee.setUserUpdated(this.currentUserLogin.get());

            target.getEmployeeNominees().add(nominee);
        }
    }
    
    private Component buildCareerHistoryTab() {

        Button btnAdd = new Button("Add Employment History", VaadinIcon.PLUS.create());
        btnAdd.addThemeVariants(ButtonVariant.LUMO_PRIMARY);
        btnAdd.addClickListener(e -> openCareerHistoryDialog(null)); // null = new

        careerHistoryGrid.removeAllColumns();
        careerHistoryGrid.setWidthFull();
        careerHistoryGrid.setAllRowsVisible(true);
        careerHistoryGrid.addThemeVariants( GridVariant.LUMO_COLUMN_BORDERS, GridVariant.LUMO_ROW_STRIPES, GridVariant.LUMO_WRAP_CELL_CONTENT);

        careerHistoryGrid.addColumn(ch -> ch.getPosition() == null ? "" : formatEnKh(ch.getPosition().getPosition(), ch.getPosition().getPositionKh()))
                .setHeader("Position").setAutoWidth(true).setResizable(true);

        careerHistoryGrid.addColumn(ch -> ch.getCareerType() == null ? "" : ch.getCareerType().getCareerTypeName())
                .setHeader("Career Type").setAutoWidth(true).setResizable(true);

        careerHistoryGrid.addColumn(ch -> ch.getSalary() == null ? "" : ( this.allowSeeSalary? ch.getSalary().toString():"XXXXX")).setHeader("Salary").setAutoWidth(true).setResizable(true);

        careerHistoryGrid.addColumn(ch -> ch.getFromDate() == null ? "" : DateTimeUtilFormart.DATE_FORMATTER.format(ch.getFromDate()))
                .setHeader("From Date").setAutoWidth(true).setResizable(true);

        careerHistoryGrid.addColumn(ch -> ch.getToDate() == null ? "" : DateTimeUtilFormart.DATE_FORMATTER.format(ch.getToDate()))
                .setHeader("To Date").setAutoWidth(true).setResizable(true);

        careerHistoryGrid.addColumn(ch -> ch.getBranch() == null ? "" : ch.getBranch().getBranchShortName())
                .setHeader("Location").setAutoWidth(true).setResizable(true);

        careerHistoryGrid.addComponentColumn(ch -> {
            Button edit = new Button(VaadinIcon.EDIT.create());
            edit.addThemeVariants(ButtonVariant.LUMO_TERTIARY, ButtonVariant.LUMO_ICON);
            edit.addClickListener(e -> openCareerHistoryDialog(ch));

            Button del = new Button(VaadinIcon.TRASH.create());
            del.addThemeVariants(ButtonVariant.LUMO_TERTIARY, ButtonVariant.LUMO_ICON, ButtonVariant.LUMO_ERROR);
            del.addClickListener(e -> {
                ConfirmDialog dlg = new ConfirmDialog();
                dlg.setHeader("Confirm delete");
                dlg.setText("Are you sure you want to delete this career history record?");
                dlg.setCancelable(true);
                dlg.setCancelText("Cancel");
                dlg.setConfirmText("Delete");
                dlg.setConfirmButtonTheme("error primary");
                dlg.addConfirmListener(ev -> {
                    careerHistoryBuffer.remove(ch);
                    careerHistoryGrid.getDataProvider().refreshAll();
                });
                dlg.open();
            });

            HorizontalLayout actions = new HorizontalLayout(edit, del);
            actions.setPadding(false);
            actions.setSpacing(true);
            actions.setMargin(false);
            actions.setJustifyContentMode(JustifyContentMode.CENTER);
            actions.setDefaultVerticalComponentAlignment(Alignment.CENTER);
            return actions;
        })
        .setHeader("Action")
        .setFrozenToEnd(true)
        .setAutoWidth(true)
        .setFlexGrow(0).setTextAlign(ColumnTextAlign.CENTER);

        careerHistoryGrid.setItems(careerHistoryBuffer);

        VerticalLayout layout = new VerticalLayout(btnAdd, careerHistoryGrid);
        layout.setPadding(false);
        layout.setSpacing(false);
        layout.setMargin(false);
        layout.setWidthFull();
        return layout;
    }

    private void openCareerHistoryDialog(EmployeeCareerHistory editing) {

        boolean isNew = (editing == null);
        EmployeeCareerHistory bean = isNew ? new EmployeeCareerHistory() : editing;

        Dialog dialog = new Dialog();
        dialog.setHeaderTitle(isNew ? "Add Career History" : "Edit Career History");
        dialog.setWidth("1000px");

        ComboBox<Positions> position = new ComboBox<>("Position");
        position.setItems(this.positionRepository.findAll(org.springframework.data.domain.Sort.by("position")));
        position.setItemLabelGenerator(p -> formatEnKh(p.getPosition(), p.getPositionKh()));
        position.setClearButtonVisible(true);

        ComboBox<CareerType> careerType = new ComboBox<>("Career Type");
        //careerType.setItems(service.getCareerTypesSorted()); // <- if you don't have, replace with your CareerTypeRepository.findAll(Sort...)
        careerType.setItems(this.careerTypeRepository.findAll(Sort.by("careerTypeName")));

        careerType.setItemLabelGenerator(ct -> ct.getCareerTypeName());
        careerType.setClearButtonVisible(true);

        BigDecimalField salary = new BigDecimalField("Salary");
        salary.setClearButtonVisible(true);

        DatePicker fromDate = new DatePicker("From Date");
        DatePicker toDate = new DatePicker("To Date");

        ComboBox<Branch> branch = new ComboBox<>("Location");
        branch.setItems(this.branchRepository.findAll(org.springframework.data.domain.Sort.by("branchShortName")));
        branch.setItemLabelGenerator(Branch::getBranchShortName);
        branch.setClearButtonVisible(true);



        // defaults (optional)
        if (this.entity != null) {
            if (bean.getPosition() == null) bean.setPosition(this.entity.getPositions());
            if (bean.getBranch() == null) bean.setBranch(this.entity.getBranch());
        }

        com.vaadin.flow.data.binder.BeanValidationBinder<EmployeeCareerHistory> b = new com.vaadin.flow.data.binder.BeanValidationBinder<>(EmployeeCareerHistory.class);

        // NOTE: bind names must match your EmployeeCareerHistory fields
        b.bind(position, "position");
        b.bind(careerType, "careerType");
        b.bind(salary, "salary");
        b.bind(fromDate, "fromDate");
        b.bind(toDate, "toDate");
        b.bind(branch, "branch");


        b.readBean(bean);

        FormLayout form = new FormLayout(
                position, careerType, salary,
                fromDate, toDate, branch
        );
        form.setWidthFull();
        form.setResponsiveSteps(
                new FormLayout.ResponsiveStep("0", 1),
                new FormLayout.ResponsiveStep("600px", 2),
                new FormLayout.ResponsiveStep("1000px", 3)
        );

        Button btnSave = new Button("Save", e -> {
            try {
                // 1) binder -> bean
                if (!b.writeBeanIfValid(bean)) return;

                // 2) basic checks
                if (bean.getFromDate() == null) {
                    showErrorMessage("From Date is required.");
                    return;
                }
                if (bean.getToDate() != null && bean.getFromDate() != null
                        && bean.getToDate().isBefore(bean.getFromDate())) {
                    showErrorMessage("To Date cannot be before From Date.");
                    return;
                }




                if (isNew) careerHistoryBuffer.add(bean);

                careerHistoryGrid.getDataProvider().refreshAll();
                dialog.close();

            } catch (Exception ex) {
                showErrorMessage(ex.getMessage());
            }
        });

        btnSave.addThemeVariants(ButtonVariant.LUMO_PRIMARY);

        Button btnCancel = new Button("Cancel", e -> dialog.close());

        dialog.getFooter().add(btnCancel, btnSave);
        dialog.add(form);
        dialog.open();
    }

    private void syncCareerHistoriesToEntity(Employee target) {

        if (target.getEmployeeCareerHistories() == null) target.setEmployeeCareerHistories(new ArrayList<>());
        else target.getEmployeeCareerHistories().clear();

        for (EmployeeCareerHistory ch : careerHistoryBuffer) {

            boolean emptyRow =
                    ch.getPosition() == null
                    && ch.getCareerType() == null
                    && ch.getSalary() == null
                    && ch.getFromDate() == null
                    && ch.getToDate() == null
                    && ch.getBranch() == null;

            if (emptyRow) continue;

            // enforce required fields (your entity has NOT NULLs)
            if (ch.getPosition() == null
                    || ch.getCareerType() == null
                    || ch.getSalary() == null
                    || ch.getFromDate() == null
                    || ch.getBranch() == null) {
                throw new IllegalStateException("Please complete all required fields in Career History tab.");
            }

            if (ch.getToDate() != null && ch.getToDate().isBefore(ch.getFromDate())) {
                throw new IllegalStateException("Career History: To Date cannot be before From Date.");
            }

            ch.setEmployee(target);

            if (ch.getId() == null) ch.setUserCreated(this.currentUserLogin.get());
            ch.setUserUpdated(this.currentUserLogin.get());

            target.getEmployeeCareerHistories().add(ch);
        }
    }
    private void syncDisciplinariesToEntity(Employee target) {

        if (target.getEmployeeDisciplinaries() == null) target.setEmployeeDisciplinaries(new ArrayList<>());
        else target.getEmployeeDisciplinaries().clear();

        for (EmployeeDisciplinary d : disciplinaryBuffer) {

            boolean emptyRow =
                    d.getDisciplinary() == null
                    && d.getStartDate() == null
                    && d.getExpireDate() == null
                    && d.getTeams() == null
                    && d.getBranch() == null
                    && d.getReportedBy() == null
                  
                    && (d.getDescription() == null || d.getDescription().trim().isEmpty());

            if (emptyRow) continue;

            // enforce required (based on your entity annotations)
            if (d.getPositions() == null) {
                // you have positions NOT NULL in EmployeeDisciplinary
                // If positions should always match employee current position:
                d.setPositions(target.getPositions());
            }

            if (d.getPositions() == null
            		|| d.getDisciplinary() == null
                    || d.getTeams() == null
                    || d.getBranch() == null
                   
                    || d.getStartDate() == null
                    || d.getExpireDate() == null) {
                throw new IllegalStateException("Please complete all required fields in Disciplinary tab.");
            }

            if (d.getExpireDate().isBefore(d.getStartDate())) {
                throw new IllegalStateException("Disciplinary: Expire Date cannot be before Start Date.");
            }

            d.setEmployee(target);

            if (d.getId() == null) d.setUserCreated(this.currentUserLogin.get());
            d.setUserUpdated(this.currentUserLogin.get());

            target.getEmployeeDisciplinaries().add(d);
        }
    }

    private void syncEducationsToEntity(Employee target) {

        if (target.getEmployeeEducations() == null) target.setEmployeeEducations(new ArrayList<>());
        else target.getEmployeeEducations().clear();

        for (EmployeeEducation ee : educationBuffer) {

            boolean emptyRow =
                    ee.getEducationType() == null
                    && ee.getEducationCenter() == null
                    && (ee.getMajor() == null || ee.getMajor().trim().isEmpty());

            if (emptyRow) continue;

            // enforce required fields (your DB has NOT NULL on major + FK)
            if (ee.getEducationType() == null || ee.getEducationCenter() == null
                    || ee.getMajor() == null || ee.getMajor().trim().isEmpty()) {
                throw new IllegalStateException("Please complete Education Type, Education Center, and Major for each education row.");
            }

            ee.setEmployee(target);

            if (ee.getId() == null) ee.setUserCreated(this.currentUserLogin.get());
            ee.setUserUpdated(this.currentUserLogin.get());

            target.getEmployeeEducations().add(ee);
        }
    }

    private Component buildEducationTab() {

        Button btnAdd = new Button("Add education", VaadinIcon.PLUS.create());
        btnAdd.addThemeVariants(ButtonVariant.LUMO_PRIMARY);

        btnAdd.addClickListener(e -> {
            EmployeeEducation ee = new EmployeeEducation();
            educationBuffer.add(ee);
            educationGrid.getDataProvider().refreshAll();
        });

        educationGrid.removeAllColumns(); // safe if called again
        educationGrid.setWidthFull();
        educationGrid.setAllRowsVisible(true);
        educationGrid.addThemeVariants(
                GridVariant.LUMO_COLUMN_BORDERS,
                GridVariant.LUMO_ROW_STRIPES,
                GridVariant.LUMO_WRAP_CELL_CONTENT
        );

        // Education Type
        educationGrid.addComponentColumn(ee -> {
            ComboBox<EducationType> cb = buildEducationTypeComboBox();
            cb.setWidthFull();
            cb.setValue(ee.getEducationType());
            cb.addValueChangeListener(ev -> ee.setEducationType(ev.getValue()));
            return cb;
        }).setHeader("Degree").setResizable(true).setAutoWidth(true);

        // Education Center
        educationGrid.addComponentColumn(ee -> {
            ComboBox<EducationCenter> cb = buildEducationCenterComboBox();
            cb.setWidthFull();
            cb.setValue(ee.getEducationCenter());
            cb.addValueChangeListener(ev -> ee.setEducationCenter(ev.getValue()));
            return cb;
        }).setHeader("Institute").setResizable(true).setAutoWidth(true);

        // Major
        educationGrid.addComponentColumn(ee -> {
            TextField tf = new TextField();
            tf.setWidthFull();
            tf.setValue(ee.getMajor() == null ? "" : ee.getMajor());
            tf.addValueChangeListener(ev -> ee.setMajor(ev.getValue()));
            return tf;
        }).setHeader("Major/Subject").setResizable(true).setAutoWidth(true);



        // End Date
        educationGrid.addComponentColumn(ee -> {
            DatePicker dp = new DatePicker();
            dp.setWidthFull();
            // NOTE: your field is EndDate (capital E). Prefer rename to endDate.
            dp.setValue(ee.getEndDate());
            dp.addValueChangeListener(ev -> ee.setEndDate(ev.getValue()));
            return dp;
        }).setHeader("Pass Year").setResizable(true).setAutoWidth(true);
        
        educationGrid.addComponentColumn(ee -> {
            ComboBox<EducationStatusEnum> cb = new ComboBox<>();
            cb.setItems(EducationStatusEnum.values());
            cb.setItemLabelGenerator(status -> {
                if (status == null) return "";

                return status.getLabel();
            });
            cb.setWidthFull();
            cb.setClearButtonVisible(true);
            cb.setValue(ee.getStatus());
            cb.addValueChangeListener(ev -> ee.setStatus(ev.getValue()));
            return cb;
        }).setHeader("Status").setResizable(true).setAutoWidth(true);

        // Delete (confirm)
        educationGrid.addComponentColumn(ee -> {
            Button del = new Button(VaadinIcon.TRASH.create());
            del.addThemeVariants(ButtonVariant.LUMO_TERTIARY, ButtonVariant.LUMO_ICON, ButtonVariant.LUMO_ERROR);

            del.addClickListener(click -> {
                ConfirmDialog dlg = new ConfirmDialog();
                dlg.setHeader("Confirm delete");
                dlg.setText("Are you sure you want to delete this education record?");
                dlg.setCancelable(true);
                dlg.setCancelText("Cancel");
                dlg.setConfirmText("Delete");
                dlg.setConfirmButtonTheme("error primary");
                dlg.addConfirmListener(ok -> {
                    educationBuffer.remove(ee);
                    educationGrid.getDataProvider().refreshAll();
                });
                dlg.open();
            });

            return del;
        }).setHeader("Action").setFrozenToEnd(true).setAutoWidth(true).setFlexGrow(0);

        educationGrid.setItems(educationBuffer);

        VerticalLayout layout = new VerticalLayout(btnAdd, educationGrid);
        layout.setPadding(false);
        layout.setSpacing(false);
        layout.setMargin(false);
        layout.setWidthFull();
        return layout;
    }
    private ComboBox<EducationType> buildEducationTypeComboBox() {
        ComboBox<EducationType> cb = new ComboBox<>();
        cb.setItems(educationTypesCache);
        cb.setItemLabelGenerator(t -> formatEnKh(t.getEducationTypeEN(), t.getEducationTypeKh()));
        cb.setPlaceholder("Select education type...");
        cb.setClearButtonVisible(true);
        return cb;
    }

    private ComboBox<EducationCenter> buildEducationCenterComboBox() {
        ComboBox<EducationCenter> cb = new ComboBox<>();
        cb.setItems(educationCentersCache);
        cb.setItemLabelGenerator(c -> formatEnKh(c.getEducationCenterEN(), c.getEducationCenterKh()));
        cb.setPlaceholder("Select education center...");
        cb.setClearButtonVisible(true);
        return cb;
    }

    private void syncVaccinationsToEntity(Employee target) {

        if (target.getEmployeeVaccinations() == null) target.setEmployeeVaccinations(new ArrayList<>());
        else target.getEmployeeVaccinations().clear();

        for (EmployeeVaccination ev : vaccinationBuffer) {

            boolean emptyRow = ev.getVaccination() == null && ev.getDateInjection() == null;
            if (emptyRow) continue;

            if (ev.getVaccination() == null || ev.getDateInjection() == null) {
                throw new IllegalStateException("Please complete Vaccine and Date Injection for each vaccination row.");
            }

            ev.setEmployee(target);

            if (ev.getId() == null) ev.setUserCreated(this.currentUserLogin.get());
            ev.setUserUpdated(this.currentUserLogin.get());

            target.getEmployeeVaccinations().add(ev);
        }
    }

    private void syncDependencyFamiliesToEntity(Employee target) {

        if (target.getDependencyFamilies() == null) target.setDependencyFamilies(new ArrayList<>());
        else target.getDependencyFamilies().clear();

        for (EmployeeDependencyFamily df : dependencyFamilyBuffer) {

            // skip totally empty rows (should not happen because dialog validates)
            boolean empty =
                    (df.getDependencyName() == null || df.getDependencyName().trim().isEmpty())
                    && df.getGender() == null
                    && df.getDob() == null
                    && df.getRelationship() == null;
            if (empty) continue;

            // enforce required (DB NOT NULL)
            if (df.getDependencyName() == null || df.getDependencyName().trim().isEmpty()
                    || df.getGender() == null
                    || df.getRelationship() == null) {
                throw new IllegalStateException("Please complete Name, Gender, and Relationship for each dependency.");
            }

            df.setEmployee(target);

            if (df.getId() == null) df.setUserCreated(this.currentUserLogin.get());
            df.setUserUpdated(this.currentUserLogin.get());

            target.getDependencyFamilies().add(df);
        }
    }

    
    private void syncLanguagesToEntity(Employee target) {

        if (target.getEmployeeLanguages() == null) target.setEmployeeLanguages(new ArrayList<>());
        else target.getEmployeeLanguages().clear();

        for (EmployeeLanguage el : languageBuffer) {

            boolean emptyRow =
                    el.getLanguage() == null
                    && el.getLanguageReading() == null
                    && el.getLanguageSpeaking() == null
                    && el.getLanguageListening() == null
                    && el.getLanguageWriting() == null;

            if (emptyRow) continue;

            // DB columns are NOT NULL -> enforce full selection before saving
            if (el.getLanguage() == null
                    || el.getLanguageReading() == null
                    || el.getLanguageSpeaking() == null
                    || el.getLanguageListening() == null
                    || el.getLanguageWriting() == null) {
                throw new IllegalStateException("Please complete Language + Reading/Speaking/Listening/Writing for each row.");
            }

            el.setEmployee(target);

            if (el.getId() == null) el.setUserCreated(this.currentUserLogin.get());
            el.setUserUpdated(this.currentUserLogin.get());

            target.getEmployeeLanguages().add(el);
        }
    }

    private void syncRelativeWorkingToEntity(Employee target) {
        //User current = authenticatedUser.get().orElse(null); // adjust to your AuthenticatedUser API

        if (target.getEmployeeRelativeWorkings() == null) target.setEmployeeRelativeWorkings(new ArrayList<>());
        else target.getEmployeeRelativeWorkings().clear();

        for (EmployeeRelativeWorking rw : relativeWorkingBuffer) {
            if (rw.getEmployeeRelative() == null && rw.getDepartment() == null && rw.getRelationship() == null) continue;

            rw.setEmployee(target);

            if (rw.getId() == null) { 
                rw.setUserCreated(this.currentUserLogin.get());
            }
            rw.setUserUpdated(this.currentUserLogin.get());

            target.getEmployeeRelativeWorkings().add(rw);
        }
    }


    private void syncEmergencyContactsToEntity(Employee target) {

        if (target.getEmployeeEmergencyContacts() == null) {
            target.setEmployeeEmergencyContacts(new ArrayList<>());
        } else {
            target.getEmployeeEmergencyContacts().clear();
        }

        for (EmployeeEmergencyContact ec : emergencyBuffer) {

            // skip empty rows (at least name OR relationship OR address)
            boolean nameBlank = (ec.getEmergencyContactName() == null || ec.getEmergencyContactName().trim().isEmpty());
            if (nameBlank && ec.getRelationship() == null && ec.getGazetteer() == null) continue;

            ec.setEmployee(target); // owning side
            if (ec.getId() == null) { // new
                ec.setUserCreated(this.currentUserLogin.get());
            }
            ec.setUserUpdated(this.currentUserLogin.get());
            
            target.getEmployeeEmergencyContacts().add(ec);
        }
    }

    private void syncSupervisionToEntity(Employee target) {
        // clear current list (orphanRemoval=true will delete removed rows)
        target.getEmployeeSupervisions().clear();

        for (EmployeeSupervision es : supervisionBuffer) {

            // skip empty lines
            if (es.getSupervisor() == null) continue;

            // prevent self as supervisor
            if (target.getId() != null
                    && es.getSupervisor().getId() != null
                    && target.getId().equals(es.getSupervisor().getId())) {
                continue;
            }

            // IMPORTANT: set owning side
            es.setEmployee(target);
            if (es.getId() == null) { // new
                es.setUserCreated(this.currentUserLogin.get());
            }
            es.setUserUpdated(this.currentUserLogin.get());

            // optional: default sort order if missing
            if (es.getSortOrder() == null) {
                es.setSortOrder(nextSupervisorSortOrder());
            }

            target.getEmployeeSupervisions().add(es);
        }
    }
    
    private Component buildDisabilityTab() {

        Button btnAdd = new Button("Add disability", VaadinIcon.PLUS.create());
        btnAdd.addThemeVariants(ButtonVariant.LUMO_PRIMARY);

        btnAdd.addClickListener(e -> {
            EmployeeDisability ed = new EmployeeDisability();
            disabilityBuffer.add(ed);
            disabilityGrid.getDataProvider().refreshAll();
        });

        disabilityGrid.setWidthFull();
        disabilityGrid.setAllRowsVisible(true);

        // Disability type (ComboBox)
        disabilityGrid.addComponentColumn(ed -> {
            ComboBox<DisabilityTypeOption> cb = buildDisabilityTypeOptionComboBox();
            cb.setWidthFull();
            cb.setValue(ed.getDisabilityTypeOption());

            cb.addValueChangeListener(ev -> {
                ed.setDisabilityTypeOption(ev.getValue());
            });

            return cb;
        }).setHeader("Disability Type").setFlexGrow(2).setResizable(true);

        // Note
        disabilityGrid.addComponentColumn(ed -> {
            TextField tf = new TextField();
            tf.setWidthFull();
            tf.setValue(ed.getNote() == null ? "" : ed.getNote());
            tf.addValueChangeListener(ev -> ed.setNote(ev.getValue()));
            return tf;
        }).setHeader("Note").setFlexGrow(3).setResizable(true);

        // Delete
        disabilityGrid.addComponentColumn(ed -> {
            Button del = new Button(VaadinIcon.TRASH.create());
            del.addThemeVariants(ButtonVariant.LUMO_TERTIARY, ButtonVariant.LUMO_ICON, ButtonVariant.LUMO_ERROR);
            del.addClickListener(click -> {
                disabilityBuffer.remove(ed);
                disabilityGrid.getDataProvider().refreshAll();
            });
            return del;
        }).setHeader("Action").setFlexGrow(0).setAutoWidth(true);

        disabilityGrid.setItems(disabilityBuffer);

        VerticalLayout layout = new VerticalLayout(btnAdd, disabilityGrid);
        layout.setPadding(false);
        layout.setSpacing(false);
        layout.setMargin(false);
        layout.setWidthFull();
        return layout;
    }


    private ComboBox<DisabilityTypeOption> buildDisabilityTypeOptionComboBox() {
        ComboBox<DisabilityTypeOption> cb = new ComboBox<>();
        cb.setItems(disabilityTypeOptionRepository.findAll()); // change "name"
        cb.setItemLabelGenerator(DisabilityTypeOption::getDisabilityTypeOptionName); // change getter
        cb.setPlaceholder("Select disability type...");
        cb.setClearButtonVisible(true);
        return cb;
    }
    private void syncDisabilityToEntity(Employee target) {

        // ensure list exists
        if (target.getEmployeeDisabilities() == null) {
            target.setEmployeeDisabilities(new ArrayList<>()); // if you have setter
        } else {
            target.getEmployeeDisabilities().clear();
        }

        for (EmployeeDisability ed : disabilityBuffer) {

            // skip empty lines
            if (ed.getDisabilityTypeOption() == null) continue;

            // owning side
            ed.setEmployee(target);
            if (ed.getId() == null) { // new
                ed.setUserCreated(this.currentUserLogin.get());
            }
            ed.setUserUpdated(this.currentUserLogin.get());

            // keep name consistent (required column)
            if (ed.getDisabilityTypeOption() == null) {
                ed.setDisabilityTypeOption(ed.getDisabilityTypeOption()); // change getter
            }

            target.getEmployeeDisabilities().add(ed);
        }
    }
    
    private boolean canSeeSalary() {
        return currentUserLogin
                .map(u -> java.util.Optional.ofNullable(u.getRoles()).orElse(java.util.Collections.emptySet())
                        .stream()
                        .anyMatch(r -> r != null && Boolean.TRUE.equals(r.getCanSeeSalary()))
                )
                .orElse(false);
    }

    private Component buildAttachmentTab() {
        if (attachmentComponent == null) {
        	attachmentComponent = new AttachmentComponent(
                    "emp_master",
                    entity,
                    attachmentRepository,
                    attachmentTypeRepository,
                    fileUploadUtility,
                    currentUserLogin,
                    uploadDir,
                    false // viewOnlyMode = false - this is for editing
                );
            
        } else {
            attachmentComponent.setEntity(entity);
            attachmentComponent.refresh();
        }
        return attachmentComponent;
    }
    private void injectStickySearchCss() {
        UI ui = UI.getCurrent();
        if (ui == null) return;

        if (ui.getElement().getProperty("stickySearchCssInjected", false)) return;
        ui.getElement().setProperty("stickySearchCssInjected", true);

        ui.getPage().executeJs("""
          if (!document.getElementById('sticky-search-css')) {
            const s = document.createElement('style');
            s.id = 'sticky-search-css';
            s.textContent = `
              /* scroll container for each tab */
              .tab-scroll {
                overflow: auto;
                max-height: 70vh;
              }

              /* sticky search/header inside each tab */
              .tab-sticky-header {
                position: sticky;
                top: 0;
                z-index: 5;
                background: var(--lumo-base-color);
                padding: var(--lumo-space-s) 0;
                border-bottom: 1px solid var(--lumo-contrast-10pct);
              }
            `;
            document.head.appendChild(s);
          }
        """);
    }
    
    private Component buildTabWithStickySearch(Component searchBar, Component body) {
    	 Div scroll;
    	 Div header;
    	if(searchBar==null) {
    		scroll = new Div( body);
    	}else {
    		header = new Div(searchBar);
            header.addClassName("tab-sticky-header");
            scroll = new Div(header, body);
    	}


        // tab content scroll container

        scroll.addClassName("tab-scroll");
        scroll.setWidthFull();

        return scroll;
    }
    @Override
    protected Set<String> getAdditionalExcludedKeys() {
        return Set.of(
        		
            "lastCareerTypeDate",
        	"teams",	
        	"category",
        	"contractType",
        	"presentAddress.parent.parent.parent.nameEn",
        	"presentAddress.parent.parent.nameEn",
        	"presentAddress.parent.nameEn",
        	"presentAddress.nameEn",
        	"presentAddress.code",
        	
        	"dob",
        	"maritalStatus",

        	"bloodGroup",
        	"nationality",
        	"ethnicity",
        	"religions",
        	"phoneNumber",
            
            "permanentAddress.parent.parent.parent.nameEn",
            "permanentAddress.parent.parent.nameEn",
            "permanentAddress.parent.nameEn",
            "permanentAddress.nameEn",
            "permanentAddress.code",
            
            "nativeLocation.parent.parent.parent.nameEn",
            "nativeLocation.parent.parent.nameEn",
            "nativeLocation.parent.nameEn",
            "nativeLocation.nameEn",            
            "nativeLocation.code",
            
            "tenureInYears",
            "hasDisability",
            
            "probationEndDate",
            "probationStatus",
            
            "currentContractStartDate",
            "currentContractEndDate",
            
            "grossSalaryUsd",
            "bank",
            "bankAccount",
            "bankAccountName",

            "contracts.donor",
            "contracts",
            "code",
            

            "lastEducationType",
            "lastOrganization",
            
            "preHaloWorkExperience",
            "preExistingMedicalCondition",
            
            "idCardNumber",
            "idCardExpiryDate",
            "nsffCardNumber",
            "nsffExpiryDate",
            "nssfRetire",
            "drivingLicense",
            "drivingLicenseExpire",
     
            "referenceVerified", 
            "referenceVerifiedDate",
            "policePlearance",
            "attendanceType"
        );
    }
    
    private ComponentRenderer<Component, Employee> buildGridTabRenderer() {
        return new ComponentRenderer<>(employee -> {
            TabSheet tabSheet = new TabSheet();
            tabSheet.setSizeFull();
            tabSheet.setWidthFull();
            //tabSheet.setHeight("600px");
            
            // Tab 1: Employee Information (A4 Paper Style)
            Tab tabTest = new Tab(VaadinIcon.USER_CARD.create(), buildTabEnKh("Employee Information", "ព័ត៌មានបុគ្គលិក"));
            
            Div reportContainer = new Div();
            reportContainer.setSizeFull();
            reportContainer.getStyle().set("min-height", "277mm");

            tabSheet.add(tabTest, reportContainer);
            
            tabSheet.addSelectedChangeListener(event -> {
                if (event.getSelectedTab() == tabTest && reportContainer.getChildren().count() == 0) {
                    // Generate report only when tab is selected
                    HashMap<String, Object> parameters = new HashMap<>();
                    Set<Long> ids = new HashSet<>();
                    
                    if (employee.getInsuranceNo() != null) {
                        ids.add(employee.getInsuranceNo().longValue());
                        parameters.put("p_insurance_no", ids);
                        parameters.put("SUBREPORT_DIR", "report_embed");
                        parameters.put("ATTACHMENT_DIR", baseAttachmentsDir.toString());
                        parameters.put("currentUserLogin", this.currentUserLogin.get().getName());
                        
                        PreviewReportDiv preview = new PreviewReportDiv("report_embed/staff_information.jasper", parameters);
                        preview.setWidthFull();
                        preview.getStyle().set("height", "277mm");
                        
                        reportContainer.removeAll();
                        reportContainer.add(preview);
                    }
                }
            });

            // Tab 2: Attachments
            Tab tabAttachment = new Tab(VaadinIcon.FOLDER.create(), buildTabEnKh("Attachments", "ឯកសារភ្ជាប់"));
            
            AttachmentComponent gridAttachmentComponent = new AttachmentComponent(
                    "emp_master",
                    employee,
                    attachmentRepository,
                    attachmentTypeRepository,
                    fileUploadUtility,
                    currentUserLogin,
                    uploadDir,
                    null,
                    true
            );
            
            tabSheet.add(tabAttachment, gridAttachmentComponent);
            for (int i = 0; i < tabSheet.getTabCount(); i++) {
                tabSheet.getTabAt(i).addThemeVariants(TabVariant.LUMO_ICON_ON_TOP);
            }
            
            tabSheet.setSelectedTab(null);
            
            return tabSheet;
        });
    }



}
