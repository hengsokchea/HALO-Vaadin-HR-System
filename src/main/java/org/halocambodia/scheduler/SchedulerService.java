package org.halocambodia.scheduler;

import jakarta.persistence.EntityManager;
import jakarta.persistence.PersistenceContext;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.transaction.support.TransactionTemplate;

import java.time.Instant;
import java.time.OffsetDateTime;
import java.time.ZoneId;
import java.util.*;
import java.util.concurrent.atomic.AtomicInteger;

@Service
public class SchedulerService {

    private static final Logger logger = LoggerFactory.getLogger(SchedulerService.class);
    private static final ZoneId PHNOM_PENH_ZONE = ZoneId.of("Asia/Phnom_Penh");
    
    // Configurable batch size with defaults
    @Value("${scheduler.batch.size:1000}")
    private int BATCH_SIZE;
    
    @Value("${scheduler.max.retries:2}")
    private int MAX_RETRIES;
    
    @Value("${scheduler.retry.delay.ms:2000}")
    private long RETRY_DELAY_MS;
    
    @Value("${scheduler.max.failures:10}")
    private int MAX_CONSECUTIVE_FAILURES;
    
    @Value("${scheduler.memory.check.interval:30000}")
    private long MEMORY_CHECK_INTERVAL_MS;
    
    @Value("${scheduler.query.limit:1000}")
    private int QUERY_LIMIT;

    private static final int MAX_STEP_RECORDS = 1000;

    @PersistenceContext
    private EntityManager entityManager;

    @Autowired
    private LogScheduleService logScheduleService;

    @Autowired
    private TransactionTemplate transactionTemplate;
    
    private int consecutiveFailures = 0;
    private String lastError = null;
    private long lastMemoryCheck = System.currentTimeMillis();
    
    // Global counters using AtomicInteger for thread safety
    private final AtomicInteger totalProcessed = new AtomicInteger(0);
    private final AtomicInteger totalFailed = new AtomicInteger(0);

    /**
     * Main entry point for running imports
     */
    public void runImport() {
        // Reset counters at the start of each run
        totalProcessed.set(0);
        totalFailed.set(0);
        
        logger.info("Starting import at: {}", OffsetDateTime.now(PHNOM_PENH_ZONE));
        logger.info("Configuration: BATCH_SIZE={}, MAX_RETRIES={}, QUERY_LIMIT={}", 
                BATCH_SIZE, MAX_RETRIES, QUERY_LIMIT);

        try {
            List<Object[]> feedsToProcess = getFeedsToProcess();
            
            if (feedsToProcess.isEmpty()) {
                logger.info("No data feeds to process");
                return;
            }

            for (Object[] feed : feedsToProcess) {
                Integer dataFeedId = (Integer) feed[0];
                String firstQuery = (String) feed[1];
                Integer newBatchId = (Integer) feed[2];
                String dataFeedName = (String) feed[3];
                Integer frequencyValue = (Integer) feed[4];

                logger.info("Processing feed: {} (ID: {}, Batch: {})", dataFeedName, dataFeedId, newBatchId);
                processDataFeed(dataFeedId, firstQuery, newBatchId, dataFeedName);
                
                // Clean up after each feed
                entityManager.clear();
                entityManager.getEntityManagerFactory().getCache().evictAll();
                checkMemoryAndGC();
            }
        } catch (Exception e) {
            logger.error("Error in runImport: {}", e.getMessage(), e);
        } finally {
            // Final cleanup
            entityManager.clear();
            entityManager.getEntityManagerFactory().getCache().evictAll();
            System.gc();
            logger.info("Import completed. Total processed: {}, Total failed: {}", 
                    totalProcessed.get(), totalFailed.get());
        }
    }

