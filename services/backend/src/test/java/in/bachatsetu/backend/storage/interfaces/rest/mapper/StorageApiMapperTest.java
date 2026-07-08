package in.bachatsetu.backend.storage.interfaces.rest.mapper;

import static org.assertj.core.api.Assertions.assertThat;

import in.bachatsetu.backend.auth.application.security.AuthenticatedUser;
import in.bachatsetu.backend.auth.domain.model.MobileNumber;
import in.bachatsetu.backend.auth.domain.model.UserId;
import in.bachatsetu.backend.shared.domain.AggregateId;
import in.bachatsetu.backend.storage.application.command.UploadFileCommand;
import in.bachatsetu.backend.storage.application.query.StoredFileResult;
import in.bachatsetu.backend.storage.application.query.UploadFileResult;
import in.bachatsetu.backend.storage.domain.model.StorageProvider;
import in.bachatsetu.backend.storage.interfaces.rest.dto.StoredFileResponse;
import in.bachatsetu.backend.storage.interfaces.rest.dto.UploadFileResponse;
import java.time.Instant;
import java.util.Set;
import java.util.UUID;
import org.junit.jupiter.api.Test;
import org.springframework.mock.web.MockMultipartFile;

class StorageApiMapperTest {

    private static final Instant NOW = Instant.parse("2026-07-08T08:00:00Z");

    private final StorageApiMapper mapper = new StorageApiMapper();

    @Test
    void mapsAMultipartFileToAnUploadCommand() {
        AggregateId tenantId = AggregateId.newId();
        AuthenticatedUser currentUser = new AuthenticatedUser(
                UserId.newId(), MobileNumber.of("+919876543210"), tenantId, Set.of("GROUP_MEMBER"),
                Set.of("storage.write"));
        MockMultipartFile file = new MockMultipartFile("file", "receipt.pdf", "application/pdf", "content".getBytes());

        UploadFileCommand command = mapper.toUploadCommand(file, currentUser);

        assertThat(command.tenantId()).isEqualTo(tenantId);
        assertThat(command.filename()).isEqualTo("receipt.pdf");
        assertThat(command.contentType()).isEqualTo("application/pdf");
        assertThat(command.content()).isEqualTo("content".getBytes());
    }

    @Test
    void fallsBackToDefaultsWhenFilenameAndContentTypeAreMissing() {
        AuthenticatedUser currentUser = new AuthenticatedUser(
                UserId.newId(), MobileNumber.of("+919876543210"), AggregateId.newId(), Set.of("GROUP_MEMBER"),
                Set.of("storage.write"));
        MockMultipartFile file = new MockMultipartFile("file", null, null, "content".getBytes());

        UploadFileCommand command = mapper.toUploadCommand(file, currentUser);

        assertThat(command.filename()).isEqualTo("upload");
        assertThat(command.contentType()).isEqualTo("application/octet-stream");
    }

    @Test
    void mapsAFileIdString() {
        AggregateId fileId = AggregateId.newId();

        assertThat(mapper.toFileId(fileId.toString())).isEqualTo(fileId);
    }

    @Test
    void mapsUploadResultToResponse() {
        UUID fileId = UUID.randomUUID();
        UploadFileResult result = new UploadFileResult(fileId, StorageProvider.LOCAL, "/data/storage/file-1");

        UploadFileResponse response = mapper.toUploadResponse(result);

        assertThat(response.fileId()).isEqualTo(fileId.toString());
        assertThat(response.provider()).isEqualTo("LOCAL");
        assertThat(response.path()).isEqualTo("/data/storage/file-1");
    }

    @Test
    void mapsMetadataResultToResponse() {
        StoredFileResult result = new StoredFileResult(
                UUID.randomUUID(), UUID.randomUUID(), StorageProvider.AWS_S3, "s3://bucket/key", "file.txt",
                "text/plain", 100L, "checksum", NOW);

        StoredFileResponse response = mapper.toMetadataResponse(result);

        assertThat(response.provider()).isEqualTo("AWS_S3");
        assertThat(response.filename()).isEqualTo("file.txt");
        assertThat(response.size()).isEqualTo(100L);
    }
}
