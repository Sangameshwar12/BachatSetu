package in.bachatsetu.backend.infrastructure.persistence.mapper;

import in.bachatsetu.backend.draw.domain.model.AuctionBid;
import in.bachatsetu.backend.draw.domain.model.BidAmount;
import in.bachatsetu.backend.infrastructure.persistence.entity.community.AuctionBidJpaEntity;
import in.bachatsetu.backend.shared.domain.AggregateId;
import in.bachatsetu.backend.shared.domain.Money;
import org.mapstruct.Context;
import org.mapstruct.Mapper;

@Mapper(config = PersistenceMapperConfiguration.class)
public interface AuctionBidJpaMapper {

    default AuctionBid toDomain(AuctionBidJpaEntity entity) {
        if (entity == null) {
            return null;
        }
        long discount = entity.getDiscountAmountPaise() > 0
                ? entity.getDiscountAmountPaise()
                : entity.getBidAmountPaise();
        return new AuctionBid(
                JpaMappingSupport.id(entity.getId()),
                JpaMappingSupport.id(entity.getMember().getId()),
                new BidAmount(new Money(discount, JpaMappingSupport.currency(entity.getCurrencyCode()))),
                entity.getSubmittedAt(),
                entity.getStatus());
    }

    default AuctionBidJpaEntity toEntity(
            AuctionBid domain,
            AggregateId tenantId,
            AggregateId groupId,
            AggregateId drawId,
            @Context JpaReferenceProvider references) {
        if (domain == null) {
            return null;
        }
        long amount = domain.amount().discount().minorUnits();
        return new AuctionBidJpaEntity(
                domain.id().value(),
                tenantId.value(),
                references.group(groupId),
                references.draw(drawId),
                references.member(domain.memberId()),
                amount,
                amount,
                domain.amount().discount().currency().getCurrencyCode(),
                null,
                domain.status(),
                domain.submittedAt());
    }
}
