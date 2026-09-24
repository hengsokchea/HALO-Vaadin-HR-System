package org.halocambodia.data;

import java.util.List;
import java.util.UUID;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

public interface DataFeedsLogRepository extends JpaRepository<DataFeedsLog, Long>, JpaSpecificationExecutor<DataFeedsLog> {
	
    @Query("SELECT COUNT(DISTINCT l.parentPrimaryKey ) " +
            "FROM DataFeedsLog l " +
            "WHERE l.hasErrors = true " +
            "AND l.parentPrimaryKey IS NOT NULL " +
            "AND l.dataFeeds.runInBackground = true")
     long countLogsWithErrorsAndParentAndActiveFeed();

    @Query(value = """
        WITH grouped AS (
            SELECT 
                COALESCE(parent_primary_key::text, data_feed_log_id::text) as group_key,
                MAX(run_start) as latest_run_start
            FROM core_system.data_feed_log
            WHERE data_feed_id = :feedId
            AND (
                :mode = 'ALL' 
                OR (:mode = 'ERROR' AND has_errors = true)
                OR (:mode = 'OK' AND (has_errors IS NULL OR has_errors = false))
            )
            GROUP BY COALESCE(parent_primary_key::text, data_feed_log_id::text)
            ORDER BY latest_run_start DESC
            OFFSET :offset
            LIMIT :limit
        )
        SELECT l.*
        FROM core_system.data_feed_log l
        INNER JOIN grouped g ON (
            COALESCE(l.parent_primary_key::text, l.data_feed_log_id::text) = g.group_key
            AND l.run_start = g.latest_run_start
        )
        WHERE l.data_feed_id = :feedId
        ORDER BY l.run_start DESC
    """, nativeQuery = true)
    List<DataFeedsLog> findLatestPerGroup(
        @Param("feedId") long feedId,
        @Param("mode") String mode,
        @Param("limit") int limit,
        @Param("offset") int offset
    );

    @Query(value = """
        SELECT COUNT(DISTINCT COALESCE(parent_primary_key::text, data_feed_log_id::text))
        FROM core_system.data_feed_log
        WHERE data_feed_id = :feedId
        AND (
            :mode = 'ALL' 
            OR (:mode = 'ERROR' AND has_errors = true)
            OR (:mode = 'OK' AND (has_errors IS NULL OR has_errors = false))
        )
    """, nativeQuery = true)
    long countGroups(@Param("feedId") long feedId, @Param("mode") String mode);

    @Query(value = """
        SELECT COUNT(*)
        FROM core_system.data_feed_log
        WHERE data_feed_id = :feedId
        AND parent_primary_key = :parentPrimaryKey
    """, nativeQuery = true)
    long countByParentKey(@Param("feedId") Long feedId, @Param("parentPrimaryKey") UUID parentPrimaryKey);

    @Query(value = """
        SELECT COUNT(*)
        FROM core_system.data_feed_log
        WHERE data_feed_id = :feedId
        AND parent_primary_key IS NULL
        AND data_feed_log_id = :selfId
    """, nativeQuery = true)
    long countBySelfId(@Param("feedId") Long feedId, @Param("selfId") Long selfId);
    
    /**
     * Count distinct parent keys with errors for a feed
     * Used by the UI for error count display
     */
    @Query(value = """
        SELECT COUNT(DISTINCT COALESCE(parent_primary_key::text, data_feed_log_id::text))
        FROM core_system.data_feed_log
        WHERE data_feed_id = :feedId
        AND has_errors = true
    """, nativeQuery = true)
    Long countErrorsByFeedId(@Param("feedId") Long feedId);
}