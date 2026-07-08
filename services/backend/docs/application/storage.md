# Storage Module

## Purpose

Introduces a provider-independent Storage module that lets other modules store and retrieve binary files
without coupling any business logic to a specific cloud provider. Supported providers: Local File System,
AWS S3, Azure Blob Storage, and Google Cloud Storage. No business module (Receipt, Payment Gateway, ...)
talks to a provider SDK directly — every module goes through this module's use cases.

## Architecture

Hexagonal/DDD, mirroring the `paymentgateway`, `notification`, and `receipt` modules exactly:

```
storage
 ├── domain          StorageProvider, StoredFile aggregate, StorageRepository port
 ├── application     ports, commands, results, use cases, services
 └── interfaces/rest adapters, DTOs, mapper, controller, exception handler, config
```

```
                 ┌────────────────────┐
                 │  StorageController │
                 └─────────┬──────────┘
                           │ application boundary only
                 ┌─────────▼──────────┐
                 │  Upload/Download/  │
                 │  Delete/GetMetadata│
                 │     Use Cases      │
                 └────┬─────────┬─────┘
                      │         │
        ┌─────────────▼──┐   ┌──▼───────────────┐
        │ StorageRepository│  │ StoragePort /     │
        │ (StoredFile)     │  │ FileDownloadPort / │
        └─────────────────┘  │ FileDeletePort     │
                              └──┬────────┬────────┘
                    ┌────────────┘        └───────────┐
              ┌─────▼─────┐                     ┌──────▼──────┐
              │   Local    │  ...also Azure/GCS  │  AWS S3      │
              │ (real I/O) │                     │ (simulated)  │
              └────────────┘                     └──────────────┘
```

The domain layer (`StorageProvider`, `StoredFile`) never imports Spring, JPA, or any cloud SDK — verified by
the existing `ForbiddenDependencyArchitectureTest` and `LayerDependencyArchitectureTest` ArchUnit rules,
which apply to every module's `..domain..`/`..application..` packages, this one included. `StorageController`
depends only on the application boundary (use cases, DTOs, mapper) — never on `StorageProvider` or any
persistence type — satisfying `CONTROLLERS_MUST_NOT_DEPEND_ON_DOMAIN_OR_INFRASTRUCTURE`; provider name
conversion (`StorageProvider` ⇄ `String`) happens inside `StorageApiMapper`, which is allowed to depend on
domain.

## Providers

| Provider | `StorageProvider` value | Implementation |
|---|---|---|
| Local File System | `LOCAL` | Real: writes to a configurable directory, one subdirectory per tenant |
| AWS S3 | `AWS_S3` | Simulated: adapter structure only, no AWS SDK |
| Azure Blob Storage | `AZURE_BLOB` | Simulated: adapter structure only, no Azure SDK |
| Google Cloud Storage | `GOOGLE_CLOUD_STORAGE` | Simulated: adapter structure only, no Google Cloud SDK |

`bachatsetu.storage.default-provider` selects which provider `UploadFileUseCase` uses. `StoragePortResolver`
picks the matching `StoragePort`/`FileDownloadPort`/`FileDeletePort` bean by `StorageProvider` at call time —
application code never names a concrete adapter class, only the enum value ("provider switching").

### Simulated Provider Adapters

**No AWS SDK, Azure SDK, or Google Cloud SDK is used anywhere in this codebase.** Following the Payment
Gateway simulated-adapter pattern from Sprint 12.2, `SimulatedAwsS3StorageAdapter`,
`SimulatedAzureBlobStorageAdapter`, and `SimulatedGoogleCloudStorageAdapter` generate a deterministic-looking
fake object key/URL on `store(...)` and log what a real SDK call would do. Because no bytes are actually
persisted anywhere for these three providers, their `download(...)`/`delete(...)` methods are placeholders
too — they log the request and return an empty result rather than fabricating file content. Only Local
Storage (`LocalFileStorageAdapter`) is a genuinely working implementation, since it needs no credentials and
is the safe default for every environment this codebase runs in.

## Upload Flow

1. `POST /api/v1/storage/files` (multipart/form-data) — the caller's tenant and actor come from the
   authenticated identity, never from the request body.
2. `StorageApiMapper` extracts filename/content-type from the `MultipartFile` (falling back to
   `"upload"`/`"application/octet-stream"` if either is blank) and builds an `UploadFileCommand`.
3. `UploadFileApplicationService` resolves the configured default provider's `StoragePort`, computes a
   SHA-256 checksum of the original bytes via `ChecksumGeneratorPort`, calls `StoragePort.store(...)` to get
   back a provider-specific path/key, and persists a new `StoredFile` aggregate recording that path,
   checksum, size, and metadata.
4. The checksum is computed once, from the caller's original bytes, and never recomputed from
   provider-returned data — it always reflects exactly what was uploaded.
5. Response: `{ fileId, provider, path }` (201 Created).

## Download Flow

1. `GET /api/v1/storage/files/{fileId}/download` — tenant-scoped; a file from another tenant is treated as
   not found.
2. `DownloadFileApplicationService` loads the `StoredFile` metadata, resolves the `FileDownloadPort` matching
   the file's own recorded provider (not the current default — a file stays associated with whichever
   provider it was actually uploaded to), and reads the bytes back.
3. Response: raw bytes with the original content type and a `Content-Disposition: attachment` header
   carrying the original filename.

## Checksum Generation

`ChecksumGeneratorPort` is a single-method abstraction (`generate(byte[]) -> String`) implemented by
`Sha256ChecksumGeneratorAdapter`, using only `java.security.MessageDigest` — no external hashing library.
The checksum is provider-independent: it is computed before the bytes reach any `StoragePort` and stored on
`StoredFile` regardless of which provider ends up holding the bytes.