    /**
     * Get feeds that need to be processed
     */
    @SuppressWarnings("unchecked")
    private List<Object[]> getFeedsToProcess() {
        return entityManager.createNativeQuery("""
            WITH feeds_to_update AS (
                SELECT data_feed_id, COALESCE(last_batch_id, 0) + 1 AS new_batch_id
                FROM core_system.data_feed
                WHERE is_active = true
                AND (
                    last_run IS NULL 
                    OR last_run + (frequency_value * INTERVAL '1 minute') <= 
                        CURRENT_TIMESTAMP AT TIME ZONE 'Asia/Phnom_Penh'
                )
                AND CAST(CURRENT_TIMESTAMP AT TIME ZONE 'Asia/Phnom_Penh' AS time) 
                    BETWEEN schedule_start_time AND schedule_end_time
                FOR UPDATE SKIP LOCKED
            )
            UPDATE core_system.data_feed df
            SET 
                last_run = CURRENT_TIMESTAMP AT TIME ZONE 'Asia/Phnom_Penh',
                last_batch_id = fu.new_batch_id
            FROM feeds_to_update fu
            WHERE df.data_feed_id = fu.data_feed_id
            RETURNING 
                df.data_feed_id, 
                df.first_query, 
                fu.new_batch_id,
                df.data_feed_name,
                df.frequency_value
        """).getResultList();
    }

    /**
     * Process a single data feed
     */
    @SuppressWarnings("unchecked")
    private void processDataFeed(Integer dataFeedID, String firstQuery, Integer newBatchId, String dataFeedName) {
        long startTime = System.currentTimeMillis();

        try {
            consecutiveFailures = 0;
            lastError = null;

            List<String> parentIds = executeFirstQueryPaginated(firstQuery);
            
            if (parentIds.isEmpty()) {
                logSuccess(dataFeedID, newBatchId, startTime, 0);
                return;
            }

            logger.info("Found {} parent IDs for feed: {}", parentIds.size(), dataFeedName);

            List<Object[]> stepQueries = getStepQueries(dataFeedID);
            
            if (stepQueries.isEmpty()) {
                logger.warn("No active steps found for feed ID: {}", dataFeedID);
                logSuccess(dataFeedID, newBatchId, startTime, 0);
                return;
            }

            // Process parent IDs in batches
            processParentIdsInBatches(dataFeedID, parentIds, stepQueries, newBatchId, startTime);
            
            // Clear parentIds to free memory
            parentIds.clear();

        } catch (Exception e) {
            logger.error("Error processing feed ID {}: {}", dataFeedID, e.getMessage(), e);
            logError(dataFeedID, newBatchId, startTime, e);
        }
    }

    /**
     * Execute first query with pagination - handles semicolons
     */
    @SuppressWarnings("unchecked")
    private List<String> executeFirstQueryPaginated(String firstQuery) {
        try {
            // Clean the query
            String cleanedQuery = firstQuery.trim();
            
            // Remove trailing semicolon if present
            if (cleanedQuery.endsWith(";")) {
                cleanedQuery = cleanedQuery.substring(0, cleanedQuery.length() - 1);
            }
            
            // Remove any semicolon followed by spaces
            cleanedQuery = cleanedQuery.replaceAll(";\\s*$", "");
            
            // Check if query already has LIMIT
            if (!cleanedQuery.toLowerCase().contains("limit")) {
                cleanedQuery = cleanedQuery + " LIMIT " + QUERY_LIMIT;
            }
            
            logger.debug("Executing first query: {}", cleanedQuery);
            return entityManager.createNativeQuery(cleanedQuery).getResultList();
        } catch (Exception e) {
            logger.error("First query failed: {}", e.getMessage(), e);
            throw new RuntimeException("First query failed", e);
        }
    }

    /**
     * Get step queries for a data feed
     */
    @SuppressWarnings("unchecked")
    private List<Object[]> getStepQueries(Integer dataFeedID) {
        return entityManager.createNativeQuery("""
            SELECT step_number, step_query
            FROM core_system.data_feed_step
            WHERE data_feed_id = ?1
            AND is_active = true
            ORDER BY step_number
        """).setParameter(1, dataFeedID).getResultList();
    }

