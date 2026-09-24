package org.halocambodia.views.attendance_management;

import java.awt.TextField;

import org.halocambodia.views.MainLayout;

import com.vaadin.flow.component.Component;
import com.vaadin.flow.component.html.H2;
import com.vaadin.flow.component.orderedlayout.VerticalLayout;
import com.vaadin.flow.router.PageTitle;
import com.vaadin.flow.router.Route;

import jakarta.annotation.security.PermitAll;

import com.vaadin.flow.component.textfield.*;

@Route(value = "timesheet-entry", layout = MainLayout.class)
@PageTitle("Timesheet Entry")
@PermitAll
public class TimesheetEntryView  extends VerticalLayout { 
	
	public TimesheetEntryView () {
		super();
		add( new H2("Leave Request")); 
	}


	
}
