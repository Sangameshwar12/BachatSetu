package in.bachatsetu.backend.draw.domain.port;

import in.bachatsetu.backend.draw.domain.model.Draw;
import in.bachatsetu.backend.draw.domain.model.DrawNumber;
import in.bachatsetu.backend.draw.domain.model.DrawType;
import in.bachatsetu.backend.shared.domain.AggregateId;
import java.time.Instant;
import java.util.List;
import java.util.Optional;

public interface DrawRepository {

    Optional<Draw> findById(AggregateId drawId);

    Optional<Draw> findById(AggregateId tenantId, AggregateId drawId);

    DrawPage<Draw> findPage(AggregateId tenantId, DrawPageRequest pageRequest);

    DrawPage<Draw> findPageByType(AggregateId tenantId, DrawType type, DrawPageRequest pageRequest);

    Optional<Draw> findByGroupAndNumber(AggregateId groupId, DrawNumber drawNumber);

    Optional<Draw> findByCycleId(AggregateId cycleId);

    /** The soonest {@link in.bachatsetu.backend.draw.domain.model.DrawStatus#SCHEDULED} draw for a group. */
    Optional<Draw> findNextScheduledByGroup(AggregateId tenantId, AggregateId groupId);

    /** Every {@link in.bachatsetu.backend.draw.domain.model.DrawStatus#SCHEDULED} draw, across every
     * tenant, whose scheduled time is at or before {@code cutoff}. Used by the automation scheduler
     * (Sprint 12.1) to find draws ready to be conducted; deliberately cross-tenant, unlike every other
     * query on this port. */
    List<Draw> findDueScheduled(Instant cutoff);

    void save(Draw draw);
}
