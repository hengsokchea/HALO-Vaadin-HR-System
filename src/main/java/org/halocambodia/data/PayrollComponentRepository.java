package org.halocambodia.data;

import java.util.List;
import java.util.Optional;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;

public interface PayrollComponentRepository
        extends JpaRepository<PayrollComponent, Long>, JpaSpecificationExecutor<PayrollComponent> {

    List<PayrollComponent> findAllByOrderBySortOrderAscComponentCodeAsc();

    List<PayrollComponent> findByActiveTrueOrderBySortOrderAscComponentCodeAsc();

    Optional<PayrollComponent> findByComponentCodeIgnoreCase(String componentCode);
}
