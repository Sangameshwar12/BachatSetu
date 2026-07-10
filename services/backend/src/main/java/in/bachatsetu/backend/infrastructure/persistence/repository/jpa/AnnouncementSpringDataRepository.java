package in.bachatsetu.backend.infrastructure.persistence.repository.jpa;

import in.bachatsetu.backend.infrastructure.persistence.entity.platform.AnnouncementJpaEntity;
import in.bachatsetu.backend.infrastructure.persistence.repository.BaseJpaRepository;
import java.time.Instant;
import java.util.List;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

public interface AnnouncementSpringDataRepository extends BaseJpaRepository<AnnouncementJpaEntity> {

    @Query("""
            SELECT announcement FROM AnnouncementJpaEntity announcement
             WHERE announcement.deleted = false
               AND announcement.startAt <= :now
               AND announcement.endAt >= :now
             ORDER BY announcement.startAt DESC
            """)
    List<AnnouncementJpaEntity> findActive(@Param("now") Instant now);
}
