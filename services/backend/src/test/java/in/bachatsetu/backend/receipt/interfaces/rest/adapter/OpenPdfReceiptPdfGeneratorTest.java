package in.bachatsetu.backend.receipt.interfaces.rest.adapter;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import in.bachatsetu.backend.receipt.application.query.ReceiptLineResult;
import in.bachatsetu.backend.receipt.application.query.ReceiptResult;
import java.nio.charset.StandardCharsets;
import java.time.Instant;
import java.util.List;
import java.util.UUID;
import org.junit.jupiter.api.Test;

class OpenPdfReceiptPdfGeneratorTest {

    private static final Instant NOW = Instant.parse("2026-08-07T08:00:00Z");

    private final OpenPdfReceiptPdfGenerator generator = new OpenPdfReceiptPdfGenerator();

    @Test
    void generatesANonEmptyPdfDocument() {
        byte[] pdf = generator.generate(receiptResult("GENERATED", null));

        assertThat(pdf).isNotEmpty();
        assertThat(pdf.length).isGreaterThan(200);
        assertThat(new String(pdf, 0, 5, StandardCharsets.ISO_8859_1)).isEqualTo("%PDF-");
        assertThat(new String(pdf, StandardCharsets.ISO_8859_1)).contains("%%EOF");
    }

    @Test
    void generatesAPdfForAReceiptWithMultipleLines() {
        ReceiptResult receipt = new ReceiptResult(
                UUID.randomUUID(),
                UUID.randomUUID(),
                UUID.randomUUID(),
                UUID.randomUUID(),
                "RCT/20260807/1A2B3C4D",
                List.of(
                        new ReceiptLineResult(UUID.randomUUID(), "CONTRIBUTION", "Monthly contribution", 400_000L, "INR"),
                        new ReceiptLineResult(UUID.randomUUID(), "PENALTY",
                                "Late payment penalty for a very long description that should wrap across "
                                        + "multiple lines within the table cell", 100_000L, "INR")),
                500_000L,
                "INR",
                "GENERATED",
                null,
                NOW,
                NOW,
                0);

        byte[] pdf = generator.generate(receipt);

        assertThat(pdf).isNotEmpty();
        assertThat(new String(pdf, 0, 5, StandardCharsets.ISO_8859_1)).isEqualTo("%PDF-");
    }

    @Test
    void generatesAPdfForACancelledReceiptWithAReason() {
        byte[] pdf = generator.generate(receiptResult("CANCELLED", "issued in error"));

        assertThat(pdf).isNotEmpty();
        assertThat(new String(pdf, 0, 5, StandardCharsets.ISO_8859_1)).isEqualTo("%PDF-");
    }

    @Test
    void producesDifferentBytesForDifferentReceipts() {
        byte[] first = generator.generate(receiptResult("GENERATED", null));
        byte[] second = generator.generate(receiptResult("DELIVERED", null));

        assertThat(first).isNotEqualTo(second);
    }

    @Test
    void rejectsNullInput() {
        assertThatThrownBy(() -> generator.generate(null)).isInstanceOf(NullPointerException.class);
    }

    private ReceiptResult receiptResult(String status, String cancellationReason) {
        return new ReceiptResult(
                UUID.randomUUID(),
                UUID.randomUUID(),
                UUID.randomUUID(),
                UUID.randomUUID(),
                "RCT/20260807/1A2B3C4D",
                List.of(new ReceiptLineResult(UUID.randomUUID(), "CONTRIBUTION", "Monthly contribution", 500_000L, "INR")),
                500_000L,
                "INR",
                status,
                cancellationReason,
                NOW,
                NOW,
                0);
    }
}
