package in.bachatsetu.backend.receipt.interfaces.rest.controller;

import in.bachatsetu.backend.auth.application.security.AuthenticatedUser;
import in.bachatsetu.backend.auth.application.security.CurrentUserProvider;
import in.bachatsetu.backend.receipt.application.query.ReceiptPdfStorageResult;
import in.bachatsetu.backend.receipt.application.usecase.GetReceiptPdfStorageUrlUseCase;
import in.bachatsetu.backend.receipt.interfaces.rest.dto.ReceiptPdfStorageUrlResponse;
import in.bachatsetu.backend.receipt.interfaces.rest.mapper.ReceiptApiMapper;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.http.MediaType;
import org.springframework.http.ProblemDetail;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/**
 * Optional Receipt PDF → Storage integration endpoint. Entirely separate from {@link ReceiptController}'s
 * pre-existing {@code /pdf} endpoint, which this class never touches — disabled by default, and only
 * registered at all when {@code bachatsetu.receipt.storage-upload.enabled=true}.
 */
@RestController
@RequestMapping(path = "/api/v1/receipts", produces = MediaType.APPLICATION_JSON_VALUE)
@Tag(name = "Receipts", description = "Optional Receipt PDF to Storage integration")
@ConditionalOnProperty(prefix = "bachatsetu.receipt.storage-upload", name = "enabled", havingValue = "true")
public class ReceiptPdfStorageController {

    private static final String PROBLEM_CONTENT_TYPE = MediaType.APPLICATION_PROBLEM_JSON_VALUE;

    private final GetReceiptPdfStorageUrlUseCase getReceiptPdfStorageUrl;
    private final CurrentUserProvider currentUserProvider;
    private final ReceiptApiMapper mapper;

    public ReceiptPdfStorageController(
            GetReceiptPdfStorageUrlUseCase getReceiptPdfStorageUrl,
            CurrentUserProvider currentUserProvider,
            ReceiptApiMapper mapper) {
        this.getReceiptPdfStorageUrl = getReceiptPdfStorageUrl;
        this.currentUserProvider = currentUserProvider;
        this.mapper = mapper;
    }

    @GetMapping("/{receiptId}/pdf/storage-url")
    @Operation(
            summary = "Upload a receipt PDF to storage",
            description = "Renders one tenant-scoped receipt as a PDF, uploads it through the Storage "
                    + "module, and returns a download URL.")
    @ApiResponses({
        @ApiResponse(responseCode = "200", description = "PDF uploaded and URL returned"),
        @ApiResponse(responseCode = "401", description = "Authentication required", content = @Content(
                mediaType = PROBLEM_CONTENT_TYPE, schema = @Schema(implementation = ProblemDetail.class))),
        @ApiResponse(responseCode = "404", description = "Receipt not found", content = @Content(
                mediaType = PROBLEM_CONTENT_TYPE, schema = @Schema(implementation = ProblemDetail.class)))
    })
    public ReceiptPdfStorageUrlResponse getStorageUrl(@PathVariable String receiptId) {
        AuthenticatedUser currentUser = currentUserProvider.requireCurrentUser();
        ReceiptPdfStorageResult result = mapper.getReceiptPdfStorageUrl(getReceiptPdfStorageUrl, currentUser, receiptId);
        return mapper.toStorageUrlResponse(result);
    }
}
