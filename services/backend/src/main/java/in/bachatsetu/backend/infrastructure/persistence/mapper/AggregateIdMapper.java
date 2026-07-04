package in.bachatsetu.backend.infrastructure.persistence.mapper;

import in.bachatsetu.backend.shared.domain.AggregateId;
import java.util.UUID;

public interface AggregateIdMapper {

    default UUID toUuid(AggregateId aggregateId) {
        return aggregateId == null ? null : aggregateId.value();
    }

    default AggregateId toAggregateId(UUID value) {
        return value == null ? null : new AggregateId(value);
    }
}
