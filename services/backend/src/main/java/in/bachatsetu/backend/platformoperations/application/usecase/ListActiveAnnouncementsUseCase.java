package in.bachatsetu.backend.platformoperations.application.usecase;

import in.bachatsetu.backend.platformoperations.application.query.AnnouncementResult;
import java.util.List;

@FunctionalInterface
public interface ListActiveAnnouncementsUseCase {

    List<AnnouncementResult> execute();
}
