package in.bachatsetu.backend.member.application.service;

import in.bachatsetu.backend.member.application.mapper.MemberApplicationMapper;
import in.bachatsetu.backend.member.application.port.TransactionPort;
import in.bachatsetu.backend.member.application.query.MemberProfileSummary;
import in.bachatsetu.backend.member.application.usecase.ListMemberProfilesUseCase;
import in.bachatsetu.backend.member.domain.model.MemberProfile;
import in.bachatsetu.backend.member.domain.port.MemberPage;
import in.bachatsetu.backend.member.domain.port.MemberPageRequest;
import in.bachatsetu.backend.member.domain.port.MemberRepository;
import in.bachatsetu.backend.shared.domain.AggregateId;
import java.util.List;
import java.util.Objects;

/** Lists tenant-scoped member profiles as compact immutable query models, paginated by the repository. */
public final class ListMemberProfilesApplicationService implements ListMemberProfilesUseCase {

    private final MemberRepository repository;
    private final TransactionPort transaction;
    private final MemberApplicationMapper mapper;

    public ListMemberProfilesApplicationService(
            MemberRepository repository,
            TransactionPort transaction,
            MemberApplicationMapper mapper) {
        this.repository = Objects.requireNonNull(repository, "repository must not be null");
        this.transaction = Objects.requireNonNull(transaction, "transaction must not be null");
        this.mapper = Objects.requireNonNull(mapper, "mapper must not be null");
    }

    @Override
    public MemberPage<MemberProfileSummary> execute(AggregateId tenantId, MemberPageRequest pageRequest) {
        Objects.requireNonNull(tenantId, "tenant id must not be null");
        Objects.requireNonNull(pageRequest, "page request must not be null");
        return transaction.execute(() -> {
            MemberPage<MemberProfile> page = repository.findPage(tenantId, pageRequest);
            List<MemberProfileSummary> summaries = page.content().stream().map(mapper::toSummary).toList();
            return new MemberPage<>(summaries, page.page(), page.size(), page.totalElements());
        });
    }
}
