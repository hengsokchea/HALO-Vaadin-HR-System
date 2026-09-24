package org.halocambodia;

import com.vaadin.flow.component.dependency.StyleSheet;
import com.vaadin.flow.component.page.AppShellConfigurator;
import com.vaadin.flow.component.page.Push;
import com.vaadin.flow.server.PWA;
import com.vaadin.flow.shared.communication.PushMode;
import com.vaadin.flow.shared.ui.Transport;
import com.vaadin.flow.theme.Theme;


import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.boot.web.servlet.support.SpringBootServletInitializer;
import org.springframework.context.annotation.ComponentScan;
import org.springframework.retry.annotation.EnableRetry;
import org.springframework.scheduling.annotation.EnableAsync;
import org.springframework.scheduling.annotation.EnableScheduling;

import com.vaadin.flow.component.dependency.StyleSheet;
import com.vaadin.flow.theme.lumo.Lumo;
//import com.vaadin.flow.theme.aura.Aura;



@EnableConfigurationProperties 
@ComponentScan(basePackages = "org.halocambodia")
@SpringBootApplication
@EnableScheduling


//@StyleSheet(Lumo.STYLESHEET)
//@StyleSheet(Lumo.UTILITY_STYLESHEET)
//@StyleSheet(Aura.STYLESHEET)
//@StyleSheet(Lumo.STYLESHEET)
//@StyleSheet(Lumo.COMPACT_STYLESHEET)
@Theme("camhris")
@PWA(
	    name = "Cam-HRIS",
	    shortName = "HRIS",
	    description = "Manage National and Internation Staff",
	    iconPath = "icons/favicon.ico",
	    offlineResources = {"offline.html"}
	)
//public class Application implements AppShellConfigurator {
@Push(value = PushMode.AUTOMATIC, transport = Transport.WEBSOCKET_XHR) // enable push
@EnableAsync
@EnableRetry
public class Application extends SpringBootServletInitializer implements AppShellConfigurator {

    
    public static void main(String[] args) {
        SpringApplication.run(Application.class, args);
    }

    
}
