package org.halocambodia.gmail;

import org.halocambodia.data.EmployeeLeave;
import org.halocambodia.data.EmployeeLeaveDetail;
import org.halocambodia.services.QRCodeService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.mail.SimpleMailMessage;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.mail.javamail.MimeMessageHelper;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Service;
import org.thymeleaf.TemplateEngine;
import org.thymeleaf.context.Context;

import jakarta.mail.internet.MimeMessage;

import org.springframework.core.io.ByteArrayResource;
import org.springframework.core.io.ClassPathResource;

@Service
public class EmailService {

    private static final Logger log = LoggerFactory.getLogger(EmailService.class);

    private final JavaMailSender mailSender;
    private final TemplateEngine templateEngine;
    
    @Autowired
    private QRCodeService qrCodeService;

    public EmailService(JavaMailSender mailSender, TemplateEngine templateEngine) {
        this.mailSender = mailSender;
        this.templateEngine = templateEngine;
    }

    @Async
    public void sendLeaveEmail(EmployeeLeave leave, String approveUrl, String rejectUrl) {

        try {
            Context context = new Context();
            context.setVariable("employeeName", leave.getEmployee().getNameEn());
            context.setVariable("department", leave.getDepartment().getName());
            
            context.setVariable("contactNumber", leave.getContractNumber());
            
            context.setVariable("annualLeaveAvailabelBalance", leave.getAnnualLeaveAvailableBalance());
            context.setVariable("sickLeaveUsed", leave.getSickLeaveUsed());
            context.setVariable("specialLeaveUsed", leave.getSpecialLeaveUsed());
            context.setVariable("unpaidLeaveUsed", leave.getUnpaidLeaveUsed());
            context.setVariable("paternityLeaveUsed", leave.getPaternityLeaveUsed());
            context.setVariable("maternityLeaveUsed", leave.getMaternityLeaveUsed());
            context.setVariable("compensatoryLeaveRemain", leave.getCompensatoryLeaveRemaining());
            
            
            leave.getEmployeeLeaveDetails().size();
            context.setVariable("leaveDetails",leave.getEmployeeLeaveDetails());
           

            context.setVariable("reason", leave.getReason());
            context.setVariable("approveUrl", approveUrl);
            context.setVariable("rejectUrl", rejectUrl);

            String html = templateEngine.process("email/leave-request", context);

            MimeMessage message = mailSender.createMimeMessage();
            //MimeMessageHelper helper = new MimeMessageHelper(message, true);
            MimeMessageHelper helper = new MimeMessageHelper(message, true, "UTF-8");

            //helper.setTo("heng.sokchea@halocambodia.org");
            helper.setTo(leave.getLineManager().getOfficialEmail().trim());
            helper.setCc("halocambodiasystem@gmail.com");
            helper.setSubject("Leave Request Approval");
            helper.setText(html, true);
            helper.addInline("companyLogo", new ClassPathResource("assets/images/HaloLogoWhite.png") );
            
            byte[] qr = qrCodeService.generateQRCode(approveUrl, 150, 150);

            helper.addInline("qrCode",new ByteArrayResource(qr),"image/png");
            
            log.info("Sending leave approval email for employee {} to manager {}",
                    leave.getEmployee().getNameEn(),
                    leave.getLineManager().getNameEn());

            mailSender.send(message);

            log.info("Leave approval email sent for employee {}", leave.getEmployee().getNameEn());

        } catch (Exception e) {
            log.error("Unable to send leave approval email", e);
        }
    }
    
