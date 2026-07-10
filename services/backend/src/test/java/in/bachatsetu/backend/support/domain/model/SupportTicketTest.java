package in.bachatsetu.backend.support.domain.model;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import in.bachatsetu.backend.shared.domain.AggregateId;
import in.bachatsetu.backend.support.domain.exception.SupportTicketDomainException;
import java.time.Instant;
import org.junit.jupiter.api.Test;

class SupportTicketTest {

    private static final Instant NOW = Instant.parse("2026-07-10T08:00:00Z");

    @Test
    void createsAnOpenTicketAndRegistersACreatedEvent() {
        AggregateId tenantId = AggregateId.newId();
        AggregateId raisedBy = AggregateId.newId();

        SupportTicket ticket = SupportTicket.create(
                AggregateId.newId(), tenantId, raisedBy, TicketCategory.PAYMENT, TicketPriority.HIGH,
                "Payment not verified", "My payment failed verification", raisedBy, NOW);

        assertThat(ticket.status()).isEqualTo(TicketStatus.OPEN);
        assertThat(ticket.assignedTo()).isNull();
        assertThat(ticket.resolvedAt()).isNull();
        assertThat(ticket.resolution()).isNull();
        assertThat(ticket.domainEvents()).hasSize(1);
    }

    @Test
    void assignsAnOpenTicket() {
        SupportTicket ticket = newOpenTicket();
        AggregateId assignee = AggregateId.newId();

        ticket.assign(assignee, assignee, NOW.plusSeconds(60));

        assertThat(ticket.status()).isEqualTo(TicketStatus.ASSIGNED);
        assertThat(ticket.assignedTo()).isEqualTo(assignee);
    }

    @Test
    void resolvesAnAssignedTicket() {
        SupportTicket ticket = newOpenTicket();
        AggregateId assignee = AggregateId.newId();
        ticket.assign(assignee, assignee, NOW.plusSeconds(60));

        ticket.resolve("Refunded the duplicate charge", assignee, NOW.plusSeconds(120));

        assertThat(ticket.status()).isEqualTo(TicketStatus.RESOLVED);
        assertThat(ticket.resolution()).isEqualTo("Refunded the duplicate charge");
        assertThat(ticket.resolvedAt()).isEqualTo(NOW.plusSeconds(120));
    }

    @Test
    void closesAResolvedTicket() {
        SupportTicket ticket = newOpenTicket();
        AggregateId assignee = AggregateId.newId();
        ticket.assign(assignee, assignee, NOW.plusSeconds(60));
        ticket.resolve("Fixed", assignee, NOW.plusSeconds(120));

        ticket.close(assignee, NOW.plusSeconds(180));

        assertThat(ticket.status()).isEqualTo(TicketStatus.CLOSED);
    }

    @Test
    void rejectsClosingATicketThatIsNotResolved() {
        SupportTicket ticket = newOpenTicket();

        assertThatThrownBy(() -> ticket.close(ticket.raisedBy(), NOW))
                .isInstanceOf(SupportTicketDomainException.class);
    }

    @Test
    void rejectsResolvingAClosedTicket() {
        SupportTicket ticket = newOpenTicket();
        AggregateId assignee = AggregateId.newId();
        ticket.assign(assignee, assignee, NOW.plusSeconds(60));
        ticket.resolve("Fixed", assignee, NOW.plusSeconds(120));
        ticket.close(assignee, NOW.plusSeconds(180));

        assertThatThrownBy(() -> ticket.resolve("Again", assignee, NOW.plusSeconds(240)))
                .isInstanceOf(SupportTicketDomainException.class);
    }

    @Test
    void rejectsAssigningAResolvedTicket() {
        SupportTicket ticket = newOpenTicket();
        AggregateId assignee = AggregateId.newId();
        ticket.assign(assignee, assignee, NOW.plusSeconds(60));
        ticket.resolve("Fixed", assignee, NOW.plusSeconds(120));

        assertThatThrownBy(() -> ticket.assign(AggregateId.newId(), assignee, NOW.plusSeconds(150)))
                .isInstanceOf(SupportTicketDomainException.class);
    }

    private SupportTicket newOpenTicket() {
        AggregateId raisedBy = AggregateId.newId();
        return SupportTicket.create(
                AggregateId.newId(), AggregateId.newId(), raisedBy, TicketCategory.LOGIN, TicketPriority.LOW,
                "Cannot log in", "OTP never arrives", raisedBy, NOW);
    }
}