    /**
     * Process parent IDs in batches - MAIN FUNCTION
     * This function processes parent IDs in configurable batch sizes with retry logic
     */
    private void processParentIdsInBatches(Integer dataFeedID, List<String> parentIds,
                                            List<Object[]> stepQueries, Integer newBatchId, long startTime) {
        int totalParents = parentIds.size();
        int batchSuccessTotal = 0;
        int batchFailedTotal = 0;
        long totalTime = 0;

        consecutiveFailures = 0;
        lastError = null;

        // Calculate total number of batches
        int totalBatches = (totalParents + BATCH_SIZE - 1) / BATCH_SIZE;
        logger.info("Starting batch processing: {} parents in {} batches of size {}", 
                totalParents, totalBatches, BATCH_SIZE);

        // Process in batches
        for (int i = 0; i < parentIds.size(); i += BATCH_SIZE) {
            int batchEnd = Math.min(i + BATCH_SIZE, parentIds.size());
            List<String> batch = parentIds.subList(i, batchEnd);
            int batchNumber = (i / BATCH_SIZE) + 1;
            
            logger.info("Processing batch {} of {} ({} parents, indices {}-{})", 
                    batchNumber, totalBatches, batch.size(), i, batchEnd - 1);

            long batchStartTime = System.currentTimeMillis();
            BatchResult batchResult = processBatchWithRetry(dataFeedID, batch, stepQueries, newBatchId, batchNumber);
            long batchDuration = System.currentTimeMillis() - batchStartTime;

            batchSuccessTotal += batchResult.successCount;
            batchFailedTotal += batchResult.failCount;
            totalTime += batchDuration;

            // Update global counters (AtomicInteger)
            totalProcessed.addAndGet(batchResult.successCount);
            totalFailed.addAndGet(batchResult.failCount);

            logger.info("Batch {} completed: {} successful, {} failed in {}ms", 
                    batchNumber, batchResult.successCount, batchResult.failCount, batchDuration);

            // Reset circuit breaker on success
            if (batchResult.successCount > 0) {
                consecutiveFailures = 0;
                lastError = null;
            }

            // Log batch summary
            logBatchSummary(dataFeedID, newBatchId, batchNumber, batchResult, batchStartTime, batchDuration);
            
            // Clear batch list to free memory
            batch.clear();
            
            // Clear persistence context after each batch
            entityManager.clear();
            
            // Check memory after each batch
            checkMemoryAndGC();
            
            // Log progress
            int processedSoFar = batchSuccessTotal + batchFailedTotal;
            double progressPercent = (double) processedSoFar / totalParents * 100;
            logger.info("Progress: {}/{} parents processed ({}%)", 
                    processedSoFar, totalParents, String.format("%.2f", progressPercent));
        }

        // Log overall summary with metrics
        logOverallSummary(dataFeedID, newBatchId, totalParents, batchSuccessTotal, batchFailedTotal, startTime, totalTime);
    }