    @Async
    public void sendLeaveActionResult( EmployeeLeave leave, String action    ) {

        try {
            Context context = new Context();

            context.setVariable("employeeName", leave.getEmployee().getNameEn());
            context.setVariable("department", leave.getDepartment().getName());
            context.setVariable("requestDate", leave.getRequestDate());
            
            context.setVariable("contactNumber", leave.getContractNumber());
            
            context.setVariable("annualLeaveAvailabelBalance", leave.getAnnualLeaveAvailableBalance());
            context.setVariable("sickLeaveUsed", leave.getSickLeaveUsed());
            context.setVariable("specialLeaveUsed", leave.getSpecialLeaveUsed());
            context.setVariable("unpaidLeaveUsed", leave.getUnpaidLeaveUsed());
            context.setVariable("paternityLeaveUsed", leave.getPaternityLeaveUsed());
            context.setVariable("maternityLeaveUsed", leave.getMaternityLeaveUsed());
            context.setVariable("compensatoryLeaveRemain", leave.getCompensatoryLeaveRemaining());
            
            
            leave.getEmployeeLeaveDetails().size();
            context.setVariable("leaveDetails",leave.getEmployeeLeaveDetails());
           

            context.setVariable("reason", leave.getReason());

            context.setVariable("action", action);

            String html =templateEngine.process("email/leave-result-email",context);
            
            MimeMessage message = mailSender.createMimeMessage();            
            MimeMessageHelper helper = new MimeMessageHelper(message, true, "UTF-8");

            helper.setTo(leave.getEmployee().getOfficialEmail());
            helper.setCc("halocambodiasystem@gmail.com");
            
            helper.setSubject("Leave Request " + action);
            helper.setText(html, true); 
            helper.addInline("companyLogo", new ClassPathResource("assets/images/HaloLogoWhite.png") );
            
            log.info("Sending leave result email for employee {} with action {}",
                    leave.getEmployee().getNameEn(), action);

            mailSender.send(message);

            log.info("Leave result email sent for employee {}", leave.getEmployee().getNameEn());

        } catch (Exception e) {
            log.error("Unable to send leave result email", e);
        }
    }
    
    
    /**
     * Sends one employee's payroll payslip as a PDF attachment.
     * This method is intentionally synchronous because Payroll Payments runs it
     * inside the shared ProgressDialog background executor and needs an accurate
     * per-employee success/failure result.
     */
    public void sendPayrollPayslip(
            String recipient,
            String employeeName,
            String payrollPeriod,
            String installmentLabel,
            String fileName,
            byte[] pdfBytes) {

        if (recipient == null || recipient.isBlank()) {
            throw new IllegalArgumentException("Employee personal email is empty.");
        }
        if (pdfBytes == null || pdfBytes.length == 0) {
            throw new IllegalArgumentException("Payslip PDF is empty.");
        }

        try {
            String safeEmployeeName = employeeName == null || employeeName.isBlank()
                    ? "Employee"
                    : employeeName.trim();
            String safePayrollPeriod = payrollPeriod == null ? "" : payrollPeriod.trim();
            String safeInstallmentLabel = installmentLabel == null ? "" : installmentLabel.trim();

            StringBuilder body = new StringBuilder();
            body.append("Dear ").append(safeEmployeeName).append(",\n\n")
                    .append("Please find attached your payroll payslip");
            if (!safePayrollPeriod.isBlank()) {
                body.append(" for ").append(safePayrollPeriod);
            }
            if (!safeInstallmentLabel.isBlank()) {
                body.append(" (").append(safeInstallmentLabel).append(")");
            }
            body.append(".\n\n")
                    .append("សូមពិនិត្យបង្កាន់ដៃប្រាក់បៀវត្សរបស់អ្នកដែលបានភ្ជាប់ជាឯកសារ PDF។\n\n")
                    .append("Regards,\nHR & Payroll");

            MimeMessage message = mailSender.createMimeMessage();
            MimeMessageHelper helper = new MimeMessageHelper(message, true, "UTF-8");
            helper.setTo(recipient.trim());
            helper.setSubject("Payroll Payslip - " + safePayrollPeriod);
            helper.setText(body.toString(), false);
            helper.addAttachment(
                    fileName == null || fileName.isBlank() ? "Payslip.pdf" : fileName,
                    new ByteArrayResource(pdfBytes),
                    "application/pdf");

            mailSender.send(message);
        } catch (Exception ex) {
            throw new IllegalStateException(
                    "Unable to send payroll payslip to " + recipient + ": " + ex.getMessage(),
                    ex);
        }
    }

}