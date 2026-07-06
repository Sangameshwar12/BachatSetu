package in.bachatsetu.backend.infrastructure.persistence.entity;

import static org.assertj.core.api.Assertions.assertThat;

import in.bachatsetu.backend.infrastructure.persistence.entity.audit.AuditLogJpaEntity;
import in.bachatsetu.backend.infrastructure.persistence.entity.community.AuctionBidJpaEntity;
import in.bachatsetu.backend.infrastructure.persistence.entity.community.DrawJpaEntity;
import in.bachatsetu.backend.infrastructure.persistence.entity.community.GroupMemberJpaEntity;
import in.bachatsetu.backend.infrastructure.persistence.entity.community.InstallmentJpaEntity;
import in.bachatsetu.backend.infrastructure.persistence.entity.community.MembershipHistoryJpaEntity;
import in.bachatsetu.backend.infrastructure.persistence.entity.community.MonthlyCycleJpaEntity;
import in.bachatsetu.backend.infrastructure.persistence.entity.community.SavingsGroupJpaEntity;
import in.bachatsetu.backend.infrastructure.persistence.entity.finance.PaymentJpaEntity;
import in.bachatsetu.backend.infrastructure.persistence.entity.finance.ReceiptJpaEntity;
import in.bachatsetu.backend.infrastructure.persistence.entity.identity.PermissionJpaEntity;
import in.bachatsetu.backend.infrastructure.persistence.entity.identity.OtpVerificationJpaEntity;
import in.bachatsetu.backend.infrastructure.persistence.entity.identity.RefreshTokenJpaEntity;
import in.bachatsetu.backend.infrastructure.persistence.entity.identity.RoleJpaEntity;
import in.bachatsetu.backend.infrastructure.persistence.entity.identity.UserJpaEntity;
import in.bachatsetu.backend.infrastructure.persistence.entity.notification.NotificationJpaEntity;
import jakarta.persistence.CascadeType;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.OneToMany;
import jakarta.persistence.OneToOne;
import java.lang.reflect.Field;
import java.util.List;
import org.junit.jupiter.api.Test;

class JpaEntityMappingTest {

    private static final List<Class<? extends BaseJpaEntity>> ENTITY_TYPES = List.of(
            UserJpaEntity.class,
            RoleJpaEntity.class,
            PermissionJpaEntity.class,
            RefreshTokenJpaEntity.class,
            OtpVerificationJpaEntity.class,
            SavingsGroupJpaEntity.class,
            GroupMemberJpaEntity.class,
            MembershipHistoryJpaEntity.class,
            MonthlyCycleJpaEntity.class,
            InstallmentJpaEntity.class,
            PaymentJpaEntity.class,
            ReceiptJpaEntity.class,
            DrawJpaEntity.class,
            AuctionBidJpaEntity.class,
            NotificationJpaEntity.class,
            AuditLogJpaEntity.class);

    @Test
    void declaresAllRequestedTypesAsJpaEntities() {
        assertThat(ENTITY_TYPES).hasSize(16).allSatisfy(type -> {
            assertThat(type).hasAnnotation(Entity.class);
            assertThat(BaseJpaEntity.class).isAssignableFrom(type);
        });
    }

    @Test
    void associationsAreLazyAndDoNotCascadeRemoval() {
        for (Class<?> entityType : ENTITY_TYPES) {
            for (Field field : entityType.getDeclaredFields()) {
                ManyToOne manyToOne = field.getAnnotation(ManyToOne.class);
                if (manyToOne != null) {
                    assertThat(manyToOne.fetch()).as(field.toString()).isEqualTo(FetchType.LAZY);
                    assertThat(manyToOne.cascade()).doesNotContain(CascadeType.REMOVE);
                }
                OneToOne oneToOne = field.getAnnotation(OneToOne.class);
                if (oneToOne != null) {
                    assertThat(oneToOne.fetch()).as(field.toString()).isEqualTo(FetchType.LAZY);
                    assertThat(oneToOne.cascade()).doesNotContain(CascadeType.REMOVE);
                    assertThat(oneToOne.orphanRemoval()).isFalse();
                }
                OneToMany oneToMany = field.getAnnotation(OneToMany.class);
                if (oneToMany != null) {
                    assertThat(oneToMany.fetch()).as(field.toString()).isEqualTo(FetchType.LAZY);
                    assertThat(oneToMany.cascade()).doesNotContain(CascadeType.REMOVE);
                    assertThat(oneToMany.orphanRemoval()).isFalse();
                }
            }
        }
    }
}