    /**
     * Process a single batch with retry logic for failed items
     * 
     * @param dataFeedID The data feed ID
     * @param batch The list of parent IDs to process in this batch
     * @param stepQueries The SQL step queries to execute
     * @param newBatchId The new batch ID
     * @param batchNumber The current batch number (for logging)
     * @return BatchResult containing success and failure counts
     */
    private BatchResult processBatchWithRetry(Integer dataFeedID, List<String> batch,
                                               List<Object[]> stepQueries, Integer newBatchId, int batchNumber) {
        // Track parents that need to be retried
        List<String> pendingParents = new ArrayList<>(batch);
        List<String> successfulParents = new ArrayList<>();
        List<String> failedParents = new ArrayList<>();
        int attempt = 1;

        // Keep retrying until all parents are processed or max retries reached
        while (!pendingParents.isEmpty() && attempt <= MAX_RETRIES) {
            if (attempt > 1) {
                logger.info("Batch {}: Retry attempt {} for {} remaining parents", 
                        batchNumber, attempt, pendingParents.size());
                try {
                    // Wait before retry to give database time to recover
                    Thread.sleep(RETRY_DELAY_MS);
                } catch (InterruptedException e) {
                    Thread.currentThread().interrupt();
                    logger.warn("Retry sleep interrupted");
                    break;
                }
            }

            // Process current batch of pending parents
            List<String> currentBatch = new ArrayList<>(pendingParents);
            pendingParents.clear(); // Clear for the next retry iteration

            for (String parentId : currentBatch) {
                try {
                    // Check circuit breaker - prevents cascading failures
                    if (consecutiveFailures >= MAX_CONSECUTIVE_FAILURES) {
                        logger.error("Circuit breaker triggered - {} consecutive failures. Last error: {}. " +
                                "Skipping remaining {} parents in batch {}.", 
                                consecutiveFailures, lastError, currentBatch.size() - successfulParents.size(), batchNumber);
                        // Add all remaining parents to failed list
                        failedParents.addAll(currentBatch);
                        // Break out of the loop
                        break;
                    }
                    
                    // Process the parent through all steps
                    boolean success = processSingleParent(dataFeedID, parentId, stepQueries, newBatchId);
                    
                    if (success) {
                        successfulParents.add(parentId);
                        // Reset circuit breaker on success
                        consecutiveFailures = 0;
                        lastError = null;
                    } else {
                        // Check if we should retry
                        if (attempt < MAX_RETRIES) {
                            pendingParents.add(parentId);
                            logger.debug("Parent {} will be retried (attempt {}/{})", 
                                    parentId, attempt + 1, MAX_RETRIES);
                        } else {
                            failedParents.add(parentId);
                            logger.error("Parent {} failed after {} attempts", parentId, MAX_RETRIES);
                        }
                    }
                } catch (Exception e) {
                    // Handle unexpected errors
                    consecutiveFailures++;
                    lastError = e.getMessage();
                    logger.error("Unexpected error processing parent {}: {}", parentId, e.getMessage());
                    
                    if (attempt < MAX_RETRIES) {
                        pendingParents.add(parentId);
                    } else {
                        failedParents.add(parentId);
                    }
                }
                
                // Clear entity manager after each parent to prevent memory leaks
                entityManager.clear();
            }

            attempt++;
            // Clear currentBatch to free memory
            currentBatch.clear();
        }

        // Log any parents that are still pending (shouldn't happen)
        if (!pendingParents.isEmpty()) {
            logger.warn("Batch {}: {} parents still pending after max retries", batchNumber, pendingParents.size());
            failedParents.addAll(pendingParents);
            pendingParents.clear();
        }

        // Create result
        BatchResult result = new BatchResult(successfulParents.size(), failedParents.size());
        
        // Clear lists to free memory
        successfulParents.clear();
        failedParents.clear();
        
        return result;
    }

    /**
     * Process a single parent ID through all steps
     */
    private boolean processSingleParent(Integer dataFeedID, String parentId,
            List<Object[]> stepQueries, Integer newBatchId) {
        long parentStart = System.currentTimeMillis();
        List<Map<String, Object>> stepRecords = new ArrayList<>();
        boolean hasError = false;
        
        try {
            final List<Map<String, Object>> finalStepRecords = stepRecords;
            
            return transactionTemplate.execute(status -> {
                try {
                    for (Object[] step : stepQueries) {
                        int stepNum = ((Number) step[0]).intValue();
                        String stepSql = (String) step[1];
                        
                        Map<String, Object> stepRecord = executeStep(stepNum, stepSql, parentId, newBatchId);
                        finalStepRecords.add(stepRecord);
                        
                        // Limit step records to prevent memory issues
                        if (finalStepRecords.size() > MAX_STEP_RECORDS) {
                            logger.warn("Step records exceeded limit for parent {}, truncating", parentId);
                            finalStepRecords.clear();
                        }
                    }
                    return true;
                } catch (Exception e) {
                    status.setRollbackOnly();
                    throw e;
                }
            });
        } catch (Exception e) {
            hasError = true;
            int failedStepNum = stepRecords.size() + 1;
            Map<String, Object> stepRecord = createErrorStepRecord(failedStepNum, stepQueries, parentId, newBatchId, e);
            stepRecords.add(stepRecord);
            logger.error("Failed to process parent {} at step {}: {}", 
                    parentId, failedStepNum, e.getMessage());
        } finally {
            // Log with a copy of stepRecords, then clear
            List<Map<String, Object>> copy = new ArrayList<>(stepRecords);
            logParentResult(dataFeedID, parentId, newBatchId, parentStart, copy, hasError);
            stepRecords.clear();
        }
        
        return !hasError;
    }

