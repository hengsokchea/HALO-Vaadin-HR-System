package org.halocambodia.data;

import java.util.Collection;
import java.util.List;
import java.util.Optional;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;
import org.hibernate.annotations.Formula;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.halocambodia.enums.EmployeeTypeEnum;

public interface EmployeeRepository  extends JpaRepository<Employee, Long>, JpaSpecificationExecutor<Employee> {



	List<Employee> findByEmployeeTypeInAndCareerType_CareerTypeGroup_CareerTypeGroupName(
		    Collection<EmployeeTypeEnum> employeeTypes,
		    String careerTypeGroupName
		);

	List<Employee> findByEmployeeTypeInOrderByNameEnAsc(
		    Collection<EmployeeTypeEnum> employeeTypes
		);

	    // Convenience method for your case
	 default List<Employee> findActiveLocalStaff() {
	        //return findByEmployeeTypeAndCareerType_CareerTypeGroup_CareerTypeGroupName("Local Staff", "Active");
	        return findByEmployeeTypeInAndCareerType_CareerTypeGroup_CareerTypeGroupName( List.of(EmployeeTypeEnum.National),"Active");
	 }

	 default List<Employee> findLocalStaffAllStatuses() {
	        return findByEmployeeTypeInOrderByNameEnAsc(List.of(EmployeeTypeEnum.National));
	 }
	 
	 default List<Employee> findActiveInternationalStaff() {
	        return findByEmployeeTypeInAndCareerType_CareerTypeGroup_CareerTypeGroupName(List.of(EmployeeTypeEnum.International),"Active");
	 }
	 
	 default List<Employee> findActiveLocalAndInternationalStaff() {
	        return findByEmployeeTypeInAndCareerType_CareerTypeGroup_CareerTypeGroupName(List.of(EmployeeTypeEnum.National,EmployeeTypeEnum.International),"Active");
	 }
	    
	 
	 Optional<Employee> findByInsuranceNo(Integer insuranceNo);
	 

	 List<Employee> findByInsuranceNoIn(Collection<Integer> insuranceNumbers);
	 
	 
	 @Query("""
			    select e
			    from Employee e
			    where
			        (:q is null or :q = '' or
			         lower(e.nameEn) like lower(concat('%', :q, '%')) or
			         lower(e.nameKh) like lower(concat('%', :q, '%')) or
			         str(e.insuranceNo) like concat('%', :q, '%'))
			    order by e.nameEn asc
			""")
			Page<Employee> searchEmployeesByNameEnKhInsurance(@Param("q") String q, Pageable pageable);

			@Query("""
			    select count(e)
			    from Employee e
			    where
			        (:q is null or :q = '' or
			         lower(e.nameEn) like lower(concat('%', :q, '%')) or
			         lower(e.nameKh) like lower(concat('%', :q, '%')) or
			         str(e.insuranceNo) like concat('%', :q, '%'))
			""")
			long countEmployeesByNameEnKhInsurance(@Param("q") String q);
			
			
		    // ✅ moved from service -> repository
		    default List<Employee> searchEmployeesByNameEnKhInsuranceList(String filter, Pageable pageable) {
		        String f = (filter == null) ? "" : filter.trim();
		        return searchEmployeesByNameEnKhInsurance(f, pageable).getContent();
		    }

		    // ✅ moved from service -> repository
		    default long countEmployeesByNameEnKhInsuranceTrimmed(String filter) {
		        String f = (filter == null) ? "" : filter.trim();
		        return countEmployeesByNameEnKhInsurance(f);
		    }

		    
		    
		    @Query("""
		    	    select e
		    	    from Employee e
		    	    where
		    	        e.employeeType in :employeeTypes
		    	        and e.careerType.careerTypeGroup.careerTypeGroupName = :careerTypeGroup
		    	        and (
		    	            :q is null or :q = '' or
		    	            lower(e.nameEn) like lower(concat('%', :q, '%')) or
		    	            lower(e.nameKh) like lower(concat('%', :q, '%')) or
		    	            str(e.insuranceNo) like concat('%', :q, '%')
		    	        )
		    	    order by e.nameEn asc
		    	""")
		    Page<Employee> searchActiveEmployees(
		    	    @Param("employeeTypes") Collection<EmployeeTypeEnum> employeeTypes,
		    	    @Param("careerTypeGroup") String careerTypeGroup,
		    	    @Param("q") String q,
		    	    Pageable pageable
		    	);
		    
		    @Query("""
		    	    select count(e)
		    	    from Employee e
		    	    where
		    	        e.employeeType in :employeeTypes
		    	        and e.careerType.careerTypeGroup.careerTypeGroupName = :careerTypeGroup
		    	        and (
		    	            :q is null or :q = '' or
		    	            lower(e.nameEn) like lower(concat('%', :q, '%')) or
		    	            lower(e.nameKh) like lower(concat('%', :q, '%')) or
		    	            str(e.insuranceNo) like concat('%', :q, '%')
		    	        )
		    	""")
		    long countActiveEmployees(
		    	    @Param("employeeTypes") Collection<EmployeeTypeEnum> employeeTypes,
		    	    @Param("careerTypeGroup") String careerTypeGroup,
		    	    @Param("q") String q
		    	);
		    
		    default List<Employee> searchActiveLocalStaff(String filter, Pageable pageable) {
		        String f = (filter == null) ? "" : filter.trim();
		        return searchActiveEmployees(
		                List.of(EmployeeTypeEnum.National),
		                "Active",
		                f,
		                pageable
		        ).getContent();
		    }
		    
		    default long countActiveLocalStaff(String filter) {
		        String f = (filter == null) ? "" : filter.trim();
		        return countActiveEmployees(
		                List.of(EmployeeTypeEnum.National),
		                "Active",
		                f
		        );
		    }
}
