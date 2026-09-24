package org.halocambodia.gmail;

import org.halocambodia.data.EmployeeLeave;
import org.halocambodia.data.EmployeeLeaveDetail;
import org.halocambodia.services.QRCodeService;
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
            
            System.out.println("==================================");
            System.out.println("Sending Leave Email");
            System.out.println("Employee : " + leave.getEmployee().getNameEn());
            System.out.println("Manager  : " + leave.getLineManager().getNameEn());
            System.out.println("Email    : " + leave.getLineManager().getOfficialEmail());
            System.out.println("Approve  : " + approveUrl);
            System.out.println("==================================");

            mailSender.send(message);
            
            System.out.println("SUCCESS: " + leave.getLineManager().getOfficialEmail());

        } catch (Exception e) {
            System.err.println("FAILED: " + leave.getLineManager().getOfficialEmail());
            e.printStackTrace();
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
            
            System.out.println("==================================");
            System.out.println("Sending Leave Email");
            System.out.println("Employee : " + leave.getEmployee().getNameEn());
            System.out.println("Manager  : " + leave.getLineManager().getNameEn());
            System.out.println("Email    : " + leave.getLineManager().getOfficialEmail());
            System.out.println("==================================");

            mailSender.send(message);
            
            System.out.println("SUCCESS: " + leave.getLineManager().getOfficialEmail());

        } catch (Exception e) {

            System.err.println("FAILED: " + leave.getLineManager().getOfficialEmail());
            e.printStackTrace();
        }
    }
    
    
}