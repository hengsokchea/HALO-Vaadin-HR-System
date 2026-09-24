package org.halocambodia.data;

import java.time.LocalDate;
import java.time.ZonedDateTime;
import java.util.List;
import java.util.UUID;

public record PolicyReadDto(
        Long policyId,
        UUID publicToken,
        String code,
        String titleEn,
        String titleKh,
        String summary,
        String content,
        LocalDate effectiveDate,
        LocalDate expiryDate,
        Long version,
        boolean requiredAcknowledge,
        String categoryCode,
        String categoryNameEn,
        String categoryNameKh,
        boolean acknowledgedCurrentVersion,
        ZonedDateTime acknowledgedAt,
        List<PolicyAttachmentDto> attachments) {
}
