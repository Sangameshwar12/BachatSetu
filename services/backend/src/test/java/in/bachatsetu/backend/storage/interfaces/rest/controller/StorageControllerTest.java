package in.bachatsetu.backend.storage.interfaces.rest.controller;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.multipart;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.header;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import in.bachatsetu.backend.auth.application.security.AuthenticatedUser;
import in.bachatsetu.backend.auth.application.security.CurrentUserProvider;
import in.bachatsetu.backend.auth.application.security.CurrentUserUnavailableException;
import in.bachatsetu.backend.auth.domain.model.MobileNumber;
import in.bachatsetu.backend.auth.domain.model.UserId;
import in.bachatsetu.backend.shared.domain.AggregateId;
import in.bachatsetu.backend.storage.application.exception.StoredFileNotFoundException;
import in.bachatsetu.backend.storage.application.query.FileDownloadResult;
import in.bachatsetu.backend.storage.application.query.StoredFileResult;
import in.bachatsetu.backend.storage.application.query.UploadFileResult;
import in.bachatsetu.backend.storage.application.usecase.DeleteFileUseCase;
import in.bachatsetu.backend.storage.application.usecase.DownloadFileUseCase;
import in.bachatsetu.backend.storage.application.usecase.GetFileMetadataUseCase;
import in.bachatsetu.backend.storage.application.usecase.UploadFileUseCase;
import in.bachatsetu.backend.storage.domain.model.StorageProvider;
import in.bachatsetu.backend.storage.interfaces.rest.exception.StorageExceptionHandler;
import in.bachatsetu.backend.storage.interfaces.rest.mapper.StorageApiMapper;
import java.time.Instant;
import java.util.Set;
import java.util.UUID;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.context.annotation.Import;
import org.springframework.mock.web.MockMultipartFile;
import org.springframework.test.web.servlet.MockMvc;

@WebMvcTest(StorageController.class)
@AutoConfigureMockMvc(addFilters = false)
@Import({StorageApiMapper.class, StorageExceptionHandler.class})
class StorageControllerTest {

    private static final Instant NOW = Instant.parse("2026-07-08T08:00:00Z");

    @Autowired
    private MockMvc mockMvc;

    @MockBean
    private UploadFileUseCase uploadFile;

    @MockBean
    private DownloadFileUseCase downloadFile;

    @MockBean
    private DeleteFileUseCase deleteFile;

    @MockBean
    private GetFileMetadataUseCase getFileMetadata;

    @MockBean
    private CurrentUserProvider currentUserProvider;

    @Test
    void uploadsAFile() throws Exception {
        when(currentUserProvider.requireCurrentUser()).thenReturn(authenticatedUser());
        UUID fileId = UUID.randomUUID();
        when(uploadFile.execute(any())).thenReturn(new UploadFileResult(fileId, StorageProvider.LOCAL, "/data/file-1"));
        MockMultipartFile file = new MockMultipartFile("file", "a.txt", "text/plain", "content".getBytes());

        mockMvc.perform(multipart("/api/v1/storage/files").file(file))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.fileId").value(fileId.toString()))
                .andExpect(jsonPath("$.provider").value("LOCAL"));
    }

    @Test
    void rejectsUnauthenticatedUpload() throws Exception {
        when(currentUserProvider.requireCurrentUser()).thenThrow(new CurrentUserUnavailableException());
        MockMultipartFile file = new MockMultipartFile("file", "a.txt", "text/plain", "content".getBytes());

        mockMvc.perform(multipart("/api/v1/storage/files").file(file))
                .andExpect(status().isUnauthorized())
                .andExpect(jsonPath("$.code").value("authentication-required"));
    }

    @Test
    void getsFileMetadata() throws Exception {
        when(currentUserProvider.requireCurrentUser()).thenReturn(authenticatedUser());
        UUID fileId = UUID.randomUUID();
        when(getFileMetadata.execute(any(), any())).thenReturn(new StoredFileResult(
                fileId, UUID.randomUUID(), StorageProvider.LOCAL, "/data/file-1", "a.txt", "text/plain", 7L,
                "checksum", NOW));

        mockMvc.perform(get("/api/v1/storage/files/" + fileId))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.filename").value("a.txt"));
    }

    @Test
    void reportsAMissingFileAsNotFound() throws Exception {
        when(currentUserProvider.requireCurrentUser()).thenReturn(authenticatedUser());
        when(getFileMetadata.execute(any(), any())).thenThrow(new StoredFileNotFoundException("not found"));

        mockMvc.perform(get("/api/v1/storage/files/" + UUID.randomUUID()))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.code").value("not-found"));
    }

    @Test
    void downloadsAFile() throws Exception {
        when(currentUserProvider.requireCurrentUser()).thenReturn(authenticatedUser());
        UUID fileId = UUID.randomUUID();
        when(downloadFile.execute(any(), any())).thenReturn(
                new FileDownloadResult(fileId, "a.txt", "text/plain", "content".getBytes()));

        mockMvc.perform(get("/api/v1/storage/files/" + fileId + "/download"))
                .andExpect(status().isOk())
                .andExpect(header().string("Content-Disposition", org.hamcrest.Matchers.containsString("a.txt")));
    }

    @Test
    void deletesAFile() throws Exception {
        when(currentUserProvider.requireCurrentUser()).thenReturn(authenticatedUser());

        mockMvc.perform(delete("/api/v1/storage/files/" + UUID.randomUUID()))
                .andExpect(status().isNoContent());
    }

    private AuthenticatedUser authenticatedUser() {
        return new AuthenticatedUser(
                UserId.newId(), MobileNumber.of("+919876543210"), AggregateId.newId(), Set.of("GROUP_MEMBER"),
                Set.of("storage.write"));
    }
}
