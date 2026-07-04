package in.bachatsetu.backend.infrastructure.persistence.adapter;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import in.bachatsetu.backend.draw.domain.port.DrawRepository;
import in.bachatsetu.backend.group.domain.port.GroupRepository;
import in.bachatsetu.backend.infrastructure.persistence.exception.PersistenceConflictException;
import in.bachatsetu.backend.member.domain.port.MemberRepository;
import in.bachatsetu.backend.notification.domain.port.NotificationRepository;
import in.bachatsetu.backend.payment.domain.port.PaymentRepository;
import in.bachatsetu.backend.receipt.domain.port.ReceiptRepository;
import in.bachatsetu.backend.user.domain.port.UserRepository;
import java.lang.reflect.Method;
import java.util.List;
import org.junit.jupiter.api.Test;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.transaction.annotation.Transactional;

class RepositoryAdapterTest {

    @Test
    void adaptersImplementTheirDomainPorts() {
        assertThat(UserRepository.class).isAssignableFrom(UserRepositoryAdapter.class);
        assertThat(GroupRepository.class).isAssignableFrom(GroupRepositoryAdapter.class);
        assertThat(MemberRepository.class).isAssignableFrom(MemberRepositoryAdapter.class);
        assertThat(PaymentRepository.class).isAssignableFrom(PaymentRepositoryAdapter.class);
        assertThat(ReceiptRepository.class).isAssignableFrom(ReceiptRepositoryAdapter.class);
        assertThat(DrawRepository.class).isAssignableFrom(DrawRepositoryAdapter.class);
        assertThat(NotificationRepository.class).isAssignableFrom(NotificationRepositoryAdapter.class);
    }

    @Test
    void adaptersDeclareReadOnlyDefaultsAndWriteSaveBoundaries() throws NoSuchMethodException {
        List<Class<?>> adapters = List.of(
                UserRepositoryAdapter.class,
                GroupRepositoryAdapter.class,
                MemberRepositoryAdapter.class,
                PaymentRepositoryAdapter.class,
                ReceiptRepositoryAdapter.class,
                DrawRepositoryAdapter.class,
                NotificationRepositoryAdapter.class);

        for (Class<?> adapter : adapters) {
            Transactional classTransaction = adapter.getAnnotation(Transactional.class);
            assertThat(classTransaction).as(adapter.getName()).isNotNull();
            assertThat(classTransaction.readOnly()).as(adapter.getName()).isTrue();

            Method save = adapter.getMethod("save", saveParameter(adapter));
            Transactional writeTransaction = save.getAnnotation(Transactional.class);
            assertThat(writeTransaction).as(save.toString()).isNotNull();
            assertThat(writeTransaction.readOnly()).as(save.toString()).isFalse();
        }
    }

    @Test
    void translatesConstraintFailuresToPersistenceConflicts() {
        assertThatThrownBy(() -> RepositoryOperations.execute(() -> {
                    throw new DataIntegrityViolationException("duplicate");
                }))
                .isInstanceOf(PersistenceConflictException.class)
                .hasMessageContaining("constraint");
    }

    private Class<?> saveParameter(Class<?> adapter) {
        return switch (adapter.getSimpleName()) {
            case "UserRepositoryAdapter" -> in.bachatsetu.backend.user.domain.model.UserProfile.class;
            case "GroupRepositoryAdapter" -> in.bachatsetu.backend.group.domain.model.SavingsGroup.class;
            case "MemberRepositoryAdapter" -> in.bachatsetu.backend.member.domain.model.MemberProfile.class;
            case "PaymentRepositoryAdapter" -> in.bachatsetu.backend.payment.domain.model.Payment.class;
            case "ReceiptRepositoryAdapter" -> in.bachatsetu.backend.receipt.domain.model.Receipt.class;
            case "DrawRepositoryAdapter" -> in.bachatsetu.backend.draw.domain.model.Draw.class;
            case "NotificationRepositoryAdapter" -> in.bachatsetu.backend.notification.domain.model.Notification.class;
            default -> throw new IllegalArgumentException("unknown adapter: " + adapter.getName());
        };
    }
}
