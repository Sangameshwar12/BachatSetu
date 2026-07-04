package in.bachatsetu.backend.infrastructure.persistence.repository.jpa;

import in.bachatsetu.backend.infrastructure.persistence.entity.community.AuctionBidJpaEntity;
import in.bachatsetu.backend.infrastructure.persistence.repository.BaseJpaRepository;
import java.util.Optional;
import java.util.UUID;

public interface AuctionBidSpringDataRepository extends BaseJpaRepository<AuctionBidJpaEntity> {

    Optional<AuctionBidJpaEntity> findByDraw_IdAndMember_IdAndDeletedFalse(UUID drawId, UUID memberId);
}
