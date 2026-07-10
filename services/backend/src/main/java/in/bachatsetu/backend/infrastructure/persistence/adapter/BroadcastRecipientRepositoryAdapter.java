package in.bachatsetu.backend.infrastructure.persistence.adapter;

import in.bachatsetu.backend.infrastructure.persistence.entity.identity.UserJpaEntity;
import in.bachatsetu.backend.infrastructure.persistence.repository.jpa.MemberSpringDataRepository;
import in.bachatsetu.backend.infrastructure.persistence.repository.jpa.UserSpringDataRepository;
import in.bachatsetu.backend.member.domain.model.GroupRole;
import in.bachatsetu.backend.platformoperations.domain.model.BroadcastRecipient;
import in.bachatsetu.backend.platformoperations.domain.model.BroadcastScope;
import in.bachatsetu.backend.platformoperations.domain.port.BroadcastRecipientRepository;
import in.bachatsetu.backend.shared.domain.AggregateId;
import java.util.List;
import java.util.Objects;
import java.util.UUID;
import org.springframework.stereotype.Repository;
import org.springframework.transaction.annotation.Transactional;

/**
 * Resolves broadcast recipients from the existing user and membership records — no separate audience/segment
 * table. "Organizers" means {@link GroupRole#ORGANIZER} or {@link GroupRole#CO_ORGANIZER}; "Members" means
 * {@link GroupRole#MEMBER}.
 */
@Repository
@ConditionalOnPersistenceRepositories
@Transactional(readOnly = true)
public class BroadcastRecipientRepositoryAdapter implements BroadcastRecipientRepository {

    private final UserSpringDataRepository userRepository;
    private final MemberSpringDataRepository memberRepository;

    public BroadcastRecipientRepositoryAdapter(
            UserSpringDataRepository userRepository, MemberSpringDataRepository memberRepository) {
        this.userRepository = Objects.requireNonNull(userRepository, "userRepository must not be null");
        this.memberRepository = Objects.requireNonNull(memberRepository, "memberRepository must not be null");
    }

    @Override
    public List<BroadcastRecipient> resolve(BroadcastScope scope, AggregateId tenantId) {
        Objects.requireNonNull(scope, "scope must not be null");
        return switch (scope) {
            case ALL_USERS -> userRepository.findAllByDeletedFalse().stream().map(this::toRecipient).toList();
            case TENANT -> userRepository.findAllByTenantIdAndDeletedFalse(requireTenantId(tenantId)).stream()
                    .map(this::toRecipient)
                    .toList();
            case ORGANIZERS -> memberRepository
                    .findDistinctUsersByRoleInAndDeletedFalse(List.of(GroupRole.ORGANIZER, GroupRole.CO_ORGANIZER))
                    .stream()
                    .map(this::toRecipient)
                    .toList();
            case MEMBERS -> memberRepository.findDistinctUsersByRoleInAndDeletedFalse(List.of(GroupRole.MEMBER))
                    .stream()
                    .map(this::toRecipient)
                    .toList();
        };
    }

    private UUID requireTenantId(AggregateId tenantId) {
        Objects.requireNonNull(tenantId, "tenantId must not be null for the TENANT broadcast scope");
        return tenantId.value();
    }

    private BroadcastRecipient toRecipient(UserJpaEntity entity) {
        return new BroadcastRecipient(new AggregateId(entity.getTenantId()), new AggregateId(entity.getId()));
    }
}
