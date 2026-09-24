package org.halocambodia.services;

import java.time.LocalDateTime;

import org.halocambodia.data.EmployeeLeave;
import org.halocambodia.data.LeaveActionAudit;
import org.halocambodia.data.LeaveActionAuditRepository;
import org.halocambodia.data.LeaveActionToken;
import org.springframework.stereotype.Service;

import jakarta.servlet.http.HttpServletRequest;

@Service
public class LeaveAuditService {

    private final LeaveActionAuditRepository repo;

    public LeaveAuditService( LeaveActionAuditRepository repo  ) {
        this.repo = repo;
    }

    public void log(HttpServletRequest request,LeaveActionToken token, EmployeeLeave leave, boolean success ) {

        String userAgent =request.getHeader("User-Agent");

        LeaveActionAudit audit =new LeaveActionAudit();

        audit.setActionType(token.getActionType());

        audit.setIpAddress(getClientIp(request));

        audit.setUserAgent(userAgent);

        audit.setBrowser(detectBrowser(userAgent));

        audit.setOperatingSystem(detectOS(userAgent));

        audit.setDeviceType(detectDevice(userAgent));

        audit.setSuccess(success);

        audit.setActionTime(LocalDateTime.now());

        audit.setLeaveActionToken(token);

        audit.setEmployeeLeave(leave);

        repo.save(audit);
    }

    private String getClientIp(HttpServletRequest request  ) {

        String xfHeader =request.getHeader("X-Forwarded-For");

        if (xfHeader == null) {
            return request.getRemoteAddr();
        }

        return xfHeader.split(",")[0];
    }

    private String detectBrowser(String ua ) {

        if (ua == null) return "Unknown";

        if (ua.contains("Chrome"))
            return "Chrome";

        if (ua.contains("Firefox"))
            return "Firefox";

        if (ua.contains("Edg"))
            return "Edge";

        if (ua.contains("Safari"))
            return "Safari";

        return "Unknown";
    }

    private String detectOS(String ua ) {

        if (ua == null) return "Unknown";

        if (ua.contains("Windows"))
            return "Windows";

        if (ua.contains("Mac"))
            return "MacOS";

        if (ua.contains("Android"))
            return "Android";

        if (ua.contains("iPhone"))
            return "iPhone";

        return "Unknown";
    }

    private String detectDevice( String ua    ) {

        if (ua == null) return "Unknown";

        if (ua.contains("Mobile"))
            return "Mobile";

        return "Desktop";
    }
}