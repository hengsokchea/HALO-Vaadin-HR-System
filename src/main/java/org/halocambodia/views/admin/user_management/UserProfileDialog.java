package org.halocambodia.views.admin.user_management;

import com.vaadin.flow.component.avatar.Avatar;
import com.vaadin.flow.component.button.Button;
import com.vaadin.flow.component.button.ButtonVariant;
import com.vaadin.flow.component.dialog.Dialog;
import com.vaadin.flow.component.html.H3;
import com.vaadin.flow.component.html.Image;
import com.vaadin.flow.component.html.NativeLabel;
import com.vaadin.flow.component.notification.Notification;
import com.vaadin.flow.component.notification.Notification.Position;
import com.vaadin.flow.component.notification.NotificationVariant;
import com.vaadin.flow.component.orderedlayout.HorizontalLayout;
import com.vaadin.flow.component.orderedlayout.VerticalLayout;
import com.vaadin.flow.component.textfield.EmailField;
import com.vaadin.flow.component.textfield.TextField;
import com.vaadin.flow.component.upload.Upload;
import com.vaadin.flow.component.upload.receivers.MultiFileMemoryBuffer;
import com.vaadin.flow.server.StreamResource;

import org.halocambodia.data.FileUploadUtility;
import org.halocambodia.data.User;
import org.halocambodia.data.UserRepository;
import org.springframework.core.env.Environment;

import java.io.IOException;
import java.io.InputStream;
import java.nio.file.Path;
import java.util.UUID;

public class UserProfileDialog extends Dialog {

    private final UserRepository userRepository;
    private final Path uploadDir;
    private static final String UserProfileSubDirectory="userProfile";
    
    private final FileUploadUtility fileUploadUtility;

    public UserProfileDialog( User currentUserLogin, UserRepository userRepository,FileUploadUtility fileUploadUtility) {
        this.userRepository = userRepository;
        this.fileUploadUtility = fileUploadUtility;
        this.uploadDir=fileUploadUtility.initializeUploadDirectory("hr", "");
        


        setResizable(false);
        setCloseOnOutsideClick(false);

        // Fields
        TextField txtName = new TextField("Name");
        txtName.setValue(currentUserLogin.getName() != null ? currentUserLogin.getName() : "");
        
        TextField txtUserName = new TextField("User Name");
        txtUserName.setValue(currentUserLogin.getUsername() != null ? currentUserLogin.getUsername() : "");
        
        EmailField txtEmail = new EmailField("Email");
        txtEmail.setValue(currentUserLogin.getEmail() != null ? currentUserLogin.getEmail() : "");

        // Upload Component
        MultiFileMemoryBuffer buffer = new MultiFileMemoryBuffer();
        Upload dropEnabledUpload = new Upload(buffer);
        dropEnabledUpload.setDropAllowed(true);
        dropEnabledUpload.setMaxFiles(1);
        dropEnabledUpload.setAcceptedFileTypes("image/jpeg", "image/png", "image/gif");
        dropEnabledUpload.setMaxFileSize(10 * 1024 * 1024); // 10MB limit

        NativeLabel dropEnabledLabel = new NativeLabel("User Profile Picture");
        dropEnabledLabel.getStyle().set("font-weight", "600");
        dropEnabledUpload.setId("upload-drop-enabled");
        dropEnabledLabel.setFor(dropEnabledUpload.getId().get());
        
        Image imageProfile;
        if (currentUserLogin.getProfileImagePath() != null && !currentUserLogin.getProfileImagePath().isEmpty()) {
            imageProfile = fileUploadUtility.createImageComponent(
                uploadDir.resolve(UserProfileSubDirectory),
                currentUserLogin.getProfileImagePath(),
                "Profile Image"
            );
        } else {
            imageProfile = fileUploadUtility.getDefaultImage();
        }
        imageProfile.setWidth("300px");


        // Layout
        VerticalLayout leftLayout = new VerticalLayout(
            imageProfile,
            new VerticalLayout(dropEnabledLabel, dropEnabledUpload)
        );
        leftLayout.setSpacing(false);
        
        VerticalLayout rightLayout = new VerticalLayout(txtName, txtUserName, txtEmail);
        rightLayout.setSpacing(false);
        
        add(new HorizontalLayout(leftLayout, rightLayout));

        // Buttons
        Button cancel = new Button("Cancel", e -> close());
        Button ok = new Button("OK", e -> {
            currentUserLogin.setName(txtName.getValue());
            currentUserLogin.setUsername(txtUserName.getValue());
            currentUserLogin.setEmail(txtEmail.getValue());
            userRepository.save(currentUserLogin);
            Notification.show("Profile updated successfully", 5000, Position.TOP_CENTER);
            close();
        });
        ok.addThemeVariants(ButtonVariant.LUMO_PRIMARY);

        getHeader().add(new H3("Profile"));
        getFooter().add(new HorizontalLayout(cancel, ok));

        // Upload handling
        dropEnabledUpload.addSucceededListener(event -> {
            try {
            	
                String uniqueFileName = fileUploadUtility.handleFileUpload(
                    uploadDir,
                    UserProfileSubDirectory,
                    buffer.getInputStream(event.getFileName()),
                    event.getFileName(),
                    currentUserLogin.getProfileImagePath()
                );
                
                currentUserLogin.setProfileImagePath(uniqueFileName);
                //imageProfile.setSrc(FileUploadUtility.createImageResource(uploadDir, uniqueFileName));
                Image newImage = fileUploadUtility.createImageComponent(
                	    uploadDir.resolve(UserProfileSubDirectory),
                	    uniqueFileName,
                	    "Profile Image"
                	);
                	imageProfile.setSrc(newImage.getSrc());

                ok.click();
            } catch (IOException e) {
                Notification.show("Failed to save file: " + e.getMessage(), 5000, Position.MIDDLE)
                    .addThemeVariants(NotificationVariant.LUMO_ERROR);
            }
        });

        dropEnabledUpload.addFailedListener(event -> {
            String errorMessage = "Upload failed: " + event.getReason().getMessage();
            if (event.getReason().getCause() != null) {
                errorMessage += " (Cause: " + event.getReason().getCause().getMessage() + ")";
            }
            Notification.show(errorMessage, 5000, Position.MIDDLE)
                .addThemeVariants(NotificationVariant.LUMO_ERROR);
        });
    }
}