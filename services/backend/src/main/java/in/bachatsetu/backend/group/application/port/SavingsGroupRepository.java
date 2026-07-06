package in.bachatsetu.backend.group.application.port;

import in.bachatsetu.backend.group.domain.model.GroupCode;
import in.bachatsetu.backend.group.domain.model.GroupId;
import in.bachatsetu.backend.group.domain.model.SavingsGroup;
import in.bachatsetu.backend.shared.domain.AggregateId;
import java.util.List;
import java.util.Optional;

/** Tenant-scoped persistence boundary required by Savings Group use cases. */
public interface SavingsGroupRepository {

    void save(SavingsGroup group);

    Optional<SavingsGroup> findById(AggregateId tenantId, GroupId groupId);

    Optional<SavingsGroup> findByGroupCode(AggregateId tenantId, GroupCode groupCode);

    boolean existsByGroupCode(AggregateId tenantId, GroupCode groupCode);

    List<SavingsGroup> findAll(AggregateId tenantId);

    void delete(AggregateId tenantId, GroupId groupId);
}
