package in.bachatsetu.backend.support.interfaces.rest.mapper;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import in.bachatsetu.backend.auth.application.security.AuthenticatedUser;
import in.bachatsetu.backend.auth.domain.model.MobileNumber;
import in.bachatsetu.backend.auth.domain.model.UserId;
import in.bachatsetu.backend.shared.domain.AggregateId;
import in.bachatsetu.backend.shared.domain.Page;
import in.bachatsetu.backend.support.application.command.CreateTicketCommand;
import in.bachatsetu.backend.support.application.query.SupportTicketResult;
import in.bachatsetu.backend.support.application.usecase.GetTicketUseCase;
import in.bachatsetu.backend.support.domain.model.TicketCategory;
import in.bachatsetu.backend.support.domain.model.TicketPriority;
import in.bachatsetu.backend.support.domain.model.TicketStatus;
import in.bachatsetu.backend.support.domain.port.SupportTicketSearchCriteria;
import in.bachatsetu.backend.support.interfaces.rest.dto.CreateTicketRequest;
import in.bachatsetu.backend.support.interfaces.rest.dto.TicketPageResponse;
import in.bachatsetu.backend.support.interfaces.rest.dto.TicketResponse;
import java.time.Instant;
import java.util.List;
import java.util.Set;
import org.junit.jupiter.api.Test;

class SupportApiMapperTest {

    private static final Instant NOW = Instant.parse("2026-07-10T08:00:00Z");

    private final SupportApiMapper mapper = new SupportApiMapper();

    @Test
    void mapsACreateRequestToACommand() {
        AuthenticatedUser currentUser = authenticatedUser();

        CreateTicketCommand command = mapper.toCreateCommand(
                currentUser, new CreateTicketRequest("PAYMENT", "HIGH", "Subject", "Description"));

        assertThat(command.tenantId()).isEqualTo(currentUser.tenantId());
        assertThat(command.category()).isEqualTo(TicketCategory.PAYMENT);
        assertThat(command.priority()).isEqualTo(TicketPriority.HIGH);
    }

    @Test
    void resolvesATicketThroughTheUseCase() {
        AggregateId ticketId = AggregateId.newId();
        GetTicketUseCase useCase = mock(GetTicketUseCase.class);
        when(useCase.execute(ticketId)).thenReturn(new SupportTicketResult(
                ticketId, AggregateId.newId(), AggregateId.newId(), TicketCategory.OTHER, TicketPriority.LOW,
                TicketStatus.OPEN, "Subject", "Description", null, NOW, null, null));

        TicketResponse response = mapper.get(useCase, ticketId.toString());

        assertThat(response.ticketId()).isEqualTo(ticketId.toString());
    }

    @Test
    void buildsSearchCriteriaWithDefaults() {
        SupportTicketSearchCriteria criteria = mapper.toSearchCriteria(
                "OPEN", null, null, null, null, null, null, null, null, "asc");

        assertThat(criteria.status()).isEqualTo(TicketStatus.OPEN);
        assertThat(criteria.page()).isEqualTo(0);
        assertThat(criteria.size()).isEqualTo(20);
    }

    @Test
    void mapsAResultPageToAPageResponse() {
        SupportTicketResult result = new SupportTicketResult(
                AggregateId.newId(), AggregateId.newId(), AggregateId.newId(), TicketCategory.LOGIN,
                TicketPriority.MEDIUM, TicketStatus.OPEN, "Subject", "Description", null, NOW, null, null);

        TicketPageResponse<TicketResponse> response = mapper.toPageResponse(new Page<>(List.of(result), 0, 20, 1));

        assertThat(response.content()).hasSize(1);
        assertThat(response.totalElements()).isEqualTo(1);
    }

    private AuthenticatedUser authenticatedUser() {
        return new AuthenticatedUser(
                UserId.newId(), MobileNumber.of("+919876543210"), AggregateId.newId(), Set.of(), Set.of());
    }
}
