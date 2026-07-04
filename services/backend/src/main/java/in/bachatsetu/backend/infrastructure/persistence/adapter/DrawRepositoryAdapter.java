package in.bachatsetu.backend.infrastructure.persistence.adapter;

import in.bachatsetu.backend.draw.domain.model.AuctionBid;
import in.bachatsetu.backend.draw.domain.model.Draw;
import in.bachatsetu.backend.draw.domain.model.DrawNumber;
import in.bachatsetu.backend.draw.domain.port.DrawRepository;
import in.bachatsetu.backend.infrastructure.persistence.entity.community.AuctionBidJpaEntity;
import in.bachatsetu.backend.infrastructure.persistence.entity.community.DrawJpaEntity;
import in.bachatsetu.backend.infrastructure.persistence.mapper.AuctionBidJpaMapper;
import in.bachatsetu.backend.infrastructure.persistence.mapper.DrawJpaMapper;
import in.bachatsetu.backend.infrastructure.persistence.mapper.JpaReferenceProvider;
import in.bachatsetu.backend.infrastructure.persistence.repository.jpa.AuctionBidSpringDataRepository;
import in.bachatsetu.backend.infrastructure.persistence.repository.jpa.DrawSpringDataRepository;
import in.bachatsetu.backend.shared.domain.AggregateId;
import java.util.Optional;
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
    public Optional<Draw> findByGroupAndNumber(AggregateId groupId, DrawNumber drawNumber) {
        return repository.findByGroup_IdAndCycle_CycleNumberAndDeletedFalse(groupId.value(), drawNumber.value())
                .map(mapper::toDomain);
    }

    @Override
    public Optional<Draw> findByCycleId(AggregateId cycleId) {
        return repository.findByCycle_IdAndDeletedFalse(cycleId.value()).map(mapper::toDomain);
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
