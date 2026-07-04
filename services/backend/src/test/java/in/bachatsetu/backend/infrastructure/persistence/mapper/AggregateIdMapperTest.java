package in.bachatsetu.backend.infrastructure.persistence.mapper;

import static org.assertj.core.api.Assertions.assertThat;

import in.bachatsetu.backend.shared.domain.AggregateId;
import java.util.UUID;
import org.junit.jupiter.api.Test;

class AggregateIdMapperTest {

    private final AggregateIdMapper mapper = new AggregateIdMapper() { };

    @Test
    void mapsAggregateIdentifiersWithoutLosingValue() {
        UUID value = UUID.randomUUID();

        assertThat(mapper.toUuid(mapper.toAggregateId(value))).isEqualTo(value);
        assertThat(mapper.toAggregateId(null)).isNull();
        assertThat(mapper.toUuid(null)).isNull();
    }
}
