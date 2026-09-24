package org.halocambodia.views.test;

import com.vaadin.flow.component.orderedlayout.VerticalLayout;
import com.vaadin.flow.router.Route;
import org.halocambodia.component.SignaturePad;

@Route("signature-test")
public class SignatureTestView extends VerticalLayout {
    public SignatureTestView() {
        add(new SignaturePad());
    }
}
