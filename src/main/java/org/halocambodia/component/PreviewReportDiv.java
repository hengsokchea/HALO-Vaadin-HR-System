package org.halocambodia.component;

import java.io.ByteArrayInputStream;
import java.util.Base64;
import java.util.HashMap;

import org.halocambodia.data.ReportService;

import com.vaadin.flow.component.Text;
import com.vaadin.flow.component.UI;
import com.vaadin.flow.component.button.Button;
import com.vaadin.flow.component.button.ButtonVariant;
import com.vaadin.flow.component.html.Anchor;
import com.vaadin.flow.component.html.Div;
import com.vaadin.flow.component.html.IFrame;
import com.vaadin.flow.component.icon.Icon;
import com.vaadin.flow.component.icon.VaadinIcon;
import com.vaadin.flow.component.orderedlayout.FlexComponent.Alignment;
import com.vaadin.flow.component.orderedlayout.VerticalLayout;
import com.vaadin.flow.server.StreamResource;

public class PreviewReportDiv extends Div {  // Changed from Dialog to Div

    public PreviewReportDiv(String reportPath, HashMap<String, Object> reportParameters) {
        


        try {
            byte[] pdfReport = ReportService.generateReport(reportPath, reportParameters);
            
            // Content area for PDF
            Div contentArea = new Div();
            contentArea.getStyle()
                .set("flex", "1")
                .set("min-height", "0") // Important for flex children with overflow
                .set("position", "relative");

            // Create a StreamResource for the PDF viewer
            StreamResource viewerResource = new StreamResource("report.pdf", 
                () -> new ByteArrayInputStream(pdfReport));
            viewerResource.setContentType("application/pdf");
            viewerResource.setCacheTime(0);

            // Create IFrame with the StreamResource
            IFrame pdfIframe = new IFrame();
            pdfIframe.setSrc(viewerResource);
            pdfIframe.setSizeFull();
            pdfIframe.getElement().setAttribute("frameborder", "0");
            pdfIframe.getElement().getStyle().set("border", "none");

            contentArea.add(pdfIframe);


            
            contentArea.setWidthFull();
            contentArea.getStyle().set("height", "277mm");
            
      
           
            add( contentArea);

        } catch (Exception e) {
            e.printStackTrace();
            Div error = new Div(new Text("Failed to generate report: " + e.getMessage()));
            error.getStyle()
                .set("color", "var(--lumo-error-color)")
                .set("padding", "20px")
                .set("text-align", "center");
            add(error);
        }
    }
}