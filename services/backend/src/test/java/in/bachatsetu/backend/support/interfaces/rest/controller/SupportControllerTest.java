package in.bachatsetu.backend.support.interfaces.rest.controller;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.header;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import in.bachatsetu.backend.auth.application.security.AuthenticatedUser;
import in.bachatsetu.backend.auth.application.security.CurrentUserProvider;
import in.bachatsetu.backend.auth.domain.model.MobileNumber;
import in.bachatsetu.backend.auth.domain.model.UserId;
import in.bachatsetu.backend.shared.domain.AggregateId;
import in.bachatsetu.backend.shared.domain.Page;
import in.bachatsetu.backend.support.application.exception.SupportApplicationException;
import in.bachatsetu.backend.support.application.exception.SupportFailureReason;
import in.bachatsetu.backend.support.application.query.SupportTicketResult;
import in.bachatsetu.backend.support.application.usecase.AssignTicketUseCase;
import in.bachatsetu.backend.support.application.usecase.CloseTicketUseCase;
import in.bachatsetu.backend.support.application.usecase.CreateTicketUseCase;
import in.bachatsetu.backend.support.application.usecase.GetTicketUseCase;
import in.bachatsetu.backend.support.application.usecase.ResolveTicketUseCase;
import in.bachatsetu.backend.support.application.usecase.SearchTicketsUseCase;
import in.bachatsetu.backend.support.domain.model.TicketCategory;
import in.bachatsetu.backend.support.domain.model.TicketPriority;
import in.bachatsetu.backend.support.domain.model.TicketStatus;
import in.bachatsetu.backend.support.interfaces.rest.exception.SupportExceptionHandler;
import in.bachatsetu.backend.support.interfaces.rest.mapper.SupportApiMapper;
import java.time.Instant;
import java.util.List;
import java.util.Set;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.context.annotation.Import;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;

@WebMvcTest(SupportController.class)
@AutoConfigureMockMvc(addFilters = false)
@Import({SupportApiMapper.class, SupportExceptionHandler.class})
class SupportControllerTest {

    private static final Instant NOW = Instant.parse("2026-07-10T08:00:00Z");

    @Autowired
    private MockMvc mockMvc;

    @MockBean
    private CreateTicketUseCase createTicket;

    @MockBean
    private GetTicketUseCase getTicket;

    @MockBean
    private SearchTicketsUseCase searchTickets;

    @MockBean
    private AssignTicketUseCase assignTicket;

    @MockBean
    private ResolveTicketUseCase resolveTicket;

    @MockBean
    private CloseTicketUseCase closeTicket;

    @MockBean
    private CurrentUserProvider currentUserProvider;

    @Test
    void createsATicket() throws Exception {
        SupportTicketResult result = ticketResult(TicketStatus.OPEN);
        when(currentUserProvider.requireCurrentUser()).thenReturn(authenticatedUser());
        when(createTicket.execute(any())).thenReturn(result);

        mockMvc.perform(post("/api/v1/support/tickets")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"category":"PAYMENT","priority":"HIGH","subject":"Payment failed","description":"Verification failed"}
                                """))
                .andExpect(status().isCreated())
                .andExpect(header().string("Location", "/api/v1/support/tickets/" + result.ticketId()))
                .andExpect(jsonPath("$.status").value("OPEN"));
    }

    @Test
    void getsATicket() throws Exception {
        AggregateId ticketId = AggregateId.newId();
        when(currentUserProvider.requireCurrentUser()).thenReturn(authenticatedUser());
        when(getTicket.execute(ticketId)).thenReturn(ticketResult(TicketStatus.OPEN));

        mockMvc.perform(get("/api/v1/support/tickets/{ticketId}", ticketId.value()))
                .andExpect(status().isOk());
    }

    @Test
    void mapsTicketNotFound() throws Exception {
        AggregateId ticketId = AggregateId.newId();
        when(currentUserProvider.requireCurrentUser()).thenReturn(authenticatedUser());
        when(getTicket.execute(ticketId)).thenThrow(new SupportApplicationException(
                SupportFailureReason.TICKET_NOT_FOUND, "no ticket"));

        mockMvc.perform(get("/api/v1/support/tickets/{ticketId}", ticketId.value()))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.code").value("ticket-not-found"));
    }

    @Test
    void searchesTickets() throws Exception {
        when(currentUserProvider.requireCurrentUser()).thenReturn(authenticatedUser());
        when(searchTickets.execute(any())).thenReturn(new Page<>(List.of(ticketResult(TicketStatus.OPEN)), 0, 20, 1));

        mockMvc.perform(get("/api/v1/support/tickets"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.totalElements").value(1));
    }

    @Test
    void rejectsInvalidPaginationParameters() throws Exception {
        when(currentUserProvider.requireCurrentUser()).thenReturn(authenticatedUser());

        mockMvc.perform(get("/api/v1/support/tickets").param("page", "-1"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.code").value("validation-error"));

        mockMvc.perform(get("/api/v1/support/tickets").param("size", "500"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.code").value("validation-error"));
    }

    @Test
    void assignsATicket() throws Exception {
        AggregateId ticketId = AggregateId.newId();
        when(currentUserProvider.requireCurrentUser()).thenReturn(authenticatedUser());
        when(assignTicket.execute(any())).thenReturn(ticketResult(TicketStatus.ASSIGNED));

        mockMvc.perform(post("/api/v1/support/tickets/{ticketId}/assign", ticketId.value())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"assigneeId\":\"" + AggregateId.newId().value() + "\"}"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value("ASSIGNED"));
    }

    @Test
    void resolvesATicket() throws Exception {
        AggregateId ticketId = AggregateId.newId();
        when(currentUserProvider.requireCurrentUser()).thenReturn(authenticatedUser());
        when(resolveTicket.execute(any())).thenReturn(ticketResult(TicketStatus.RESOLVED));

        mockMvc.perform(post("/api/v1/support/tickets/{ticketId}/resolve", ticketId.value())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"resolution\":\"Fixed\"}"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value("RESOLVED"));
    }

    @Test
    void closesATicket() throws Exception {
        AggregateId ticketId = AggregateId.newId();
        when(currentUserProvider.requireCurrentUser()).thenReturn(authenticatedUser());
        when(closeTicket.execute(any())).thenReturn(ticketResult(TicketStatus.CLOSED));

        mockMvc.perform(post("/api/v1/support/tickets/{ticketId}/close", ticketId.value()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value("CLOSED"));
    }

    private SupportTicketResult ticketResult(TicketStatus status) {
        return new SupportTicketResult(
                AggregateId.newId(), AggregateId.newId(), AggregateId.newId(), TicketCategory.PAYMENT,
                TicketPriority.HIGH, status, "Subject", "Description", null, NOW, null, null);
    }

    private AuthenticatedUser authenticatedUser() {
        return new AuthenticatedUser(
                UserId.newId(), MobileNumber.of("+919876543210"), AggregateId.newId(), Set.of(), Set.of());
    }
}
