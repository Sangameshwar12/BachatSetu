# Support Module

## Overview

The `support` module is a standalone DDD module implementing customer support ticketing for the BachatSetu
platform team. Any authenticated user may raise a ticket; every other operation (viewing, searching,
assigning, resolving, closing) is restricted to the `PLATFORM_ADMIN` role.

## Architecture

```
support/
  domain/
    model/       SupportTicket (aggregate root), TicketCategory, TicketPriority, TicketStatus
    event/       SupportTicketCreated, SupportTicketAssigned, SupportTicketResolved, SupportTicketClosed
    exception/   SupportTicketDomainException
    port/        SupportTicketRepository, SupportTicketSearchCriteria
  application/
    port/        ClockPort, TransactionPort, DomainEventPublisherPort
    command/     CreateTicketCommand, AssignTicketCommand, ResolveTicketCommand, CloseTicketCommand
    query/       SupportTicketResult
    exception/   SupportFailureReason, SupportApplicationException
    usecase/     CreateTicketUseCase, GetTicketUseCase, SearchTicketsUseCase, AssignTicketUseCase,
                 ResolveTicketUseCase, CloseTicketUseCase
    service/     One application service per use case above
    mapper/      SupportApplicationMapper
  interfaces/rest/
    controller/  SupportController
    mapper/      SupportApiMapper
    dto/         CreateTicketRequest, AssignTicketRequest, ResolveTicketRequest, TicketResponse,
                 TicketPageResponse
    exception/   SupportExceptionHandler
    config/      SupportApplicationConfig, SupportInfrastructureConfig
    adapter/     SystemSupportClockAdapter, SpringSupportTransactionAdapter,
                 ApplicationEventSupportEventPublisherAdapter
```

Persistence follows the codebase-wide convention: the JPA entity, mapper, Spring Data repository, and
repository adapter all live under `infrastructure.persistence.*` (`SupportTicketJpaEntity`,
`SupportTicketJpaMapper`, `SupportTicketSpringDataRepository`, `SupportTicketRepositoryAdapter`), exactly
like every other module's persistence adapters — the module's own tree holds only the domain port
(`SupportTicketRepository`) it implements. `SupportTicket` is cross-tenant (a platform administrator can act
on a ticket from any tenant), so the repository has no tenant-scoping in its method signatures — the same
pattern already used by `admin.domain.port.PlatformUserRepository`.

Pagination and sorting reuse the module-agnostic `shared.domain.Page`, `shared.domain.PageQuery`, and
`shared.domain.SortDirection` types (new, additive classes in the already-shared `shared` package) rather
than the admin module's `PlatformPage`/`PlatformPageRequest`/`SortDirection` — see
[platform-operations.md](platform-operations.md#module-cycle-avoidance) for why.

## SupportTicket Lifecycle

```
create() ─────────────► OPEN
                           │ assign()
                           ▼
                        ASSIGNED ───────────► IN_PROGRESS  (reachable in the domain model; no dedicated
                           │                                REST transition exists yet — see Known
                           │ resolve()                      Limitations)
                           ▼
                        RESOLVED
                           │ close()
                           ▼
                        CLOSED
```

- `assign()` is rejected once a ticket is `RESOLVED` or `CLOSED`.
- `resolve()` requires a non-blank resolution and is rejected if the ticket is already `RESOLVED` or `CLOSED`.
- `close()` requires the ticket to already be `RESOLVED`.

## Sequence: Raising and Resolving a Ticket

```
User                SupportController        CreateTicketUseCase        SupportTicketRepository
 │  POST /tickets           │                          │                           │
 ├──────────────────────────►                          │                           │
 │                          │  toCreateCommand(user, req)                          │
 │                          ├─────────────────────────►│                          │
 │                          │                          │  SupportTicket.create()  │
 │                          │                          ├──────────────────────────►
 │                          │                          │  save()                  │
 │                          │                          │◄──────────────────────────
 │                          │                          │  publish(events)         │
 │                          │◄─────────────────────────┤                          │
 │  200 OK (OPEN)           │                          │                           │
 │◄──────────────────────────                          │                           │
 ...
Platform Admin      SupportController        ResolveTicketUseCase       SupportTicketRepository
 │  POST /tickets/{id}/resolve                         │                           │
 ├──────────────────────────►                          │                           │
 │                          │  toResolveCommand(id, user, req)                     │
 │                          ├─────────────────────────►│                          │
 │                          │                          │  ticket.resolve(...)     │
 │                          │                          │  save() + publish(events)│
 │                          │◄─────────────────────────┤                          │
 │  200 OK (RESOLVED)       │                          │                           │
 │◄──────────────────────────                          │                           │
```

## API Summary

| Method | Path | Access | Description |
|---|---|---|---|
| POST | `/api/v1/support/tickets` | Any authenticated user | Raise a ticket |
| GET | `/api/v1/support/tickets/{ticketId}` | `PLATFORM_ADMIN` | Get one ticket |
| GET | `/api/v1/support/tickets` | `PLATFORM_ADMIN` | Search tickets (filter + paginate) |
| POST | `/api/v1/support/tickets/{ticketId}/assign` | `PLATFORM_ADMIN` | Assign a ticket |
| POST | `/api/v1/support/tickets/{ticketId}/resolve` | `PLATFORM_ADMIN` | Resolve a ticket |
| POST | `/api/v1/support/tickets/{ticketId}/close` | `PLATFORM_ADMIN` | Close a resolved ticket |

Search supports `status`, `priority`, `category`, `tenantId`, `raisedBy`, `createdAfter`, `createdBefore`,
`page`, `size`, and `direction` (`asc`/`desc`, sorted by `createdAt`) as optional query parameters.

## Audit Integration

`SupportAuditListener` (in `audit.interfaces.rest.event`, matching the codebase's established
cycle-avoidance pattern of reacting to domain events rather than being called directly) records
`SUPPORT_TICKET_CREATED`, `SUPPORT_TICKET_ASSIGNED`, `SUPPORT_TICKET_RESOLVED`, and `SUPPORT_TICKET_CLOSED`
audit entries. Each handler independently catches and logs any failure, so an audit-recording failure never
surfaces to the caller.

## Errors

| Condition | HTTP status | Problem code |
|---|---|---|
| Ticket not found | 404 | `ticket-not-found` |
| Invalid lifecycle transition (e.g. resolving a closed ticket) | 422 | `ticket-validation-failed` |
| Request validation failure | 400 | `validation-error` |
| Not authenticated | 401 | `authentication-required` |
| Missing `PLATFORM_ADMIN` role | 403 | `platform-administrator-required` |

## Configuration

| Property | Default | Purpose |
|---|---|---|
| `bachatsetu.support.rest.enabled` | `true` | Enables `SupportController` |

## Known Limitations

- **`IN_PROGRESS` has no dedicated REST transition.** The domain model supports the status (a ticket
  legitimately sits `IN_PROGRESS` between being assigned and resolved), but the sprint's API list
  (Create/Get/Search/Assign/Resolve/Close) has no "start progress" action, so today a ticket moves directly
  from `ASSIGNED` to `RESOLVED` through the exposed API. Adding an explicit transition is straightforward
  future work and does not require any redesign.
