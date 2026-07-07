package in.bachatsetu.backend.receipt.interfaces.rest.adapter;

import com.lowagie.text.Document;
import com.lowagie.text.DocumentException;
import com.lowagie.text.Element;
import com.lowagie.text.Font;
import com.lowagie.text.FontFactory;
import com.lowagie.text.PageSize;
import com.lowagie.text.Paragraph;
import com.lowagie.text.Phrase;
import com.lowagie.text.Rectangle;
import com.lowagie.text.pdf.PdfPCell;
import com.lowagie.text.pdf.PdfPTable;
import com.lowagie.text.pdf.PdfWriter;
import in.bachatsetu.backend.receipt.application.port.ReceiptPdfGenerator;
import in.bachatsetu.backend.receipt.application.query.ReceiptLineResult;
import in.bachatsetu.backend.receipt.application.query.ReceiptResult;
import java.awt.Color;
import java.io.ByteArrayOutputStream;
import java.math.BigDecimal;
import java.text.DecimalFormat;
import java.time.ZoneOffset;
import java.time.format.DateTimeFormatter;
import java.util.Locale;
import java.util.Objects;

/** Renders a receipt's application view as a professional-looking PDF document using OpenPDF. */
public final class OpenPdfReceiptPdfGenerator implements ReceiptPdfGenerator {

    private static final Color BRAND_COLOR = new Color(0x1F, 0x4E, 0x79);
    private static final DateTimeFormatter RECEIPT_DATE =
            DateTimeFormatter.ofPattern("dd MMM yyyy", Locale.ENGLISH).withZone(ZoneOffset.UTC);
    private static final DateTimeFormatter GENERATED_AT =
            DateTimeFormatter.ofPattern("dd MMM yyyy, HH:mm:ss 'UTC'", Locale.ENGLISH).withZone(ZoneOffset.UTC);

    private static final Font TITLE_FONT = FontFactory.getFont(FontFactory.HELVETICA_BOLD, 22, BRAND_COLOR);
    private static final Font SUBTITLE_FONT = FontFactory.getFont(FontFactory.HELVETICA, 12, Color.DARK_GRAY);
    private static final Font LABEL_FONT = FontFactory.getFont(FontFactory.HELVETICA_BOLD, 10, Color.BLACK);
    private static final Font VALUE_FONT = FontFactory.getFont(FontFactory.HELVETICA, 10, Color.BLACK);
    private static final Font TABLE_HEADER_FONT = FontFactory.getFont(FontFactory.HELVETICA_BOLD, 10, Color.WHITE);
    private static final Font TABLE_CELL_FONT = FontFactory.getFont(FontFactory.HELVETICA, 10, Color.BLACK);
    private static final Font TOTAL_FONT = FontFactory.getFont(FontFactory.HELVETICA_BOLD, 12, Color.BLACK);
    private static final Font FOOTER_FONT = FontFactory.getFont(FontFactory.HELVETICA_OBLIQUE, 8, Color.GRAY);

    @Override
    @SuppressWarnings("PMD.CloseResource")
    public byte[] generate(ReceiptResult receipt) {
        Objects.requireNonNull(receipt, "receipt must not be null");
        // Document does not implement Closeable, so try-with-resources is not available;
        // the finally block below always closes it when open instead.
        Document document = new Document(PageSize.A4, 54, 54, 54, 54);
        ByteArrayOutputStream output = new ByteArrayOutputStream();
        try {
            PdfWriter.getInstance(document, output);
            document.open();
            document.add(title());
            document.add(subtitle());
            document.add(spacer());
            document.add(detailsTable(receipt));
            document.add(spacer());
            document.add(linesTable(receipt));
            document.add(totalParagraph(receipt));
            document.add(spacer());
            document.add(generatedAtParagraph(receipt));
            document.add(spacer());
            document.add(footer());
        } catch (DocumentException exception) {
            throw new IllegalStateException("failed to render receipt PDF", exception);
        } finally {
            if (document.isOpen()) {
                document.close();
            }
        }
        return output.toByteArray();
    }

    private Paragraph title() {
        Paragraph paragraph = new Paragraph("BachatSetu", TITLE_FONT);
        paragraph.setAlignment(Element.ALIGN_CENTER);
        return paragraph;
    }

