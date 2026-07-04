package in.bachatsetu.backend.group.domain.port;

import in.bachatsetu.backend.group.domain.model.GroupCode;
import in.bachatsetu.backend.group.domain.model.SavingsGroup;
import in.bachatsetu.backend.shared.domain.AggregateId;
import java.util.Optional;

public interface GroupRepository {

    Optional<SavingsGroup> findById(AggregateId groupId);

    Optional<SavingsGroup> findByCode(AggregateId tenantId, GroupCode code);

    void save(SavingsGroup group);
}
