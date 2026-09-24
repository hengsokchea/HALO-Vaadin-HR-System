package org.halocambodia.data;
import java.sql.Timestamp;
import java.time.ZoneId;
import java.time.ZonedDateTime;
import java.time.format.DateTimeFormatter;
import java.util.function.Function;

import jakarta.persistence.criteria.CriteriaBuilder;
import jakarta.persistence.criteria.Expression;
public class DateTimeUtilFormart {

	
    public static final ZoneId CAMBODIA_ZONE = ZoneId.of("Asia/Phnom_Penh");
    
    public static final DateTimeFormatter DATE_TIME_FORMATTER =DateTimeFormatter.ofPattern("dd-MM-yyyy HH:mm:ss") .withZone(CAMBODIA_ZONE);
    public static final DateTimeFormatter DATE_FORMATTER = DateTimeFormatter.ofPattern("dd-MM-yyyy");
    
    public static String formatDateTime(ZonedDateTime value) {
    	if (value == null) {
            return "—";
        }

        return DATE_TIME_FORMATTER.format(value);
    }

    

    

	//    public static final DateTimeFormatter DATE_TIME_FORMATTER = DateTimeFormatter.ofPattern("dd-MM-yyyy HH:mm:ss");
	//    public static final DateTimeFormatter DATE_FORMATTER = DateTimeFormatter.ofPattern("dd-MM-yyyy");
	    
	    public static Expression<String> DATE_TIME_FORMATTER_DB(CriteriaBuilder criteriaBuilder, Expression<?> dateField) {
	        return criteriaBuilder.function(
	            "to_char",
	            String.class,
	            criteriaBuilder.function("timezone", Timestamp.class, criteriaBuilder.literal("Asia/Phnom_Penh"), dateField),
	            criteriaBuilder.literal("DD-MM-YYYY HH24:MI:SS")
	        );
	    }
	    public static Expression<String> DATE_FORMATTER_DB(CriteriaBuilder criteriaBuilder, Expression<?> dateField) {
	        return criteriaBuilder.function(
	            "to_char",
	            String.class,
	            dateField,
	            criteriaBuilder.literal("DD-MM-YYYY")
	        );
	    }


}
