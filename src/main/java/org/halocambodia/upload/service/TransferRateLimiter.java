package org.halocambodia.upload.service;

import java.util.concurrent.locks.LockSupport;

import org.springframework.stereotype.Component;

@Component
public class TransferRateLimiter {

    public Session open(long bytesPerSecond) {
        return new Session(bytesPerSecond);
    }

    public static final class Session {

        private final long bytesPerSecond;
        private final long startedAtNanos;
        private long transferred;

        private Session(long bytesPerSecond) {
            this.bytesPerSecond = Math.max(0, bytesPerSecond);
            this.startedAtNanos = System.nanoTime();
        }

        public void afterWrite(int bytes) {
            if (bytesPerSecond <= 0 || bytes <= 0) {
                return;
            }

            transferred += bytes;

            long expectedNanos =
                (long) ((transferred * 1_000_000_000D) / bytesPerSecond);

            long actualNanos = System.nanoTime() - startedAtNanos;
            long delay = expectedNanos - actualNanos;

            if (delay > 0) {
                LockSupport.parkNanos(delay);
            }
        }
    }
}
