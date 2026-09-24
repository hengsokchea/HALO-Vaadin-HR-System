package org.halocambodia.utility;

import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.RejectedExecutionException;
import java.util.concurrent.ThreadFactory;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicInteger;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

import jakarta.annotation.PreDestroy;

/**
 * Lifecycle-managed executor for JasperReports work.
 *
 * <p>Do not create static, unmanaged executors inside Vaadin views. In an
 * external Tomcat deployment those threads can outlive a WAR reload/redeploy
 * and retain the stopped WebappClassLoader. The next class/resource lookup
 * then fails with Tomcat's "web application instance has been stopped already"
 * error.</p>
 *
 * <p>This component owns the report worker threads and waits for active report
 * generation to finish while the Spring web application context is shutting
 * down, before Tomcat invalidates the web application class loader.</p>
 */
@Component
public final class ReportExecutorManager {

    private static final Logger log = LoggerFactory.getLogger(ReportExecutorManager.class);
    private static final AtomicInteger THREAD_SEQUENCE = new AtomicInteger();
    private static volatile ReportExecutorManager instance;

    private final AtomicBoolean stopping = new AtomicBoolean(false);
    private final ExecutorService executor;

    public ReportExecutorManager() {
        int processors = Runtime.getRuntime().availableProcessors();
        int poolSize = Math.max(2, Math.min(4, processors));
        ClassLoader applicationClassLoader = ReportExecutorManager.class.getClassLoader();

        ThreadFactory threadFactory = task -> {
            Thread thread = new Thread(
                    task,
                    "jasper-report-" + THREAD_SEQUENCE.incrementAndGet());
            thread.setDaemon(false);
            thread.setContextClassLoader(applicationClassLoader);
            return thread;
        };

        this.executor = Executors.newFixedThreadPool(poolSize, threadFactory);
        instance = this;
        log.info("Jasper report executor started with {} worker(s)", poolSize);
    }

    /**
     * Returns the executor belonging to the currently running web application.
     */
    public static ExecutorService executor() {
        ReportExecutorManager current = instance;
        if (current == null || current.stopping.get()) {
            throw new RejectedExecutionException(
                    "The application is stopping; new report generation is not accepted.");
        }
        return current.executor;
    }

    /**
     * Spring invokes this before the web application class loader is stopped.
     */
    @PreDestroy
    public void shutdown() {
        if (!stopping.compareAndSet(false, true)) {
            return;
        }

        // Prevent new work from finding this executor while shutdown is in progress.
        instance = null;

        log.info("Stopping Jasper report executor; waiting for active reports to finish");
        executor.shutdown();

        try {
            // Give active Jasper/PDF work time to finish while the webapp classloader
            // is still valid. This is intentionally longer than a normal UI timeout.
            if (!executor.awaitTermination(120, TimeUnit.SECONDS)) {
                log.warn("Jasper report executor did not stop within 120 seconds; interrupting remaining tasks");
                executor.shutdownNow();

                if (!executor.awaitTermination(10, TimeUnit.SECONDS)) {
                    log.warn("Jasper report executor still has tasks after forced shutdown");
                }
            }
        } catch (InterruptedException ex) {
            Thread.currentThread().interrupt();
            executor.shutdownNow();
            log.warn("Interrupted while stopping Jasper report executor", ex);
        }
    }
}
