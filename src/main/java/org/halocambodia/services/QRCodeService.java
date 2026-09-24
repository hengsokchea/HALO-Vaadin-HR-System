package org.halocambodia.services;

import java.awt.AlphaComposite;
import java.awt.Color;
import java.awt.Graphics2D;
import java.awt.RenderingHints;
import java.awt.Shape;
import java.awt.geom.RoundRectangle2D;
import java.awt.image.BufferedImage;
import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.InputStream;
import java.util.HashMap;
import java.util.Map;

import javax.imageio.ImageIO;

import org.springframework.stereotype.Service;

import com.google.zxing.BarcodeFormat;
import com.google.zxing.EncodeHintType;
import com.google.zxing.MultiFormatWriter;
import com.google.zxing.client.j2se.MatrixToImageWriter;
import com.google.zxing.common.BitMatrix;
import com.google.zxing.qrcode.decoder.ErrorCorrectionLevel;
import com.vaadin.flow.server.StreamResource;

@Service
public class QRCodeService {

    private static final String LOGO_PATH =
            "/assets/images/HALO_logo.png"; // Change to your logo

    /**
     * Existing method used by EmailService.
     */
    public byte[] generateQRCode(String text, int width, int height) {

        try {

            BufferedImage image = generateImage(text, width);

            ByteArrayOutputStream stream = new ByteArrayOutputStream();

            ImageIO.write(image, "PNG", stream);

            return stream.toByteArray();

        } catch (Exception ex) {
            throw new RuntimeException(ex);
        }
    }

    /**
     * For Vaadin Image component.
     */
    public StreamResource createStreamResource(String text) {

        return new StreamResource("qrcode.png", () ->

                new ByteArrayInputStream(
                        generateQRCode(text, 600, 600)));
    }

    /**
     * Generate BufferedImage with logo.
     */
    public BufferedImage generateImage(String text, int size) {

        try {

            Map<EncodeHintType, Object> hints = new HashMap<>();

            hints.put(
                    EncodeHintType.ERROR_CORRECTION,
                    ErrorCorrectionLevel.H);

            hints.put(
                    EncodeHintType.MARGIN,
                    1);

            BitMatrix matrix =
                    new MultiFormatWriter().encode(
                            text,
                            BarcodeFormat.QR_CODE,
                            size,
                            size,
                            hints);

            BufferedImage qr =
                    MatrixToImageWriter.toBufferedImage(matrix);

            drawLogo(qr);

            return qr;

        } catch (Exception ex) {
            throw new RuntimeException(ex);
        }
    }

    private void drawLogo(BufferedImage qr) {

        try (InputStream is =
                     getClass().getResourceAsStream(LOGO_PATH)) {

            if (is == null) {
                return;
            }

            BufferedImage logo = ImageIO.read(is);

            Graphics2D g = qr.createGraphics();

            g.setRenderingHint(
                    RenderingHints.KEY_ANTIALIASING,
                    RenderingHints.VALUE_ANTIALIAS_ON);

            int qrSize = qr.getWidth();

            int logoSize = qrSize / 5;

            int x = (qrSize - logoSize) / 2;
            int y = (qrSize - logoSize) / 2;

            int padding = 8;

            Shape bg =
                    new RoundRectangle2D.Double(
                            x - padding,
                            y - padding,
                            logoSize + padding * 2,
                            logoSize + padding * 2,
                            20,
                            20);

            g.setColor(Color.WHITE);
            g.fill(bg);

            g.setComposite(AlphaComposite.SrcOver);

            g.drawImage(
                    logo,
                    x,
                    y,
                    logoSize,
                    logoSize,
                    null);

            g.dispose();

        } catch (Exception ignored) {
        }
    }
}