    /**
     * Execute a single step
     */
    private Map<String, Object> executeStep(int stepNum, String stepSql, String parentId, Integer newBatchId) {
        long stepStart = System.currentTimeMillis();
        
        Map<String, Object> stepRecord = new LinkedHashMap<>();
        stepRecord.put("step", "step_" + stepNum);
        stepRecord.put("PARENT_PRIMARY_KEY", parentId);
        stepRecord.put("sql", stepSql);
        stepRecord.put("batch_id", newBatchId);

        try {
            int affectedRows = entityManager.createNativeQuery(stepSql)
                    .setParameter("PARENT_PRIMARY_KEY", parentId)
                    .executeUpdate();

            long executionTime = System.currentTimeMillis() - stepStart;
            stepRecord.put("execution_time_ms", executionTime);
            stepRecord.put("affected_rows", affectedRows);
            stepRecord.put("status", "success");
            
            logger.debug("Step {} completed: {} rows affected in {}ms", 
                    stepNum, affectedRows, executionTime);
            
            return stepRecord;
        } catch (Exception e) {
            long executionTime = System.currentTimeMillis() - stepStart;
            stepRecord.put("execution_time_ms", executionTime);
            stepRecord.put("status", "failed");
            stepRecord.put("error_message", extractRootCause(e).getMessage());
            stepRecord.put("error_type", e.getClass().getSimpleName());
            logger.error("Step {} failed after {}ms: {}", stepNum, executionTime, e.getMessage());
            throw e;
        }
    }

    /**
     * Create error record for a failed step
     */
    private Map<String, Object> createErrorStepRecord(int stepNum, List<Object[]> stepQueries,
                                                       String parentId, Integer newBatchId, Exception e) {
        Map<String, Object> stepRecord = new LinkedHashMap<>();
        stepRecord.put("step", "step_" + stepNum);
        stepRecord.put("PARENT_PRIMARY_KEY", parentId);
        stepRecord.put("batch_id", newBatchId);
        stepRecord.put("status", "failed");
        stepRecord.put("error_message", extractRootCause(e).getMessage());
        stepRecord.put("error_type", e.getClass().getSimpleName());

        if (stepNum - 1 < stepQueries.size()) {
            stepRecord.put("sql", stepQueries.get(stepNum - 1)[1].toString());
        }

        return stepRecord;
    }

    /**
     * Check memory usage and trigger GC if needed
     */
    private void checkMemoryAndGC() {
        long now = System.currentTimeMillis();
        if (now - lastMemoryCheck < MEMORY_CHECK_INTERVAL_MS) {
            return;
        }
        lastMemoryCheck = now;
        
        Runtime runtime = Runtime.getRuntime();
        long maxMemory = runtime.maxMemory();
        long totalMemory = runtime.totalMemory();
        long freeMemory = runtime.freeMemory();
        long usedMemory = totalMemory - freeMemory;
        double memoryUsagePercent = (double) usedMemory / maxMemory * 100;
        
        // If memory usage > 70%, force GC and clear caches
        if (memoryUsagePercent > 70) {
            logger.warn("High memory usage detected: {}%. Forcing GC and clearing cache.", 
                    String.format("%.2f", memoryUsagePercent));
            entityManager.clear();
            entityManager.getEntityManagerFactory().getCache().evictAll();
            System.gc();
            
            try {
                Thread.sleep(1000); // Give GC time to work
            } catch (InterruptedException ignored) {}
            
            // Log memory after GC
            totalMemory = runtime.totalMemory();
            freeMemory = runtime.freeMemory();
            usedMemory = totalMemory - freeMemory;
            memoryUsagePercent = (double) usedMemory / maxMemory * 100;
            logger.info("Memory after GC: {}/{} MB ({}%)", 
                    usedMemory / (1024 * 1024), 
                    maxMemory / (1024 * 1024),
                    String.format("%.2f", memoryUsagePercent));
        }
    }

