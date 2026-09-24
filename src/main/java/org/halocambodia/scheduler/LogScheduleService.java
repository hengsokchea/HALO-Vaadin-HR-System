package org.halocambodia.scheduler;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import jakarta.persistence.EntityManager;
import jakarta.persistence.PersistenceContext;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.dao.DataAccessException;
import org.springframework.retry.annotation.Backoff;
import org.springframework.retry.annotation.Retryable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.OffsetDateTime;
import java.util.Map;

@Service
public class LogScheduleService {

    private static final Logger logger = LoggerFactory.getLogger(LogScheduleService.class);
    private static final ObjectMapper objectMapper = new ObjectMapper();

    @PersistenceContext
    private EntityManager entityManager;

    /**
     * Log data feed entry - WITH RETRY for transient failures
     */
    @Transactional
    @Retryable(
        value = {DataAccessException.class, jakarta.persistence.PersistenceException.class},
        maxAttempts = 3,
        backoff = @Backoff(delay = 1000, multiplier = 2)
    )
    public void logDataFeedEntry(Integer dataFeedId, Integer batchId, Integer createdBy, 
                                  Boolean hasErrors, Map<String, Object> data, 
                                  OffsetDateTime runStart, String parentPrimaryKey) {
        try {
            // Convert Map to proper JSON string using Jackson
            String jsonData = "{}";
            if (data != null && !data.isEmpty()) {
                try {
                    jsonData = objectMapper.writeValueAsString(data);
                } catch (JsonProcessingException e) {
                    logger.error("Failed to serialize data to JSON: {}", e.getMessage());
                    jsonData = String.format("{\"error\":\"%s\"}", escapeJson(e.getMessage()));
                }
            }
            
            String sql = """
                INSERT INTO core_system.data_feed_log
                (data_feed_id, batch_id, data, created_by, has_errors, run_start, run_end, parent_primary_key)
                VALUES (?, ?, CAST(? AS jsonb), ?, ?, ?, ?, CAST(? AS UUID))
            """;

            entityManager.createNativeQuery(sql)
                .setParameter(1, dataFeedId)
                .setParameter(2, batchId)
                .setParameter(3, jsonData)
                .setParameter(4, createdBy)
                .setParameter(5, hasErrors != null && hasErrors)
                .setParameter(6, runStart)
                .setParameter(7, OffsetDateTime.now())
                .setParameter(8, parentPrimaryKey)
                .executeUpdate();

            if (parentPrimaryKey != null) {
                logger.debug("Successfully logged for parent ID {}", parentPrimaryKey);
            } else {
                logger.debug("Successfully logged summary for feed ID {}", dataFeedId);
            }

        } catch (Exception e) {
            // Log error but DON'T rethrow - this prevents transaction rollback
            logger.error("CRITICAL LOGGING ERROR: Failed to log data for parent {}. Error: {}", 
                parentPrimaryKey, e.getMessage(), e);
        }
    }

    private String escapeJson(String s) {
        if (s == null) return "";
        return s.replace("\\", "\\\\")
                .replace("\"", "\\\"")
                .replace("\n", "\\n")
                .replace("\r", "\\r")
                .replace("\t", "\\t");
    }
}