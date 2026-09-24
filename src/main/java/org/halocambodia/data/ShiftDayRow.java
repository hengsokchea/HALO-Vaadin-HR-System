package org.halocambodia.data;


import java.time.DayOfWeek;
import java.time.LocalDate;
import java.time.Month;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;


public class ShiftDayRow {
	private Integer dayOfMonth; //1-31
	private ShiftCycleDetail monthJanuary;
	private ShiftCycleDetail monthFebruary;
	private ShiftCycleDetail monthMarch;
	private ShiftCycleDetail monthApril;
	private ShiftCycleDetail monthMay;
	private ShiftCycleDetail monthJune;
	private ShiftCycleDetail monthJuly;
	private ShiftCycleDetail monthAugust;
	private ShiftCycleDetail monthSeptember;
	private ShiftCycleDetail monthOctober;
	private ShiftCycleDetail monthNovember;
	private ShiftCycleDetail monthDecember;
	public ShiftDayRow(Integer dayOfMonth, ShiftCycleDetail monthJanuary, ShiftCycleDetail monthFebruary,
			ShiftCycleDetail monthMarch, ShiftCycleDetail monthApril, ShiftCycleDetail monthMay,
			ShiftCycleDetail monthJune, ShiftCycleDetail monthJuly, ShiftCycleDetail monthAugust,
			ShiftCycleDetail monthSeptember, ShiftCycleDetail monthOctober, ShiftCycleDetail monthNovember,
			ShiftCycleDetail monthDecember) {
		super();
		this.dayOfMonth = dayOfMonth;
		this.monthJanuary = monthJanuary;
		this.monthFebruary = monthFebruary;
		this.monthMarch = monthMarch;
		this.monthApril = monthApril;
		this.monthMay = monthMay;
		this.monthJune = monthJune;
		this.monthJuly = monthJuly;
		this.monthAugust = monthAugust;
		this.monthSeptember = monthSeptember;
		this.monthOctober = monthOctober;
		this.monthNovember = monthNovember;
		this.monthDecember = monthDecember;
	}
	public ShiftDayRow(Integer dayOfMonth) {
        this.dayOfMonth = dayOfMonth;
    }
	public Integer getDayOfMonth() {
		return dayOfMonth;
	}
	public void setDayOfMonth(Integer dayOfMonth) {
		this.dayOfMonth = dayOfMonth;
	}
	public ShiftCycleDetail getMonthJanuary() {
		return monthJanuary;
	}
	public void setMonthJanuary(ShiftCycleDetail monthJanuary) {
		this.monthJanuary = monthJanuary;
	}
	public ShiftCycleDetail getMonthFebruary() {
		return monthFebruary;
	}
	public void setMonthFebruary(ShiftCycleDetail monthFebruary) {
		this.monthFebruary = monthFebruary;
	}
	public ShiftCycleDetail getMonthMarch() {
		return monthMarch;
	}
	public void setMonthMarch(ShiftCycleDetail monthMarch) {
		this.monthMarch = monthMarch;
	}
	public ShiftCycleDetail getMonthApril() {
		return monthApril;
	}
	public void setMonthApril(ShiftCycleDetail monthApril) {
		this.monthApril = monthApril;
	}
	public ShiftCycleDetail getMonthMay() {
		return monthMay;
	}
	public void setMonthMay(ShiftCycleDetail monthMay) {
		this.monthMay = monthMay;
	}
	public ShiftCycleDetail getMonthJune() {
		return monthJune;
	}
	public void setMonthJune(ShiftCycleDetail monthJune) {
		this.monthJune = monthJune;
	}
	public ShiftCycleDetail getMonthJuly() {
		return monthJuly;
	}
	public void setMonthJuly(ShiftCycleDetail monthJuly) {
		this.monthJuly = monthJuly;
	}
	public ShiftCycleDetail getMonthAugust() {
		return monthAugust;
	}
	public void setMonthAugust(ShiftCycleDetail monthAugust) {
		this.monthAugust = monthAugust;
	}
	public ShiftCycleDetail getMonthSeptember() {
		return monthSeptember;
	}
	public void setMonthSeptember(ShiftCycleDetail monthSeptember) {
		this.monthSeptember = monthSeptember;
	}
	public ShiftCycleDetail getMonthOctober() {
		return monthOctober;
	}
	public void setMonthOctober(ShiftCycleDetail monthOctober) {
		this.monthOctober = monthOctober;
	}
	public ShiftCycleDetail getMonthNovember() {
		return monthNovember;
	}
	public void setMonthNovember(ShiftCycleDetail monthNovember) {
		this.monthNovember = monthNovember;
	}
	public ShiftCycleDetail getMonthDecember() {
		return monthDecember;
	}
	public void setMonthDecember(ShiftCycleDetail monthDecember) {
		this.monthDecember = monthDecember;
	}
    
    public static List<ShiftDayRow> buildDayRows(List<ShiftCycleDetail> shiftCycleDetails) {
        
        Map<Integer, ShiftDayRow> shiftDayRows = new HashMap<>();
        
        // 1. Group the details by day of the month and sort them
        Map<Integer, List<ShiftCycleDetail>> detailsByDay = shiftCycleDetails.stream()
            .sorted(Comparator.comparing(ShiftCycleDetail::getCycleDate))
            .collect(Collectors.groupingBy(detail -> detail.getCycleDate().getDayOfMonth()));

        // 2. Iterate through each day of the month
        for (int day = 1; day <= 31; day++) {
            ShiftDayRow row = new ShiftDayRow(day);
            List<ShiftCycleDetail> dailyDetails = detailsByDay.getOrDefault(day, Collections.emptyList());

            // 3. Populate the correct month field for each ShiftDayRow
            for (ShiftCycleDetail detail : dailyDetails) {
                switch (detail.getCycleDate().getMonth()) {
                    case java.time.Month.JANUARY:
                        row.setMonthJanuary(detail);
                        break;
                    case java.time.Month.FEBRUARY:
                        row.setMonthFebruary(detail);
                        break;
                    case java.time.Month.MARCH:
                        row.setMonthMarch(detail);
                        break;
                    case java.time.Month.APRIL:
                        row.setMonthApril(detail);
                        break;
                    case java.time.Month.MAY:
                        row.setMonthMay(detail);
                        break;
                    case java.time.Month.JUNE:
                        row.setMonthJune(detail);
                        break;
                    case java.time.Month.JULY:
                        row.setMonthJuly(detail);
                        break;
                    case java.time.Month.AUGUST:
                        row.setMonthAugust(detail);
                        break;
                    case java.time.Month.SEPTEMBER:
                        row.setMonthSeptember(detail);
                        break;
                    case java.time.Month.OCTOBER:
                        row.setMonthOctober(detail);
                        break;
                    case java.time.Month.NOVEMBER:
                        row.setMonthNovember(detail);
                        break;
                    case java.time.Month.DECEMBER:
                        row.setMonthDecember(detail);
                        break;
                }
            }
            shiftDayRows.put(day, row);
        }

        // 4. Return the sorted list of ShiftDayRow objects
        return new ArrayList<>(shiftDayRows.values()).stream()
            .sorted(Comparator.comparing(ShiftDayRow::getDayOfMonth))
            .collect(Collectors.toList());
    }

}