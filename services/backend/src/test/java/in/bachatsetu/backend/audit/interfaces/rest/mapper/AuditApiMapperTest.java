package in.bachatsetu.backend.audit.interfaces.rest.mapper;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import in.bachatsetu.backend.audit.application.command.CreateAuditEntryCommand;
import in.bachatsetu.backend.audit.application.query.AuditEntryResult;
import in.bachatsetu.backend.audit.domain.model.AuditEventType;
import in.bachatsetu.backend.audit.domain.port.AuditPage;
import in.bachatsetu.backend.audit.domain.port.AuditSearchCriteria;
import in.bachatsetu.backend.audit.domain.port.AuditSortField;
import in.bachatsetu.backend.audit.domain.port.SortDirection;
import in.bachatsetu.backend.audit.interfaces.rest.dto.AuditEntryResponse;
import in.bachatsetu.backend.audit.interfaces.rest.dto.CreateAuditEntryRequest;
import in.bachatsetu.backend.audit.interfaces.rest.dto.PageResponse;
import in.bachatsetu.backend.auth.application.security.AuthenticatedUser;
import in.bachatsetu.backend.auth.domain.model.MobileNumber;
import in.bachatsetu.backend.auth.domain.model.UserId;
import in.bachatsetu.backend.shared.domain.AggregateId;
import java.time.Instant;
import java.util.List;
import java.util.Set;
import java.util.UUID;
import org.junit.jupiter.api.Test;

class AuditApiMapperTest {

    private static final Instant NOW = Instant.parse("2026-07-08T08:00:00Z");

    private final AuditApiMapper mapper = new AuditApiMapper();

    @Test
    void mapsARequestToACreateCommandUsingTheCallersIdentity() {
        AggregateId tenantId = AggregateId.newId();
        AggregateId resourceId = AggregateId.newId();
        AuthenticatedUser currentUser = authenticatedUser(tenantId);
        CreateAuditEntryRequest request = new CreateAuditEntryRequest(
                "LOGIN", "auth", "User", resourceId.toString(), "LOGIN", "signed in", "127.0.0.1", "agent", "{}");

        CreateAuditEntryCommand command = mapper.toCreateCommand(request, currentUser);

        assertThat(command.tenantId()).isEqualTo(tenantId);
        assertThat(command.actorId()).isEqualTo(currentUser.userId().toAggregateId());
        assertThat(command.eventType()).isEqualTo(AuditEventType.LOGIN);
        assertThat(command.moduleName()).isEqualTo("auth");
        assertThat(command.resourceType()).isEqualTo("User");
        assertThat(command.resourceId()).isEqualTo(resourceId);
        assertThat(command.action()).isEqualTo("LOGIN");
        assertThat(command.description()).isEqualTo("signed in");
        assertThat(command.metadata()).isEqualTo("{}");
    }

    @Test
    void mapsARequestWithNoResourceIdToANullResourceId() {
        AuthenticatedUser currentUser = authenticatedUser(AggregateId.newId());
        CreateAuditEntryRequest request = new CreateAuditEntryRequest(
                "SYSTEM_EVENT", "automation", null, null, "SYSTEM_EVENT", null, null, null, null);

        CreateAuditEntryCommand command = mapper.toCreateCommand(request, currentUser);

        assertThat(command.resourceId()).isNull();
    }

    @Test
    void mapsAnAuditIdString() {
        AggregateId auditId = AggregateId.newId();

        assertThat(mapper.toAuditId(auditId.toString())).isEqualTo(auditId);
    }

