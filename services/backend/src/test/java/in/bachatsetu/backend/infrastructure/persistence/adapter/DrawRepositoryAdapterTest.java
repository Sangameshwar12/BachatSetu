package in.bachatsetu.backend.infrastructure.persistence.adapter;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import in.bachatsetu.backend.draw.domain.model.BidAmount;
import in.bachatsetu.backend.draw.domain.model.Draw;
import in.bachatsetu.backend.draw.domain.model.DrawNumber;
import in.bachatsetu.backend.draw.domain.model.DrawType;
import in.bachatsetu.backend.draw.domain.port.DrawPage;
import in.bachatsetu.backend.draw.domain.port.DrawPageRequest;
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
import in.bachatsetu.backend.shared.domain.Money;
import java.time.Instant;
import java.util.List;
import java.util.Optional;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;

class DrawRepositoryAdapterTest {

    private static final Instant NOW = Instant.parse("2026-07-07T08:00:00Z");

    private DrawSpringDataRepository repository;
    private AuctionBidSpringDataRepository bidRepository;
    private DrawJpaMapper mapper;
    private AuctionBidJpaMapper bidMapper;
    private JpaReferenceProvider references;
    private DrawRepositoryAdapter adapter;

    @BeforeEach
    void setUp() {
        repository = mock(DrawSpringDataRepository.class);
        bidRepository = mock(AuctionBidSpringDataRepository.class);
        mapper = mock(DrawJpaMapper.class);
        bidMapper = mock(AuctionBidJpaMapper.class);
        references = mock(JpaReferenceProvider.class);
        adapter = new DrawRepositoryAdapter(repository, bidRepository, mapper, bidMapper, references);
    }

    @Test
    void findsByLegacyIdentifier() {
        AggregateId drawId = AggregateId.newId();
        DrawJpaEntity entity = mock(DrawJpaEntity.class);
        Draw draw = newDraw(drawId);
        when(repository.findByIdAndDeletedFalse(drawId.value())).thenReturn(Optional.of(entity));
        when(mapper.toDomain(entity)).thenReturn(draw);

        assertThat(adapter.findById(drawId)).contains(draw);
    }

    @Test
    void findsByTenantScopedIdentifier() {
        AggregateId tenantId = AggregateId.newId();
        AggregateId drawId = AggregateId.newId();
        DrawJpaEntity entity = mock(DrawJpaEntity.class);
        Draw draw = newDraw(drawId);
        when(repository.findByTenantIdAndIdAndDeletedFalse(tenantId.value(), drawId.value()))
                .thenReturn(Optional.of(entity));
        when(mapper.toDomain(entity)).thenReturn(draw);

        assertThat(adapter.findById(tenantId, drawId)).contains(draw);
    }

    @Test
    void reportsNoTenantScopedMatch() {
        AggregateId tenantId = AggregateId.newId();
        AggregateId drawId = AggregateId.newId();
        when(repository.findByTenantIdAndIdAndDeletedFalse(tenantId.value(), drawId.value()))
                .thenReturn(Optional.empty());

        assertThat(adapter.findById(tenantId, drawId)).isEmpty();
    }

    @Test
    void findsByGroupAndNumber() {
        AggregateId groupId = AggregateId.newId();
        DrawNumber number = new DrawNumber(3);
        DrawJpaEntity entity = mock(DrawJpaEntity.class);
        Draw draw = newDraw(AggregateId.newId());
        when(repository.findByGroup_IdAndCycle_CycleNumberAndDeletedFalse(groupId.value(), number.value()))
                .thenReturn(Optional.of(entity));
        when(mapper.toDomain(entity)).thenReturn(draw);

        assertThat(adapter.findByGroupAndNumber(groupId, number)).contains(draw);
    }

    @Test
    void findsByCycleId() {
        AggregateId cycleId = AggregateId.newId();
        DrawJpaEntity entity = mock(DrawJpaEntity.class);
        Draw draw = newDraw(AggregateId.newId());
        when(repository.findByCycle_IdAndDeletedFalse(cycleId.value())).thenReturn(Optional.of(entity));
        when(mapper.toDomain(entity)).thenReturn(draw);

        assertThat(adapter.findByCycleId(cycleId)).contains(draw);
    }

    @Test
    void findsPageAndAppliesRequestedSort() {
        AggregateId tenantId = AggregateId.newId();
        DrawJpaEntity entity = mock(DrawJpaEntity.class);
        Draw draw = newDraw(AggregateId.newId());
        DrawPageRequest pageRequest = new DrawPageRequest(0, 20, DrawSortField.SCHEDULED_AT, SortDirection.DESC);
        when(repository.findAllByTenantIdAndDeletedFalse(eq(tenantId.value()), any(Pageable.class)))
                .thenReturn(new PageImpl<>(List.of(entity), PageRequest.of(0, 20), 1));
        when(mapper.toDomain(entity)).thenReturn(draw);

        DrawPage<Draw> page = adapter.findPage(tenantId, pageRequest);

        assertThat(page.content()).containsExactly(draw);
        assertThat(page.totalElements()).isEqualTo(1);
    }

    @Test
    void findsPageWithDefaultCreatedAtSort() {
        AggregateId tenantId = AggregateId.newId();
        DrawPageRequest pageRequest = new DrawPageRequest(1, 5, DrawSortField.CREATED_AT, SortDirection.ASC);
        Page<DrawJpaEntity> emptyPage = new PageImpl<>(List.of(), PageRequest.of(1, 5), 6);
        when(repository.findAllByTenantIdAndDeletedFalse(eq(tenantId.value()), any(Pageable.class)))
                .thenReturn(emptyPage);

        DrawPage<Draw> page = adapter.findPage(tenantId, pageRequest);

        assertThat(page.page()).isEqualTo(1);
        assertThat(page.size()).isEqualTo(5);
        assertThat(page.totalElements()).isEqualTo(6);
    }

    @Test
    void savesNewDrawAndItsBids() {
        AggregateId actorId = AggregateId.newId();
        Draw draw = newDraw(AggregateId.newId());
        draw.open(actorId, NOW.plusSeconds(3600));
        draw.submitBid(AggregateId.newId(), new BidAmount(Money.inr(10_000)), actorId, NOW.plusSeconds(3700));
        DrawJpaEntity candidate = mock(DrawJpaEntity.class);
        AuctionBidJpaEntity bidCandidate = mock(AuctionBidJpaEntity.class);
        when(repository.findById(draw.id().value())).thenReturn(Optional.empty());
        when(mapper.toEntity(draw, references)).thenReturn(candidate);
        when(bidRepository.findById(draw.bids().get(0).id().value())).thenReturn(Optional.empty());
        when(bidMapper.toEntity(draw.bids().get(0), draw.tenantId(), draw.groupId(), draw.id(), references))
                .thenReturn(bidCandidate);

        adapter.save(draw);

        verify(repository).save(candidate);
        verify(bidRepository).save(bidCandidate);
    }

    private Draw newDraw(AggregateId drawId) {
        return Draw.schedule(
                drawId,
                AggregateId.newId(),
                AggregateId.newId(),
                AggregateId.newId(),
                new DrawNumber(1),
                DrawType.AUCTION,
                NOW.plusSeconds(3600),
                AggregateId.newId(),
                NOW);
    }
}
