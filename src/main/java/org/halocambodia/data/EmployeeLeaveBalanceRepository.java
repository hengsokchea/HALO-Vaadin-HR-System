package org.halocambodia.data;
import java.util.List;
import java.util.Optional;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

public interface EmployeeLeaveBalanceRepository  extends JpaRepository<EmployeeLeaveBalance, Long>, JpaSpecificationExecutor<EmployeeLeaveBalance> {
	
    // 🔹 Find all balances by employee insurance number
    @Query("""
           SELECT elb 
           FROM EmployeeLeaveBalance elb 
           WHERE elb.employee.insuranceNo = :insuranceNo
           """)
    List<EmployeeLeaveBalance> findByEmployeeInsuranceNo(@Param("insuranceNo") Integer insuranceNo);

    // 🔹 Find all balances by insurance number and year
    @Query("""
           SELECT elb 
           FROM EmployeeLeaveBalance elb 
           WHERE elb.employee.insuranceNo = :insuranceNo 
           AND elb.year = :year
           """)
    List<EmployeeLeaveBalance> findByEmployeeInsuranceNoAndYear(@Param("insuranceNo") Integer insuranceNo,
                                                               @Param("year") Integer year);
    
 // 🔹 Find all balances by insurance number, year, and leave type
    @Query("""
           SELECT elb 
           FROM EmployeeLeaveBalance elb 
           WHERE elb.employee.insuranceNo = :insuranceNo 
           AND elb.year = :year
           AND elb.leaveType.id = :leaveTypeId
           """)
    List<EmployeeLeaveBalance> findByEmployeeInsuranceNoAndYearAndLeaveType(
            @Param("insuranceNo") Integer insuranceNo,
            @Param("year") Integer year,
            @Param("leaveTypeId") Long leaveTypeId);
    
}
