package org.group1.coffeeshopapi.common.util;

import com.google.zxing.BarcodeFormat;
import com.google.zxing.EncodeHintType;
import com.google.zxing.WriterException;
import com.google.zxing.client.j2se.MatrixToImageWriter;
import com.google.zxing.common.BitMatrix;
import com.google.zxing.qrcode.QRCodeWriter;
import com.google.zxing.qrcode.decoder.ErrorCorrectionLevel;
import org.group1.coffeeshopapi.common.exception.InvalidOperationException;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.util.Map;

// Renders raw QR payload text (e.g. a KHQR string) into a scannable PNG image.
public final class QrImageUtil {

    private QrImageUtil() {
    }

    public static byte[] toPng(String content, int size) {
        try {
            Map<EncodeHintType, Object> hints = Map.of(
                    EncodeHintType.ERROR_CORRECTION, ErrorCorrectionLevel.M,
                    EncodeHintType.MARGIN, 1);
            BitMatrix matrix = new QRCodeWriter().encode(content, BarcodeFormat.QR_CODE, size, size, hints);

            ByteArrayOutputStream out = new ByteArrayOutputStream();
            MatrixToImageWriter.writeToStream(matrix, "PNG", out);
            return out.toByteArray();
        } catch (WriterException | IOException e) {
            throw new InvalidOperationException("Unable to render QR image: " + e.getMessage());
        }
    }
}
