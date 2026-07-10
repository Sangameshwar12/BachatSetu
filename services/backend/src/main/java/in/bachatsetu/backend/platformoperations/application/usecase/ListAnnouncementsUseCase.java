package in.bachatsetu.backend.platformoperations.application.usecase;

import in.bachatsetu.backend.platformoperations.application.query.AnnouncementResult;
import in.bachatsetu.backend.shared.domain.Page;
import in.bachatsetu.backend.shared.domain.PageQuery;

@FunctionalInterface
public interface ListAnnouncementsUseCase {

    Page<AnnouncementResult> execute(PageQuery pageQuery);
}
