package org.halocambodia.component;

import com.vaadin.flow.component.AttachEvent;
import com.vaadin.flow.component.Composite;
import com.vaadin.flow.component.dependency.JsModule;
import com.vaadin.flow.component.dependency.NpmPackage;

@NpmPackage(value = "signature_pad", version = "5.1.4")
@JsModule("./js/signature-pad-setup.js")
public class SignaturePad extends Composite<Canvas> {

    private boolean initialized;

    public SignaturePad() {
        getContent().getElement().getStyle()
                .set("border", "1px solid #ccc")
                .set("width", "100%")
                .set("height", "200px");
    }

    @Override
    protected void onAttach(AttachEvent attachEvent) {
        super.onAttach(attachEvent);
        initialized = false;

        getContent().getElement()
                .executeJs("window.initSignaturePad(this)")
                .then(Void.class, result -> initialized = true);
    }

    public void clear() {
        getContent().getElement()
                .executeJs("window.clearSignaturePad(this)");
    }

    public void getImageData(Callback callback) {
        getContent().getElement()
                .executeJs("return window.getSignaturePadData(this)")
                .then(String.class, callback::onData);
    }

    public void loadImage(String dataUrl) {
        if (dataUrl == null || dataUrl.isBlank()) {
            return;
        }

        if (initialized) {
            getContent().getElement().executeJs(
                    "window.loadSignaturePadData(this, $0)", dataUrl);
        } else {
            getContent().getElement().executeJs(
                    """
                    setTimeout(
                        () => window.loadSignaturePadData(this, $0),
                        100
                    )
                    """,
                    dataUrl
            );
        }
    }

    @FunctionalInterface
    public interface Callback {
        void onData(String base64);
    }
}