package in.bachatsetu.backend.platformoperations.application.usecase;

import in.bachatsetu.backend.platformoperations.application.command.PublishAnnouncementCommand;
import in.bachatsetu.backend.platformoperations.application.query.AnnouncementResult;

@FunctionalInterface
public interface PublishAnnouncementUseCase {

    AnnouncementResult execute(PublishAnnouncementCommand command);
}
