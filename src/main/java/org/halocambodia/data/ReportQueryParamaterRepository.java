package org.halocambodia.data;

import java.util.Optional;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;

public interface ReportQueryParamaterRepository  extends JpaRepository<ReportQueryParamater, Long>, JpaSpecificationExecutor<ReportQueryParamater> {


}
