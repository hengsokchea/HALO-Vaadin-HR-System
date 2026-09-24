package org.halocambodia.views.login;


import com.vaadin.flow.component.button.Button;
import com.vaadin.flow.component.button.ButtonVariant;
import com.vaadin.flow.component.html.Image;
import com.vaadin.flow.component.html.Paragraph;
import com.vaadin.flow.component.html.Span;
import com.vaadin.flow.component.login.LoginI18n;
import com.vaadin.flow.component.login.LoginOverlay;
import com.vaadin.flow.component.orderedlayout.VerticalLayout;
import com.vaadin.flow.component.orderedlayout.FlexComponent;
import com.vaadin.flow.component.orderedlayout.HorizontalLayout;
import com.vaadin.flow.router.BeforeEnterEvent;
import com.vaadin.flow.router.BeforeEnterObserver;
import com.vaadin.flow.router.PageTitle;
import com.vaadin.flow.router.Route;
import com.vaadin.flow.router.internal.RouteUtil;
import com.vaadin.flow.server.VaadinService;
import com.vaadin.flow.server.VaadinSession;
import com.vaadin.flow.server.auth.AnonymousAllowed;
import com.vaadin.flow.server.streams.DownloadHandler;
import com.vaadin.flow.theme.lumo.LumoUtility;
import java.time.Year;
import org.halocambodia.security.AuthenticatedUser;
import org.halocambodia.security.WebAuthnLoginService;
import org.halocambodia.views.WebAuthnBridge;
import org.vaadin.lineawesome.LineAwesomeIcon;
import com.vaadin.flow.server.VaadinServletRequest;

@AnonymousAllowed
@PageTitle("Login")
@Route(value = "login")
public class LoginView extends LoginOverlay implements BeforeEnterObserver {

    private final AuthenticatedUser authenticatedUser;
    private final LoginI18n i18nConfig;
    private final WebAuthnBridge webAuthnBridge;
    private final WebAuthnLoginService webAuthnLoginService;
    
    private HorizontalLayout divider;
    private Button fingerprintLogin;
   
