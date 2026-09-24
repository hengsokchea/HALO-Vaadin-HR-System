package org.halocambodia.views;

import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.function.Consumer;
import java.util.function.Supplier;

import org.springframework.security.concurrent.DelegatingSecurityContextExecutorService;

import com.vaadin.flow.component.UI;
import com.vaadin.flow.component.html.Span;
import com.vaadin.flow.component.icon.Icon;
import com.vaadin.flow.component.icon.VaadinIcon;
import com.vaadin.flow.component.orderedlayout.FlexComponent;
import com.vaadin.flow.component.orderedlayout.VerticalLayout;
import com.vaadin.flow.component.progressbar.ProgressBar;

/**
 * Shared application progress dialog for long-running View actions.
 *
 * <p>The dialog uses the same CustomDialog / PreviewReport presentation and
 * provides a reusable {@link #runAsync(String, String, Supplier, Consumer,
 * Consumer)} helper. Background work runs outside the Vaadin UI thread so the
 * progress dialog can render immediately. Spring Security's context is
 * propagated automatically to the worker thread.</p>
 */
public final class ProgressDialog extends CustomDialog {

    private static final String DEFAULT_WIDTH =
            "min(640px, calc(100vw - 32px))";

    private static final String DEFAULT_HEIGHT = "320px";

    private static final ExecutorService BACKGROUND_EXECUTOR =
            new DelegatingSecurityContextExecutorService(
                    Executors.newVirtualThreadPerTaskExecutor());

    private boolean allowClose;

    public ProgressDialog(String title, String message) {
        super(title);
        configureDialog();
        buildLoadingContent(message);
    }

    private void configureDialog() {
        setWidth(DEFAULT_WIDTH);
        setHeight(DEFAULT_HEIGHT);
        setMaxWidth("calc(100vw - 32px)");
        setMaxHeight("calc(100dvh - 32px)");

        setCloseOnOutsideClick(false);
        setCloseOnEsc(false);

        // Reuse the same dialog styling as PreviewReport / Print Payslip.
        addClassName("preview-report-dialog");

        UI.getCurrent().getPage().retrieveExtendedClientDetails(details -> {
            if (details.getBodyClientWidth() < 768) {
                setWidth("calc(100vw - 16px)");
                setMaxWidth("calc(100vw - 16px)");
                setHeight("300px");
            }
        });
    }

    private void buildLoadingContent(String message) {
        VerticalLayout loadingLayout = new VerticalLayout();
        loadingLayout.setSizeFull();
        loadingLayout.setPadding(true);
        loadingLayout.setSpacing(true);
        loadingLayout.setAlignItems(FlexComponent.Alignment.CENTER);
        loadingLayout.setJustifyContentMode(
                FlexComponent.JustifyContentMode.CENTER);

        loadingLayout.getStyle()
                .set("background", "var(--lumo-base-color)");

        Icon spinner = VaadinIcon.SPINNER.create();
        spinner.setSize("70px");
        spinner.getStyle()
                .set("animation", "spin 1s linear infinite")
                .set("color", "var(--lumo-primary-color)");

        ProgressBar progressBar = new ProgressBar();
        progressBar.setIndeterminate(true);
        progressBar.setWidth("min(400px, calc(100% - 32px))");
        progressBar.setHeight("20px");

        Span loadingText = new Span(
                message == null || message.isBlank()
                        ? "Processing... | កំពុងដំណើរការ..."
                        : message);

        loadingText.getStyle()
                .set("font-size", "var(--lumo-font-size-m)")
                .set("color", "var(--lumo-secondary-text-color)")
                .set("white-space", "normal")
                .set("line-height", "1.45")
                .set("text-align", "center")
                .set("max-width", "520px");

        ensureSpinnerAnimation();

        loadingLayout.add(spinner, progressBar, loadingText);
        add(loadingLayout);
    }

    private static void ensureSpinnerAnimation() {
        UI.getCurrent().getPage().executeJs(
                "if (!document.getElementById('spin-animation')) {"
                        + " const style = document.createElement('style');"
                        + " style.id = 'spin-animation';"
                        + " style.textContent = "
                        + "'@keyframes spin { 0% { transform: rotate(0deg); } "
                        + "100% { transform: rotate(360deg); } }';"
                        + " document.head.appendChild(style);"
                        + "}");
    }

