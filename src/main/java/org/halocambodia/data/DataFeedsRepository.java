package org.halocambodia.data;

import java.util.List;
import java.util.Optional;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;
import org.springframework.data.jpa.repository.Query;

public interface DataFeedsRepository  extends JpaRepository<DataFeeds, Long>, JpaSpecificationExecutor<DataFeeds> {

	@Query(value = """
		    SELECT
		        data_feed_id,
		        COUNT(DISTINCT COALESCE(parent_primary_key::text,
		                                data_feed_log_id::text))
		    FROM core_system.data_feed_log
		    WHERE has_errors = true
		    GROUP BY data_feed_id
		    """, nativeQuery = true)
		List<Object[]> getErrorCounts();
}