    public LoginView(AuthenticatedUser authenticatedUser, WebAuthnBridge webAuthnBridge,WebAuthnLoginService webAuthnLoginService) {
        this.authenticatedUser = authenticatedUser;
        this.webAuthnBridge = webAuthnBridge;
        this.webAuthnLoginService=webAuthnLoginService;

        setAction(RouteUtil.getRoutePath(VaadinService.getCurrent().getContext(), getClass()));

        Image logo = new Image( DownloadHandler.forClassResource(getClass(), "/assets/images/HALO_Logo_Light.png"), "HALO Cambodia");
        logo.setHeight("80px");

        Span title = new Span("Cam-HRIS");
        title.getStyle()
        .set("font-weight", "700")
        .set("font-size", "32px")
        .set("color", "white")
        .set("letter-spacing", "-0.5px");

        Paragraph desc = new Paragraph("ប្រព័ន្ធព័ត៌មានគ្រប់គ្រងធនធានមនុស្សកម្ពុជា \n Human Resource Information System");
        desc.getStyle()
            .set("font-size", "13px")
            .set("color", "white")
            .set("text-align", "center")
            .set("white-space", "pre-line");

        VerticalLayout header = new VerticalLayout(logo, title, desc);
        header.setWidthFull();
        header.setSpacing(false);
        header.setMargin(false);
        header.setAlignItems(FlexComponent.Alignment.CENTER);
        header.setPadding(false);
        header.setJustifyContentMode(FlexComponent.JustifyContentMode.CENTER);

        setTitle(header);
        setDescription("");
        setForgotPasswordButtonVisible(false);
        
        // Configure Khmer and English labels for the login form
        i18nConfig = LoginI18n.createDefault(); // Initialize the field
        
        // Configure the form labels
        LoginI18n.Form i18nForm = i18nConfig.getForm();
        
        i18nForm.setTitle(""); // Remove default title since we have our custom header
        i18nForm.setUsername("ឈ្មោះអ្នកប្រើ | Username");
        i18nForm.setPassword("ពាក្យសម្ងាត់ | Password");
        i18nForm.setSubmit("ចូល | Log in");
        i18nForm.setForgotPassword(""); // Hide forgot password text
        
        i18nConfig.setForm(i18nForm);
        
        LoginI18n.ErrorMessage i18nErrorMessage = i18nConfig.getErrorMessage();
        i18nErrorMessage.setTitle("ការចូលមិនបានសម្រាប់ / Login failed");
        i18nErrorMessage.setMessage(
            "សូមពិនិត្យថាតើអ្នកបានបញ្ចូលឈ្មោះអ្នកប្រើ និងពាក្យសម្ងាត់ឲ្យបានត្រឹមត្រូវ\n" +
            "Please check your username and password and try again."
        );
        i18nConfig.setErrorMessage(i18nErrorMessage);
        
        setI18n(i18nConfig);
        

        Paragraph text = new Paragraph(
        	    "🔐 សូមរក្សាពាក្យសម្ងាត់របស់អ្នកជាសម្ងាត់\n🔐 Keep your password private"
        	);
        text.setWidthFull();
        text.addClassName(LumoUtility.TextAlignment.CENTER);
        text.getStyle()
        .set("color", "#00447A")
        .set("white-space", "pre-line")
        .set("font-size", "12px")
        .set("margin-top", "10px");
        
        getFooter().add(text);
        
        fingerprintLogin  = new Button("Biometric / Passkey Login",LineAwesomeIcon.FINGERPRINT_SOLID.create());
        fingerprintLogin.addThemeVariants(ButtonVariant.PRIMARY);
        fingerprintLogin.setWidthFull();
        fingerprintLogin.setTooltipText("Use fingerprint, face recognition, PIN, or scan a QR code with your phone.\n" +  "ប្រើស្នាមម្រាមដៃ ការស្គាល់មុខ លេខសម្ងាត់ (PIN) ឬស្កេន QR Code ដោយទូរស័ព្ទ។");
        
        
        
        Span leftLine = new Span();
        leftLine.getStyle()
                .set("flex-grow", "1")
                .set("height", "2px")
                .set("background", "#f6a15a");

        Span rightLine = new Span();
        rightLine.getStyle()
                 .set("flex-grow", "1")
                 .set("height", "2px")
                 .set("background", "#f6a15a");

        Span orText = new Span("OR");
        orText.getStyle()
              .set("font-size", "12px")
              .set("font-weight", "600")
              .set("color", "#f6a15a")
              .set("padding", "0 10px");

        divider = new HorizontalLayout(
                leftLine,
                orText,
                rightLine
        );
        divider.setWidthFull();
        divider.setPadding(false);
        divider.setSpacing(false);
        divider.setAlignItems(FlexComponent.Alignment.CENTER);

       // getFooter().add(divider);
        
        
        
      //  getFooter().add(fingerprintLogin);
        
        Paragraph version = new Paragraph("Version 1.0");
        version.setWidthFull();
        version.getStyle()
               .set("font-size", "10px")
               .set("color", "#9CA3AF")
               .set("text-align", "center")
               .set("margin", "8px 0 0 0");

        Paragraph copyright =new Paragraph("© " + Year.now().getValue() +" The HALO Trust Cambodia");
        copyright.setWidthFull();
        copyright.getStyle()
                 .set("font-size", "10px")
                 .set("color", "#9CA3AF")
                 .set("text-align", "center")
                 .set("margin", "0");
        
        boolean showBiometricLogin = isBiometricLoginAllowed();
        
       // getFooter().add(version);
       //().add(copyright);
        if (showBiometricLogin) {
            getFooter().add(divider);
            getFooter().add(fingerprintLogin);
        
	        fingerprintLogin.addClickListener(event -> {
	
	        	String challenge =webAuthnLoginService.generateChallenge();
	
	        	VaadinSession.getCurrent().setAttribute("webauthn-login-challenge",challenge);
	
	            getElement().executeJs("""
	            		
	                function base64urlToUint8Array(base64url) {
	
	                    const base64 =
	                        base64url
	                            .replace(/-/g, '+')
	                            .replace(/_/g, '/');
	
	                    const padded =
	                        base64 +
	                        '='.repeat((4 - base64.length % 4) % 4);
	
	                    const binary = atob(padded);
	
	                    return Uint8Array.from(
	                        binary,
	                        c => c.charCodeAt(0)
	                    );
	                }
	                            		
	                navigator.credentials.get({
	                    publicKey: {
	                        challenge:base64urlToUint8Array($0),
	                        userVerification: "required",
	                        timeout: 60000
	                    }
	                })
	                .then(assertion => {
	
					const response = {
					
					    credentialId: assertion.id,
					
					    rawId: btoa(
					        String.fromCharCode(
					            ...new Uint8Array(assertion.rawId)
					        )
					    ),
					
					    authenticatorData: btoa(
					        String.fromCharCode(
					            ...new Uint8Array(
					                assertion.response.authenticatorData
					            )
					        )
					    ),
					
					    clientDataJSON: btoa(
					        String.fromCharCode(
					            ...new Uint8Array(
					                assertion.response.clientDataJSON
					            )
					        )
					    ),
					
					    signature: btoa(
					        String.fromCharCode(
					            ...new Uint8Array(
					                assertion.response.signature
					            )
					        )
					    ),
					
					    userHandle:
					        assertion.response.userHandle
					            ? btoa(
					                String.fromCharCode(
					                    ...new Uint8Array(
					                        assertion.response.userHandle
					                    )
					                )
					              )
					            : null
					};
	
	                    $1.$server.verifyLogin(
	                        JSON.stringify(response)
	                    )
	                    .then(result => {
	            		     console.log("VERIFY RESULT =", result);
	                        if(result === "SUCCESS") {
	
	            		       $1.$server.loginSuccess();
	                      
	
	                        } else {
	
	                            alert(result);
	                        }
	
	                    });
	
	                })
	                .catch(error => {
	
	                    alert(error.message);
	
	                });
	
	            """,
	            challenge,
	            webAuthnBridge.getElement());
	
	        } );
	        
	        getElement().appendChild( webAuthnBridge.getElement()); 
        }
        
        getFooter().add(version);
        getFooter().add(copyright);
        setOpened(true);

    }

