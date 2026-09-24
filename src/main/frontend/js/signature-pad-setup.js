import SignaturePad from 'signature_pad';

window.initSignaturePad = function (canvas) {
    canvas.signaturePad = new SignaturePad(canvas);
};

window.clearSignaturePad = function (canvas) {
    if (canvas.signaturePad) {
        canvas.signaturePad.clear();
    }
};

window.getSignaturePadData = function (canvas) {
    return canvas.signaturePad && !canvas.signaturePad.isEmpty()
        ? canvas.signaturePad.toDataURL()
        : null;
};

window.loadSignaturePadData = function (canvas, dataUrl) {
    if (canvas.signaturePad) {
        canvas.signaturePad.fromDataURL(dataUrl);
    } else {
        console.warn("SignaturePad is not initialized", canvas);
    }
};