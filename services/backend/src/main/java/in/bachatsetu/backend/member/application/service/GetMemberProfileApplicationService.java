package in.bachatsetu.backend.member.application.service;

import in.bachatsetu.backend.member.application.exception.MemberProfileNotFoundException;
import in.bachatsetu.backend.member.application.mapper.MemberApplicationMapper;
import in.bachatsetu.backend.member.application.port.TransactionPort;
import in.bachatsetu.backend.member.application.query.MemberProfileResult;
import in.bachatsetu.backend.member.application.security.MemberAuthorizationService;
import in.bachatsetu.backend.member.application.usecase.GetMemberProfileUseCase;
import in.bachatsetu.backend.member.domain.model.MemberProfile;
import in.bachatsetu.backend.member.domain.port.MemberRepository;
import in.bachatsetu.backend.shared.domain.AggregateId;
import java.util.Objects;

/** Retrieves and maps a tenant-scoped Member profile aggregate, restricted to the member themselves. */
public final class GetMemberProfileApplicationService implements GetMemberProfileUseCase {

    private final MemberRepository repository;
    private final TransactionPort transaction;
    private final MemberApplicationMapper mapper;
    private final MemberAuthorizationService authorization;

    public GetMemberProfileApplicationService(
            MemberRepository repository,
            TransactionPort transaction,
            MemberApplicationMapper mapper,
            MemberAuthorizationService authorization) {
        this.repository = Objects.requireNonNull(repository, "repository must not be null");
        this.transaction = Objects.requireNonNull(transaction, "transaction must not be null");
        this.mapper = Objects.requireNonNull(mapper, "mapper must not be null");
        this.authorization = Objects.requireNonNull(authorization, "authorization must not be null");
    }

    @Override
    public MemberProfileResult execute(AggregateId tenantId, AggregateId memberId, AggregateId actorId) {
        Objects.requireNonNull(tenantId, "tenant id must not be null");
        Objects.requireNonNull(memberId, "member id must not be null");
        Objects.requireNonNull(actorId, "actor id must not be null");
        return transaction.execute(() -> {
            MemberProfile member = repository.findById(tenantId, memberId)
                    .orElseThrow(() -> new MemberProfileNotFoundException("member profile does not exist"));
            authorization.requireSelf(member, actorId);
            return mapper.toResult(member);
        });
    }
}
