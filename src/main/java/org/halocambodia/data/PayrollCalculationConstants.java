package org.halocambodia.data;

/**
 * Payroll-wide calculation constants.
 *
 * The payroll policy uses a fixed 22-workday divisor for monthly salary and
 * policy day-rate calculations. Keep this value in one place so the
 * calculation repositories cannot silently drift apart.
 */
public final class PayrollCalculationConstants {

    public static final int STANDARD_MONTH_WORKDAYS = 22;

    private PayrollCalculationConstants() {
        // Utility class.
    }
}
