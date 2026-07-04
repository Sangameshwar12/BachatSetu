package in.bachatsetu.backend.infrastructure.persistence.entity;

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
import in.bachatsetu.backend.infrastructure.persistence.entity.identity.RoleJpaEntity;
import in.bachatsetu.backend.infrastructure.persistence.entity.identity.UserJpaEntity;
import in.bachatsetu.backend.infrastructure.persistence.entity.notification.NotificationJpaEntity;
import org.hibernate.boot.MetadataSources;
import org.hibernate.boot.registry.StandardServiceRegistry;
import org.hibernate.boot.registry.StandardServiceRegistryBuilder;
import org.hibernate.cfg.AvailableSettings;
import org.hibernate.dialect.PostgreSQLDialect;
import org.junit.jupiter.api.Test;

class HibernateMetadataTest {

    @Test
    void hibernateBuildsTheCompleteEntityMetamodel() {
        StandardServiceRegistry registry = new StandardServiceRegistryBuilder()
                .applySetting(AvailableSettings.DIALECT, PostgreSQLDialect.class.getName())
                .applySetting("hibernate.boot.allow_jdbc_metadata_access", false)
                .build();
        try {
            MetadataSources sources = new MetadataSources(registry);
            sources.addAnnotatedClass(UserJpaEntity.class);
            sources.addAnnotatedClass(RoleJpaEntity.class);
            sources.addAnnotatedClass(PermissionJpaEntity.class);
            sources.addAnnotatedClass(GroupJpaEntity.class);
            sources.addAnnotatedClass(MemberJpaEntity.class);
            sources.addAnnotatedClass(MonthlyCycleJpaEntity.class);
            sources.addAnnotatedClass(InstallmentJpaEntity.class);
            sources.addAnnotatedClass(PaymentJpaEntity.class);
            sources.addAnnotatedClass(ReceiptJpaEntity.class);
            sources.addAnnotatedClass(DrawJpaEntity.class);
            sources.addAnnotatedClass(AuctionBidJpaEntity.class);
            sources.addAnnotatedClass(NotificationJpaEntity.class);
            sources.addAnnotatedClass(AuditLogJpaEntity.class);

            try (var sessionFactory = sources.buildMetadata().buildSessionFactory()) {
                // Building the factory validates entity names, associations, and mappedBy references.
            }
        } finally {
            StandardServiceRegistryBuilder.destroy(registry);
        }
    }
}
