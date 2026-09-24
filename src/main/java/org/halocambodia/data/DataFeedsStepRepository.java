package org.halocambodia.data;

import java.util.List;
import java.util.Optional;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;

public interface DataFeedsStepRepository  extends JpaRepository<DataFeedsStep, Long>, JpaSpecificationExecutor<DataFeedsStep> {


}
