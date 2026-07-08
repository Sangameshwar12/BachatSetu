package in.bachatsetu.backend.auction.interfaces.rest.config;

import in.bachatsetu.backend.auction.application.mapper.AuctionApplicationMapper;
import in.bachatsetu.backend.auction.application.port.ClockPort;
import in.bachatsetu.backend.auction.application.port.DomainEventPublisherPort;
import in.bachatsetu.backend.auction.application.port.TransactionPort;
import in.bachatsetu.backend.auction.application.service.CloseAuctionApplicationService;
import in.bachatsetu.backend.auction.application.service.CreateAuctionApplicationService;
import in.bachatsetu.backend.auction.application.service.GetAuctionApplicationService;
import in.bachatsetu.backend.auction.application.service.GetWinnerApplicationService;
import in.bachatsetu.backend.auction.application.service.ListAuctionsApplicationService;
import in.bachatsetu.backend.auction.application.service.PlaceBidApplicationService;
import in.bachatsetu.backend.auction.application.usecase.CloseAuctionUseCase;
import in.bachatsetu.backend.auction.application.usecase.CreateAuctionUseCase;
import in.bachatsetu.backend.auction.application.usecase.GetAuctionUseCase;
import in.bachatsetu.backend.auction.application.usecase.GetWinnerUseCase;
import in.bachatsetu.backend.auction.application.usecase.ListAuctionsUseCase;
import in.bachatsetu.backend.auction.application.usecase.PlaceBidUseCase;
import in.bachatsetu.backend.draw.application.security.DrawAuthorizationService;
import in.bachatsetu.backend.draw.domain.port.DrawRepository;
import in.bachatsetu.backend.group.application.port.SavingsGroupRepository;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

/**
 * Composes framework-free Auction application services when all outbound ports exist.
 *
 * <p>Reuses the pre-existing {@link DrawAuthorizationService} bean registered by
 * {@code DrawApplicationConfig} rather than declaring a duplicate — both configuration classes are
 * discovered by the same component scan and share one Spring {@code ApplicationContext}.
 *
 * <p>Gated on {@code bachatsetu.persistence.repositories.enabled} rather than a
 * cross-configuration-class {@code @ConditionalOnBean} check: regular (non-auto-configuration)
 * {@code @Configuration} classes discovered by component scanning have no guaranteed processing
 * order relative to one another, so a class-level {@code @ConditionalOnBean} referencing ports
 * defined by {@code AuctionInfrastructureConfig} was evaluated non-deterministically and could
 * skip this configuration even when every required port was actually present.
 */
@Configuration(proxyBeanMethods = false)
@ConditionalOnProperty(
        prefix = "bachatsetu.persistence.repositories",
        name = "enabled",
        havingValue = "true",
        matchIfMissing = true)
public class AuctionApplicationConfig {

    @Bean
    public AuctionApplicationMapper auctionApplicationMapper() {
        return new AuctionApplicationMapper();
    }

    @Bean
    public CreateAuctionUseCase createAuctionUseCase(
            DrawRepository repository,
            SavingsGroupRepository groupRepository,
            DomainEventPublisherPort eventPublisher,
            ClockPort clock,
            TransactionPort transaction,
            AuctionApplicationMapper mapper,
            DrawAuthorizationService authorization) {
        return new CreateAuctionApplicationService(
                repository, groupRepository, eventPublisher, clock, transaction, mapper, authorization);
    }

    @Bean
    public PlaceBidUseCase placeBidUseCase(
            DrawRepository repository,
            SavingsGroupRepository groupRepository,
            DomainEventPublisherPort eventPublisher,
            ClockPort clock,
            TransactionPort transaction,
            AuctionApplicationMapper mapper) {
        return new PlaceBidApplicationService(repository, groupRepository, eventPublisher, clock, transaction, mapper);
    }

    @Bean
    public CloseAuctionUseCase closeAuctionUseCase(
            DrawRepository repository,
            SavingsGroupRepository groupRepository,
            DomainEventPublisherPort eventPublisher,
            ClockPort clock,
            TransactionPort transaction,
            AuctionApplicationMapper mapper,
            DrawAuthorizationService authorization) {
        return new CloseAuctionApplicationService(
                repository, groupRepository, eventPublisher, clock, transaction, mapper, authorization);
    }

    @Bean
    public GetAuctionUseCase getAuctionUseCase(
            DrawRepository repository,
            TransactionPort transaction,
            AuctionApplicationMapper mapper) {
        return new GetAuctionApplicationService(repository, transaction, mapper);
    }

    @Bean
    public ListAuctionsUseCase listAuctionsUseCase(
            DrawRepository repository,
            TransactionPort transaction,
            AuctionApplicationMapper mapper) {
        return new ListAuctionsApplicationService(repository, transaction, mapper);
    }

    @Bean
    public GetWinnerUseCase getWinnerUseCase(
            DrawRepository repository,
            TransactionPort transaction,
            AuctionApplicationMapper mapper) {
        return new GetWinnerApplicationService(repository, transaction, mapper);
    }
}
