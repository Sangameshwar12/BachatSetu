package in.bachatsetu.backend.receipt.application;

import static in.bachatsetu.backend.receipt.application.ApplicationTestFixture.createCommand;
import static in.bachatsetu.backend.receipt.application.ApplicationTestFixture.directTransaction;
import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import in.bachatsetu.backend.receipt.application.command.CreateReceiptCommand;
import in.bachatsetu.backend.receipt.application.exception.ReceiptApplicationException;
import in.bachatsetu.backend.receipt.application.exception.ReceiptNotFoundException;
import in.bachatsetu.backend.receipt.application.port.DomainEventPublisherPort;
import in.bachatsetu.backend.receipt.application.port.TransactionPort;
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
        CreateReceiptCommand create = createCommand();

        assertThat(create.lines()).isNotEmpty();
    }

    @Test
    void commandContractsRejectNullContext() {
        CreateReceiptCommand create = createCommand();

        assertThatThrownBy(() -> new CreateReceiptCommand(
                        null, create.paymentId(), create.memberId(), create.lines(), create.actorId()))
                .isInstanceOf(NullPointerException.class);
        assertThatThrownBy(() -> new CreateReceiptCommand(
                        create.tenantId(), null, create.memberId(), create.lines(), create.actorId()))
                .isInstanceOf(NullPointerException.class);
        assertThatThrownBy(() -> new CreateReceiptCommand(
                        create.tenantId(), create.paymentId(), null, create.lines(), create.actorId()))
                .isInstanceOf(NullPointerException.class);
        assertThatThrownBy(() -> new CreateReceiptCommand(
                        create.tenantId(), create.paymentId(), create.memberId(), null, create.actorId()))
                .isInstanceOf(NullPointerException.class);
        assertThatThrownBy(() -> new CreateReceiptCommand(
                        create.tenantId(), create.paymentId(), create.memberId(), create.lines(), null))
                .isInstanceOf(NullPointerException.class);
    }

    @Test
    void outboundPortsExposeRequiredOperations() {
        AtomicReference<List<?>> published = new AtomicReference<>();
        DomainEventPublisherPort publisher = published::set;
        TransactionPort transaction = directTransaction();

        publisher.publish(List.of());
        assertThat(published.get()).isEmpty();
        assertThat(transaction.execute(() -> "committed")).isEqualTo("committed");
    }

    @Test
    void useCaseAndExceptionContractsArePresent() {
        List<Class<?>> useCases = List.of(
                in.bachatsetu.backend.receipt.application.usecase.CreateReceiptUseCase.class,
                in.bachatsetu.backend.receipt.application.usecase.GetReceiptUseCase.class,
                in.bachatsetu.backend.receipt.application.usecase.ListReceiptsUseCase.class);

        assertThat(useCases).allMatch(Class::isInterface);
        assertThat(new ReceiptApplicationException("application failure")).hasMessage("application failure");
        assertThat(new ReceiptNotFoundException("missing")).isInstanceOf(ReceiptApplicationException.class);
    }

    @Test
    void domainPortExposesTenantScopedLookupAndPagination() {
        Set<String> methods = Arrays.stream(in.bachatsetu.backend.receipt.domain.port.ReceiptRepository.class
                        .getDeclaredMethods())
                .map(Method::getName)
                .collect(Collectors.toSet());

        assertThat(methods).contains(
                "findById", "findByNumber", "findByPaymentId", "findPage", "save");
    }
}
