package in.bachatsetu.backend.notification.application.service;

import in.bachatsetu.backend.notification.application.mapper.NotificationApplicationMapper;
import in.bachatsetu.backend.notification.application.port.TransactionPort;
import in.bachatsetu.backend.notification.application.query.NotificationSummary;
import in.bachatsetu.backend.notification.application.usecase.ListNotificationsUseCase;
import in.bachatsetu.backend.notification.domain.model.Notification;
import in.bachatsetu.backend.notification.domain.port.NotificationPage;
import in.bachatsetu.backend.notification.domain.port.NotificationPageRequest;
import in.bachatsetu.backend.notification.domain.port.NotificationRepository;
import in.bachatsetu.backend.shared.domain.AggregateId;
import java.util.List;
import java.util.Objects;

/** Lists tenant-scoped notifications as compact immutable query models, paginated by the repository. */
public final class ListNotificationsApplicationService implements ListNotificationsUseCase {

    private final NotificationRepository repository;
    private final TransactionPort transaction;
    private final NotificationApplicationMapper mapper;

    public ListNotificationsApplicationService(
            NotificationRepository repository,
            TransactionPort transaction,
            NotificationApplicationMapper mapper) {
        this.repository = Objects.requireNonNull(repository, "repository must not be null");
        this.transaction = Objects.requireNonNull(transaction, "transaction must not be null");
        this.mapper = Objects.requireNonNull(mapper, "mapper must not be null");
    }

    @Override
    public NotificationPage<NotificationSummary> execute(AggregateId tenantId, NotificationPageRequest pageRequest) {
        Objects.requireNonNull(tenantId, "tenant id must not be null");
        Objects.requireNonNull(pageRequest, "page request must not be null");
        return transaction.execute(() -> {
            NotificationPage<Notification> page = repository.findPage(tenantId, pageRequest);
            List<NotificationSummary> summaries = page.content().stream().map(mapper::toSummary).toList();
            return new NotificationPage<>(summaries, page.page(), page.size(), page.totalElements());
        });
    }
}
