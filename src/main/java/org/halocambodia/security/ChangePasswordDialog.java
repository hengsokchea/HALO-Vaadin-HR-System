package org.halocambodia.security;

import org.halocambodia.data.User;
import org.halocambodia.data.UserRepository;
import org.springframework.security.crypto.bcrypt.BCrypt;

import com.vaadin.flow.component.Html;
import com.vaadin.flow.component.button.Button;
import com.vaadin.flow.component.button.ButtonVariant;
import com.vaadin.flow.component.dialog.Dialog;
import com.vaadin.flow.component.html.H2;
import com.vaadin.flow.component.html.Paragraph;
import com.vaadin.flow.component.html.Span;
import com.vaadin.flow.component.notification.Notification;
import com.vaadin.flow.component.notification.NotificationVariant;
import com.vaadin.flow.component.notification.Notification.Position;
import com.vaadin.flow.component.orderedlayout.FlexComponent.JustifyContentMode;
import com.vaadin.flow.component.orderedlayout.HorizontalLayout;
import com.vaadin.flow.component.orderedlayout.VerticalLayout;
import com.vaadin.flow.component.textfield.PasswordField;

import com.vaadin.flow.component.button.Button;
import com.vaadin.flow.component.checkbox.Checkbox;
import com.vaadin.flow.component.dialog.Dialog;
import com.vaadin.flow.component.html.H3;
import com.vaadin.flow.component.html.Paragraph;
import com.vaadin.flow.component.orderedlayout.HorizontalLayout;
import com.vaadin.flow.component.orderedlayout.VerticalLayout;
import com.vaadin.flow.component.textfield.PasswordField;
import com.vaadin.flow.component.textfield.TextField;
import com.vaadin.flow.data.value.ValueChangeMode;

