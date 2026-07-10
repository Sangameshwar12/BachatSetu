package in.bachatsetu.backend.infrastructure.persistence.adapter;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import in.bachatsetu.backend.infrastructure.persistence.entity.identity.UserJpaEntity;
import in.bachatsetu.backend.infrastructure.persistence.repository.jpa.MemberSpringDataRepository;
import in.bachatsetu.backend.infrastructure.persistence.repository.jpa.UserSpringDataRepository;
import in.bachatsetu.backend.member.domain.model.GroupRole;
import in.bachatsetu.backend.platformoperations.domain.model.BroadcastRecipient;
import in.bachatsetu.backend.platformoperations.domain.model.BroadcastScope;
import in.bachatsetu.backend.shared.domain.AggregateId;
import java.util.List;
import java.util.UUID;
import org.junit.jupiter.api.Test;

class BroadcastRecipientRepositoryAdapterTest {

    private final UserSpringDataRepository userRepository = mock(UserSpringDataRepository.class);
    private final MemberSpringDataRepository memberRepository = mock(MemberSpringDataRepository.class);
    private final BroadcastRecipientRepositoryAdapter adapter =
            new BroadcastRecipientRepositoryAdapter(userRepository, memberRepository);

    @Test
    void resolvesAllUsers() {
        UserJpaEntity user = userEntity();
        when(userRepository.findAllByDeletedFalse()).thenReturn(List.of(user));

        List<BroadcastRecipient> recipients = adapter.resolve(BroadcastScope.ALL_USERS, null);

        assertThat(recipients).hasSize(1);
        assertThat(recipients.get(0).userId().value()).isEqualTo(user.getId());
    }

    @Test
    void resolvesUsersInASpecificTenant() {
        UUID tenantId = UUID.randomUUID();
        UserJpaEntity user = userEntity();
        when(userRepository.findAllByTenantIdAndDeletedFalse(tenantId)).thenReturn(List.of(user));

        List<BroadcastRecipient> recipients = adapter.resolve(BroadcastScope.TENANT, new AggregateId(tenantId));

        assertThat(recipients).hasSize(1);
    }

    @Test
    void rejectsATenantScopeWithNoTenantId() {
        org.assertj.core.api.Assertions.assertThatThrownBy(() -> adapter.resolve(BroadcastScope.TENANT, null))
                .isInstanceOf(NullPointerException.class);
    }

    @Test
    void resolvesOrganizersAsOrganizerAndCoOrganizerRoles() {
        UserJpaEntity user = userEntity();
        when(memberRepository.findDistinctUsersByRoleInAndDeletedFalse(
                        List.of(GroupRole.ORGANIZER, GroupRole.CO_ORGANIZER)))
                .thenReturn(List.of(user));

        List<BroadcastRecipient> recipients = adapter.resolve(BroadcastScope.ORGANIZERS, null);

        assertThat(recipients).hasSize(1);
    }

    @Test
    void resolvesMembersAsTheMemberRoleOnly() {
        UserJpaEntity user = userEntity();
        when(memberRepository.findDistinctUsersByRoleInAndDeletedFalse(List.of(GroupRole.MEMBER)))
                .thenReturn(List.of(user));

        List<BroadcastRecipient> recipients = adapter.resolve(BroadcastScope.MEMBERS, null);

        assertThat(recipients).hasSize(1);
    }

    private UserJpaEntity userEntity() {
        UserJpaEntity user = mock(UserJpaEntity.class);
        when(user.getId()).thenReturn(UUID.randomUUID());
        when(user.getTenantId()).thenReturn(UUID.randomUUID());
        return user;
    }
}
