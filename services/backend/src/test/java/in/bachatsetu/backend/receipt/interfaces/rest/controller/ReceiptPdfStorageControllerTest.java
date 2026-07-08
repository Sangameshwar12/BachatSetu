package in.bachatsetu.backend.receipt.interfaces.rest.controller;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import in.bachatsetu.backend.auth.application.security.AuthenticatedUser;
import in.bachatsetu.backend.auth.application.security.CurrentUserProvider;
import in.bachatsetu.backend.auth.application.security.CurrentUserUnavailableException;
import in.bachatsetu.backend.auth.domain.model.MobileNumber;
import in.bachatsetu.backend.auth.domain.model.UserId;
import in.bachatsetu.backend.receipt.application.query.ReceiptPdfStorageResult;
import in.bachatsetu.backend.receipt.application.usecase.GetReceiptPdfStorageUrlUseCase;
import in.bachatsetu.backend.receipt.interfaces.rest.exception.ReceiptExceptionHandler;
import in.bachatsetu.backend.receipt.interfaces.rest.mapper.ReceiptApiMapper;
import in.bachatsetu.backend.shared.domain.AggregateId;
import java.util.Set;
import java.util.UUID;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.context.annotation.Import;
import org.springframework.test.context.TestPropertySource;
import org.springframework.test.web.servlet.MockMvc;

@WebMvcTest(ReceiptPdfStorageController.class)
@AutoConfigureMockMvc(addFilters = false)
@Import({ReceiptApiMapper.class, ReceiptExceptionHandler.class})
@TestPropertySource(properties = "bachatsetu.receipt.storage-upload.enabled=true")
class ReceiptPdfStorageControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @MockBean
    private GetReceiptPdfStorageUrlUseCase getReceiptPdfStorageUrl;

    @MockBean
    private CurrentUserProvider currentUserProvider;

    @Test
    void returnsADownloadUrlForAnUploadedReceiptPdf() throws Exception {
        when(currentUserProvider.requireCurrentUser()).thenReturn(authenticatedUser());
        UUID receiptId = UUID.randomUUID();
        UUID fileId = UUID.randomUUID();
        when(getReceiptPdfStorageUrl.execute(any(), any(), any())).thenReturn(
                new ReceiptPdfStorageResult(fileId, "/api/v1/storage/files/" + fileId + "/download"));

        mockMvc.perform(get("/api/v1/receipts/" + receiptId + "/pdf/storage-url"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.fileId").value(fileId.toString()))
                .andExpect(jsonPath("$.downloadUrl").value("/api/v1/storage/files/" + fileId + "/download"));
    }

    @Test
    void rejectsUnauthenticatedRequests() throws Exception {
        when(currentUserProvider.requireCurrentUser()).thenThrow(new CurrentUserUnavailableException());

        mockMvc.perform(get("/api/v1/receipts/" + UUID.randomUUID() + "/pdf/storage-url"))
                .andExpect(status().isUnauthorized())
                .andExpect(jsonPath("$.code").value("authentication-required"));
    }

    private AuthenticatedUser authenticatedUser() {
        return new AuthenticatedUser(
                UserId.newId(), MobileNumber.of("+919876543210"), AggregateId.newId(), Set.of("GROUP_MEMBER"),
                Set.of("receipt.read"));
    }
}