public class ChangePasswordDialog extends Dialog {
	private UserRepository userRepository;
    public ChangePasswordDialog(User currentUserLogin,UserRepository userRepository) {
    	this.userRepository=userRepository;
    	
    	setResizable(false);
        setCloseOnOutsideClick(false);
        // Password fields
        PasswordField currentPassword = new PasswordField("Current Password");
        PasswordField newPassword = new PasswordField("New Password");
        PasswordField confirmPassword = new PasswordField("Confirm Password");

        currentPassword.setWidthFull();
        newPassword.setWidthFull();
        confirmPassword.setWidthFull();

        // Rule checkboxes (disabled to prevent user input)
        Checkbox ruleLength8 = createRuleCheckbox("At least 8 characters long");
        Checkbox ruleLength64 = createRuleCheckbox("Between 10 and 64 characters long");
        Checkbox ruleUpper = createRuleCheckbox("At least 1 uppercase letter");
        Checkbox ruleLower = createRuleCheckbox("At least 1 lowercase letter");
        Checkbox ruleDigit = createRuleCheckbox("At least 1 digit");
        Checkbox ruleSpecial = createRuleCheckbox("At least 1 special character");
        Checkbox ruleGroup3 = createRuleCheckbox("Characters from at least 3 of the groups: uppercase, lowercase, digits, & specials");
        Checkbox ruleDifferent = createRuleCheckbox("Different from current password");
        Checkbox ruleStrong = createRuleCheckbox("Minimum strength of Strong (simulated)");
        Checkbox ruleMatch = createRuleCheckbox("New and Confirm password match");

        VerticalLayout rulesLayout = new VerticalLayout(
                new Paragraph("Password Complexity Rules"),
                ruleLength8,
                ruleLength64,
                ruleUpper,
                ruleLower,
                ruleDigit,
                ruleSpecial,
                ruleGroup3,
                ruleDifferent,
                ruleStrong,
                ruleMatch
        );
        rulesLayout.setPadding(false);
        rulesLayout.setSpacing(false);

        // Instruction layout
        VerticalLayout instructions = new VerticalLayout(
                new Paragraph("Instructions"),
                new Paragraph("1. Enter the current password."),
                new Paragraph("2. Provide a new password that satisfies the following complexity rules."),
                new Paragraph("3. Repeat the new password to confirm."),
                new Paragraph("4. All checkboxes must be ticked to proceed.")
        );
        instructions.setPadding(false);
        instructions.setSpacing(false);

        // Left: fields | Right: instructions
        HorizontalLayout contentLayout = new HorizontalLayout(
                new VerticalLayout(currentPassword, newPassword, confirmPassword),
                instructions
        );
        contentLayout.setWidthFull();
        contentLayout.setFlexGrow(1, contentLayout.getComponentAt(0));
        contentLayout.setFlexGrow(1, contentLayout.getComponentAt(1));

        // Cancel button
        Button cancel = new Button("Cancel", e -> close());

        // OK button (declared before lambda for reuse inside it)
        Button ok = new Button("OK");
        ok.addThemeVariants(ButtonVariant.LUMO_PRIMARY);
        

        

        ok.addClickListener(e -> {
            boolean allValid = rulesLayout.getChildren()
                    .filter(component -> component instanceof Checkbox)
                    .map(component -> (Checkbox) component)
                    .allMatch(Checkbox::getValue);

            if (allValid) {
            	try {
            	     // Verify current password
                    String enteredCurrentPassword = currentPassword.getValue();
                    String storedHashedPassword = currentUserLogin.getHashedPassword();

                    if (storedHashedPassword == null || !BCrypt.checkpw(enteredCurrentPassword, storedHashedPassword)) {
                        Notification.show("Incorrect current password", 6000, Position.MIDDLE).addThemeVariants(NotificationVariant.LUMO_ERROR);
                        return;
                    }
                    
                    // Ensure password is hashed before saving
                    currentUserLogin.setHashedPassword(BCrypt.hashpw(newPassword.getValue(), BCrypt.gensalt()));
                    userRepository.save(currentUserLogin);
                    Notification.show("Password changed successfully!", 3000, Notification.Position.MIDDLE).addThemeVariants(NotificationVariant.LUMO_SUCCESS);
                    close();
                } catch (Exception ex) {
                	ex.printStackTrace();
                    Notification.show("Failed to save password: " + ex.getMessage(), 6000, Notification.Position.MIDDLE).addThemeVariants(NotificationVariant.LUMO_ERROR);
                }
            } else {
                Notification.show("Please meet all password rules",6000,Position.MIDDLE).addThemeVariants(NotificationVariant.LUMO_ERROR);
            }
        });

        HorizontalLayout buttons = new HorizontalLayout(cancel, ok);

        // 🔄 Real-time validation
        Runnable validatePassword = () -> {
            String password = newPassword.getValue();
            String confirm = confirmPassword.getValue();
            String current = currentPassword.getValue();

            ruleLength8.setValue(password.length() >= 8);           
            ruleLength64.setValue(password.length() >= 10 && password.length() <= 64);
            ruleUpper.setValue(password.matches(".*[A-Z].*"));
            ruleLower.setValue(password.matches(".*[a-z].*"));
            ruleDigit.setValue(password.matches(".*\\d.*"));
            ruleSpecial.setValue(password.matches(".*[^a-zA-Z0-9].*"));

            int groups = 0;
            if (ruleUpper.getValue()) groups++;
            if (ruleLower.getValue()) groups++;
            if (ruleDigit.getValue()) groups++;
            if (ruleSpecial.getValue()) groups++;
            ruleGroup3.setValue(groups >= 3);

            ruleDifferent.setValue(!password.equals(current));
            ruleStrong.setValue(password.length() >= 12 && groups >= 3);
            ruleMatch.setValue(password.equals(confirm));
        };

        // Listen for changes in fields
        newPassword.setValueChangeMode(ValueChangeMode.EAGER);
        confirmPassword.setValueChangeMode(ValueChangeMode.EAGER);
        currentPassword.setValueChangeMode(ValueChangeMode.EAGER);
        
        newPassword.addValueChangeListener(e -> validatePassword.run());
        confirmPassword.addValueChangeListener(e -> validatePassword.run());
        currentPassword.addValueChangeListener(e -> validatePassword.run());

        // Final assembly
        getHeader().add(new H3("Change Password"));
        add( contentLayout, rulesLayout );
        getFooter().add(buttons);
    }

    private Checkbox createRuleCheckbox(String text) {
        Checkbox checkbox = new Checkbox(text);
        checkbox.setReadOnly(true);
       
        return checkbox;
    }
}