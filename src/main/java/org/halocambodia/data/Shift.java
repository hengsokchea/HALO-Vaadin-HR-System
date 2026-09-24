package org.halocambodia.data;

import java.time.LocalTime;
import java.util.ArrayList;
import java.util.List;

import jakarta.persistence.*;
import jakarta.validation.constraints.AssertTrue;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.Getter;
import lombok.Setter;

@Entity
@Table(name = "shift", schema = "public")
@Getter
@Setter
public class Shift extends AbstractEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.SEQUENCE, generator = "public.shift_shift_id_seq")
    @SequenceGenerator(name = "public.shift_shift_id_seq",sequenceName = "public.shift_shift_id_seq",initialValue = 1,allocationSize = 1)
    @Column(name = "shift_id")
    private Long id;
    
	@Override
	public Long getId() {
	    return id;
	}

    @Column(name = "shift_name", nullable = false, unique = true)
    @NotBlank(message = "Shift name cannot be null or empty")
    private String shiftName;

    @Column(name = "start_time", nullable = false)
    @NotNull(message = "Start time cannot be null")
    private LocalTime startTime;

    @Column(name = "end_time", nullable = false)
    @NotNull(message = "End time cannot be null")
    private LocalTime endTime;

    @Column(name = "break_minutes", nullable = false)
    @NotNull(message = "Break minutes cannot be null")
    private Integer breakMinutes;

    @Column(name = "remark")
    private String remark;
    
    @Column(name = "morning_start_time", nullable = false)
    @NotNull(message = "Morning Start time cannot be null")
    private LocalTime morningStartTime;

    @Column(name = "morning_end_time", nullable = false)
    @NotNull(message = "Morning time cannot be null")
    private LocalTime morningEndTime;

    @Column(name = "morning_break_minutes", nullable = false)
    @NotNull(message = "Morning Break minutes cannot be null")
    private Integer morningBreakMinutes;
    
    @Column(name = "afternoon_start_time", nullable = false)
    @NotNull(message = "Afternoon Start time cannot be null")
    private LocalTime afternoonStartTime;

    @Column(name = "afternoon_end_time", nullable = false)
    @NotNull(message = "Afternoon time cannot be null")
    private LocalTime afternoonEndTime;

    @Column(name = "afternoon_break_minutes", nullable = false)
    @NotNull(message = "Afternoon Break minutes cannot be null")
    private Integer afternoonBreakMinutes;
    

    @OneToMany(mappedBy = "shift", cascade = CascadeType.ALL, fetch = FetchType.LAZY, orphanRemoval = true)
    private List<ShiftCycle> shiftCycles = new ArrayList<>();
    
    @AssertTrue(message = "End time must be after start time")
    public boolean isEndTimeAfterStartTime() {
        if (startTime == null || endTime == null) return true; // Let @NotNull handle missing fields
        return endTime.isAfter(startTime);
    }
}
