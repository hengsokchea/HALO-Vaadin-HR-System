package org.halocambodia.data;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.SQLException;
import java.util.HashMap;
import java.util.Locale;
import java.util.Map;

import net.sf.jasperreports.engine.JRDataSource;
import net.sf.jasperreports.engine.JRException;
import net.sf.jasperreports.engine.JRParameter;
import net.sf.jasperreports.engine.JasperCompileManager;
import net.sf.jasperreports.engine.JasperExportManager;
import net.sf.jasperreports.engine.JasperFillManager;
import net.sf.jasperreports.engine.JasperPrint;
import net.sf.jasperreports.engine.JasperReport;
import net.sf.jasperreports.engine.export.ooxml.JRXlsxExporter;
import net.sf.jasperreports.engine.util.JRLoader;
import net.sf.jasperreports.export.SimpleExporterInput;
import net.sf.jasperreports.export.SimpleOutputStreamExporterOutput;

/**
 * Central JasperReports execution service.
 *
 * <p>All Jasper operations are executed with the application class loader as
 * the thread context class loader. This is important when report generation is
 * started from CompletableFuture/executor threads under Tomcat/Vaadin because
 * JasperReports, its font/image extensions and PDF exporter use the thread
 * context class loader to resolve runtime classes/resources.</p>
 */
public final class ReportService {

    private static final ClassLoader APPLICATION_CLASS_LOADER = ReportService.class.getClassLoader();

    private ReportService() {
    }

    public static byte[] generateReportExternal(
            Path templatePath,
            Map<String, Object> parameters) throws JRException {

        if (templatePath == null) {
            throw new JRException("templatePath is null");
        }
        if (!Files.exists(templatePath)) {
            throw new JRException("Template not found: " + templatePath.toAbsolutePath());
        }

        return withApplicationClassLoader(() -> {
            Map<String, Object> params = copyParameters(parameters);
            Path parent = templatePath.toAbsolutePath().getParent();
            if (parent != null) {
                params.put("SUBREPORT_DIR", parent + java.io.File.separator);
            }

            try (Connection conn = getConnection();
                 InputStream in = Files.newInputStream(templatePath);
                 ByteArrayOutputStream out = new ByteArrayOutputStream()) {

                JasperReport report = loadReport(in, templatePath.getFileName().toString());
                JasperPrint print = JasperFillManager.fillReport(report, params, conn);
                JasperExportManager.exportReportToPdfStream(print, out);
                return out.toByteArray();
            } catch (IOException e) {
                throw new JRException("IO error opening template: " + templatePath, e);
            } catch (SQLException e) {
                throw new JRException("SQL Error: " + e.getMessage(), e);
            }
        });
    }

    public static byte[] generateReportExternal(
            Path templatePath,
            Map<String, Object> parameters,
            Path attachmentDir) throws JRException {

        if (templatePath == null) {
            throw new JRException("templatePath is null");
        }
        if (!Files.exists(templatePath)) {
            throw new JRException("Template not found: " + templatePath.toAbsolutePath());
        }

        return withApplicationClassLoader(() -> {
            Map<String, Object> params = copyParameters(parameters);
            Path parent = templatePath.toAbsolutePath().getParent();
            if (parent != null) {
                params.put("SUBREPORT_DIR", parent + java.io.File.separator);
            }

            if (attachmentDir != null) {
                Path absoluteAttachmentDir = attachmentDir.toAbsolutePath();
                params.put("ATTACHMENT_DIR", absoluteAttachmentDir.toString());
                Path logoDir = absoluteAttachmentDir.getParent();
                if (logoDir != null) {
                    params.put("LOGO_DIR", logoDir.toString());
                }
            }

            try (Connection conn = getConnection();
                 InputStream in = Files.newInputStream(templatePath);
                 ByteArrayOutputStream out = new ByteArrayOutputStream()) {

                JasperReport report = loadReport(in, templatePath.getFileName().toString());
                JasperPrint print = JasperFillManager.fillReport(report, params, conn);
                JasperExportManager.exportReportToPdfStream(print, out);
                return out.toByteArray();
            } catch (IOException e) {
                throw new JRException("IO error opening template: " + templatePath, e);
            } catch (SQLException e) {
                throw new JRException("SQL Error: " + e.getMessage(), e);
            }
        });
    }

    /**
     * Generates a PDF from a classpath Jasper template.
     * Supports both .jasper and .jrxml templates.
     */
    public static byte[] generateReport(
            String templatePath,
            Map<String, Object> parameters) throws JRException {

        if (templatePath == null || templatePath.isBlank()) {
            throw new JRException("Report template path is empty");
        }

        return withApplicationClassLoader(() -> {
            Map<String, Object> params = copyParameters(parameters);

            // Optional caller-provided data source. Payroll Payslip uses this,
            // while normal reports continue to use the JDBC connection.
            Object suppliedDataSource = params.remove(JRParameter.REPORT_DATA_SOURCE);

            try (InputStream inputStream = openClasspathResource(templatePath);
                 ByteArrayOutputStream outputStream = new ByteArrayOutputStream()) {

                JasperReport jasperReport = loadReport(inputStream, templatePath);
                JasperPrint jasperPrint;

                if (suppliedDataSource instanceof JRDataSource dataSource) {
                    jasperPrint = JasperFillManager.fillReport(jasperReport, params, dataSource);
                } else {
                    try (Connection conn = getConnection()) {
                        jasperPrint = JasperFillManager.fillReport(jasperReport, params, conn);
                    }
                }

                JasperExportManager.exportReportToPdfStream(jasperPrint, outputStream);
                return outputStream.toByteArray();
            } catch (IOException e) {
                throw new JRException("IO error reading report template: " + templatePath, e);
            } catch (SQLException e) {
                throw new JRException("SQL Error: " + e.getMessage(), e);
            }
        });
    }

