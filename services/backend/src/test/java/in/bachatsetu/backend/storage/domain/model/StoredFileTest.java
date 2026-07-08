package in.bachatsetu.backend.storage.domain.model;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import in.bachatsetu.backend.shared.domain.AggregateId;
import in.bachatsetu.backend.shared.domain.AuditInfo;
import java.time.Instant;
import org.junit.jupiter.api.Test;

class StoredFileTest {

    private static final Instant NOW = Instant.parse("2026-07-08T08:00:00Z");

    @Test
    void uploadsAFileWithEveryFieldRecorded() {
        AggregateId tenantId = AggregateId.newId();
        AggregateId actorId = AggregateId.newId();

        StoredFile file = StoredFile.upload(
                AggregateId.newId(), tenantId, StorageProvider.LOCAL, "/data/storage/tenant/file-1",
                "receipt.pdf", "application/pdf", 2048L, "abc123", actorId, NOW);

        assertThat(file.tenantId()).isEqualTo(tenantId);
        assertThat(file.provider()).isEqualTo(StorageProvider.LOCAL);
        assertThat(file.path()).isEqualTo("/data/storage/tenant/file-1");
        assertThat(file.originalFilename()).isEqualTo("receipt.pdf");
        assertThat(file.contentType()).isEqualTo("application/pdf");
        assertThat(file.size()).isEqualTo(2048L);
        assertThat(file.checksum()).isEqualTo("abc123");
        assertThat(file.uploadedAt()).isEqualTo(NOW);
        assertThat(file.auditInfo().createdBy()).isEqualTo(actorId);
    }

    @Test
    void rejectsABlankPath() {
        assertThatThrownBy(() -> newFile(" ", "file.pdf", "application/pdf", "checksum"))
                .isInstanceOf(IllegalArgumentException.class);
    }

    @Test
    void rejectsABlankFilename() {
        assertThatThrownBy(() -> newFile("/data/path", " ", "application/pdf", "checksum"))
                .isInstanceOf(IllegalArgumentException.class);
    }

    @Test
    void rejectsABlankContentType() {
        assertThatThrownBy(() -> newFile("/data/path", "file.pdf", " ", "checksum"))
                .isInstanceOf(IllegalArgumentException.class);
    }

    @Test
    void rejectsABlankChecksum() {
        assertThatThrownBy(() -> newFile("/data/path", "file.pdf", "application/pdf", " "))
                .isInstanceOf(IllegalArgumentException.class);
    }

    @Test
    void rejectsANegativeSize() {
        assertThatThrownBy(() -> new StoredFile(
                        AggregateId.newId(), AggregateId.newId(), StorageProvider.LOCAL, "/data/path", "file.pdf",
                        "application/pdf", -1L, "checksum", NOW,
                        AuditInfo.createdBy(AggregateId.newId(), NOW), 0))
                .isInstanceOf(IllegalArgumentException.class);
    }

    private StoredFile newFile(String path, String filename, String contentType, String checksum) {
        return StoredFile.upload(
                AggregateId.newId(), AggregateId.newId(), StorageProvider.LOCAL, path, filename, contentType, 10L,
                checksum, AggregateId.newId(), NOW);
    }
}