    @Test
    void mapsSearchParametersToCriteriaScopedToTheCallersTenant() {
        AggregateId tenantId = AggregateId.newId();
        AggregateId actorId = AggregateId.newId();
        AuthenticatedUser currentUser = authenticatedUser(tenantId);

        AuditSearchCriteria criteria = mapper.toSearchCriteria(
                currentUser, actorId.toString(), "payment", "PAYMENT_VERIFIED", NOW.minusSeconds(60), NOW, 1, 10,
                "createdAt", "asc");

        assertThat(criteria.tenantId()).isEqualTo(tenantId);
        assertThat(criteria.actorId()).isEqualTo(actorId);
        assertThat(criteria.moduleName()).isEqualTo("payment");
        assertThat(criteria.eventType()).isEqualTo(AuditEventType.PAYMENT_VERIFIED);
        assertThat(criteria.dateFrom()).isEqualTo(NOW.minusSeconds(60));
        assertThat(criteria.dateTo()).isEqualTo(NOW);
        assertThat(criteria.page()).isEqualTo(1);
        assertThat(criteria.size()).isEqualTo(10);
        assertThat(criteria.sortField()).isEqualTo(AuditSortField.CREATED_AT);
        assertThat(criteria.direction()).isEqualTo(SortDirection.ASC);
    }

    @Test
    void mapsSearchParametersWithEveryOptionalFilterOmitted() {
        AuthenticatedUser currentUser = authenticatedUser(AggregateId.newId());

        AuditSearchCriteria criteria = mapper.toSearchCriteria(
                currentUser, null, null, null, null, null, 0, 20, "createdAt", "desc");

        assertThat(criteria.actorId()).isNull();
        assertThat(criteria.moduleName()).isNull();
        assertThat(criteria.eventType()).isNull();
        assertThat(criteria.direction()).isEqualTo(SortDirection.DESC);
    }

    @Test
    void rejectsAnUnsupportedSortField() {
        AuthenticatedUser currentUser = authenticatedUser(AggregateId.newId());

        assertThatThrownBy(() -> mapper.toSearchCriteria(
                        currentUser, null, null, null, null, null, 0, 20, "unsupported", "desc"))
                .isInstanceOf(IllegalArgumentException.class);
    }

    @Test
    void rejectsAnUnsupportedSortDirection() {
        AuthenticatedUser currentUser = authenticatedUser(AggregateId.newId());

        assertThatThrownBy(() -> mapper.toSearchCriteria(
                        currentUser, null, null, null, null, null, 0, 20, "createdAt", "sideways"))
                .isInstanceOf(IllegalArgumentException.class);
    }

    @Test
    void mapsAResultToAResponse() {
        UUID auditId = UUID.randomUUID();
        UUID tenantId = UUID.randomUUID();
        AuditEntryResult result = new AuditEntryResult(
                auditId, tenantId, null, AuditEventType.LOGIN, "auth", null, null, "LOGIN", null, null, null, null,
                NOW);

        AuditEntryResponse response = mapper.toResponse(result);

        assertThat(response.auditId()).isEqualTo(auditId.toString());
        assertThat(response.tenantId()).isEqualTo(tenantId.toString());
        assertThat(response.actorId()).isNull();
        assertThat(response.eventType()).isEqualTo("LOGIN");
        assertThat(response.createdAt()).isEqualTo(NOW);
    }

    @Test
    void mapsAPageOfResultsToAPageResponse() {
        AuditEntryResult result = new AuditEntryResult(
                UUID.randomUUID(), UUID.randomUUID(), null, AuditEventType.LOGIN, "auth", null, null, "LOGIN",
                null, null, null, null, NOW);
        AuditPage<AuditEntryResult> page = new AuditPage<>(List.of(result), 0, 20, 1);

        PageResponse<AuditEntryResponse> response = mapper.toPageResponse(page);

        assertThat(response.content()).hasSize(1);
        assertThat(response.totalElements()).isEqualTo(1);
        assertThat(response.totalPages()).isEqualTo(1);
        assertThat(response.hasNext()).isFalse();
        assertThat(response.hasPrevious()).isFalse();
    }

    private AuthenticatedUser authenticatedUser(AggregateId tenantId) {
        return new AuthenticatedUser(
                UserId.newId(), MobileNumber.of("+919876543210"), tenantId, Set.of("GROUP_MEMBER"),
                Set.of("audit.write"));
    }
}
