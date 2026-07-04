package in.bachatsetu.backend.infrastructure.persistence.adapter;

import in.bachatsetu.backend.infrastructure.persistence.entity.community.MemberJpaEntity;
import in.bachatsetu.backend.infrastructure.persistence.exception.PersistenceMappingException;
import in.bachatsetu.backend.infrastructure.persistence.mapper.JpaReferenceProvider;
import in.bachatsetu.backend.infrastructure.persistence.mapper.MemberJpaMapper;
import in.bachatsetu.backend.infrastructure.persistence.repository.jpa.MemberSpringDataRepository;
import in.bachatsetu.backend.member.domain.model.GroupParticipation;
import in.bachatsetu.backend.member.domain.model.MemberNumber;
import in.bachatsetu.backend.member.domain.model.MemberProfile;
import in.bachatsetu.backend.member.domain.port.MemberRepository;
import in.bachatsetu.backend.shared.domain.AggregateId;
import java.util.List;
import java.util.Optional;
import org.springframework.stereotype.Repository;
import org.springframework.transaction.annotation.Transactional;

@Repository
@ConditionalOnPersistenceRepositories
@Transactional(readOnly = true)
public class MemberRepositoryAdapter implements MemberRepository {

    private final MemberSpringDataRepository repository;
    private final MemberJpaMapper mapper;
    private final JpaReferenceProvider references;

    public MemberRepositoryAdapter(
            MemberSpringDataRepository repository,
            MemberJpaMapper mapper,
            JpaReferenceProvider references) {
        this.repository = repository;
        this.mapper = mapper;
        this.references = references;
    }

    @Override
    public Optional<MemberProfile> findById(AggregateId memberId) {
        return repository.findByIdAndDeletedFalse(memberId.value()).map(mapper::toDomain);
    }

    @Override
    public Optional<MemberProfile> findByUserId(AggregateId tenantId, AggregateId userId) {
        List<MemberJpaEntity> rows = repository
                .findAllByTenantIdAndUser_IdAndDeletedFalseOrderByJoinedAtAsc(tenantId.value(), userId.value());
        return rows.isEmpty() ? Optional.empty() : Optional.of(assemble(rows));
    }

    @Override
    public Optional<MemberProfile> findByMemberNumber(AggregateId tenantId, MemberNumber memberNumber) {
        return repository
                .findFirstByTenantIdAndMemberNumberAndDeletedFalseOrderByJoinedAtAsc(
                        tenantId.value(), memberNumber.value())
                .map(mapper::toDomain);
    }

    @Override
    @Transactional
    public void save(MemberProfile member) {
        if (member.participations().isEmpty()) {
            throw new PersistenceMappingException("member must contain at least one group participation");
        }
        RepositoryOperations.execute(() -> {
            for (GroupParticipation participation : member.participations()) {
                Optional<MemberJpaEntity> existing = repository.findById(participation.id().value());
                MemberJpaEntity candidate = mapper.toEntity(member, participation, references);
                repository.save(RepositoryOperations.preserveState(candidate, existing));
            }
            return null;
        });
    }

    private MemberProfile assemble(List<MemberJpaEntity> rows) {
        MemberProfile first = mapper.toDomain(rows.getFirst());
        List<GroupParticipation> participations = rows.stream()
                .map(mapper::toDomain)
                .flatMap(profile -> profile.participations().stream())
                .toList();
        return new MemberProfile(
                first.id(),
                first.tenantId(),
                first.userId(),
                first.memberNumber(),
                first.status(),
                participations,
                first.consents(),
                first.auditInfo(),
                first.version());
    }
}
