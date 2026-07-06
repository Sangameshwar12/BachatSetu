package in.bachatsetu.backend.member.application.service;

import in.bachatsetu.backend.member.application.exception.MemberProfileNotFoundException;
import in.bachatsetu.backend.member.application.mapper.MemberApplicationMapper;
import in.bachatsetu.backend.member.application.port.TransactionPort;
import in.bachatsetu.backend.member.application.query.MemberProfileResult;
import in.bachatsetu.backend.member.application.usecase.GetMemberProfileUseCase;
import in.bachatsetu.backend.member.domain.model.MemberProfile;
import in.bachatsetu.backend.member.domain.port.MemberRepository;
import in.bachatsetu.backend.shared.domain.AggregateId;
import java.util.Objects;

/** Retrieves and maps a tenant-scoped Member profile aggregate. */
public final class GetMemberProfileApplicationService implements GetMemberProfileUseCase {

    private final MemberRepository repository;
    private final TransactionPort transaction;
    private final MemberApplicationMapper mapper;

    public GetMemberProfileApplicationService(
            MemberRepository repository,
            TransactionPort transaction,
            MemberApplicationMapper mapper) {
        this.repository = Objects.requireNonNull(repository, "repository must not be null");
        this.transaction = Objects.requireNonNull(transaction, "transaction must not be null");
        this.mapper = Objects.requireNonNull(mapper, "mapper must not be null");
    }

    @Override
    public MemberProfileResult execute(AggregateId tenantId, AggregateId memberId) {
        Objects.requireNonNull(tenantId, "tenant id must not be null");
        Objects.requireNonNull(memberId, "member id must not be null");
        return transaction.execute(() -> {
            MemberProfile member = repository.findById(tenantId, memberId)
                    .orElseThrow(() -> new MemberProfileNotFoundException("member profile does not exist"));
            return mapper.toResult(member);
        });
    }
}
