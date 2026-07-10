package in.bachatsetu.backend.dashboard.interfaces.rest.config;

import in.bachatsetu.backend.dashboard.application.service.GetMemberDashboardApplicationService;
import in.bachatsetu.backend.dashboard.application.service.GetOrganizerDashboardApplicationService;
import in.bachatsetu.backend.dashboard.application.usecase.GetMemberDashboardUseCase;
import in.bachatsetu.backend.dashboard.application.usecase.GetOrganizerDashboardUseCase;
import in.bachatsetu.backend.draw.domain.port.DrawRepository;
import in.bachatsetu.backend.group.application.port.SavingsGroupRepository;
import in.bachatsetu.backend.invitation.domain.port.GroupInvitationRepository;
import in.bachatsetu.backend.member.domain.port.MemberRepository;
import in.bachatsetu.backend.notification.domain.port.NotificationRepository;
import in.bachatsetu.backend.payment.domain.port.PaymentRepository;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

/**
 * Composes the framework-free dashboard application services purely from other modules' existing
 * domain/application ports — the dashboard module owns no aggregate of its own.
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
public class DashboardApplicationConfig {

    @Bean
    public GetMemberDashboardUseCase getMemberDashboardUseCase(
            MemberRepository memberRepository,
            SavingsGroupRepository groupRepository,
            PaymentRepository paymentRepository,
            DrawRepository drawRepository,
            NotificationRepository notificationRepository) {
        return new GetMemberDashboardApplicationService(
                memberRepository, groupRepository, paymentRepository, drawRepository, notificationRepository);
    }

    @Bean
    public GetOrganizerDashboardUseCase getOrganizerDashboardUseCase(
            SavingsGroupRepository groupRepository,
            GroupInvitationRepository invitationRepository,
            PaymentRepository paymentRepository,
            DrawRepository drawRepository) {
        return new GetOrganizerDashboardApplicationService(
                groupRepository, invitationRepository, paymentRepository, drawRepository);
    }
}
