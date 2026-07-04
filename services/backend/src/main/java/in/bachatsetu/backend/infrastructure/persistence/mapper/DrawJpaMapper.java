package in.bachatsetu.backend.infrastructure.persistence.mapper;

import in.bachatsetu.backend.draw.domain.model.AuctionBid;
import in.bachatsetu.backend.draw.domain.model.Draw;
import in.bachatsetu.backend.draw.domain.model.DrawNumber;
import in.bachatsetu.backend.draw.domain.model.DrawStatus;
import in.bachatsetu.backend.infrastructure.persistence.entity.community.AuctionBidJpaEntity;
import in.bachatsetu.backend.infrastructure.persistence.entity.community.DrawJpaEntity;
import in.bachatsetu.backend.shared.domain.AggregateId;
import java.util.List;
import org.mapstruct.Context;
import org.mapstruct.Mapper;
import org.mapstruct.factory.Mappers;

@Mapper(config = PersistenceMapperConfiguration.class)
public interface DrawJpaMapper {

    AuctionBidJpaMapper BID_MAPPER = Mappers.getMapper(AuctionBidJpaMapper.class);

    default Draw toDomain(DrawJpaEntity entity) {
        if (entity == null) {
            return null;
        }
        List<AuctionBid> bids = entity.getBids().stream().map(BID_MAPPER::toDomain).toList();
        AggregateId winnerId = entity.getSelectedMember() == null
                ? null
                : JpaMappingSupport.id(entity.getSelectedMember().getId());
        return new Draw(
                JpaMappingSupport.id(entity.getId()),
                JpaMappingSupport.id(entity.getTenantId()),
                JpaMappingSupport.id(entity.getGroup().getId()),
                JpaMappingSupport.id(entity.getCycle().getId()),
                new DrawNumber(entity.getCycle().getCycleNumber()),
                entity.getType(),
                entity.getScheduledAt(),
                entity.getStatus(),
                bids,
                winnerId,
                JpaMappingSupport.auditInfo(entity),
                entity.getVersion());
    }

    default DrawJpaEntity toEntity(Draw domain, @Context JpaReferenceProvider references) {
        if (domain == null) {
            return null;
        }
        return new DrawJpaEntity(
                domain.id().value(),
                domain.tenantId().value(),
                references.group(domain.groupId()),
                references.cycle(domain.cycleId()),
                domain.winnerMemberId() == null ? null : references.member(domain.winnerMemberId()),
                domain.type(),
                domain.status(),
                domain.scheduledAt(),
                domain.status() == DrawStatus.COMPLETED ? domain.auditInfo().updatedAt() : null,
                0,
                "INR");
    }
}
