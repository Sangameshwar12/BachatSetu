package in.bachatsetu.backend.infrastructure.persistence.adapter;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import in.bachatsetu.backend.auth.domain.exception.RefreshTokenConflictException;
import in.bachatsetu.backend.auth.domain.model.RefreshToken;
import in.bachatsetu.backend.auth.domain.model.RefreshTokenHash;
import in.bachatsetu.backend.auth.domain.model.RefreshTokenId;
import in.bachatsetu.backend.auth.domain.model.TokenSessionId;
import in.bachatsetu.backend.auth.domain.model.UserId;
import in.bachatsetu.backend.infrastructure.persistence.entity.identity.RefreshTokenJpaEntity;
import in.bachatsetu.backend.infrastructure.persistence.mapper.JpaReferenceProvider;
import in.bachatsetu.backend.infrastructure.persistence.mapper.RefreshTokenJpaMapper;
import in.bachatsetu.backend.infrastructure.persistence.repository.jpa.RefreshTokenSpringDataRepository;
import in.bachatsetu.backend.shared.domain.AggregateId;
import in.bachatsetu.backend.shared.domain.AuditInfo;
import java.time.Instant;
import java.util.Optional;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.dao.OptimisticLockingFailureException;

class RefreshTokenRepositoryAdapterTest {

    private static final Instant NOW = Instant.parse("2026-07-22T08:00:00Z");
    private static final AggregateId ACTOR_ID = AggregateId.newId();

    private RefreshTokenSpringDataRepository repository;
    private RefreshTokenJpaMapper mapper;
    private JpaReferenceProvider references;
    private RefreshTokenRepositoryAdapter adapter;

    @BeforeEach
    void setUp() {
        repository = mock(RefreshTokenSpringDataRepository.class);
        mapper = mock(RefreshTokenJpaMapper.class);
        references = mock(JpaReferenceProvider.class);
        adapter = new RefreshTokenRepositoryAdapter(repository, mapper, references);
    }

    @Test
    void replacesCurrentAndReplacementTokens() {
        RefreshToken current = token(RefreshTokenId.newId());
        RefreshToken replacement = token(RefreshTokenId.newId());
        RefreshTokenJpaEntity currentEntity = mock(RefreshTokenJpaEntity.class);
        RefreshTokenJpaEntity replacementEntity = mock(RefreshTokenJpaEntity.class);
        when(repository.findById(current.refreshTokenId().value())).thenReturn(Optional.empty());
        when(repository.findById(replacement.refreshTokenId().value())).thenReturn(Optional.empty());
        when(mapper.toEntity(current, references)).thenReturn(currentEntity);
        when(mapper.toEntity(replacement, references)).thenReturn(replacementEntity);

        adapter.replace(current, replacement);

        verify(repository).save(currentEntity);
        verify(repository).save(replacementEntity);
        verify(repository).flush();
    }

    @Test
    void translatesAConcurrentReplaceConflictIntoARefreshTokenConflictException() {
        RefreshToken current = token(RefreshTokenId.newId());
        RefreshToken replacement = token(RefreshTokenId.newId());
        RefreshTokenJpaEntity currentEntity = mock(RefreshTokenJpaEntity.class);
        when(repository.findById(current.refreshTokenId().value())).thenReturn(Optional.empty());
        when(mapper.toEntity(current, references)).thenReturn(currentEntity);
        when(repository.save(currentEntity)).thenThrow(new OptimisticLockingFailureException("stale version"));

        assertThatThrownBy(() -> adapter.replace(current, replacement))
                .isInstanceOf(RefreshTokenConflictException.class)
                .hasCauseInstanceOf(in.bachatsetu.backend.infrastructure.persistence.exception.PersistenceConflictException.class);
    }

    @Test
    void translatesAConcurrentRecordReuseConflictIntoARefreshTokenConflictException() {
        RefreshToken reused = token(RefreshTokenId.newId());
        RefreshTokenJpaEntity reusedEntity = mock(RefreshTokenJpaEntity.class);
        when(repository.findById(reused.refreshTokenId().value())).thenReturn(Optional.empty());
        when(mapper.toEntity(reused, references)).thenReturn(reusedEntity);
        when(repository.save(reusedEntity)).thenThrow(new OptimisticLockingFailureException("stale version"));

        assertThatThrownBy(() -> adapter.recordReuse(reused, Optional.empty()))
                .isInstanceOf(RefreshTokenConflictException.class)
                .hasCauseInstanceOf(in.bachatsetu.backend.infrastructure.persistence.exception.PersistenceConflictException.class);
    }

    @Test
    void findsActiveTokenForUserAndSession() {
        UserId userId = UserId.newId();
        TokenSessionId sessionId = TokenSessionId.newId();
        RefreshTokenJpaEntity entity = mock(RefreshTokenJpaEntity.class);
        RefreshToken domain = token(RefreshTokenId.newId());
        when(repository.findByUser_IdAndSessionIdAndStatusAndDeletedFalse(
                        userId.value(), sessionId.value(), in.bachatsetu.backend.auth.domain.model.TokenStatus.ACTIVE))
                .thenReturn(Optional.of(entity));
        when(mapper.toDomain(entity)).thenReturn(domain);

        assertThat(adapter.findActive(userId, sessionId)).contains(domain);
    }

    private RefreshToken token(RefreshTokenId id) {
        return RefreshToken.rehydrate(
                id,
                UserId.newId(),
                AggregateId.newId(),
                TokenSessionId.newId(),
                RefreshTokenHash.encoded("H".repeat(60)),
                NOW.minusSeconds(60),
                NOW.plusSeconds(2_592_000),
                in.bachatsetu.backend.auth.domain.model.TokenStatus.ACTIVE,
                null,
                AuditInfo.createdBy(ACTOR_ID, NOW.minusSeconds(60)),
                0);
    }
}
