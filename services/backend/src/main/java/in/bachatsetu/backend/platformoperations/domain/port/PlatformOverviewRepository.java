package in.bachatsetu.backend.platformoperations.domain.port;

import in.bachatsetu.backend.platformoperations.domain.model.PlatformOverviewSnapshot;
import java.time.Instant;

/** Computes the platform-wide dashboard snapshot on demand — no scheduled aggregation, no caching. */
public interface PlatformOverviewRepository {

    PlatformOverviewSnapshot compute(Instant todayStart, Instant todayEnd);
}
