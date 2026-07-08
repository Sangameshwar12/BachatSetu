package in.bachatsetu.backend.storage.application.usecase;

import in.bachatsetu.backend.storage.application.command.UploadFileCommand;
import in.bachatsetu.backend.storage.application.query.UploadFileResult;

/** Stores a new file and records its metadata. */
@FunctionalInterface
public interface UploadFileUseCase {

    UploadFileResult execute(UploadFileCommand command);
}
