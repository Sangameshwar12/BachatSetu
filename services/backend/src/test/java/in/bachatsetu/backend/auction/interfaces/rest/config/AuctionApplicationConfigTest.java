package in.bachatsetu.backend.auction.interfaces.rest.config;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;

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
import in.bachatsetu.backend.draw.application.security.DrawAuthorizationService;
import in.bachatsetu.backend.draw.domain.port.DrawRepository;
import in.bachatsetu.backend.group.application.port.SavingsGroupRepository;
import org.junit.jupiter.api.Test;

class AuctionApplicationConfigTest {

    private final AuctionApplicationConfig config = new AuctionApplicationConfig();
    private final AuctionApplicationMapper mapper = config.auctionApplicationMapper();
    private final DrawRepository repository = mock(DrawRepository.class);
    private final SavingsGroupRepository groupRepository = mock(SavingsGroupRepository.class);
    private final DomainEventPublisherPort eventPublisher = mock(DomainEventPublisherPort.class);
    private final ClockPort clock = mock(ClockPort.class);
    private final TransactionPort transaction = mock(TransactionPort.class);
    private final DrawAuthorizationService authorization = new DrawAuthorizationService();

    @Test
    void composesCreateAuctionUseCase() {
        assertThat(config.createAuctionUseCase(
                        repository, groupRepository, eventPublisher, clock, transaction, mapper, authorization))
                .isInstanceOf(CreateAuctionApplicationService.class);
    }

    @Test
    void composesPlaceBidUseCase() {
        assertThat(config.placeBidUseCase(repository, groupRepository, eventPublisher, clock, transaction, mapper))
                .isInstanceOf(PlaceBidApplicationService.class);
    }

    @Test
    void composesCloseAuctionUseCase() {
        assertThat(config.closeAuctionUseCase(
                        repository, groupRepository, eventPublisher, clock, transaction, mapper, authorization))
                .isInstanceOf(CloseAuctionApplicationService.class);
    }

    @Test
    void composesGetAuctionUseCase() {
        assertThat(config.getAuctionUseCase(repository, transaction, mapper))
                .isInstanceOf(GetAuctionApplicationService.class);
    }

    @Test
    void composesListAuctionsUseCase() {
        assertThat(config.listAuctionsUseCase(repository, transaction, mapper))
                .isInstanceOf(ListAuctionsApplicationService.class);
    }

    @Test
    void composesGetWinnerUseCase() {
        assertThat(config.getWinnerUseCase(repository, transaction, mapper))
                .isInstanceOf(GetWinnerApplicationService.class);
    }
}
