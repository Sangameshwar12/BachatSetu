package in.bachatsetu.backend.admin.domain.port;

import in.bachatsetu.backend.admin.domain.model.PlatformStatistics;

/** Computes platform-wide totals on demand, aggregating across every existing module's own repositories. */
public interface PlatformStatisticsRepository {

    PlatformStatistics compute();
}
