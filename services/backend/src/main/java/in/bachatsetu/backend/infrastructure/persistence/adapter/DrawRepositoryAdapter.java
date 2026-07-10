package in.bachatsetu.backend.infrastructure.persistence.adapter;

import in.bachatsetu.backend.draw.domain.model.AuctionBid;
import in.bachatsetu.backend.draw.domain.model.Draw;
import in.bachatsetu.backend.draw.domain.model.DrawNumber;
import in.bachatsetu.backend.draw.domain.model.DrawStatus;
import in.bachatsetu.backend.draw.domain.model.DrawType;
import in.bachatsetu.backend.draw.domain.port.DrawPage;
import in.bachatsetu.backend.draw.domain.port.DrawPageRequest;
import in.bachatsetu.backend.draw.domain.port.DrawRepository;
import in.bachatsetu.backend.draw.domain.port.DrawSortField;
import in.bachatsetu.backend.draw.domain.port.SortDirection;
import in.bachatsetu.backend.infrastructure.persistence.entity.community.AuctionBidJpaEntity;
import in.bachatsetu.backend.infrastructure.persistence.entity.community.DrawJpaEntity;
import in.bachatsetu.backend.infrastructure.persistence.mapper.AuctionBidJpaMapper;
import in.bachatsetu.backend.infrastructure.persistence.mapper.DrawJpaMapper;
import in.bachatsetu.backend.infrastructure.persistence.mapper.JpaReferenceProvider;
import in.bachatsetu.backend.infrastructure.persistence.repository.jpa.AuctionBidSpringDataRepository;
import in.bachatsetu.backend.infrastructure.persistence.repository.jpa.DrawSpringDataRepository;
import in.bachatsetu.backend.shared.domain.AggregateId;
import java.time.Instant;
import java.util.List;
import java.util.Optional;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Repository;
import org.springframework.transaction.annotation.Transactional;

@Repository
@ConditionalOnPersistenceRepositories
@Transactional(readOnly = true)
public class DrawRepositoryAdapter implements DrawRepository {

    private final DrawSpringDataRepository repository;
    private final AuctionBidSpringDataRepository bidRepository;
    private final DrawJpaMapper mapper;
    private final AuctionBidJpaMapper bidMapper;
    private final JpaReferenceProvider references;

    public DrawRepositoryAdapter(
            DrawSpringDataRepository repository,
            AuctionBidSpringDataRepository bidRepository,
            DrawJpaMapper mapper,
            AuctionBidJpaMapper bidMapper,
            JpaReferenceProvider references) {
        this.repository = repository;
        this.bidRepository = bidRepository;
        this.mapper = mapper;
        this.bidMapper = bidMapper;
        this.references = references;
    }

    @Override
    public Optional<Draw> findById(AggregateId drawId) {
        return repository.findByIdAndDeletedFalse(drawId.value()).map(mapper::toDomain);
    }

    @Override
    public Optional<Draw> findById(AggregateId tenantId, AggregateId drawId) {
        return repository.findByTenantIdAndIdAndDeletedFalse(tenantId.value(), drawId.value())
                .map(mapper::toDomain);
    }

    @Override
    public DrawPage<Draw> findPage(AggregateId tenantId, DrawPageRequest pageRequest) {
        Pageable pageable = PageRequest.of(pageRequest.page(), pageRequest.size(), toSort(pageRequest));
        Page<DrawJpaEntity> page = repository.findAllByTenantIdAndDeletedFalse(tenantId.value(), pageable);
        List<Draw> content = page.getContent().stream().map(mapper::toDomain).toList();
        return new DrawPage<>(content, page.getNumber(), page.getSize(), page.getTotalElements());
    }

    @Override
    public DrawPage<Draw> findPageByType(AggregateId tenantId, DrawType type, DrawPageRequest pageRequest) {
        Pageable pageable = PageRequest.of(pageRequest.page(), pageRequest.size(), toSort(pageRequest));
        Page<DrawJpaEntity> page =
                repository.findAllByTenantIdAndTypeAndDeletedFalse(tenantId.value(), type, pageable);
        List<Draw> content = page.getContent().stream().map(mapper::toDomain).toList();
        return new DrawPage<>(content, page.getNumber(), page.getSize(), page.getTotalElements());
    }

    private Sort toSort(DrawPageRequest pageRequest) {
        String property = toSortProperty(pageRequest.sortField());
        Sort.Direction direction = pageRequest.direction() == SortDirection.DESC
                ? Sort.Direction.DESC
                : Sort.Direction.ASC;
        return Sort.by(direction, property);
    }

    private String toSortProperty(DrawSortField sortField) {
        if (sortField == DrawSortField.SCHEDULED_AT) {
            return "scheduledAt";
        }
        return "createdAt";
    }

    @Override
    public Optional<Draw> findByGroupAndNumber(AggregateId groupId, DrawNumber drawNumber) {
        return repository.findByGroup_IdAndCycle_CycleNumberAndDeletedFalse(groupId.value(), drawNumber.value())
                .map(mapper::toDomain);
    }

    @Override
    public Optional<Draw> findByCycleId(AggregateId cycleId) {
        return repository.findByCycle_IdAndDeletedFalse(cycleId.value()).map(mapper::toDomain);
    }

    @Override
    public Optional<Draw> findNextScheduledByGroup(AggregateId tenantId, AggregateId groupId) {
        return repository.findFirstByTenantIdAndGroup_IdAndStatusAndDeletedFalseOrderByScheduledAtAsc(
                        tenantId.value(), groupId.value(), DrawStatus.SCHEDULED)
                .map(mapper::toDomain);
    }

    @Override
    public List<Draw> findDueScheduled(Instant cutoff) {
        return repository.findAllByStatusAndScheduledAtLessThanEqualAndDeletedFalse(DrawStatus.SCHEDULED, cutoff)
                .stream().map(mapper::toDomain).toList();
    }

    @Override
    @Transactional
    public void save(Draw draw) {
        RepositoryOperations.execute(() -> {
            Optional<DrawJpaEntity> existing = repository.findById(draw.id().value());
            DrawJpaEntity candidate = mapper.toEntity(draw, references);
            repository.save(RepositoryOperations.preserveState(candidate, existing));
            for (AuctionBid bid : draw.bids()) {
                Optional<AuctionBidJpaEntity> existingBid = bidRepository.findById(bid.id().value());
                AuctionBidJpaEntity bidCandidate = bidMapper.toEntity(
                        bid, draw.tenantId(), draw.groupId(), draw.id(), references);
                bidRepository.save(RepositoryOperations.preserveState(bidCandidate, existingBid));
            }
            return null;
        });
    }
}
