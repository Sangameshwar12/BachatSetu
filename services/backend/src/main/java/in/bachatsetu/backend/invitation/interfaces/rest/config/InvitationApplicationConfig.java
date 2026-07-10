package in.bachatsetu.backend.invitation.interfaces.rest.config;

import in.bachatsetu.backend.group.application.port.SavingsGroupRepository;
import in.bachatsetu.backend.invitation.application.port.ClockPort;
import in.bachatsetu.backend.invitation.application.port.DomainEventPublisherPort;
import in.bachatsetu.backend.invitation.application.port.InvitationCodeGeneratorPort;
import in.bachatsetu.backend.invitation.application.port.InvitationTokenGeneratorPort;
import in.bachatsetu.backend.invitation.application.port.TransactionPort;
import in.bachatsetu.backend.invitation.application.service.AcceptInvitationApplicationService;
import in.bachatsetu.backend.invitation.application.service.CreateInvitationApplicationService;
import in.bachatsetu.backend.invitation.application.service.GetCurrentInvitationApplicationService;
import in.bachatsetu.backend.invitation.application.service.PreviewInvitationApplicationService;
import in.bachatsetu.backend.invitation.application.service.RevokeInvitationApplicationService;
import in.bachatsetu.backend.invitation.application.usecase.AcceptInvitationUseCase;
import in.bachatsetu.backend.invitation.application.usecase.CreateInvitationUseCase;
import in.bachatsetu.backend.invitation.application.usecase.GetCurrentInvitationUseCase;
import in.bachatsetu.backend.invitation.application.usecase.PreviewInvitationUseCase;
import in.bachatsetu.backend.invitation.application.usecase.RevokeInvitationUseCase;
import in.bachatsetu.backend.invitation.domain.port.GroupInvitationRepository;
import in.bachatsetu.backend.user.domain.port.UserRepository;
import java.time.Duration;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

/**
 * Composes the framework-free invitation application services.
 *
 * <p>Gated on {@code bachatsetu.persistence.repositories.enabled} for the same non-deterministic
 * component-scan ordering reason documented on {@code NotificationApplicationConfig}.
 */
@Configuration(proxyBeanMethods = false)
@ConditionalOnProperty(
        prefix = "bachatsetu.persistence.repositories",
        name = "enabled",
        havingValue = "true",
        matchIfMissing = true)
public class InvitationApplicationConfig {

    @Bean
    public CreateInvitationUseCase createInvitationUseCase(
            GroupInvitationRepository invitationRepository,
            SavingsGroupRepository groupRepository,
            InvitationCodeGeneratorPort codeGenerator,
            InvitationTokenGeneratorPort tokenGenerator,
            DomainEventPublisherPort eventPublisher,
            ClockPort clock,
            TransactionPort transaction,
            @Value("${bachatsetu.invitation.validity:P7D}") Duration validity) {
        return new CreateInvitationApplicationService(
                invitationRepository, groupRepository, codeGenerator, tokenGenerator, eventPublisher, clock,
                transaction, validity);
    }

    @Bean
    public RevokeInvitationUseCase revokeInvitationUseCase(
            GroupInvitationRepository invitationRepository,
            SavingsGroupRepository groupRepository,
            DomainEventPublisherPort eventPublisher,
            ClockPort clock,
            TransactionPort transaction) {
        return new RevokeInvitationApplicationService(invitationRepository, groupRepository, eventPublisher, clock, transaction);
    }

    @Bean
    public GetCurrentInvitationUseCase getCurrentInvitationUseCase(
            GroupInvitationRepository invitationRepository,
            SavingsGroupRepository groupRepository,
            TransactionPort transaction) {
        return new GetCurrentInvitationApplicationService(invitationRepository, groupRepository, transaction);
    }

    @Bean
    public PreviewInvitationUseCase previewInvitationUseCase(
            GroupInvitationRepository invitationRepository,
            SavingsGroupRepository groupRepository,
            UserRepository userProfileRepository,
            TransactionPort transaction) {
        return new PreviewInvitationApplicationService(
                invitationRepository, groupRepository, userProfileRepository, transaction);
    }

    @Bean
    public AcceptInvitationUseCase acceptInvitationUseCase(
            GroupInvitationRepository invitationRepository,
            SavingsGroupRepository groupRepository,
            DomainEventPublisherPort eventPublisher,
            in.bachatsetu.backend.group.application.port.DomainEventPublisherPort groupEventPublisher,
            ClockPort clock,
            TransactionPort transaction) {
        return new AcceptInvitationApplicationService(
                invitationRepository, groupRepository, eventPublisher, groupEventPublisher, clock, transaction);
    }
}