## Metadata Storage

`StoredFile` is a new aggregate (permitted under this sprint's rules) holding: `id`, `tenantId`, `provider`,
`path`, `originalFilename`, `contentType`, `size`, `checksum`, `uploadedAt`. It never references any other
module's aggregate — callers (Receipt, and any future module) identify their uploaded file only by the
returned `fileId`, matching this codebase's rule that cross-module relationships are identifiers, not object
references. Persisted additively in `storage.stored_files` (see Configuration/migration below), following
the same audit-column and soft-delete conventions (`created_at/by`, `updated_at/by`, `version`, `is_deleted`,
`deleted_at`) as every other table in this schema.

## Delete Flow

`DELETE /api/v1/storage/files/{fileId}` — `DeleteFileApplicationService` loads the file, calls
`FileDeletePort.delete(path)` to remove the physical object **first**, and only then soft-deletes the
metadata row. If the physical removal fails, the metadata is left intact so the operation can be retried
against a file that still genuinely exists. Returns 204 No Content.

## Configuration

```yaml
bachatsetu:
  storage:
    enabled: ${STORAGE_ENABLED:true}
    default-provider: ${STORAGE_DEFAULT_PROVIDER:LOCAL}
    local:
      path: ${STORAGE_LOCAL_PATH:./data/storage}
    aws:
      bucket: ${STORAGE_AWS_BUCKET:}
      region: ${STORAGE_AWS_REGION:}
      access-key-id: ${STORAGE_AWS_ACCESS_KEY_ID:}
      secret-access-key: ${STORAGE_AWS_SECRET_ACCESS_KEY:}
    azure:
      account-name: ${STORAGE_AZURE_ACCOUNT_NAME:}
      account-key: ${STORAGE_AZURE_ACCOUNT_KEY:}
      container-name: ${STORAGE_AZURE_CONTAINER_NAME:}
    gcp:
      bucket: ${STORAGE_GCP_BUCKET:}
      project-id: ${STORAGE_GCP_PROJECT_ID:}
      credentials-json: ${STORAGE_GCP_CREDENTIALS_JSON:}
```

Every AWS/Azure/GCP credential defaults to an empty string via `${ENV_VAR:}` placeholders — never hardcoded.
These three providers' settings exist ahead of real SDK integration; today's simulated adapters do not read
them (only `local.path` is actually used, by `LocalFileStorageAdapter`).

Persistence: `db/migration/V8__storage_files.sql` — additive only. Creates a new `storage` schema and the
`storage.stored_files` table; does not alter any existing table.

## Receipt Integration

The pre-existing Receipt PDF endpoint (`GET /api/v1/receipts/{id}/pdf`, backed by `GetReceiptPdfUseCase`) is
completely unchanged — same signature, same behavior, same response. A separate, additive, disabled-by-default
integration chains it into Storage:

```
Receipt PDF (GetReceiptPdfUseCase, unchanged)
        ↓
Storage Upload (UploadFileUseCase)
        ↓
StoredFile (persisted by the Storage module)
        ↓
return download URL
```

`GetReceiptPdfStorageUrlUseCase` (new, in the `receipt.application` package) calls `GetReceiptPdfUseCase`
unchanged, uploads the resulting bytes through `UploadFileUseCase`, and returns a URL pointing at the
Storage module's own `/api/v1/storage/files/{id}/download` endpoint — never a raw provider-internal path
(which, for Local Storage, is a filesystem path and must not be exposed to a client).

Configuration: `bachatsetu.receipt.storage-upload.enabled` (default `false`). The `GetReceiptPdfStorageUrlUseCase`
bean and the `ReceiptPdfStorageController` (`GET /api/v1/receipts/{id}/pdf/storage-url`) only exist at all when
this is explicitly set to `true` — disabled means the feature is entirely absent from the application context,
not merely inactive.

Payment Gateway is **not** integrated with Storage in this sprint (gateway webhook/receipt attachments are a
future concern) — only ensured to be reusable: the same `UploadFileUseCase`/`DownloadFileUseCase` any module
needs are already in place.

## Testing

Domain (`StoredFileTest`), application services (upload/download/delete/metadata, including not-found and
null-command cases), provider adapters (`LocalFileStorageAdapterTest` — real round-trip store/download/delete
against a JUnit `@TempDir`; `SimulatedStorageAdapterTest` — structural checks for all three cloud providers),
checksum generation (`Sha256ChecksumGeneratorAdapterTest`, including the well-known SHA-256 digest of an empty
input), infrastructure adapters (clock/transaction), REST mapper and controller (upload/metadata/download/delete,
including unauthenticated and not-found cases), infrastructure/application config wiring (provider bean counts,
persistence-gating), persistence adapter, and a Testcontainers-based persistence integration test covering
save/reload, soft-delete, and cross-tenant isolation. The Receipt integration adds its own application-service
and controller tests, plus additional `ReceiptApiMapper` mapping tests — all without touching any pre-existing
Receipt test.

## Limitations

- AWS S3, Azure Blob, and Google Cloud Storage adapters are structural placeholders: no real SDK, no network
  call, no actual byte persistence. `download`/`delete` for these three providers log the request and return
  a placeholder result rather than real data.
- No distributed lock or optimistic-concurrency guard for concurrent uploads/deletes of the same file under
  high concurrency.
- No file size limit is enforced by this module beyond Spring's default multipart configuration.
- No virus/content scanning.
- The Receipt PDF → Storage integration always uses the configured default provider; there is no per-request
  provider override.