    /**
     * Log batch summary
     */
    private void logBatchSummary(Integer dataFeedID, Integer batchId, int batchNumber,
                                  BatchResult result, long startTime, long duration) {
        Map<String, Object> batchLog = new LinkedHashMap<>();
        batchLog.put("batch_number", batchNumber);
        batchLog.put("batch_size", result.successCount + result.failCount);
        batchLog.put("success_count", result.successCount);
        batchLog.put("fail_count", result.failCount);
        batchLog.put("duration_ms", duration);
        batchLog.put("success_rate", String.format("%.2f%%", 
            result.successCount > 0 ? (double) result.successCount / (result.successCount + result.failCount) * 100 : 0));

        logger.info("Batch {} summary: {}", batchNumber, batchLog);
        batchLog.clear();
    }

    /**
     * Log overall summary with metrics
     */
    private void logOverallSummary(Integer dataFeedID, Integer batchId, int totalParents,
                                   int totalSuccess, int totalFailed, long startTime, long totalTime) {
        Map<String, Object> metrics = collectMetrics(totalSuccess, totalFailed, totalTime);
        
        logger.info("=== OVERALL SUMMARY ===");
        logger.info("Feed ID: {}", dataFeedID);
        logger.info("Batch ID: {}", batchId);
        logger.info("Total Parents: {}", totalParents);
        logger.info("Success: {}", totalSuccess);
        logger.info("Failed: {}", totalFailed);
        logger.info("Success Rate: {}%", metrics.get("success_rate"));
        logger.info("Total Time: {}ms", totalTime);
        logger.info("Avg Time/Parent: {}ms", metrics.get("avg_time_per_parent_ms"));
        logger.info("Parents/sec: {}", metrics.get("parents_per_second"));
        logger.info("=========================");

        Map<String, Object> summaryLog = new LinkedHashMap<>();
        summaryLog.put("Overal_Status", totalFailed == 0 ? "success" : "partial_success");
        summaryLog.put("total_parents", totalParents);
        summaryLog.put("success_count", totalSuccess);
        summaryLog.put("fail_count", totalFailed);
        summaryLog.put("success_rate", metrics.get("success_rate"));
        summaryLog.put("total_time_ms", totalTime);
        summaryLog.put("avg_time_per_parent_ms", metrics.get("avg_time_per_parent_ms"));
        summaryLog.put("parents_per_second", metrics.get("parents_per_second"));
        summaryLog.put("batch_size", BATCH_SIZE);
        summaryLog.put("max_retries", MAX_RETRIES);

        logScheduleService.logDataFeedEntry(
            dataFeedID, 
            batchId, 
            1, 
            totalFailed > 0, 
            summaryLog,
            OffsetDateTime.ofInstant(Instant.ofEpochMilli(startTime), PHNOM_PENH_ZONE),
            null
        );
        
        // Clear memory
        summaryLog.clear();
        metrics.clear();
    }

    /**
     * Collect performance metrics
     */
    private Map<String, Object> collectMetrics(int successCount, int failCount, long totalTime) {
        int total = successCount + failCount;
        Map<String, Object> metrics = new LinkedHashMap<>();
        metrics.put("total_parents", total);
        metrics.put("success_count", successCount);
        metrics.put("fail_count", failCount);
        metrics.put("success_rate", String.format("%.2f%%", 
            total > 0 ? (double) successCount / total * 100 : 0));
        metrics.put("total_time_ms", totalTime);
        metrics.put("avg_time_per_parent_ms", String.format("%.2f", 
            total > 0 ? (double) totalTime / total : 0));
        metrics.put("parents_per_second", String.format("%.2f", 
            totalTime > 0 ? (double) total / (totalTime / 1000.0) : 0));
        return metrics;
    }