    private boolean isBiometricLoginAllowed() {
        String host = VaadinServletRequest.getCurrent().getServerName();
        
        // Check for localhost (including variations) and web.halocambodia.local
        return "localhost".equalsIgnoreCase(host) ||
               "127.0.0.1".equals(host) ||
               "0:0:0:0:0:0:0:1".equals(host) || // IPv6 localhost
               "web.halocambodia.local".equalsIgnoreCase(host);
    }
    
    @Override
    public void beforeEnter(BeforeEnterEvent event) {

    	
        if (authenticatedUser.get().isPresent()) {
            setOpened(false);
            event.forwardTo("");
            return;
        }

        var params = event.getLocation().getQueryParameters().getParameters();
        
        // Use the i18nConfig field from the constructor
        if (params.containsKey("error")) {
            i18nConfig.getErrorMessage().setTitle("Incorrect username or password");
            i18nConfig.getErrorMessage().setMessage(
                "Check that you have entered the correct username and password and try again."
                + "\n\nត្រូវបញ្ចូលឈ្មោះអ្នកប្រើ និងពាក្យសម្ងាត់ឲ្យត្រឹមត្រូវ ម្ដងទៀត។"
            );
            setI18n(i18nConfig);
            setError(true);
        } else if (params.containsKey("disabled")) {
            i18nConfig.getErrorMessage().setTitle("Account disabled");
            i18nConfig.getErrorMessage().setMessage(
                "Your account has been disabled. Please contact the administrator."
                + "\n\nគណនីរបស់អ្នកត្រូវបានផ្អាក។ សូមទាក់ទងអ្នកគ្រប់គ្រង។"
            );
            setI18n(i18nConfig);
            setError(true);
        } else {
            setError(false);
        }
    }
}