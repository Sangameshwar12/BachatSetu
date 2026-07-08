package in.bachatsetu.backend.receipt.application.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import in.bachatsetu.backend.receipt.application.query.ReceiptPdfResult;
import in.bachatsetu.backend.receipt.application.query.ReceiptPdfStorageResult;
import in.bachatsetu.backend.receipt.application.usecase.GetReceiptPdfUseCase;
import in.bachatsetu.backend.shared.domain.AggregateId;
import in.bachatsetu.backend.storage.application.command.UploadFileCommand;
import in.bachatsetu.backend.storage.application.query.UploadFileResult;
import in.bachatsetu.backend.storage.application.usecase.UploadFileUseCase;
import in.bachatsetu.backend.storage.domain.model.StorageProvider;
import java.util.UUID;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;

class GetReceiptPdfStorageUrlApplicationServiceTest {

    private GetReceiptPdfUseCase getReceiptPdf;
    private UploadFileUseCase uploadFile;
    private GetReceiptPdfStorageUrlApplicationService service;

    @BeforeEach
    void setUp() {
        getReceiptPdf = mock(GetReceiptPdfUseCase.class);
        uploadFile = mock(UploadFileUseCase.class);
        service = new GetReceiptPdfStorageUrlApplicationService(getReceiptPdf, uploadFile);
    }

    @Test
    void uploadsTheRenderedPdfAndReturnsADownloadUrl() {
        AggregateId tenantId = AggregateId.newId();
        AggregateId receiptId = AggregateId.newId();
        AggregateId actorId = AggregateId.newId();
        ReceiptPdfResult pdf = new ReceiptPdfResult(new byte[] {1, 2, 3}, "RCT-1.pdf");
        when(getReceiptPdf.execute(tenantId, receiptId)).thenReturn(pdf);
        UUID fileId = UUID.randomUUID();
        when(uploadFile.execute(any())).thenReturn(new UploadFileResult(fileId, StorageProvider.LOCAL, "/data/file-1"));

        ReceiptPdfStorageResult result = service.execute(tenantId, receiptId, actorId);

        assertThat(result.fileId()).isEqualTo(fileId);
        assertThat(result.downloadUrl()).isEqualTo("/api/v1/storage/files/" + fileId + "/download");
        ArgumentCaptor<UploadFileCommand> captor = ArgumentCaptor.forClass(UploadFileCommand.class);
        verify(uploadFile).execute(captor.capture());
        assertThat(captor.getValue().filename()).isEqualTo("RCT-1.pdf");
        assertThat(captor.getValue().contentType()).isEqualTo("application/pdf");
        assertThat(captor.getValue().content()).containsExactly(1, 2, 3);
    }
}