    /**
     * Log parent result
     */
    private void logParentResult(Integer dataFeedID, String parentId, Integer batchId,
                                  long startTime, List<Map<String, Object>> stepRecords, boolean hasError) {
        long endTime = System.currentTimeMillis();
        Map<String, Object> parentLog = new LinkedHashMap<>();
        parentLog.put("Overal_Status", hasError ? "failed" : "success");
        parentLog.put("PARENT_PRIMARY_KEY", parentId);
        parentLog.put("Overal_process_start_time_ms", startTime);
        parentLog.put("Overal_process_end_time_ms", endTime);
        parentLog.put("Overal_process_duration_ms", endTime - startTime);
        
        if (stepRecords != null && !stepRecords.isEmpty()) {
            parentLog.put("step_sqls", stepRecords);
        }

        logScheduleService.logDataFeedEntry(
            dataFeedID, 
            batchId, 
            1, 
            hasError, 
            parentLog,
            OffsetDateTime.ofInstant(Instant.ofEpochMilli(startTime), PHNOM_PENH_ZONE),
            parentId
        );
        
        // Clear parentLog to free memory
        parentLog.clear();
    }

    /**
     * Log success
     */
    private void logSuccess(Integer dataFeedID, Integer batchId, long startTime, int rowsAffected) {
        long endTime = System.currentTimeMillis();
        Map<String, Object> stepRecord = new LinkedHashMap<>();
        stepRecord.put("step", "step_0");
        stepRecord.put("PARENT_PRIMARY_KEY", null);
        stepRecord.put("sql", null);
        stepRecord.put("batch_id", batchId);
        stepRecord.put("execution_time_ms", endTime - startTime);
        stepRecord.put("affected_rows", rowsAffected > 0 ? rowsAffected : "No results found");
        stepRecord.put("status", "success");

        Map<String, Object> parentLog = new LinkedHashMap<>();
        parentLog.put("Overal_Status", "success");
        parentLog.put("PARENT_PRIMARY_KEY", null);
        parentLog.put("Overal_process_start_time_ms", startTime);
        parentLog.put("Overal_process_end_time_ms", endTime);
        parentLog.put("Overal_process_duration_ms", endTime - startTime);
        parentLog.put("step_sqls", List.of(stepRecord));

        logScheduleService.logDataFeedEntry(
            dataFeedID, 
            batchId, 
            1, 
            false, 
            parentLog,
            OffsetDateTime.ofInstant(Instant.ofEpochMilli(startTime), PHNOM_PENH_ZONE),
            null
        );
        
        parentLog.clear();
        stepRecord.clear();
    }

    /**
     * Log error
     */
    private void logError(Integer dataFeedID, Integer batchId, long startTime, Exception e) {
        long endTime = System.currentTimeMillis();
        Map<String, Object> parentLog = new LinkedHashMap<>();
        parentLog.put("Overal_Status", "failed");
        parentLog.put("PARENT_PRIMARY_KEY", null);
        parentLog.put("Overal_process_start_time_ms", startTime);
        parentLog.put("Overal_process_end_time_ms", endTime);
        parentLog.put("Overal_process_duration_ms", endTime - startTime);
        parentLog.put("error_message", extractRootCause(e).getMessage());
        parentLog.put("error_type", e.getClass().getSimpleName());

        logScheduleService.logDataFeedEntry(
            dataFeedID, 
            batchId, 
            1, 
            true, 
            parentLog,
            OffsetDateTime.ofInstant(Instant.ofEpochMilli(startTime), PHNOM_PENH_ZONE),
            null
        );
        
        parentLog.clear();
    }

    /**
     * Extract root cause from exception chain
     */
    private Throwable extractRootCause(Throwable e) {
        while (e.getCause() != null && e.getCause() != e) {
            e = e.getCause();
        }
        return e;
    }

    /**
     * Inner class to hold batch results
     */
    private static class BatchResult {
        private final int successCount;
        private final int failCount;

        public BatchResult(int successCount, int failCount) {
            this.successCount = successCount;
            this.failCount = failCount;
        }

        public int getSuccessCount() {
            return successCount;
        }

        public int getFailCount() {
            return failCount;
        }
        
        @Override
        public String toString() {
            return String.format("BatchResult{success=%d, failed=%d}", successCount, failCount);
        }
    }
}