    /**
     * Generates XLSX output from a classpath Jasper template.
     * Supports both .jasper and .jrxml templates.
     */
    public static byte[] generateExcelReport(
            String templatePath,
            Map<String, Object> parameters) throws JRException {

        if (templatePath == null || templatePath.isBlank()) {
            throw new JRException("Report template path is empty");
        }

        return withApplicationClassLoader(() -> {
            Map<String, Object> params = copyParameters(parameters);
            Object suppliedDataSource = params.remove(JRParameter.REPORT_DATA_SOURCE);

            try (InputStream inputStream = openClasspathResource(templatePath);
                 ByteArrayOutputStream outputStream = new ByteArrayOutputStream()) {

                JasperReport jasperReport = loadReport(inputStream, templatePath);
                JasperPrint jasperPrint;

                if (suppliedDataSource instanceof JRDataSource dataSource) {
                    jasperPrint = JasperFillManager.fillReport(jasperReport, params, dataSource);
                } else {
                    try (Connection conn = getConnection()) {
                        jasperPrint = JasperFillManager.fillReport(jasperReport, params, conn);
                    }
                }

                JRXlsxExporter exporter = new JRXlsxExporter();
                exporter.setExporterInput(new SimpleExporterInput(jasperPrint));
                exporter.setExporterOutput(new SimpleOutputStreamExporterOutput(outputStream));
                exporter.exportReport();

                return outputStream.toByteArray();
            } catch (IOException e) {
                throw new JRException("IO error reading report template: " + templatePath, e);
            } catch (SQLException e) {
                throw new JRException("SQL Error: " + e.getMessage(), e);
            }
        });
    }

    private static Map<String, Object> copyParameters(Map<String, Object> parameters) {
        Map<String, Object> params = new HashMap<>();
        if (parameters != null) {
            params.putAll(parameters);
        }
        return params;
    }

    private static InputStream openClasspathResource(String templatePath) throws JRException {
        InputStream inputStream = APPLICATION_CLASS_LOADER.getResourceAsStream(templatePath);
        if (inputStream == null) {
            throw new JRException("Report template not found: " + templatePath);
        }
        return inputStream;
    }

    private static JasperReport loadReport(InputStream inputStream, String templateName) throws JRException {
        String name = templateName.toLowerCase(Locale.ROOT);

        if (name.endsWith(".jrxml")) {
            return JasperCompileManager.compileReport(inputStream);
        }
        if (name.endsWith(".jasper")) {
            Object loaded = JRLoader.loadObject(inputStream);
            if (!(loaded instanceof JasperReport jasperReport)) {
                throw new JRException("Template is not a JasperReport: " + templateName);
            }
            return jasperReport;
        }

        throw new JRException(
                "Unsupported template extension (use .jrxml or .jasper): " + templateName);
    }

    /**
     * Executes Jasper work with a stable application context class loader.
     *
     * <p>This is shared by PDF, Excel, embedded, payroll and externally uploaded
     * Jasper reports, so the fix is not limited to one screen/report.</p>
     */
    private static <T> T withApplicationClassLoader(JasperOperation<T> operation) throws JRException {
        Thread thread = Thread.currentThread();
        ClassLoader previous = thread.getContextClassLoader();

        try {
            if (previous != APPLICATION_CLASS_LOADER) {
                thread.setContextClassLoader(APPLICATION_CLASS_LOADER);
            }
            return operation.run();
        } catch (JRException e) {
            throw e;
        } catch (LinkageError e) {
            // Includes NoClassDefFoundError such as java/nio/ByteOrder. Preserve
            // the real cause but convert it to the standard Jasper exception
            // path used by all report UIs.
            throw new JRException(
                    "JasperReports runtime class loading error: " + rootMessage(e), e);
        } catch (Exception e) {
            throw new JRException("Error generating report: " + rootMessage(e), e);
        } finally {
            if (thread.getContextClassLoader() != previous) {
                thread.setContextClassLoader(previous);
            }
        }
    }

    private static String rootMessage(Throwable throwable) {
        Throwable current = throwable;
        while (current.getCause() != null && current.getCause() != current) {
            current = current.getCause();
        }
        String message = current.getMessage();
        return (message == null || message.isBlank())
                ? current.getClass().getName()
                : message;
    }

    @FunctionalInterface
    private interface JasperOperation<T> {
        T run() throws Exception;
    }

    private static Connection getConnection() throws SQLException {
        String url = "jdbc:postgresql://192.168.10.9:5432/cam_hr_live";
        String username = "cam_hr_admin";
        String password = "cAm$29Hr$22AdMiN";
        return DriverManager.getConnection(url, username, password);
    }
}