    private Paragraph subtitle() {
        Paragraph paragraph = new Paragraph("Payment Receipt", SUBTITLE_FONT);
        paragraph.setAlignment(Element.ALIGN_CENTER);
        paragraph.setSpacingAfter(16f);
        return paragraph;
    }

    private Paragraph spacer() {
        return new Paragraph(" ");
    }

    private PdfPTable detailsTable(ReceiptResult receipt) throws DocumentException {
        PdfPTable table = new PdfPTable(2);
        table.setWidthPercentage(100);
        table.setWidths(new float[] {1f, 2f});
        addDetailRow(table, "Receipt Number", receipt.number());
        addDetailRow(table, "Receipt Date", RECEIPT_DATE.format(receipt.generatedAt()));
        addDetailRow(table, "Member", receipt.memberId().toString());
        addDetailRow(table, "Payment Reference", receipt.paymentId().toString());
        addDetailRow(table, "Payment Amount", formatAmount(receipt.totalAmountPaise()));
        addDetailRow(table, "Currency", receipt.currencyCode());
        return table;
    }

    private void addDetailRow(PdfPTable table, String label, String value) {
        table.addCell(borderlessCell(new Phrase(label, LABEL_FONT)));
        table.addCell(borderlessCell(new Phrase(value, VALUE_FONT)));
    }

    private PdfPCell borderlessCell(Phrase phrase) {
        PdfPCell cell = new PdfPCell(phrase);
        cell.setBorder(Rectangle.NO_BORDER);
        cell.setPaddingBottom(4f);
        cell.setPaddingTop(4f);
        return cell;
    }

    private PdfPTable linesTable(ReceiptResult receipt) throws DocumentException {
        PdfPTable table = new PdfPTable(3);
        table.setWidthPercentage(100);
        table.setWidths(new float[] {1.2f, 3f, 1.3f});
        table.setSpacingBefore(8f);
        addHeaderCell(table, "Type");
        addHeaderCell(table, "Description");
        addHeaderCell(table, "Amount");
        for (ReceiptLineResult line : receipt.lines()) {
            table.addCell(bodyCell(line.type(), Element.ALIGN_LEFT));
            table.addCell(bodyCell(line.description(), Element.ALIGN_LEFT));
            table.addCell(bodyCell(formatAmount(line.amountPaise()) + " " + line.currencyCode(), Element.ALIGN_RIGHT));
        }
        return table;
    }

    private void addHeaderCell(PdfPTable table, String text) {
        PdfPCell cell = new PdfPCell(new Phrase(text, TABLE_HEADER_FONT));
        cell.setBackgroundColor(BRAND_COLOR);
        cell.setPadding(6f);
        table.addCell(cell);
    }

    private PdfPCell bodyCell(String text, int alignment) {
        PdfPCell cell = new PdfPCell(new Phrase(text, TABLE_CELL_FONT));
        cell.setPadding(6f);
        cell.setHorizontalAlignment(alignment);
        return cell;
    }

    private Paragraph totalParagraph(ReceiptResult receipt) {
        Paragraph paragraph = new Paragraph(
                "Total: " + formatAmount(receipt.totalAmountPaise()) + " " + receipt.currencyCode(), TOTAL_FONT);
        paragraph.setAlignment(Element.ALIGN_RIGHT);
        paragraph.setSpacingBefore(10f);
        return paragraph;
    }

    private Paragraph generatedAtParagraph(ReceiptResult receipt) {
        return new Paragraph("Generated: " + GENERATED_AT.format(receipt.generatedAt()), VALUE_FONT);
    }

    private Paragraph footer() {
        Paragraph paragraph = new Paragraph(
                "This receipt was generated electronically by BachatSetu.", FOOTER_FONT);
        paragraph.setAlignment(Element.ALIGN_CENTER);
        paragraph.setSpacingBefore(24f);
        return paragraph;
    }

    private String formatAmount(long amountPaise) {
        BigDecimal amount = BigDecimal.valueOf(amountPaise, 2);
        DecimalFormat format = new DecimalFormat("#,##0.00");
        return format.format(amount);
    }
}
