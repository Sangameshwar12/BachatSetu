package in.bachatsetu.backend.support.interfaces.rest.controller;

import in.bachatsetu.backend.auth.application.security.AuthenticatedUser;
import in.bachatsetu.backend.auth.application.security.CurrentUserProvider;
import in.bachatsetu.backend.shared.domain.Page;
import in.bachatsetu.backend.support.application.query.SupportTicketResult;
import in.bachatsetu.backend.support.application.usecase.AssignTicketUseCase;
import in.bachatsetu.backend.support.application.usecase.CloseTicketUseCase;
import in.bachatsetu.backend.support.application.usecase.CreateTicketUseCase;
import in.bachatsetu.backend.support.application.usecase.GetTicketUseCase;
import in.bachatsetu.backend.support.application.usecase.ResolveTicketUseCase;
import in.bachatsetu.backend.support.application.usecase.SearchTicketsUseCase;
import in.bachatsetu.backend.support.interfaces.rest.dto.AssignTicketRequest;
import in.bachatsetu.backend.support.interfaces.rest.dto.CreateTicketRequest;
import in.bachatsetu.backend.support.interfaces.rest.dto.ResolveTicketRequest;
import in.bachatsetu.backend.support.interfaces.rest.dto.TicketPageResponse;
import in.bachatsetu.backend.support.interfaces.rest.dto.TicketResponse;
import in.bachatsetu.backend.support.interfaces.rest.mapper.SupportApiMapper;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import java.time.Instant;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.http.MediaType;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

/**
 * Customer support ticket endpoints. Any authenticated user may raise a ticket; every other operation is
 * restricted to the platform administrator role.
 */
@RestController
@RequestMapping(path = "/api/v1/support/tickets", produces = MediaType.APPLICATION_JSON_VALUE)
@Tag(name = "Support", description = "Customer support ticketing")
@ConditionalOnProperty(prefix = "bachatsetu.support.rest", name = "enabled", havingValue = "true", matchIfMissing = true)
public class SupportController {

    private final CreateTicketUseCase createTicket;
    private final GetTicketUseCase getTicket;
    private final SearchTicketsUseCase searchTickets;
    private final AssignTicketUseCase assignTicket;
    private final ResolveTicketUseCase resolveTicket;
    private final CloseTicketUseCase closeTicket;
    private final CurrentUserProvider currentUserProvider;
    private final SupportApiMapper mapper;

    public SupportController(
            CreateTicketUseCase createTicket,
            GetTicketUseCase getTicket,
            SearchTicketsUseCase searchTickets,
            AssignTicketUseCase assignTicket,
            ResolveTicketUseCase resolveTicket,
            CloseTicketUseCase closeTicket,
            CurrentUserProvider currentUserProvider,
            SupportApiMapper mapper) {
        this.createTicket = createTicket;
        this.getTicket = getTicket;
        this.searchTickets = searchTickets;
        this.assignTicket = assignTicket;
        this.resolveTicket = resolveTicket;
        this.closeTicket = closeTicket;
        this.currentUserProvider = currentUserProvider;
        this.mapper = mapper;
    }

    @PostMapping(consumes = MediaType.APPLICATION_JSON_VALUE)
    @Operation(summary = "Raise a support ticket", description = "Any authenticated user may raise a ticket.")
    public TicketResponse create(@Valid @RequestBody CreateTicketRequest request) {
        AuthenticatedUser currentUser = currentUserProvider.requireCurrentUser();
        SupportTicketResult result = createTicket.execute(mapper.toCreateCommand(currentUser, request));
        return mapper.toResponse(result);
    }

    @GetMapping("/{ticketId}")
    @Operation(summary = "Get a support ticket", description = "Platform administrator only.")
    @PreAuthorize("hasRole('PLATFORM_ADMIN')")
    public TicketResponse get(@PathVariable String ticketId) {
        currentUserProvider.requireCurrentUser();
        return mapper.get(getTicket, ticketId);
    }

    @GetMapping
    @Operation(summary = "Search support tickets", description = "Platform administrator only.")
    @PreAuthorize("hasRole('PLATFORM_ADMIN')")
    public TicketPageResponse<TicketResponse> search(
            @RequestParam(required = false) String status,
            @RequestParam(required = false) String priority,
            @RequestParam(required = false) String category,
            @RequestParam(required = false) String tenantId,
            @RequestParam(required = false) String raisedBy,
            @RequestParam(required = false) Instant createdAfter,
            @RequestParam(required = false) Instant createdBefore,
            @RequestParam(required = false) Integer page,
            @RequestParam(required = false) Integer size,
            @RequestParam(defaultValue = "desc") String direction) {
        currentUserProvider.requireCurrentUser();
        Page<SupportTicketResult> result = searchTickets.execute(mapper.toSearchCriteria(
                status, priority, category, tenantId, raisedBy, createdAfter, createdBefore, page, size, direction));
        return mapper.toPageResponse(result);
    }

    @PostMapping("/{ticketId}/assign")
    @Operation(summary = "Assign a support ticket", description = "Platform administrator only.")
    @PreAuthorize("hasRole('PLATFORM_ADMIN')")
    public TicketResponse assign(@PathVariable String ticketId, @Valid @RequestBody AssignTicketRequest request) {
        AuthenticatedUser currentUser = currentUserProvider.requireCurrentUser();
        SupportTicketResult result = assignTicket.execute(mapper.toAssignCommand(ticketId, currentUser, request));
        return mapper.toResponse(result);
    }

    @PostMapping("/{ticketId}/resolve")
    @Operation(summary = "Resolve a support ticket", description = "Platform administrator only.")
    @PreAuthorize("hasRole('PLATFORM_ADMIN')")
    public TicketResponse resolve(@PathVariable String ticketId, @Valid @RequestBody ResolveTicketRequest request) {
        AuthenticatedUser currentUser = currentUserProvider.requireCurrentUser();
        SupportTicketResult result = resolveTicket.execute(mapper.toResolveCommand(ticketId, currentUser, request));
        return mapper.toResponse(result);
    }

    @PostMapping("/{ticketId}/close")
    @Operation(summary = "Close a support ticket", description = "Platform administrator only.")
    @PreAuthorize("hasRole('PLATFORM_ADMIN')")
    public TicketResponse close(@PathVariable String ticketId) {
        AuthenticatedUser currentUser = currentUserProvider.requireCurrentUser();
        SupportTicketResult result = closeTicket.execute(mapper.toCloseCommand(ticketId, currentUser));
        return mapper.toResponse(result);
    }
}
