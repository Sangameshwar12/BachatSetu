package in.bachatsetu.backend.draw.application.usecase;

import in.bachatsetu.backend.draw.application.query.DrawSummary;
import in.bachatsetu.backend.draw.domain.port.DrawPage;
import in.bachatsetu.backend.draw.domain.port.DrawPageRequest;
import in.bachatsetu.backend.shared.domain.AggregateId;

/** Lists compact draw views within a tenant, paginated at the persistence boundary. */
@FunctionalInterface
public interface ListDrawsUseCase {

    DrawPage<DrawSummary> execute(AggregateId tenantId, DrawPageRequest pageRequest);
}
