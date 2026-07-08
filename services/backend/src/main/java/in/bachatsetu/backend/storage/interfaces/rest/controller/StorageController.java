package in.bachatsetu.backend.storage.interfaces.rest.controller;

import in.bachatsetu.backend.auth.application.security.AuthenticatedUser;
import in.bachatsetu.backend.auth.application.security.CurrentUserProvider;
import in.bachatsetu.backend.storage.application.query.FileDownloadResult;
import in.bachatsetu.backend.storage.application.query.StoredFileResult;
import in.bachatsetu.backend.storage.application.query.UploadFileResult;
import in.bachatsetu.backend.storage.application.usecase.DeleteFileUseCase;
import in.bachatsetu.backend.storage.application.usecase.DownloadFileUseCase;
import in.bachatsetu.backend.storage.application.usecase.GetFileMetadataUseCase;
import in.bachatsetu.backend.storage.application.usecase.UploadFileUseCase;
import in.bachatsetu.backend.storage.interfaces.rest.dto.StoredFileResponse;
import in.bachatsetu.backend.storage.interfaces.rest.dto.UploadFileResponse;
import in.bachatsetu.backend.storage.interfaces.rest.mapper.StorageApiMapper;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.tags.Tag;
import java.net.URI;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.http.ContentDisposition;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ProblemDetail;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.multipart.MultipartFile;

/** Exposes Storage use cases without leaking domain or persistence models. Authenticated and tenant-scoped. */
@RestController
@RequestMapping(path = "/api/v1/storage/files", produces = MediaType.APPLICATION_JSON_VALUE)
@Tag(name = "Storage", description = "Provider-independent binary file storage")
@ConditionalOnProperty(prefix = "bachatsetu.storage", name = "enabled", havingValue = "true", matchIfMissing = true)
public class StorageController {

    private static final String PROBLEM_CONTENT_TYPE = MediaType.APPLICATION_PROBLEM_JSON_VALUE;

    private final UploadFileUseCase uploadFile;
    private final DownloadFileUseCase downloadFile;
    private final DeleteFileUseCase deleteFile;
    private final GetFileMetadataUseCase getFileMetadata;
    private final CurrentUserProvider currentUserProvider;
    private final StorageApiMapper mapper;

    public StorageController(
            UploadFileUseCase uploadFile,
            DownloadFileUseCase downloadFile,
            DeleteFileUseCase deleteFile,
            GetFileMetadataUseCase getFileMetadata,
            CurrentUserProvider currentUserProvider,
            StorageApiMapper mapper) {
        this.uploadFile = uploadFile;
        this.downloadFile = downloadFile;
        this.deleteFile = deleteFile;
        this.getFileMetadata = getFileMetadata;
        this.currentUserProvider = currentUserProvider;
        this.mapper = mapper;
    }

    @PostMapping(consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    @Operation(summary = "Upload a file", description = "Stores a file through the configured default storage provider.")
    @ApiResponses({
        @ApiResponse(responseCode = "201", description = "File uploaded"),
        @ApiResponse(responseCode = "400", description = "Invalid request", content = @Content(
                mediaType = PROBLEM_CONTENT_TYPE, schema = @Schema(implementation = ProblemDetail.class))),
        @ApiResponse(responseCode = "401", description = "Authentication required", content = @Content(
                mediaType = PROBLEM_CONTENT_TYPE, schema = @Schema(implementation = ProblemDetail.class)))
    })
    public ResponseEntity<UploadFileResponse> upload(@RequestParam("file") MultipartFile file) {
        AuthenticatedUser currentUser = currentUserProvider.requireCurrentUser();
        UploadFileResult result = uploadFile.execute(mapper.toUploadCommand(file, currentUser));
        return ResponseEntity.created(URI.create("/api/v1/storage/files/" + result.fileId()))
                .body(mapper.toUploadResponse(result));
    }

    @GetMapping("/{fileId}")
    @Operation(summary = "Get file metadata", description = "Retrieves metadata for one tenant-scoped stored file.")
    @ApiResponses({
        @ApiResponse(responseCode = "200", description = "Metadata returned"),
        @ApiResponse(responseCode = "401", description = "Authentication required", content = @Content(
                mediaType = PROBLEM_CONTENT_TYPE, schema = @Schema(implementation = ProblemDetail.class))),
        @ApiResponse(responseCode = "404", description = "File not found", content = @Content(
                mediaType = PROBLEM_CONTENT_TYPE, schema = @Schema(implementation = ProblemDetail.class)))
    })
    public StoredFileResponse get(@PathVariable String fileId) {
        AuthenticatedUser currentUser = currentUserProvider.requireCurrentUser();
        StoredFileResult result = getFileMetadata.execute(currentUser.tenantId(), mapper.toFileId(fileId));
        return mapper.toMetadataResponse(result);
    }

    @GetMapping(path = "/{fileId}/download", produces = MediaType.APPLICATION_OCTET_STREAM_VALUE)
    @Operation(summary = "Download a file", description = "Returns one tenant-scoped stored file's bytes.")
    @ApiResponses({
        @ApiResponse(responseCode = "200", description = "File returned"),
        @ApiResponse(responseCode = "401", description = "Authentication required", content = @Content(
                mediaType = PROBLEM_CONTENT_TYPE, schema = @Schema(implementation = ProblemDetail.class))),
        @ApiResponse(responseCode = "404", description = "File not found", content = @Content(
                mediaType = PROBLEM_CONTENT_TYPE, schema = @Schema(implementation = ProblemDetail.class)))
    })
    public ResponseEntity<byte[]> download(@PathVariable String fileId) {
        AuthenticatedUser currentUser = currentUserProvider.requireCurrentUser();
        FileDownloadResult result = downloadFile.execute(currentUser.tenantId(), mapper.toFileId(fileId));
        ContentDisposition disposition = ContentDisposition.attachment().filename(result.filename()).build();
        return ResponseEntity.ok()
                .contentType(MediaType.parseMediaType(result.contentType()))
                .header(HttpHeaders.CONTENT_DISPOSITION, disposition.toString())
                .body(result.content());
    }

    @DeleteMapping("/{fileId}")
    @Operation(
            summary = "Delete a file",
            description = "Removes the physical object and soft-deletes its metadata.")
    @ApiResponses({
        @ApiResponse(responseCode = "204", description = "File deleted"),
        @ApiResponse(responseCode = "401", description = "Authentication required", content = @Content(
                mediaType = PROBLEM_CONTENT_TYPE, schema = @Schema(implementation = ProblemDetail.class))),
        @ApiResponse(responseCode = "404", description = "File not found", content = @Content(
                mediaType = PROBLEM_CONTENT_TYPE, schema = @Schema(implementation = ProblemDetail.class)))
    })
    public ResponseEntity<Void> delete(@PathVariable String fileId) {
        AuthenticatedUser currentUser = currentUserProvider.requireCurrentUser();
        deleteFile.execute(currentUser.tenantId(), mapper.toFileId(fileId));
        return ResponseEntity.status(HttpStatus.NO_CONTENT).build();
    }
}
