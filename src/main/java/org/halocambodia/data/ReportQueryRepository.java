package org.halocambodia.data;

import java.util.Optional;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;

public interface ReportQueryRepository  extends JpaRepository<ReportQuery, Long>, JpaSpecificationExecutor<ReportQuery> {
	Optional<ReportQuery> findByReportSlug(String name);
	long countByReportSlugIgnoreCaseAndIdNot(String name, Long id);

	long countByReportSlugIgnoreCase(String name);

}
