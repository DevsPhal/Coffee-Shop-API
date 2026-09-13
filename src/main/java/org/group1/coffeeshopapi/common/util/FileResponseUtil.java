package org.group1.coffeeshopapi.common.util;

import org.springframework.http.ContentDisposition;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;

// Shared plumbing for the handful of endpoints that hand back a generated file (a receipt PDF, a
// report Excel export) instead of the usual ApiResponse-wrapped JSON body every other endpoint uses.
public final class FileResponseUtil {

    // MediaType has no built-in constant for .xlsx.
    public static final MediaType XLSX = MediaType.parseMediaType(
            "application/vnd.openxmlformats-officedocument.spreadsheetml.sheet");

    private FileResponseUtil() {
    }

    // inline lets a browser open the file directly (e.g. a receipt to preview/print on the spot);
    // false forces a "Save As" download (e.g. a report meant to be opened in Excel).
    public static ResponseEntity<byte[]> respond(byte[] content, MediaType mediaType, String filename, boolean inline) {
        ContentDisposition disposition = (inline ? ContentDisposition.inline() : ContentDisposition.attachment())
                .filename(filename)
                .build();
        return ResponseEntity.ok()
                .contentType(mediaType)
                .header(HttpHeaders.CONTENT_DISPOSITION, disposition.toString())
                .body(content);
    }
}
