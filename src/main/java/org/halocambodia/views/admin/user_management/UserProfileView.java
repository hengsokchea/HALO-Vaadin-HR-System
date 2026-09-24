package org.halocambodia.views.admin.user_management;

import org.halocambodia.views.MainLayout;

import com.vaadin.flow.component.dependency.Uses;
import com.vaadin.flow.component.html.H3;
import com.vaadin.flow.component.html.Image;
import com.vaadin.flow.component.icon.Icon;
import com.vaadin.flow.component.orderedlayout.HorizontalLayout;
import com.vaadin.flow.component.orderedlayout.VerticalLayout;
import com.vaadin.flow.component.textfield.TextField;
import com.vaadin.flow.router.PageTitle;
import com.vaadin.flow.router.Route;
import com.vaadin.flow.server.StreamResource;

import jakarta.annotation.security.PermitAll;

@PageTitle("User Profile")
@Route(value = "user-profile/:id?", layout = MainLayout.class)
@PermitAll
@Uses(Icon.class)
public class UserProfileView extends VerticalLayout {
    public UserProfileView() {
        setSpacing(false);
        add(new H3("Profile"));
        HorizontalLayout contentLayout=new HorizontalLayout();
        
        contentLayout.setSizeFull();
        
        VerticalLayout leftLayout=new VerticalLayout(new H3("Left"));
        leftLayout.getStyle()
        .set("border", "2px solid black")
        .set("border-radius", "5px") // Optional
        .set("padding", "10px");     // Optional
        
        StreamResource imageResource = new StreamResource("",
                () -> getClass().getResourceAsStream("/assets/userProfile/profile.jpg"));
        Image imageProfile = new Image(imageResource, "");
        imageProfile.setWidth("100px");
        
        TextField txtName=new TextField("Name");
        TextField txtUserName=new TextField("User Name");
        TextField txtEmail=new TextField("Email");
        
        leftLayout.add(imageProfile,txtName,txtUserName,txtEmail);
        
        
        VerticalLayout rightLayout=new VerticalLayout(new H3("Right"));
        rightLayout.getStyle()
        .set("border", "2px solid black")
        .set("border-radius", "5px") // Optional
        .set("padding", "10px");     // Optional
        
        
        contentLayout.add(leftLayout,rightLayout);
        
        
        
        add(contentLayout);
        setSizeFull();
    }

}
