package in.bachatsetu.backend.platformoperations.domain.port;

import in.bachatsetu.backend.platformoperations.domain.model.Announcement;
import in.bachatsetu.backend.shared.domain.AggregateId;
import in.bachatsetu.backend.shared.domain.Page;
import in.bachatsetu.backend.shared.domain.PageQuery;
import java.time.Instant;
import java.util.List;
import java.util.Optional;

public interface AnnouncementRepository {

    void save(Announcement announcement);

    Optional<Announcement> findById(AggregateId announcementId);

    Page<Announcement> findAll(PageQuery pageQuery);

    List<Announcement> findActive(Instant now);
}