    /**
     * Close the dialog after the background operation has completed.
     */
    public void finish() {
        allowClose = true;
        super.close();
    }

    /**
     * Prevent the inherited Close button from dismissing an operation while it
     * is still running. Minimize/maximize from CustomDialog remains available.
     */
    @Override
    public void close() {
        if (allowClose) {
            super.close();
        }
    }

    /**
     * Run a long operation with the shared progress dialog.
     *
     * <p>The authenticated Spring Security context is propagated to the worker
     * thread. Success/error callbacks always execute on the Vaadin UI thread.
     * The dialog is closed automatically after either callback.</p>
     */
    public static <T> void runAsync(
            String title,
            String message,
            Supplier<T> task,
            Consumer<T> onSuccess,
            Consumer<Exception> onError) {

        UI ui = UI.getCurrent();
        if (ui == null) {
            throw new IllegalStateException(
                    "ProgressDialog.runAsync must be called from a Vaadin UI thread.");
        }

        ProgressDialog progress = new ProgressDialog(title, message);
        progress.open();

        CompletableFuture
                .supplyAsync(task, BACKGROUND_EXECUTOR)
                .whenComplete((result, failure) -> {
                    try {
                        ui.access(() -> {
                            try {
                                if (failure != null) {
                                    if (onError != null) {
                                        onError.accept(asException(failure));
                                    }
                                } else if (onSuccess != null) {
                                    onSuccess.accept(result);
                                }
                            } finally {
                                progress.finish();
                            }
                        });
                    } catch (Exception ignore) {
                        // The user may have navigated away while the task ran.
                    }
                });
    }

    /**
     * Convenience overload for actions that do not return a value.
     */
    public static void runAsync(
            String title,
            String message,
            Runnable task,
            Runnable onSuccess,
            Consumer<Exception> onError) {

        runAsync(
                title,
                message,
                () -> {
                    task.run();
                    return null;
                },
                ignored -> {
                    if (onSuccess != null) {
                        onSuccess.run();
                    }
                },
                onError);
    }

    /**
     * Run an existing UI-mutating task after the progress dialog has had a
     * chance to render. Use this for legacy View refresh methods that both
     * load data and update Vaadin components in the same method.
     *
     * <p>The task itself runs inside {@link UI#access(com.vaadin.flow.server.Command)}
     * so Grid/component mutations remain on the Vaadin UI thread. The short
     * background hand-off lets the current request finish first, allowing the
     * loading dialog to become visible before the refresh starts.</p>
     */
    public static void runUiTask(
            String title,
            String message,
            Runnable uiTask,
            Runnable onSuccess,
            Consumer<Exception> onError) {

        UI ui = UI.getCurrent();
        if (ui == null) {
            throw new IllegalStateException(
                    "ProgressDialog.runUiTask must be called from a Vaadin UI thread.");
        }

        ProgressDialog progress = new ProgressDialog(title, message);
        progress.open();

        CompletableFuture
                .runAsync(() -> {
                    try {
                        // Give the current Vaadin request time to flush the
                        // opened dialog to the browser before the UI refresh
                        // acquires the UI lock.
                        Thread.sleep(120);
                    } catch (InterruptedException ex) {
                        Thread.currentThread().interrupt();
                    }

                    try {
                        ui.access(() -> {
                            try {
                                uiTask.run();
                                if (onSuccess != null) {
                                    onSuccess.run();
                                }
                            } catch (Throwable failure) {
                                if (onError != null) {
                                    onError.accept(asException(failure));
                                }
                            } finally {
                                progress.finish();
                            }
                        });
                    } catch (Exception ignore) {
                        // The user may have navigated away while waiting.
                    }
                }, BACKGROUND_EXECUTOR);
    }

    private static Exception asException(Throwable failure) {
        Throwable current = failure;
        while (current.getCause() != null && current.getCause() != current) {
            current = current.getCause();
        }

        return current instanceof Exception exception
                ? exception
                : new RuntimeException(current);
    }
}
