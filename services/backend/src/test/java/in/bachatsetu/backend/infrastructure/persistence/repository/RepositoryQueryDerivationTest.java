package in.bachatsetu.backend.infrastructure.persistence.repository;

import static org.assertj.core.api.Assertions.assertThatCode;

import in.bachatsetu.backend.infrastructure.persistence.entity.audit.AuditLogJpaEntity;
import in.bachatsetu.backend.infrastructure.persistence.entity.community.AuctionBidJpaEntity;
import in.bachatsetu.backend.infrastructure.persistence.entity.community.DrawJpaEntity;
import in.bachatsetu.backend.infrastructure.persistence.entity.community.GroupJpaEntity;
import in.bachatsetu.backend.infrastructure.persistence.entity.community.InstallmentJpaEntity;
import in.bachatsetu.backend.infrastructure.persistence.entity.community.MemberJpaEntity;
import in.bachatsetu.backend.infrastructure.persistence.entity.community.MonthlyCycleJpaEntity;
import in.bachatsetu.backend.infrastructure.persistence.entity.finance.PaymentJpaEntity;
import in.bachatsetu.backend.infrastructure.persistence.entity.finance.ReceiptJpaEntity;
import in.bachatsetu.backend.infrastructure.persistence.entity.identity.PermissionJpaEntity;
import in.bachatsetu.backend.infrastructure.persistence.entity.identity.OtpVerificationJpaEntity;
import in.bachatsetu.backend.infrastructure.persistence.entity.identity.RefreshTokenJpaEntity;
import in.bachatsetu.backend.infrastructure.persistence.entity.identity.RoleJpaEntity;
import in.bachatsetu.backend.infrastructure.persistence.entity.identity.UserJpaEntity;
import in.bachatsetu.backend.infrastructure.persistence.entity.notification.NotificationJpaEntity;
import in.bachatsetu.backend.infrastructure.persistence.repository.jpa.AuctionBidSpringDataRepository;
import in.bachatsetu.backend.infrastructure.persistence.repository.jpa.AuditLogSpringDataRepository;
import in.bachatsetu.backend.infrastructure.persistence.repository.jpa.DrawSpringDataRepository;
import in.bachatsetu.backend.infrastructure.persistence.repository.jpa.GroupSpringDataRepository;
import in.bachatsetu.backend.infrastructure.persistence.repository.jpa.InstallmentSpringDataRepository;
import in.bachatsetu.backend.infrastructure.persistence.repository.jpa.MemberSpringDataRepository;
import in.bachatsetu.backend.infrastructure.persistence.repository.jpa.MonthlyCycleSpringDataRepository;
import in.bachatsetu.backend.infrastructure.persistence.repository.jpa.NotificationSpringDataRepository;
import in.bachatsetu.backend.infrastructure.persistence.repository.jpa.PaymentSpringDataRepository;
import in.bachatsetu.backend.infrastructure.persistence.repository.jpa.PermissionSpringDataRepository;
import in.bachatsetu.backend.infrastructure.persistence.repository.jpa.OtpVerificationSpringDataRepository;
import in.bachatsetu.backend.infrastructure.persistence.repository.jpa.RefreshTokenSpringDataRepository;
import in.bachatsetu.backend.infrastructure.persistence.repository.jpa.ReceiptSpringDataRepository;
import in.bachatsetu.backend.infrastructure.persistence.repository.jpa.RoleSpringDataRepository;
import in.bachatsetu.backend.infrastructure.persistence.repository.jpa.UserSpringDataRepository;
import java.lang.reflect.Method;
import java.util.List;
import org.junit.jupiter.api.Test;
import org.springframework.data.repository.query.parser.PartTree;

class RepositoryQueryDerivationTest {

    @Test
    void allCustomRepositoryMethodsResolveAgainstTheirEntityModels() {
        List<RepositoryModel> repositories = List.of(
                model(UserSpringDataRepository.class, UserJpaEntity.class),
                model(RoleSpringDataRepository.class, RoleJpaEntity.class),
                model(PermissionSpringDataRepository.class, PermissionJpaEntity.class),
                model(RefreshTokenSpringDataRepository.class, RefreshTokenJpaEntity.class),
                model(OtpVerificationSpringDataRepository.class, OtpVerificationJpaEntity.class),
                model(GroupSpringDataRepository.class, GroupJpaEntity.class),
                model(MemberSpringDataRepository.class, MemberJpaEntity.class),
                model(MonthlyCycleSpringDataRepository.class, MonthlyCycleJpaEntity.class),
                model(InstallmentSpringDataRepository.class, InstallmentJpaEntity.class),
                model(PaymentSpringDataRepository.class, PaymentJpaEntity.class),
                model(ReceiptSpringDataRepository.class, ReceiptJpaEntity.class),
                model(DrawSpringDataRepository.class, DrawJpaEntity.class),
                model(AuctionBidSpringDataRepository.class, AuctionBidJpaEntity.class),
                model(NotificationSpringDataRepository.class, NotificationJpaEntity.class),
                model(AuditLogSpringDataRepository.class, AuditLogJpaEntity.class));

        for (RepositoryModel repository : repositories) {
            for (Method method : repository.repositoryType().getDeclaredMethods()) {
                assertThatCode(() -> new PartTree(method.getName(), repository.entityType()))
                        .as("%s.%s", repository.repositoryType().getSimpleName(), method.getName())
                        .doesNotThrowAnyException();
            }
        }
    }

    private RepositoryModel model(Class<?> repositoryType, Class<?> entityType) {
        return new RepositoryModel(repositoryType, entityType);
    }

    private record RepositoryModel(Class<?> repositoryType, Class<?> entityType) {
    }
}
