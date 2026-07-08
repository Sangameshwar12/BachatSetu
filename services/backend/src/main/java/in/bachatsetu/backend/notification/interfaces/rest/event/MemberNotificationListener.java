package in.bachatsetu.backend.notification.interfaces.rest.event;

import in.bachatsetu.backend.member.domain.event.MemberStatusChanged;
import in.bachatsetu.backend.member.domain.model.MemberProfile;
import in.bachatsetu.backend.member.domain.port.MemberRepository;
import in.bachatsetu.backend.notification.application.command.CreateNotificationCommand;
import in.bachatsetu.backend.notification.application.usecase.CreateNotificationUseCase;
import in.bachatsetu.backend.notification.domain.model.NotificationCategory;
import in.bachatsetu.backend.notification.domain.model.NotificationChannel;
import in.bachatsetu.backend.shared.domain.AggregateId;
import in.bachatsetu.backend.user.domain.event.UserContactChanged;
import java.util.Map;
import java.util.Objects;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.stereotype.Component;
import org.springframework.transaction.event.TransactionPhase;
import org.springframework.transaction.event.TransactionalEventListener;

/**
 * Notifies a member of the two Member-owned changes this sprint integrates: their membership status
 * changing, and their profile contact details changing.
 *
 * <p>Reacts to the pre-existing {@link MemberStatusChanged} domain event (raised on {@link MemberProfile})
 * for status changes. {@link MemberProfile} itself has no profile-detail fields of its own (name, email,
 * phone belong to {@code user.domain.model.UserProfile}), so "Profile Updated" is realized through the
 * pre-existing {@link UserContactChanged} event raised on the User aggregate instead. Since {@code
 * UserProfile} has no tenant of its own, the tenant is resolved by looking up the corresponding {@link
 * MemberProfile} through {@link MemberRepository#findByUserId(AggregateId)}; a user with no member profile
 * yet (nothing to notify against) is silently skipped.
 *
 * <p>Each handler listens with {@link TransactionPhase#AFTER_COMMIT} and independently catches and logs any
 * failure, so a notification failure never appears to fail the already-committed member/user change.
 */
@Component
@ConditionalOnProperty(
        prefix = "bachatsetu.persistence.repositories",
        name = "enabled",
        havingValue = "true",
        matchIfMissing = true)
public class MemberNotificationListener {

    private static final Logger LOGGER = LoggerFactory.getLogger(MemberNotificationListener.class);

    private final MemberRepository memberRepository;
    private final CreateNotificationUseCase createNotification;

    public MemberNotificationListener(
            MemberRepository memberRepository, CreateNotificationUseCase createNotification) {
        this.memberRepository = Objects.requireNonNull(memberRepository, "member repository must not be null");
        this.createNotification = Objects.requireNonNull(createNotification, "create notification must not be null");
    }

    @TransactionalEventListener(phase = TransactionPhase.AFTER_COMMIT)
    public void onMemberStatusChanged(MemberStatusChanged event) {
        try {
            memberRepository.findById(event.aggregateId()).ifPresent(member -> notify(
                    member.tenantId(), member.userId(), "Status Changed",
                    "Your membership status changed to " + event.newStatus() + "."));
        } catch (RuntimeException exception) {
            LOGGER.error("Failed to send a status-changed notification for member {}", event.aggregateId(), exception);
        }
    }

    @TransactionalEventListener(phase = TransactionPhase.AFTER_COMMIT)
    public void onUserContactChanged(UserContactChanged event) {
        try {
            memberRepository.findByUserId(event.aggregateId()).ifPresent(member -> notify(
                    member.tenantId(), event.aggregateId(), "Profile Updated",
                    "Your profile information has been updated."));
        } catch (RuntimeException exception) {
            LOGGER.error("Failed to send a profile-updated notification for user {}", event.aggregateId(), exception);
        }
    }

    private void notify(AggregateId tenantId, AggregateId recipientUserId, String title, String body) {
        createNotification.execute(new CreateNotificationCommand(
                tenantId,
                recipientUserId,
                recipientUserId.value().toString(),
                NotificationChannel.PUSH,
                NotificationCategory.MEMBER,
                Map.of("title", title, "body", body),
                recipientUserId));
    }
}
