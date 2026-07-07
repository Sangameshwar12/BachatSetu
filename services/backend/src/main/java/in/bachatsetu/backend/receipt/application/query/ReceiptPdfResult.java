package in.bachatsetu.backend.receipt.application.query;

import java.util.Objects;

/** Rendered PDF bytes for one receipt, paired with a suggested download file name. */
public final class ReceiptPdfResult {

    private final byte[] content;
    private final String fileName;

    public ReceiptPdfResult(byte[] content, String fileName) {
        Objects.requireNonNull(content, "content must not be null");
        this.content = content.clone();
        this.fileName = Objects.requireNonNull(fileName, "file name must not be null");
    }

    public byte[] content() {
        return content.clone();
    }

    public String fileName() {
        return fileName;
    }
}
