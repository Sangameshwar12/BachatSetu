package in.bachatsetu.backend.auction.application;

import static in.bachatsetu.backend.auction.application.ApplicationTestFixture.NOW;
import static in.bachatsetu.backend.auction.application.ApplicationTestFixture.createCommand;
import static in.bachatsetu.backend.auction.application.ApplicationTestFixture.directTransaction;
import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import in.bachatsetu.backend.auction.application.command.CloseAuctionCommand;
import in.bachatsetu.backend.auction.application.command.CreateAuctionCommand;
import in.bachatsetu.backend.auction.application.command.PlaceBidCommand;
import in.bachatsetu.backend.auction.application.exception.AuctionApplicationException;
import in.bachatsetu.backend.auction.application.exception.AuctionNotFoundException;
import in.bachatsetu.backend.auction.application.port.ClockPort;
import in.bachatsetu.backend.auction.application.port.DomainEventPublisherPort;
import in.bachatsetu.backend.auction.application.port.TransactionPort;
import in.bachatsetu.backend.shared.domain.AggregateId;
import in.bachatsetu.backend.shared.domain.Money;
import java.lang.reflect.Method;
import java.util.Arrays;
import java.util.List;
import java.util.Set;
import java.util.concurrent.atomic.AtomicReference;
import java.util.stream.Collectors;
import org.junit.jupiter.api.Test;

class ApplicationContractTest {

    @Test
    void commandContractsCarryRequiredContext() {
        CreateAuctionCommand create = createCommand();
        AggregateId id = AggregateId.newId();

        assertThat(create.number()).isNotNull();
        assertThat(new PlaceBidCommand(id, id, id, Money.inr(1_000), id).auctionId()).isEqualTo(id);
        assertThat(new CloseAuctionCommand(id, id, id, id).winnerId()).isEqualTo(id);
    }

    @Test
    void commandContractsRejectNullContext() {
        CreateAuctionCommand create = createCommand();
        AggregateId id = AggregateId.newId();

        assertThatThrownBy(() -> new CreateAuctionCommand(
                        null, create.groupId(), create.cycleId(), create.number(), create.actorId()))
                .isInstanceOf(NullPointerException.class);
        assertThatThrownBy(() -> new CreateAuctionCommand(
                        create.tenantId(), null, create.cycleId(), create.number(), create.actorId()))
                .isInstanceOf(NullPointerException.class);
        assertThatThrownBy(() -> new CreateAuctionCommand(
                        create.tenantId(), create.groupId(), null, create.number(), create.actorId()))
                .isInstanceOf(NullPointerException.class);
        assertThatThrownBy(() -> new CreateAuctionCommand(
                        create.tenantId(), create.groupId(), create.cycleId(), null, create.actorId()))
                .isInstanceOf(NullPointerException.class);
        assertThatThrownBy(() -> new CreateAuctionCommand(
                        create.tenantId(), create.groupId(), create.cycleId(), create.number(), null))
                .isInstanceOf(NullPointerException.class);

        assertThatThrownBy(() -> new PlaceBidCommand(null, id, id, Money.inr(1_000), id))
                .isInstanceOf(NullPointerException.class);
        assertThatThrownBy(() -> new PlaceBidCommand(id, null, id, Money.inr(1_000), id))
                .isInstanceOf(NullPointerException.class);
        assertThatThrownBy(() -> new PlaceBidCommand(id, id, null, Money.inr(1_000), id))
                .isInstanceOf(NullPointerException.class);
        assertThatThrownBy(() -> new PlaceBidCommand(id, id, id, null, id))
                .isInstanceOf(NullPointerException.class);
        assertThatThrownBy(() -> new PlaceBidCommand(id, id, id, Money.inr(1_000), null))
                .isInstanceOf(NullPointerException.class);

        assertThatThrownBy(() -> new CloseAuctionCommand(null, id, id, id)).isInstanceOf(NullPointerException.class);
        assertThatThrownBy(() -> new CloseAuctionCommand(id, null, id, id)).isInstanceOf(NullPointerException.class);
        assertThatThrownBy(() -> new CloseAuctionCommand(id, id, null, id)).isInstanceOf(NullPointerException.class);
        assertThatThrownBy(() -> new CloseAuctionCommand(id, id, id, null)).isInstanceOf(NullPointerException.class);
    }

    @Test
    void outboundPortsExposeRequiredOperations() {
        ClockPort clock = () -> NOW;
        AtomicReference<List<?>> published = new AtomicReference<>();
        DomainEventPublisherPort publisher = published::set;
        TransactionPort transaction = directTransaction();

        assertThat(clock.now()).isEqualTo(NOW);
        publisher.publish(List.of());
        assertThat(published.get()).isEmpty();
        assertThat(transaction.execute(() -> "committed")).isEqualTo("committed");
    }

    @Test
    void useCaseAndExceptionContractsArePresent() {
        List<Class<?>> useCases = List.of(
                in.bachatsetu.backend.auction.application.usecase.CreateAuctionUseCase.class,
                in.bachatsetu.backend.auction.application.usecase.PlaceBidUseCase.class,
                in.bachatsetu.backend.auction.application.usecase.CloseAuctionUseCase.class,
                in.bachatsetu.backend.auction.application.usecase.GetAuctionUseCase.class,
                in.bachatsetu.backend.auction.application.usecase.ListAuctionsUseCase.class,
                in.bachatsetu.backend.auction.application.usecase.GetWinnerUseCase.class);

        assertThat(useCases).allMatch(Class::isInterface);
        assertThat(new AuctionApplicationException("application failure")).hasMessage("application failure");
        assertThat(new AuctionNotFoundException("missing")).isInstanceOf(AuctionApplicationException.class);
    }

    @Test
    void domainPortExposesTenantScopedLookupAndTypeFilteredPagination() {
        Set<String> methods = Arrays.stream(in.bachatsetu.backend.draw.domain.port.DrawRepository.class
                        .getDeclaredMethods())
                .map(Method::getName)
                .collect(Collectors.toSet());

        assertThat(methods).contains("findById", "findPage", "findPageByType", "save");
    }
}
