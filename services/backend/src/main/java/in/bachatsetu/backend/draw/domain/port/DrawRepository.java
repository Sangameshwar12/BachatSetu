package in.bachatsetu.backend.draw.domain.port;

import in.bachatsetu.backend.draw.domain.model.Draw;
import in.bachatsetu.backend.draw.domain.model.DrawNumber;
import in.bachatsetu.backend.shared.domain.AggregateId;
import java.util.Optional;

public interface DrawRepository {

    Optional<Draw> findById(AggregateId drawId);

    Optional<Draw> findById(AggregateId tenantId, AggregateId drawId);

    DrawPage<Draw> findPage(AggregateId tenantId, DrawPageRequest pageRequest);

    Optional<Draw> findByGroupAndNumber(AggregateId groupId, DrawNumber drawNumber);

    Optional<Draw> findByCycleId(AggregateId cycleId);

    void save(Draw draw);
}
