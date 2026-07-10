package in.bachatsetu.backend.platformoperations.domain.port;

import in.bachatsetu.backend.platformoperations.domain.model.BroadcastRecipient;
import in.bachatsetu.backend.platformoperations.domain.model.BroadcastScope;
import in.bachatsetu.backend.shared.domain.AggregateId;
import java.util.List;

/** Resolves the target audience for a broadcast notification. */
public interface BroadcastRecipientRepository {

    List<BroadcastRecipient> resolve(BroadcastScope scope, AggregateId tenantId);
}
